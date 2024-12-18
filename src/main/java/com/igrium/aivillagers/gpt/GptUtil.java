package com.igrium.aivillagers.gpt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.igrium.aivillagers.subsystems.AISubsystem;

import io.github.sashirestela.openai.common.tool.ToolCall;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.Chat.Choice;
import io.github.sashirestela.openai.domain.chat.ChatMessage.ResponseMessage;
import net.minecraft.entity.Entity;

public class GptUtil {

    public static record AIContext(Entity villager, AISubsystem aiSubsystem) {};

    /**
     * This is <i>extremely</i> dumb, but the OpenAI library doesn't provide an easy
     * way to pass context into function.
     */
    public static final ThreadLocal<AIContext> AI_CONTEXT = new ThreadLocal<>();

    /**
     * Read an entire chat stream and construct a single response choice from it.
     * @param chatStream Chat stream to read.
     * @param chunkConsumer Called every time a chat segment is recieved.
     * @return The chat choice.
     */
    public static Choice getResponse(Stream<Chat> chatStream, Consumer<Chat> chunkConsumer) {
        Choice choice = new Choice();
        choice.setIndex(0);
        ResponseMessage chatMsgResponse = new ResponseMessage();
        List<ToolCall> toolCalls = new ArrayList<>();
        
        StringBuilder content = new StringBuilder();

        chatStream.forEach(responseChunk -> {
            if (chunkConsumer != null)
                chunkConsumer.accept(responseChunk);

            List<Choice> choices = responseChunk.getChoices();
            if (choices.isEmpty()) return;

            Choice firstChoice = choices.get(0);
            ResponseMessage msgSegmennt = firstChoice.getMessage();
            if (msgSegmennt.getRole() != null) {
                // Update the chat role with the last role we recieved
                chatMsgResponse.setRole(msgSegmennt.getRole());
            }

            if (msgSegmennt.getContent() != null) {
                content.append(msgSegmennt.getContent());
            }
            
            // TODO: get tool calls working
            // int currentToolIndex = -1;
            // StringBuilder functionArgs = new StringBuilder();
            // if (msgSegmennt.getToolCalls() != null) {
            //     var toolCall = msgSegmennt.getToolCalls().get(0);
            //     if (toolCall.getIndex() != currentToolIndex) {
            //         if (toolCalls.size() > 0) {
            //             toolCalls.get(toolCalls.size() - 1).getFunction().setArguments(functionArgs.toString());
            //         }
            //     }
            // }
            
            if (firstChoice.getFinishReason() != null) {
                // We're finished, so set message content.
                if (content.length() > 0) {
                    chatMsgResponse.setContent(content.toString());
                }
                choice.setMessage(chatMsgResponse);
                choice.setFinishReason(firstChoice.getFinishReason());
            }
        });

        return choice;
    }
}
