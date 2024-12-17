package com.igrium.aivillagers.subsystems.impl;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import com.igrium.aivillagers.gpt.GptUtil;
import com.igrium.aivillagers.subsystems.AISubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.subsystems.SpeechSubsystem.SpeechStream;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.chat.Chat.Choice;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

public class GptAISubsystem implements AISubsystem {

    public static final SubsystemType<GptAISubsystem> TYPE = SubsystemType.create(GptAISubsystem::new, GptAIConfig.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(GptAISubsystem.class);

    public static final class GptAIConfig {
        public String baseUrl = "https://api.openai.com";
        public String apiKey;
        public String model = "gpt-4o-mini";
        public int maxCompletionTokens = 200;
    }

    private final SimpleOpenAI openAI;
    private final AIManager aiManager;
    private final GptAIConfig config;

    public GptAISubsystem(AIManager aiManager, GptAIConfig config) {
        this.aiManager = aiManager;
        this.openAI = SimpleOpenAI.builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .build();
        this.config = config;
    }

    public SimpleOpenAI getOpenAI() {
        return openAI;
    }
    public AIManager getAiManager() {
        return aiManager;
    }

    private final Lock chatLock = new ReentrantLock();

    @Override
    public void onSpokenTo(Entity villager, ServerPlayerEntity player, String message) {
        Util.getMainWorkerExecutor().execute(() -> onSpokenToInternal(villager, player, message));
    }

    protected void onSpokenToInternal(Entity villager, ServerPlayerEntity player, String message) {
        List<ChatMessage> messageHistory = ChatHistoryComponent.get(villager).getMessageHistory();
        if (messageHistory.isEmpty()) {
            messageHistory.add(SystemMessage.of("You are a Minecraft villager."));
        }
        messageHistory.add(UserMessage.of(message));
        
        ChatRequest request = ChatRequest.builder()
                .model(config.model)
                .messages(messageHistory)
                // .stream(true)
                .maxCompletionTokens(config.maxCompletionTokens)
                .build();

        // openAI.chatCompletions().createStream(request)
        //         .thenAccept(res -> handleStreamResponse(villager, player, res))
        //         .exceptionally(e -> {
                    // LOGGER.error("Error accessing OpenAI", e);
                    // aiManager.getSpeechSubsystem().speak(villager, "Error accessing OpenAI!");
                    // return null;
        //         }).whenComplete((r, e) ->  chatLock.unlock());

        openAI.chatCompletions().create(request)
                .thenAccept(chat -> handleResponse(villager, player, chat))
                .exceptionally(e -> {
                    LOGGER.error("Error accessing OpenAI", e);
                    aiManager.getSpeechSubsystem().speak(villager, "Error accessing OpenAI!");
                    return null;
                });
    }

    protected void handleResponse(Entity villager, ServerPlayerEntity player, Chat chat) {
        var choices = chat.getChoices();
        if (choices == null || choices.isEmpty())
            return;
        
        String message = choices.get(0).getMessage().getContent();
        if (message != null && !message.isBlank())
            aiManager.getSpeechSubsystem().speak(villager, message);
        ChatHistoryComponent.get(villager).getMessageHistory().add(choices.get(0).getMessage());
    }

    protected void handleStreamResponse(Entity villager, ServerPlayerEntity player, Stream<Chat> stream) {
        // StringBuffer builder = new StringBuffer();
        // try (SpeechStream speech = aiManager.getSpeechSubsystem().openSpeechStream(villager)) {
        //     stream.filter(resp -> resp.getChoices().size() > 0 && resp.firstContent() != null)
        //             .map(resp -> resp.firstContent())
        //             .forEach(str -> {
        //                 builder.append(str);
        //                 speech.acceptToken(str);
        //             });
        // }
        
        Choice response;
        try (SpeechStream speech = aiManager.getSpeechSubsystem().openSpeechStream(villager)) {
            response = GptUtil.getResponse(stream, chunk -> {
                var choices = chunk.getChoices();
                if (choices != null && !choices.isEmpty()) {
                    speech.acceptToken(choices.get(0).getMessage().getContent());
                }
                // String firstContent = chunk.firstContent();
                // if (firstContent != null) {
                //     speech.acceptToken(chunk.firstContent());
                // }
            });
        }
        
        ChatHistoryComponent.get(villager).getMessageHistory().add(response.getMessage());
    }

    @Override
    public void onDamage(Entity villager, DamageSource source, float amount) {
        // onSpokenTo(villager, null, "I have punched you for " + amount + " damage");
    }
    
}
