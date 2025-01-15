package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
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
            LOGGER.info("AudioInputStream has {} samples.", in.getFrameLength());
//            try {
//                byte[] bytes = in.readAllBytes();
//                LOGGER.info("TTS Bytes: " + bytesToHex(bytes));
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
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
    protected abstract CompletableFuture<AudioInputStream> doTextToSpeech(String message);

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
