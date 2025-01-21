package com.igrium.aivillagers.com.igrium.elevenlabs

import com.igrium.elevenlabs.ElevenLabsWSException
import com.igrium.elevenlabs.requests.VoiceSettings
import com.igrium.elevenlabs.util.PacketInputStream
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.future.future
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.WebSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer

class ElevenLabsWSConnection(val ws: WebSocketSession) {

    private val packetInputStream = PacketInputStream();

    private val LOGGER = LoggerFactory.getLogger(javaClass);

    var onError: (e: Exception) -> Unit = { LOGGER.error("ElevenLabs error: ", it) }

    fun setOnError(onError: Consumer<Exception>) {
        this.onError = { onError.accept(it) }
    }

    /**
     * An input stream containing the audio data as it arrives.
     */
    val inputStream get() = packetInputStream;
    private var closed = false;

    suspend fun run() {
        try {
            while (!closed) {
                val frame = ws.incoming.receive()
                if (frame is Frame.Text) {
                    val res = Json.decodeFromString(ELStreamResponse.serializer(), frame.readText())
                    handleResponse(res)
                }
            }
        } catch (e: Exception) {
            onError(e)
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
            val packet = Base64.getDecoder().decode(res.audio)
            println("received audio packet with ${packet.size} bytes")
            packetInputStream.addPacket(packet)
        }

        if (res.isFinal == true) {
            packetInputStream.setEOF()
            closed = true;
        }
    }

    fun sendTTSText(text: String): CompletableFuture<*> {
        val str = Json.encodeToString(ELStreamText(text));
        LOGGER.info(str)
        return ws.future { ws.send(str) }
    }

    fun close(): CompletableFuture<*> {
        return ws.future {
            ws.send("{\"text\": \"\"}")
        }
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