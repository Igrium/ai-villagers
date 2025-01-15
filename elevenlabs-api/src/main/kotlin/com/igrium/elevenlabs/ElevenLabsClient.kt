package com.igrium.elevenlabs

import com.igrium.elevenlabs.requests.OutputFormat
import com.igrium.elevenlabs.requests.TTSRequest
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers


/**
 * The primary client used to communicate with ElevenLabs
 */
public class ElevenLabsClient @JvmOverloads constructor(
    private val apiKey: String,
    private val baseUrl: URI = URI.create("https://api.elevenlabs.io"),
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) {

    public suspend fun streamTTS(
        params: TTSRequest,
        voiceId: String,
        outputFormat: OutputFormat = OutputFormat.MP3_44100_128
    ): InputStream {

        val url = baseUrl.resolve("/v1/text-to-speech/${voiceId}/stream?output_format=${outputFormat.getSerialName()}")

        val req = HttpRequest.newBuilder(url)
            .header("xi-api-key", apiKey)
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(Json.encodeToString(params)))
            .build()

        val res = httpClient.sendAsync(req, BodyHandlers.ofInputStream()).await()
        if (res.statusCode() != 200) {
            throw ElevenLabsException(url.toString(), res.statusCode());
        }
        return res.body()
    }
}