package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import com.igrium.aivillagers.util.AudioUtils;
import com.igrium.aivillagers.util.ConcurrentIteratorQueue;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        doTextToSpeech(message, null).whenComplete((in, e) -> {
            if (e != null) {
                LOGGER.error("Error generating text-to-speech", e);
                return;
            }
            LOGGER.info("Got TTS response in {}ms", Util.getMeasuringTimeMs() - startTime);
            LOGGER.info("AudioInputStream has {} samples.", in.getFrameLength());

            audioManager.playAudioFromEntity(entity, in);
        });
    }

    @Override
    public SpeechStream openSpeechStream(Entity entity) {
        return SpeechSubsystem.super.openSpeechStream(entity);
//        return new Text2SpeechStream(entity);
    }

    /**
     * Perform a text-to-speech request.
     *
     * @param message The text to say.
     * @param prevText The text that came before the text of the current request.
     * @return A future that completes with an input stream containing the audio stream.
     * The future should complete as a response is received from the API,
     * and the input stream will be filled as the audio is generated.
     */
    protected abstract CompletableFuture<AudioInputStream> doTextToSpeech(String message, @Nullable String prevText);


    protected final CompletableFuture<AudioInputStream> tryTextToSpeech(String message, @Nullable String prevText) {
        return doTextToSpeech(message, prevText).exceptionally(e -> {
            LOGGER.error("Error generating text-to-speech:", e);
            return AIVillagers.getInstance().getErrorSound().getInputStream();
        });
    }

    private class Text2SpeechStream implements SpeechStream {

        final Entity villager;

        private StringBuilder sb = new StringBuilder();
        private final StringBuilder complete = new StringBuilder();

        private final ConcurrentIteratorQueue<AudioInputStream> audioStreams = new ConcurrentIteratorQueue<>();
        private boolean startedPlaying = false;

        private volatile boolean closed;
        // The number of text-to-speech jobs we're waiting on
        private final AtomicInteger jobs = new AtomicInteger(0);

        private Text2SpeechStream(Entity villager) {
            this.villager = villager;
        }

        @Override
        public synchronized void acceptToken(String token) {
            sb.append(token);
            if (token.matches("[.,?!;:—\\-\\[\\](){}]")) {
                flush();
            }
        }

        synchronized void flush() {
            String msg = sb.toString();
            LOGGER.info(msg);
            if (!msg.isBlank()) {
                jobs.incrementAndGet();
                tryTextToSpeech(sb.toString(), complete.toString()).whenComplete(this::acceptStream);
            }
            complete.append(sb);
            sb = new StringBuilder();
        }

        synchronized void acceptStream(AudioInputStream stream, Throwable e) {
            LOGGER.info("Audio stream: {}", stream);
            try {
                if (e != null) {
                    LOGGER.error("Error downloading audio stream: ", e);
                } else {
                    audioStreams.add(stream);

                    if (!startedPlaying) {
                        startedPlaying = true;
                        startPlaying();
                    }

                }
                if (jobs.decrementAndGet() <= 0 && closed) {
                    audioStreams.setComplete();
                }
            } catch (Exception ex) {
                LOGGER.error("Error playing stream: ", ex);
            }

        }

        void startPlaying() {
            LOGGER.info("Playing audio");
            SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
            if (audioManager == null) {
                LOGGER.error("Simple VC was not setup properly; audio will not play.");
                return;
            }
            audioManager.playAudioFromEntity(villager, AudioUtils.concat(SpeechAudioManager.FORMAT, audioStreams));
        }

        @Override
        public synchronized void close() {
            closed = true;
            flush();
            if (jobs.get() <= 0) {
                audioStreams.setComplete();
            }
        }
    }
}
