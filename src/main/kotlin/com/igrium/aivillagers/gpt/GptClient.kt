package com.igrium.aivillagers.gpt

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.igrium.aivillagers.AIManager
import com.igrium.aivillagers.subsystems.AISubsystem
import com.igrium.aivillagers.subsystems.impl.GptAISubsystem
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.entity.Entity
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
    private val model: String = "gpt-4o-mini"
) {

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
        aiInterface.getHistory(villager).add(msg);
        return scope.future { doChatCompletion(villager, target) }
    }

    /**
     * Check the villager's message history and perform a chat completion request.
     * @param villager The subject villager.
     * @param target The entity we're currently speaking with.
     */
    private suspend fun doChatCompletion(villager: Entity, target: Entity?): String? {
        val request = ChatCompletionRequest(
            model = ModelId(this.model),
            messages = aiInterface.getHistory(villager)
        )

        val completion = openAI.chatCompletion(request);

        val choices = completion.choices
        if (choices.isEmpty()) return null;

        val message = choices[0].message;
        val msgContent = message.content;

        // This is done here because message may have been received
        // in response to a tool call rather than a direct request from the AI subsystem
        if (!msgContent.isNullOrBlank()) {
            aiSubsystem.doSpeak(villager, null, msgContent)
        }
        aiInterface.getHistory(villager).add(message);
        return msgContent;
//        val toolCalls = message.toolCalls;
//        if (toolCalls != null) {
//            var hadToolCall: Boolean = false;
//            for (call in toolCalls) {
//
//            }
//        }
    }


    private fun callCurrentWeather(args: JsonObject): String {
        val location = args.getValue("location").jsonPrimitive.content;
        return when (location) {
            "San Francisco" -> """"{"location": "San Francisco", "temperature": "72", "unit": "fahrenheit"}"""
            "Tokyo" -> """{"location": "Tokyo", "temperature": "10", "unit": "celsius"}"""
            "Paris" -> """{"location": "Paris", "temperature": "22", "unit": "celsius"}"""
            else -> """{"location": "$location", "temperature": "unknown", "unit": "unknown"}"""
        }
    }

    fun stop() {
        scope.cancel("AI subsystem shutting down...");

    }
}