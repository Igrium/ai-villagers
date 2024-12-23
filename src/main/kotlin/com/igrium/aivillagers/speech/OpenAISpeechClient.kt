package com.igrium.aivillagers.speech

import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.SpeechResponseFormat
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CompletableFuture

class OpenAISpeechClient (
    private val apiKey: String
) {

    private val openAI = OpenAI(
        token = apiKey

    )

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("OpenAI Speech Client"))

    @JvmOverloads
    fun streamTextAsync(message: String, voice: String = "echo"): CompletableFuture<InputStream> {
        return scope.future { streamText(message, Voice(voice)).inputStream() }
    }

    // TODO: How do we make it actually stream the text?
    private suspend fun streamText(message: String, voice: Voice = Voice.Echo): ByteArray {
        val req = SpeechRequest(
            model = ModelId("tts-1"),
            voice = voice,
            input = message,
            responseFormat = SpeechResponseFormat("wav")
        )
        return openAI.speech(req);
    }
}