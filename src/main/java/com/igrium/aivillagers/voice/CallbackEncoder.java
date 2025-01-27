package com.igrium.aivillagers.voice;

import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import java.io.IOException;

/**
 * An Mp3 encoder that calls a callback when it receives samples.
 */
public class CallbackEncoder implements Mp3Encoder {


    public interface EncodeSamplesCallback {
        /**
         * Called before samples are passed to the base encoder.
         *
         * @param samples    Samples being encoded.
         * @param buffer     All the samples that have been encoded already.
         * @param squaredSum The squared sum of all the samples in the buffer.
         *                   Use when calculating RMS to avoid looping the entire buffer.
         */
        void onEncode(short[] samples, ShortList buffer, long squaredSum);
    }

    public interface CloseCallback {
        /**
         * Called before the base encoder is closed.
         *
         * @param buffer     All the samples that have been encoded.
         * @param squaredSum The squared sum of all the samples in the buffer.
         *                   Use when calculating RMS to avoid looping the entire buffer.
         */
        void onClose(ShortList buffer, long squaredSum);
    }

    private final Mp3Encoder base;

    private final EncodeSamplesCallback encodeCallback;
    private final CloseCallback closeCallback;

    private final ShortList buffer = new ShortArrayList(8192);
    private long squaredSum = 0;

    public CallbackEncoder(Mp3Encoder base, EncodeSamplesCallback encodeCallback, CloseCallback closeCallback) {
        this.base = base;
        this.encodeCallback = encodeCallback;
        this.closeCallback = closeCallback;
    }

    public CallbackEncoder(Mp3Encoder base, EncodeSamplesCallback encodeCallback) {
        this(base, encodeCallback, (samples, sum) -> {});
    }

    @Override
    public void encode(short[] samples) throws IOException {
        for (short s : samples) {
            squaredSum += (s * s);
        }
        buffer.addElements(buffer.size(), samples);

        encodeCallback.onEncode(samples, buffer, squaredSum);
        base.encode(samples);
    }

    @Override
    public void close() throws IOException {
        closeCallback.onClose(buffer, squaredSum);
        base.close();
    }
}
