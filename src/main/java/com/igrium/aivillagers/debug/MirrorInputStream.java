package com.igrium.aivillagers.debug;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An InputStream wrapper that mirrors all its contents to a file for testing.
 */
public class MirrorInputStream extends InputStream {

    private final InputStream base;
    private final OutputStream mirror;

    public MirrorInputStream(InputStream base, OutputStream mirror) {
        this.base = base;
        this.mirror = mirror;
    }

    @Override
    public int read() throws IOException {
        int val = base.read();
        if (val >= 0) {
            mirror.write(val);
        }
        return val;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int num = base.read(b, off, len);
        if (num > 0) {
            mirror.write(b, off, num);
        }
        return num;
    }

    @Override
    public void close() throws IOException {
        super.close();
        base.close();
        mirror.close();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorInputStream.class);

    /**
     * Attempt to create a debug mirror as a one-liner. If something fails creating the mirror,
     * log an error and return the original input stream.
     * @param in The input stream to mirror.
     * @param output File to write to.
     * @return The mirror, or the original input stream if mirror creation failed.
     */
    public static InputStream createDebugMirror(InputStream in, Path output) {
        try {
            var mirror = new MirrorInputStream(in, new BufferedOutputStream(Files.newOutputStream(output)));
            LOGGER.info("Created input stream mirror at {}", output);
            return mirror;
        } catch (Exception e) {
            LOGGER.error("Error creating debug mirror:", e);
            return in;
        }
    }
}
