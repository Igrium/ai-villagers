package com.igrium.aivillagers.voice;

import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Writes a player's mic data to an Mp3 encoder in real-time.
 */
public class VoiceWriter {

    private static final int SAMPLE_RATE = 48000;

    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceWriter.class);

    private record AudioSegment(short[] data, long timestamp) {

        public int getLengthMs() {
            return getAudioTimeMs(data.length);
        }

        public long getEndTime() {
            return getLengthMs() + timestamp;
        }
    };

    private final Queue<AudioSegment> segments = new ConcurrentLinkedQueue<>();

    private final int timeout;
    private final Mp3Encoder encoder;

    @Nullable
    private Runnable onClose;

    /**
     * Create a voice writer.
     *
     * @param encoder The output stream to write to.
     * @param timeout If no audio packet is received in this many milliseconds, consider the audio finished
     *                and close the output stream. <code>-1</code> to disable auto-close.
     */
    public VoiceWriter(Mp3Encoder encoder, int timeout) {
        this.encoder = encoder;
        this.timeout = timeout;
    }

    /**
     * Add a callback to be called if this writer auto-closes due to inactivity.
     */
    public void setOnClose(@Nullable Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Add a segment of audio to be played "now".
     * @param data 16-bit PCM audio data
     */
    public void addAudio(short[] data) {
        segments.add(new AudioSegment(data, System.currentTimeMillis()));
    }

    // The end time of the last segment written.
    private long lastSegmentWritten = -1;
    private long lastRealSegmentWritten = -1;

    /**
     * Flush this writer to the output stream.
     * @param addSilence If true, add silence at the end of the audio stream to catch up to system time.
     */
    public synchronized void flush(boolean addSilence) throws IOException {
        AudioSegment segment;

        while ((segment = segments.poll()) != null) {
            // Silence between segments
            if (lastSegmentWritten > 0) {
                int silenceMs = (int) (segment.timestamp - lastSegmentWritten);
                if (silenceMs > 0) {
                    encoder.encode(generateSilence(silenceMs));
                }
            }

            // AUDIO
            encoder.encode(segment.data);
            lastSegmentWritten = segment.getEndTime();
            lastRealSegmentWritten = lastSegmentWritten;
        }
        long now = System.currentTimeMillis();

        if (timeout > 0 && lastRealSegmentWritten > 0 && (now - lastRealSegmentWritten) > timeout) {
            close();
            return;
        }

        if (addSilence && lastSegmentWritten > 0) {
            int silenceMs = (int) (now - lastSegmentWritten);
            if (silenceMs > 0) {
                encoder.encode(generateSilence(silenceMs));
                lastSegmentWritten = now;
            }
        }
    }

    public synchronized void close() throws IOException {
        if (onClose != null) {
            onClose.run();
        }
        encoder.close();
    }

    private static int getAudioTimeMs(int audioShortLength) {
        return audioShortLength / getSamplesPerMs();
    }

    private static int getSamplesPerMs() {
        return SAMPLE_RATE / 1000;
    }

    private static short[] generateSilence(int lengthMs) {
        int samples = lengthMs * getSamplesPerMs();
        return new short[samples];
    }
}
