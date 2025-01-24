package com.igrium.aivillagers.voice;

import com.igrium.aivillagers.util.AudioUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Detects when a player starts talking and creates an input stream with the data.
 */
public class VoiceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceListener.class);
    private VoiceCapture writer;

    private final VoicechatApi api;
    private final int maxSilenceMs;

    private final Consumer<InputStream> onVoiceCapture;
    private final Executor voiceCaptureExecutor;


    /**
     * Create a VoiceListener
     *
     * @param api                  Simple voice chat API instance.
     * @param onVoiceCapture       Called when a player starts talking with am mp3-encoded input stream of their audio.
     * @param voiceCaptureExecutor The executor on which to call <code>onVoiceCapture</code>
     * @param maxSilenceMs         The amount of milliseconds of silence before phrase is considered complete.
     */
    public VoiceListener(VoicechatApi api, Consumer<InputStream> onVoiceCapture, Executor voiceCaptureExecutor, int maxSilenceMs) {
        this.api = api;
        this.maxSilenceMs = maxSilenceMs;
        this.onVoiceCapture = onVoiceCapture;
        this.voiceCaptureExecutor = voiceCaptureExecutor;
    }


    public synchronized void consumeVoicePacket(short[] packet) {
        if (writer == null) {
            var in = new PipedInputStream();
            PipedOutputStream out;
            try {
                out = new PipedOutputStream(in);
            } catch (IOException e) {
                // Shouldn't ever happen
                throw new RuntimeException(e);
            }

            Mp3Encoder encoder = api.createMp3Encoder(AudioUtils.FORMAT, 320, 5, out);
            writer = new VoiceCapture(encoder, AudioUtils.FORMAT);

            voiceCaptureExecutor.execute(() ->  onVoiceCapture.accept(in));
        }
        writer.addPacket(packet);
    }

    int tickNum = 0;

    /**
     * Flush the cached data to the encoder and possibly close the file if enough silence has elapsed.
     */
    public synchronized void tick() {
        if (writer != null && tickNum % 2 == 0) {
            try {
                if (writer.getLastAudioCaptured() > 0 && System.currentTimeMillis() - writer.getLastAudioCaptured() > maxSilenceMs) {
                    writer.flushAndClose();
                    writer = null;
                } else {
                    writer.flush();
                }
            } catch (IOException e) {
                LOGGER.error("Error capturing voice data:", e);
                writer = null;
            }
        }
        tickNum++;
    }

    public static class Builder {
        private VoicechatApi api;
        private Consumer<InputStream> onVoiceCapture;
        private Executor voiceCaptureExecutor = Util.getMainWorkerExecutor();
        private int maxSilenceMs = 350;

        public Builder(VoicechatApi api, Consumer<InputStream> onVoiceCapture) {
            this.api = api;
            this.onVoiceCapture = onVoiceCapture;
        }

        public Builder setApi(VoicechatApi api) {
            this.api = Objects.requireNonNull(api);
            return this;
        }

        public Builder setOnVoiceCapture(Consumer<InputStream> onVoiceCapture) {
            this.onVoiceCapture = Objects.requireNonNull(onVoiceCapture);
            return this;
        }

        public Builder setVoiceCaptureExecutor(Executor voiceCaptureExecutor) {
            this.voiceCaptureExecutor = Objects.requireNonNull(voiceCaptureExecutor);
            return this;
        }

        public Builder setMaxSilenceMs(int maxSilenceMs) {
            this.maxSilenceMs = maxSilenceMs;
            return this;
        }

        public VoiceListener build() {
            return new VoiceListener(api, onVoiceCapture, voiceCaptureExecutor, maxSilenceMs);
        }
    }
}
