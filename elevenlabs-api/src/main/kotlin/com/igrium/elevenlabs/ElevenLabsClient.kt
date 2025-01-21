package com.igrium.elevenlabs

import com.igrium.aivillagers.com.igrium.elevenlabs.ElevenLabsWSConnection
import com.igrium.elevenlabs.requests.OutputFormat
import com.igrium.elevenlabs.requests.TTSRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.URI


/**
 * The primary client used to communicate with ElevenLabs
 */
public class ElevenLabsClient @JvmOverloads constructor(
    private val apiKey: String,
    private val baseUrl: URI = URI.create("https://api.elevenlabs.io"),
    private val httpClient: HttpClient = HttpClient(Java) {
        install(WebSockets)
    },
    var printDebug: Boolean = false
) {

    suspend fun streamTTS(
        params: TTSRequest,
        voiceId: String,
        outputFormat: OutputFormat = OutputFormat.MP3_44100_128
    ): InputStream {

        val url = baseUrl.resolve("/v1/text-to-speech/$voiceId/stream?output_format=${outputFormat.getSerialName()}")
//
        val startTime = System.currentTimeMillis();
        val res = httpClient.post(url.toString()) {
            contentType(ContentType.Application.Json)
            header("xi-api-key", apiKey);
            setBody(Json.encodeToString(params))
        }

        if (res.status.value != 200) {
            throw ElevenLabsException(url.toString(), res.status.value, res.body())
        }

        println("Received response from ElevenLabs in ${System.currentTimeMillis() - startTime}ms");
        return res.bodyAsChannel().toInputStream()

//        val req = HttpRequest.newBuilder(url)
//            .header("xi-api-key", apiKey)
//            .header("Content-Type", "application/json")
//            .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(params)))
//            .build()
//
//        if (printDebug) {
//            println("ElevenLabs Request URL: " + req.uri())
//            println("ElevenLabs Request Headers: " + req.headers())
//            println("ElevenLabs Request Content: " + Json.encodeToString(params))
//        }
//        val res = httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream()).await()
//        if (res.statusCode() != 200) {
//            val msg = res.body().bufferedReader().readText()
//            throw ElevenLabsException(url.toString(), res.statusCode(), msg);
//        }
//        return res.body()
    }

    suspend fun openWSConnection(
        voiceId: String,
        outputFormat: OutputFormat = OutputFormat.MP3_44100_128,
//        modelId: String? = null,
//        languageCode: String? = null,
    ): ElevenLabsWSConnection {
        val url = "wss://api.elevenlabs.io/v1/text-to-speech/$voiceId/stream-input?output_format=${outputFormat.getSerialName()}"

        val ws = httpClient.webSocketSession(url) {
            header("xi-api-key", apiKey)
        }
        val connection = ElevenLabsWSConnection(ws);
        ws.launch { connection.run() }

//        httpClient.newWebSocketBuilder()
//            .header("xi-api-key", apiKey)
//            .buildAsync(url, listener).await()
        return connection
    }
}