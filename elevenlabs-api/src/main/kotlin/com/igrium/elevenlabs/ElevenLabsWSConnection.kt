package com.igrium.aivillagers.com.igrium.elevenlabs

import com.igrium.elevenlabs.ElevenLabsWSException
import com.igrium.elevenlabs.requests.VoiceSettings
import com.igrium.elevenlabs.util.PacketInputStream
import io.ktor.websocket.*
import kotlinx.coroutines.future.future
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.http.WebSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class ElevenLabsWSConnection(val ws: WebSocketSession) {

    private val packetInputStream = PacketInputStream();

    /**
     * An input stream containing the audio data as it arrives.
     */
    val inputStream get() = packetInputStream;
    private var closed = false;

    suspend fun run() {
        while (!closed) {
            val frame = ws.incoming.receive()
            if (frame is Frame.Text) {
                val res = Json.decodeFromString(ELStreamResponse.serializer(), frame.readText())
                handleResponse(res)
            }
        }
    }

    private fun handleResponse(res: ELStreamResponse) {
        if (res.error != null) {
            throw ElevenLabsWSException(
                res.error,
                res.code ?: 0,
                res.message ?: res.error
            )
        }

        if (res.audio != null) {
            packetInputStream.addPacket(Base64.getDecoder().decode(res.audio))
        }

        if (res.isFinal == true) {
            packetInputStream.setEOF()
            closed = true;
        }
    }

    fun sendTTSText(text: String): CompletableFuture<*> {
        val str = Json.encodeToString(ELStreamText(text));
        println(str)
        return ws.future { ws.send(str) }
    }

    fun close(): CompletableFuture<*> {
        return ws.future {
            ws.send("{\"text\": \"\"}")
            closed = true
        }
    }
}

class ElevenLabsWSConnectionOld : WebSocket.Listener {
    private var wsOrNull: WebSocket? = null
    val ws get() = requireNotNull(wsOrNull) { "No websocket connection found." }

    private val packetInputStream = PacketInputStream();

    /**
     * An input stream containing the audio data as it arrives.
     */
    val inputStream get() = packetInputStream;

    override fun onOpen(webSocket: WebSocket?) {
        super.onOpen(webSocket)
        this.wsOrNull = webSocket;
    }

    override fun onText(webSocket: WebSocket?, data: CharSequence?, last: Boolean): CompletionStage<*>? {
        val testPath = Paths.get("./ws.json").toAbsolutePath();
        Files.newBufferedWriter(testPath).use {
            it.write(data.toString())
            println("Wrote WS data to " + testPath);
        }
        val res = Json.decodeFromString(ELStreamResponse.serializer(), data.toString())
        if (res.error != null) {
            throw ElevenLabsWSException(
                res.error,
                res.code ?: 0,
                res.message ?: res.error
            )
        }

        if (res.audio != null) {
            packetInputStream.addPacket(Base64.getDecoder().decode(res.audio))
        }

        if (res.isFinal == true) {
            packetInputStream.setEOF()
            close();
        }
        return null;
    }

    fun sendTTSText(text: String): CompletableFuture<*> {
        val str = Json.encodeToString(ELStreamText(text));
        println(str)
        return ws.sendText(str, true)
    }

    override fun onError(webSocket: WebSocket?, error: Throwable?) {
        error?.printStackTrace();
    }

    override fun onClose(webSocket: WebSocket?, statusCode: Int, reason: String?): CompletionStage<*>? {
        packetInputStream.setEOF()
        return null;
    }

    fun close(): CompletableFuture<*> {
        return ws.sendText("{\"text\": \"\"}", true)
    }
}

@Serializable
data class ELStreamText(
    val text: String,
    @SerialName("voice_settings")
    val voiceSettings: VoiceSettings? = null
)


@Serializable
data class ELStreamResponse(
    val audio: String? = null,
    val isFinal: Boolean? = false,
    val normalizedAlignment: ELStreamAlignment? = null,
    val alignment: ELStreamAlignment? = null,
    val error: String? = null,
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class ELStreamAlignment(
    val charStartTimesMs: Array<Int>,
    val charDurationsMs: Array<Int>,
    val chars: Array<String>

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ELStreamAlignment

        if (!charStartTimesMs.contentEquals(other.charStartTimesMs)) return false
        if (!charDurationsMs.contentEquals(other.charDurationsMs)) return false
        if (!chars.contentEquals(other.chars)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = charStartTimesMs.contentHashCode()
        result = 31 * result + charDurationsMs.contentHashCode()
        result = 31 * result + chars.contentHashCode()
        return result
    }
}