package com.igrium.aivillagers.util;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.ShortBuffer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.function.Consumer;

public final class AudioUtils {
    public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

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

    /**
     * An Mp3Encoder that just encodes into PCM 16-bit 48khz
     */
    public static class PCMEncoder implements Mp3Encoder {

        private final OutputStream out;
        private final VoicechatApi api;

        public PCMEncoder(OutputStream out, VoicechatApi api) {
            this.out = out;
            this.api = api;
        }

        @Override
        public void encode(short[] samples) throws IOException {
            out.write(api.getAudioConverter().shortsToBytes(samples));
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }


    /**
     * Create an MP3 encoder suitable for sending data to speech-to-text services.
     *
     * @param api Voice chat API instance.
     * @param out Output stream to write to.
     * @return The encoder.
     */
    public static Mp3Encoder createGenericMp3Encoder(VoicechatApi api, OutputStream out) {
        return api.createMp3Encoder(FORMAT, 320, 6, out);
    }
}
