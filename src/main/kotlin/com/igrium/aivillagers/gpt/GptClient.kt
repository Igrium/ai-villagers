package com.igrium.aivillagers.gpt

import com.aallam.openai.api.chat.*
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.igrium.aivillagers.chat.LiteralMessage
import com.igrium.aivillagers.chat.Message
import com.igrium.aivillagers.subsystems.impl.GptAISubsystem
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import net.minecraft.entity.Entity
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The internal code to communicate with OpenAI. Written in kotlin for ease of readability.
 * @param apiKey The OpenAI api token.
 * @param timeout The amount of time to wait for a response.
 */
class GptClient @JvmOverloads constructor(
    private val aiInterface: VillagerAIInterface,
    private val aiSubsystem: GptAISubsystem,
    private val apiKey: String,
    private val timeout: Int = 60000,
    private val modelName: String = "gpt-4o-mini"
) {
    private val availableFunctions = Collections.synchronizedMap(mutableMapOf<String, ToolCallFunction>(
        "offerTrade" to offerTradeFunction
    ));

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("OpenAI Interface"))

    private val openAI = OpenAI(
        token = apiKey,
        timeout = Timeout(socket = 60.toDuration(DurationUnit.MILLISECONDS))
    )

    /**
     * Send a message to the language model.
     * @param villager The villager we're sending the message on behalf of.
     * @param target The player we're currently talking to, if any.
     * @param message Message to send.
     * @return A future that completes with the first response returned from the language model.
     * **Note**:
     * it's possible the model will send more messages as the result of tool calls. While these will be handled,
     * they will not return here.
     */
    fun sendMessage(villager: Entity, target: Entity?, message: String): CompletableFuture<String?> {
        val msg = ChatMessage(
            role = ChatRole.User,
            content = message
        )
//        val history = aiInterface.getChatHistory(villager);
//        if (history.isEmpty()) {
//            history.add(ChatMessage(
//                role = ChatRole.System,
//                content = "You are a Minecraft villager like the ones from Villager News. Villagers speak casually, are easily offended, stubborn, and charge exorbitant prices. Short phrases"
//            ))
//        }
//        aiInterface.getChatHistory(villager).add(msg);
        aiInterface.getMessageHistory(villager).add(LiteralMessage(msg));
        return scope.future { doChatCompletion(villager, target) }
    }

    /**
     * Check the villager's message history and perform a chat completion request.
     * Could be called from the AI subsystem or from the return of a tool call.
     * @param villager The subject villager.
     * @param target The entity we're currently speaking with.
     * @return The message content the LLM returned, if any.
     */
    private suspend fun doChatCompletion(villager: Entity, target: Entity?): String? {

        val request = chatCompletionRequest {
            model = ModelId(modelName)
            messages = aiInterface.getChatHistory(villager)
            tools {
                for ((name, func) in availableFunctions) {
                    function(name, func)
                }
                toolChoice = ToolChoice.Auto
            }
        }

        val response = openAI.chatCompletion(request);

        val choices = response.choices
        if (choices.isEmpty()) return null;

        val message = choices[0].message;
        val history = aiInterface.getMessageHistory(villager)
        history.add(LiteralMessage(message));
//        history.add(message);

        val msgContent = message.content;

        // This is done here because message may have been received
        // in response to a tool call rather than a direct request from the AI subsystem
        if (!msgContent.isNullOrBlank()) {
            aiSubsystem.doSpeak(villager, null, msgContent)
        }

        // HANDLE TOOL CALLS
        for (toolCall in message.toolCalls.orEmpty()) {
            require(toolCall is ToolCall.Function) { "Tool call is not a function!" }
            val name = toolCall.function.name
            val function = availableFunctions[name]
            require(function != null) { "Unknown function: $name" }

            scope.launch {
                val result = function.impl(aiInterface, villager, target, toolCall.function.argumentsAsJson())
                history.add(
                    LiteralMessage(
                        ChatMessage(
                            role = ChatRole.Tool,
                            content = result,
                            toolCallId = toolCall.id
                        )
                    )
                )
                doChatCompletion(villager, target);
            }

        }

        return msgContent;

    }


    fun stop() {
        scope.cancel("AI subsystem shutting down...");

    }
}