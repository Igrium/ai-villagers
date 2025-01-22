package com.igrium.elevenlabs.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketInputStream extends InputStream {

    private final Queue<byte[]> queue = new ConcurrentLinkedQueue<>();

    private byte[] current = null;
    private int head;
    /**
     * If true, no more packets will be received. Still empty the current queue.
     */
    private volatile boolean isEOF;

    public synchronized void setEOF() {
        isEOF = true;
        this.notifyAll();
    }

    public void addPacket(byte[] packet) {
        if (isEOF) {
            throw new IllegalStateException("Stream has been marked as EOF.");
        }
        queue.add(packet);
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Override
    public int read() throws IOException {
        if (current == null || head >= current.length) {
            nextPacket();
        }
        if (current == null) return -1; // If it's still null, we're EOF.

        int val = current[head] & 0xff;
        head++;
        return val;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        if (current == null || head >= current.length) {
            nextPacket();
        }
        if (current == null) return -1; // If it's still null, we're EOF.

        // Shortcut if we're not needing to span arrays.
        if (head + len <= current.length) {
            System.arraycopy(current, head, b, off, len);
            head += len;
            return len;
        } else {
            int bytesRead = 0;
            while (bytesRead < len && current != null) {
                // Calculate how many bytes can be copied from the current packet
                int bytesAvailable = current.length - head;
                int bytesToCopy = Math.min(len - bytesRead, bytesAvailable);

                System.arraycopy(current, head, b, off + bytesRead, bytesToCopy);
                head += bytesToCopy;
                bytesRead += bytesToCopy;

                if (head >= current.length) {
                    nextPacket();
                }
            }
            return bytesRead;
        }

    }

    private synchronized void nextPacket() throws IOException {
        current = queue.poll();
        head = 0;
        if (current == null && !isEOF) {
            try {
                this.wait();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while reading", e);
            }
            nextPacket();
        }
    }
}
