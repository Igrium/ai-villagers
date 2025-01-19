package com.igrium.aivillagers.speech

import com.igrium.aivillagers.com.igrium.elevenlabs.ElevenLabsWSConnection
import com.igrium.elevenlabs.ElevenLabsClient
import com.igrium.elevenlabs.requests.OutputFormat
import com.igrium.elevenlabs.requests.TTSRequest
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import net.minecraft.util.Util
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.util.concurrent.CompletableFuture

class ElevenLabsSpeechClient @JvmOverloads constructor(
    val apiKey: String,
    val voiceId: String,
    val baseUrl: String = "https://api.elevenlabs.io",
) {
    private val client: ElevenLabsClient = ElevenLabsClient(
        apiKey = this.apiKey,
        baseUrl = URI.create(baseUrl)
//        httpClient = HttpClient.newBuilder()
//            .executor(Util.getIoWorkerExecutor())
//            .build()
    )

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("OpenAI Speech Client"))

    @JvmOverloads
    fun streamTTS(message: String, format: OutputFormat = OutputFormat.PCM_22050): CompletableFuture<InputStream> {
        return scope.future { client.streamTTS(TTSRequest(text = message), voiceId, format) }
    }

    @JvmOverloads
    fun openWSConnection(format: OutputFormat = OutputFormat.PCM_22050): CompletableFuture<ElevenLabsWSConnection> {
        return scope.future { client.openWSConnection(voiceId, format) }
    }
//    private suspend fun streamTTS(message: String): InputStream {
//        return client.streamTTS(TTSRequest(text = message), voiceId = this.voiceId);
//    }
}