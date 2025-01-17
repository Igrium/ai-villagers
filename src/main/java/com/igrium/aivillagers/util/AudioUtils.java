package com.igrium.aivillagers.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Iterator;

public final class AudioUtils {
    /**
     * Concatenate a series of audio input streams.
     * @param format Format to convert the audio to.
     * @param streams An iterator that will return the audio streams, in order.
     * @return The combined stream.
     */
    public static AudioInputStream concat(AudioFormat format, Iterator<AudioInputStream> streams) {
        return new AudioInputStream(new SequenceInputStream(new AudioEnumerator(format, streams)), format, Integer.MAX_VALUE);
    }

    private record AudioEnumerator(AudioFormat format, Iterator<AudioInputStream> iterator)
            implements Enumeration<AudioInputStream> {

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public AudioInputStream nextElement() {
            AudioInputStream next = iterator.next();
            if (next.getFormat().equals(format)) {
                return next;
            } else {
                return AudioSystem.getAudioInputStream(format, next);
            }
        }
    }
}
