package com.igrium.aivillagers.voice;

import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.util.AudioUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
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

    private final VoicechatApi api;
    private final int maxSilence;
    private final Function<OutputStream, Mp3Encoder> encoderFactory;
    private final Consumer<InputStream> onPlayerTalking;

//    private VoiceCapture currentCapture;
    private final AtomicReference<VoiceCapture> currentCapture = new AtomicReference<>();

    /**
     * Create a voice listener.
     * @param api API instance.
     * @param maxSilence The milliseconds of silence allowed before phrase is considered complete.
     * @param onPlayerTalking Callback for when a player has begun talking.
     *                        Called on the thread from which <code>consumeVoicePacket()</code> is called.
     */
    public VoiceListener(VoicechatApi api, int maxSilence, Consumer<InputStream> onPlayerTalking) {
        this.api = api;
        this.maxSilence = maxSilence;
        this.encoderFactory = this::createDefaultEncoder;
        this.onPlayerTalking = onPlayerTalking;
    }

    public VoiceListener(VoicechatApi api, int maxSilence, Function<OutputStream, Mp3Encoder> encoderFactory, Consumer<InputStream> onPlayerTalking) {
        this.api = api;
        this.maxSilence = maxSilence;
        this.encoderFactory = encoderFactory;
        this.onPlayerTalking = onPlayerTalking;
    }

    public void consumeVoicePacket(short[] data) {
        PipedInputStream in = null;
        VoiceCapture cap;
        synchronized (this) {
            cap = currentCapture.get();
            if (cap == null) {
                in = new PipedInputStream();
                PipedOutputStream out;
                try {
                    out = new PipedOutputStream(in);
                } catch (IOException e) {
                    throw new RuntimeException(e); // Should never happen
                }
                Mp3Encoder encoder = encoderFactory.apply(out);
                cap = new VoiceCapture(encoder, AudioUtils.FORMAT);
                onPlayerTalking.accept(in);
                currentCapture.set(cap);
            }
        }

        if (in != null) {
            onPlayerTalking.accept(in);
        }
        cap.addPacket(data);
    }

    private Mp3Encoder createDefaultEncoder(OutputStream out) {
        return api.createMp3Encoder(AudioUtils.FORMAT, 320, 4, out);
    }

    /**
     * Flush all audio packets into the encoder and check if we've exceeded max silence time.
     */
    public synchronized void tick() {
        long now = System.currentTimeMillis();
        var cap = currentCapture.get();
        if (cap == null) return;

        // TODO: find a way to interrupt if flush doesn't complete in time
        try {
            if (maxSilence >= 0 && now - cap.getLastAudioCaptured() > maxSilence) {
                cap.flushAndClose();
                if (!currentCapture.compareAndSet(cap, null)) {
                    LOGGER.warn("Some kind of race condition caused currentCapture to be updated during tick.");
                };
            } else {
                cap.flush();
            }
        } catch (IOException e) {
            AIVillagers.LOGGER.error("Error flushing audio capture:", e);
            currentCapture.compareAndSet(cap, null);
        }
    }
}
