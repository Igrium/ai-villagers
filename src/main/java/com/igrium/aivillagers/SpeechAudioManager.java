package com.igrium.aivillagers;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import net.minecraft.entity.Entity;

/**
 * Handles audio processing for simple voice chat.
 */
public class SpeechAudioManager implements Closeable {

    public static SpeechAudioManager getInstance() {
        var plugin = SpeechVCPlugin.getInstance();
        return plugin != null ? plugin.getAudioManager() : null;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechAudioManager.class);
    private static final int FRAME_SIZE = 960;

    private static final short[] EMPTY_FRAME = new short[FRAME_SIZE];

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    private final SpeechVCPlugin plugin;

    public SpeechAudioManager(SpeechVCPlugin plugin) {
        this.plugin = plugin;
    }

    public SpeechVCPlugin getPlugin() {
        return plugin;
    }

    public VoicechatApi getApi() {
        return plugin.getApi();
    }
    
    private ExecutorService audioProcessingService = new ThreadPoolExecutor(3, 10, 30, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    public Executor getAudioProcessingService() {
        return audioProcessingService;
    }

    /**
     * Create an audio player that will stream the wav data from an input stream.
     * @param channel Audio channel to play to.
     * @param audio Audio data to play.
     * @return The audio player.
     * @implNote The stream will automatically be closed when the audio has finished loading.
     */
    public AudioPlayer createAudioPlayer(AudioChannel channel, InputStream audio) {
        WavStreamingAudioPlayer player = new WavStreamingAudioPlayer();
        audioProcessingService.execute(() -> player.convertSafe(audio)); // Begin conversion on audio processing thread.
        return player.getOrCreateAudioPlayer(channel);
    }

    public AudioPlayer playAudioFromEntity(Entity entity, InputStream audio) {
        try {
            EntityAudioChannel channel = getPlugin().getServerApi().createEntityAudioChannel(UUID.randomUUID(),
            getPlugin().getServerApi().fromEntity(entity));
    
            if (channel == null) {
                LOGGER.warn("Unable to create audio channel for {}", entity.getNameForScoreboard());
                return null;
            }

            AudioPlayer player = createAudioPlayer(channel, audio);
            player.startPlaying();
            return player;
        } catch (Exception e) {
            LOGGER.error("Error playing audio from entity.", e);
            return null;
        }
    }

    @Override
    public void close() {
        LOGGER.info("Shutting down audio processing service");
        audioProcessingService.shutdown();
    }
    
    /**
     * Takes care of streaming as wav file into an audio player.
     */
    private class WavStreamingAudioPlayer {
        AudioPlayer audioPlayer;

        Queue<short[]> frames = new ConcurrentLinkedDeque<>();
        volatile boolean isDone;

        public AudioPlayer getOrCreateAudioPlayer(AudioChannel channel) {
            if (audioPlayer == null) {
                audioPlayer = getPlugin().getServerApi().createAudioPlayer(channel, getApi().createEncoder(), this::getFrame);
            }
            return audioPlayer;
        }

        public short[] getFrame() {
            // When frames in queue run out, see if the input stream is still running.
            // Check this before so we know the input stream didn't finish after the frame was pulled.
            boolean isDone = this.isDone;
            short[] frame = frames.poll();
            if (frame == null) {
                return isDone ? null : EMPTY_FRAME;
            }
            return frame;
        }

        /**
         * Convert the audio file into an array of shorts that can be read by
         * getFrame().
         * Will block until audio conversion is complete. Should be called on Audio
         * manager thread.
         * 
         * @param in Input stream of audio file.
         * @throws UnsupportedAudioFileException If the file's format is unsupported.
         * @throws IOException                   If an IO exception occurs.
         */
        public void convert(InputStream in) throws UnsupportedAudioFileException, IOException {
            AudioInputStream source = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            AudioInputStream converted = AudioSystem.getAudioInputStream(FORMAT, source);

            byte[] buffer = new byte[FRAME_SIZE * 2]; // Frame size is in shorts.
            
            while(converted.read(buffer) > 0) {
                short[] frame = getApi().getAudioConverter().bytesToShorts(buffer);
                frames.add(frame); // Should cause getFrame to be able to return this frame.
                Arrays.fill(buffer, (byte) 0);
            }
            in.close();
            isDone = true;
        }

        public void convertSafe(InputStream in) {
            try {
                convert(in);
            } catch (Exception e) {
                LOGGER.error("An error occured decoding TTS audio: ", e);
                isDone = true;
            }
        }
    }
}
