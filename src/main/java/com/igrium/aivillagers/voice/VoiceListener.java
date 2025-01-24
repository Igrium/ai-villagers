package com.igrium.aivillagers.voice;

import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.util.AudioUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Detects when a player starts talking and creates an input stream with the data.
 */
public class VoiceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceListener.class);
    private VoiceCapture writer;
    private final VoicechatApi api;
    private int maxSilenceMs = 350;
    private final Consumer<InputStream> onVoiceCapture;

    public VoiceListener(VoicechatApi api, Consumer<InputStream> onVoiceCapture) {
        this.api = api;
        this.onVoiceCapture = onVoiceCapture;
    }

    public int getMaxSilenceMs() {
        return maxSilenceMs;
    }

    public void setMaxSilenceMs(int maxSilenceMs) {
        this.maxSilenceMs = maxSilenceMs;
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

            Util.getMainWorkerExecutor().execute(() ->  onVoiceCapture.accept(in));
        }
        writer.addPacket(packet);
    }

    int tickNum = 0;

    public synchronized void tick() {
        if (writer != null && tickNum % 2 == 0) {
            try {
                if (writer.getLastAudioCaptured() > 0 && System.currentTimeMillis() - writer.getLastAudioCaptured() > maxSilenceMs) {
                    LOGGER.info("Silence for {} ms", System.currentTimeMillis() - writer.getLastAudioCaptured());
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
}
