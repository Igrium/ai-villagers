package com.igrium.aivillagers.voice;

import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * An Mp3 encoder that saves all its data to a buffer until a condition is met,
 * at which point it creates a child encoder and releases all the data into it.
 */
public class GatedEncoder implements Mp3Encoder {
    public interface EncoderPredicate {
        /**
         * Called every sample to see if the gate should be opened.
         * @param samples All the samples that have been collected so far.
         * @param squaredSum The sum of all the squared samples. Use when calculating the root-mean-square
         *                   so we don't have to query the entire buffer every time.
         * @return If the gate should open.
         */
        boolean test(ShortList samples, long squaredSum);
    }

    private final Supplier<Mp3Encoder> childSupplier;
    private final EncoderPredicate predicate;

    @Nullable
    private Mp3Encoder child;

    private final ShortList buffer = new ShortArrayList(8192);
    private long squaredSum = 0;

    public GatedEncoder(Supplier<Mp3Encoder> childSupplier, EncoderPredicate predicate) {
        this.childSupplier = childSupplier;
        this.predicate = predicate;
    }

    @Override
    public void encode(short[] samples) throws IOException {
        if (child == null) {
            for (short s : samples) {
                squaredSum += (s * s);
            }
            buffer.addElements(buffer.size(), samples);

            if (predicate.test(buffer, squaredSum)) {
                child = childSupplier.get();
                child.encode(buffer.toShortArray());
            }
        } else {
            child.encode(samples);
        }
    }

    @Override
    public void close() throws IOException {
        if (child != null) {
            child.close();
        } else {
            LoggerFactory.getLogger(getClass()).warn("Gated encoder never opened!");
        }
    }

    /**
     * Return a GatedEncoder that opens once the root-mean-squared of the buffered samples passes a certain threshold.
     *
     * @param childSupplier Function to create the child encoder.
     * @param threshold     The threshold to pass.
     * @return The encoder.
     */
    public static GatedEncoder threshold(Supplier<Mp3Encoder> childSupplier, int threshold) {
        int thresholdSquared = threshold * threshold;
        return new GatedEncoder(childSupplier, (samples, sum) -> sum / samples.size() > thresholdSquared);
    }
}
