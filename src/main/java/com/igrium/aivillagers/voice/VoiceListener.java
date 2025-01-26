package com.igrium.aivillagers.voice;

import com.igrium.aivillagers.util.AudioUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Detects when a player starts talking and outputs data to an outputstream.
 */
public class VoiceListener {
    // I REALLY don't like how much indirection is going on in here.
    public interface EncoderFactory {
        Mp3Encoder create(VoicechatApi api, Supplier<OutputStream> out);

        /**
         * Applies a threshold gated encoder around another encoder factory.
         * @param child Child encoder factory.
         * @param threshold The threshold.
         * @return The wrapped encoder factory.
         * @see GatedEncoder#threshold
         */
        static EncoderFactory threshold(EncoderFactory child, int threshold) {
            return (api, out) -> GatedEncoder.threshold(() -> child.create(api, out), threshold);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceListener.class);
    private VoiceCapture writer;

    private final VoicechatApi api;

//    private final Consumer<InputStream> onVoiceCapture;
    private final Supplier<OutputStream> onVoiceCapture;
    private final EncoderFactory encoderFactory;
    private final int maxSilenceMs;


    /**
     * Create a VoiceListener
     *
     * @param api            Simple voice chat API instance.
     * @param onVoiceCapture Called when a player starts talking with am mp3-encoded input stream of their audio.
     * @param encoderFactory Called to create the Mp3Encoder to write to.
     * @param maxSilenceMs   The amount of milliseconds of silence before phrase is considered complete.
     */
    public VoiceListener(VoicechatApi api, Supplier<OutputStream> onVoiceCapture, EncoderFactory encoderFactory, int maxSilenceMs) {
        this.api = api;
        this.encoderFactory = encoderFactory;
        this.maxSilenceMs = maxSilenceMs;
        this.onVoiceCapture = onVoiceCapture;
    }


    public synchronized void consumeVoicePacket(short[] packet) {
        if (writer == null) {
            Mp3Encoder encoder = encoderFactory.create(api, onVoiceCapture);
            writer = new VoiceCapture(encoder, AudioUtils.FORMAT);

//            voiceCaptureExecutor.execute(() ->  onVoiceCapture.accept(in));
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
        private Supplier<OutputStream> onVoiceCapture;
        private EncoderFactory encoderFactory = (api, out) -> AudioUtils.createGenericMp3Encoder(api, out.get());
        private int maxSilenceMs = 350;

        public Builder(VoicechatApi api, Supplier<OutputStream> onVoiceCapture) {
            this.api = api;
            this.onVoiceCapture = onVoiceCapture;
        }

        public Builder setApi(VoicechatApi api) {
            this.api = Objects.requireNonNull(api);
            return this;
        }

        public Builder setOnVoiceCapture(Supplier<OutputStream> onVoiceCapture) {
            this.onVoiceCapture = Objects.requireNonNull(onVoiceCapture);
            return this;
        }

        public Builder setEncoderFactory(EncoderFactory encoderFactory) {
            this.encoderFactory = encoderFactory;
            return this;
        }

        public Builder setMaxSilenceMs(int maxSilenceMs) {
            this.maxSilenceMs = maxSilenceMs;
            return this;
        }

        public VoiceListener build() {
            return new VoiceListener(api, onVoiceCapture, encoderFactory, maxSilenceMs);
        }
    }
}
