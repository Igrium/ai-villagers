package com.igrium.aivillagers;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
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

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
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

    private static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    private final SpeechVCPlugin plugin;

    public SpeechAudioManager(SpeechVCPlugin plugin) {
        this.plugin = plugin;
    }

    public SpeechVCPlugin getPlugin() {
        return plugin;
    }

    public VoicechatServerApi getApi() {
        return plugin.getServerApi();
    }
    
    private ExecutorService audioProcessingService = new ThreadPoolExecutor(3, 10, 30, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    public Executor getAudioProcessingService() {
        return audioProcessingService;
    }

    /**
     * Create an audio player that will stream PCM wav data from an input stream.
     * 
     * @param channel Audio channel to play to.
     * @param audio   Audio data.
     * @return The audio player.
     * @throws UnsupportedAudioFileException If the audio format isn't supported.
     * @throws IOException                   If an IO exception occurs trying to
     *                                       parse the audio format.
     * @implNote The input stream will automatically close once the audio is done.
     */
    public AudioPlayer streamAudio(AudioChannel channel, InputStream audio)
            throws UnsupportedAudioFileException, IOException {
        long time = System.currentTimeMillis();
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(audio));
        LOGGER.info("Took {}ms to initialize audio input stream", System.currentTimeMillis() - time);
        return streamAudio(channel, audioStream);
    }

    /**
     * Create an audio player that will stream audio from an input stream.
     * 
     * @param channel Audio channel to play to.
     * @param audio   Audio data.
     * @return The audio player.
     * @implNote The input stream will automatically close once the audio is done.
     */
    public AudioPlayer streamAudio(AudioChannel channel, AudioInputStream audio) {
        return new StreamingAudioPlayer(channel, audio).audioPlayer;
    }

    /**
     * A helper method to create an audio input stream from an input stream as a one-liner.
     * @param in Input stream.
     * @return The audio input stream.
     */
    public static AudioInputStream getAudioInputStream(InputStream in) {
        try {
            return AudioSystem.getAudioInputStream(new BufferedInputStream(in));
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Play audio from an entity.
     *
     * @param entity Entity to play from.
     * @param audio  An input stream with the audio data.
     * @return The audio player with the audio.
     * @implNote The input stream will automatically close once the audio is done.
     */
    public AudioPlayer playAudioFromEntity(Entity entity, AudioInputStream audio) {
        try {
            // EntityAudioChannel channel = getPlugin().getServerApi().createEntityAudioChannel(UUID.randomUUID(),
            //         getPlugin().getServerApi().fromEntity(entity));

            LocationalAudioChannel channel = getApi().createLocationalAudioChannel(UUID.randomUUID(),
                    getApi().fromServerLevel(entity.getWorld()),
                    getApi().createPosition(entity.getX(), entity.getY(), entity.getZ()));

            if (channel == null) {
                LOGGER.warn("Unable to create audio channel for {}", entity.getNameForScoreboard());
                return null;
            }
      
            channel.setCategory(SpeechVCPlugin.VILLAGER_CATEGORY);
            channel.setDistance(100);

            AudioPlayer player = streamAudio(channel, audio);
            long time = System.currentTimeMillis();
            player.setOnStopped(() -> {
                LOGGER.info("Audio finished playing in {}ms", System.currentTimeMillis() - time);
            });

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
    
    private class StreamingAudioPlayer {
        AudioPlayer audioPlayer;
        byte[] readBuffer = new byte[FRAME_SIZE * 2]; // Frame size is in shorts
        AudioInputStream audio;

        StreamingAudioPlayer(AudioChannel channel, AudioInputStream audio) {
            this.audio = AudioSystem.getAudioInputStream(FORMAT, audio);
            this.audioPlayer = getApi().createAudioPlayer(channel, getApi().createEncoder(), this::getFrame);
        }

        synchronized short[] getFrame() {
            try {
                // TODO: can we find a more efficient way to clear this if array wasn't filled?
                Arrays.fill(readBuffer, (byte) 0);
                int read = audio.read(readBuffer);

                if (read < 0) {
                    audio.close();
                    return null;
                }
                return getApi().getAudioConverter().bytesToShorts(readBuffer);
            } catch (Exception e) {
                LOGGER.info("Error reading audio stream:", e);
                return null;
            }
        }
    }

}
