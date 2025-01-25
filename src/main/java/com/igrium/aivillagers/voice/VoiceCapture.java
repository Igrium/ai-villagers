package com.igrium.aivillagers.voice;

import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortLists;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Captures a player's voice and feeds it into an Mp3Encoder in real-time.
 * @implSpec This class makes use of <code>System.currentTimeMillis</code> extensively,
 * so it's not suitable for non-real-time processing. Also, the class is thread-safe.
 */
public class VoiceCapture {
    private final Mp3Encoder encoder;
    private final AudioFormat format;

    public Mp3Encoder getEncoder() {
        return encoder;
    }

    public AudioFormat getFormat() {
        return format;
    }

    private final ShortList buffer = new ShortArrayList();
    private final Queue<AudioSegment> segments = new ConcurrentLinkedQueue<>();

    public VoiceCapture(Mp3Encoder encoder, AudioFormat format) {
        this.encoder = encoder;
        this.format = format;
    }

    private volatile long lastAudioCaptured = -1;

    /**
     * Get the timestamp at which the last audio packet was received.
     * @return System time of last time stamp. <code>-1</code> if no audio has been received yet.
     */
    public long getLastAudioCaptured() {
        return lastAudioCaptured;
    }

    /**
     * Add a packet of audio data to the recording.
     * @param data Data to add. If empty, end the segment using the current timestamp and queue it for encoding.
     */
    public void addPacket(short[] data) {
        // We received an end packet. End the segment.
        if (data == null || data.length == 0) {
            endSegment();
            return;
        }

        synchronized (buffer) {
            buffer.addElements(buffer.size(), data);
        }
        lastAudioCaptured = System.currentTimeMillis();
    }

    /**
     * End the current segment using the current timestamp and queue it for encoding.
     */
    public void endSegment() {
        short[] data;
        synchronized (buffer) {
            if (buffer.isEmpty())
                return;

            data = buffer.toShortArray();
            buffer.clear();
        }
        long startTime = System.currentTimeMillis() - samplesToMs(data.length);
        segments.add(new AudioSegment(data, startTime));
    }

    private volatile long lastEncodedTimestamp = -1;

    /**
     * Write all the cached audio data into the encoder.
     * @throws IOException If an IO exception is thrown by the encoder.
     * @implNote More segments are added before this function returns, they will be written as well.
     */
    public synchronized void flush() throws IOException {
        AudioSegment segment;
        while ((segment = segments.poll()) != null) {
            // Silence
            if (lastEncodedTimestamp > 0) {
                int silenceMs = (int) (segment.timestamp - lastEncodedTimestamp);
                short[] silence = new short[msToSamples(silenceMs)];
                encoder.encode(silence);
            }

            encoder.encode(segment.data);
            lastEncodedTimestamp = segment.getEndTimestamp();
        }
    }

    /**
     * Flush all voice capture data and close the encoder.
     * @throws IOException If an IO exception is thrown by the encoder.
     */
    public synchronized void flushAndClose() throws IOException {
        flush();
        encoder.close();
    }

    /**
     * Shortcut for <code>encoder.close()</code>
     * @throws IOException If the encoder throws an IO exception.
     */
    public synchronized void close() throws IOException {
        encoder.close();
    }

    private class AudioSegment {
        final short[] data;
        final long timestamp;

        AudioSegment(short[] data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        int getLengthMs() {
            return samplesToMs(data.length);
        }

        long getEndTimestamp() {
            return timestamp + getLengthMs();
        }
    }

    private int samplesToMs(int numSamples) {
        return numSamples * 1000 / (int) format.getSampleRate();
    }

    private int msToSamples(int ms) {
        return ms * (int)format.getSampleRate() / 1000;
    }
}
