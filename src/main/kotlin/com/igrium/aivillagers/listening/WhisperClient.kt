package com.igrium.aivillagers.listening

import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.igrium.aivillagers.util.FutureList
import com.mojang.datafixers.util.Either
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import net.minecraft.server.network.ServerPlayerEntity
import okio.Pipe
import okio.buffer
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.util.*
import java.util.function.BiConsumer
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class WhisperClient(
    /**
     * The OpenAI API key.
     */
    apiKey: String,

    /**
     * A callback for when a player has finished talking and their voice has processed.
     */
    private val onProcessSpeech: (player: ServerPlayerEntity, speech: String) -> Unit
) {

    constructor(apiKey: String, onProcessSpeech: BiConsumer<ServerPlayerEntity, String>) :
            this(apiKey, { p, s -> onProcessSpeech.accept(p, s) })

    private val logger = LoggerFactory.getLogger(javaClass)

    private val openAI = OpenAI(
        token = apiKey,
        timeout = Timeout(socket = 60.toDuration(DurationUnit.MILLISECONDS)),
        logging = LoggingConfig(logLevel = LogLevel.None)
    )

    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("OpenAI Interface"))

    private val futures = Collections.synchronizedMap(WeakHashMap<ServerPlayerEntity, FutureList<String>>())

    /**
     * Call when a voice listener returns an input stream with data.
     */
    fun handleVoiceCapture(player: ServerPlayerEntity): OutputStream {

        val pipe = Pipe(65536) // 2 ^ 15
        val req = TranscriptionRequest(
            audio = FileSource("mic.mp3", pipe.source),
            model = ModelId("whisper-1")
        )
        val fList = futures.computeIfAbsent(player) {
            FutureList {
                handleResults(player, it)
            }
        }

        fList.submit(scope.future {
            try {
                openAI.transcription(req).text
            } catch (e: Exception) {
                logger.error("Error receiving text from openai:", e)
                return@future ""
            }
        })

        return pipe.sink.buffer().outputStream()
    }


    private fun handleResults(player: ServerPlayerEntity, results: List<Either<String, Throwable>>) {
        val builder = StringBuilder()
        for (res in results) {
            res.ifLeft {
                builder.append(it)
                builder.append(" ")
            }
            res.ifRight { logger.error("Whisper error: ", it) }
        }
        onProcessSpeech(player, builder.toString())
    }

}