package com.igrium.aivillagers.com.igrium.elevenlabs

import com.igrium.elevenlabs.ElevenLabsWSException
import com.igrium.elevenlabs.requests.VoiceSettings
import com.igrium.elevenlabs.util.PacketInputStream
import io.ktor.websocket.*
import kotlinx.coroutines.future.future
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class ElevenLabsWSConnection(val ws: WebSocketSession,
                             private val voiceSettings: VoiceSettings? = null,
                             val printDebug: Boolean = false) {

    private val packetInputStream = PacketInputStream();

    private val logger = LoggerFactory.getLogger(javaClass);

    var onError: (e: Exception) -> Unit = { logger.error("ElevenLabs error: ", it) }

    /**
     * A future that gets completed when the first packet of data is received with
     * the amount of time it took to receive the packet.
     * If there's an error before the first packet is received, the future fails
     * with that error. (`onError` is still called)
     */
    val onReceiveFirstData: CompletableFuture<Int> = CompletableFuture()

    fun setOnError(onError: Consumer<Exception>) {
        this.onError = { onError.accept(it) }
    }

    /**
     * An input stream containing the audio data as it arrives.
     */
    val inputStream get() = packetInputStream;
    private var closed = false;

    private var startTime: Long = 0

    suspend fun run() {
        startTime = System.currentTimeMillis()
        try {
            while (!closed) {
                val frame = ws.incoming.receive()
                if (frame is Frame.Text) {
                    val res = Json.decodeFromString(ELStreamResponse.serializer(), frame.readText())
                    handleResponse(res)
                }
            }
        } catch (e: Exception) {
            if (!onReceiveFirstData.isDone)
                onReceiveFirstData.completeExceptionally(e)
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
        if (!onReceiveFirstData.isDone)
            onReceiveFirstData.complete((System.currentTimeMillis() - startTime).toInt())

        if (res.audio != null) {
            val packet = Base64.getDecoder().decode(res.audio)
            if (printDebug)
                logger.info("received audio packet with ${packet.size} bytes")
            packetInputStream.addPacket(packet)
        }

        if (res.isFinal == true) {
            packetInputStream.setEOF()
            closed = true;
        }
    }

    private var isFirstMessage = true

    fun sendTTSText(text: String): CompletableFuture<*> {
        val msg = if (isFirstMessage) ELStreamText(text, voiceSettings) else ELStreamText(text)

        val str = Json.encodeToString(msg);
        if (printDebug)
            logger.info(str)

        isFirstMessage = false
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