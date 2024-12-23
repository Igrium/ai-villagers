package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * A standard superclass for all speech subsystems that will call some sort of text-to-speech API.
 */
public abstract class Text2SpeechSubsystem implements SpeechSubsystem {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void speak(Entity entity, String message) {
        SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
        if (audioManager == null) {
            LOGGER.error("Simple VC was not setup properly; audio will not play.");
            return;
        }

        long startTime = Util.getMeasuringTimeMs();
        doTextToSpeech(message).whenComplete((in, e) -> {
            if (e != null) {
                LOGGER.error("Error generating text-to-speech", e);
                return;
            }
            LOGGER.info("Got TTS response in {}ms", Util.getMeasuringTimeMs() - startTime);
            audioManager.playAudioFromEntity(entity, in);
        });
    }

    /**
     * Perform a text-to-speech request.
     *
     * @param message The text to say.
     * @return A future that completes with an input stream containing the audio stream.
     * The future should complete as a response is received from the API,
     * and the input stream will be filled as the audio is generated.
     */
    protected abstract CompletableFuture<InputStream> doTextToSpeech(String message);
}
