package com.igrium.aivillagers.subsystems.impl;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.playht.PlayHT;
import com.igrium.playht.SpeechStreamRequest;
import com.igrium.playht.SpeechStreamRequest.OutputFormat;
import com.igrium.playht.SpeechStreamRequest.VoiceEngine;

import net.minecraft.entity.Entity;

public class PlayHTSpeechSubsystem implements SpeechSubsystem {

    protected static final Logger LOGGER = LoggerFactory.getLogger(PlayHTSpeechSubsystem.class);

    public static class PlayHTConfig {
        public String apiKey = "";
        public String apiUser = "";
        public String voice = "s3://voice-cloning-zero-shot/9d10e3be-833b-4868-8a6a-67d91233344d/original/manifest.json";
    }

    public static final SubsystemType<PlayHTSpeechSubsystem> TYPE = SubsystemType.create(PlayHTSpeechSubsystem::new, PlayHTConfig.class);

    private PlayHT playHT;
    private String voice;

    public PlayHTSpeechSubsystem(AIManager aiManager, PlayHTConfig config) {
        playHT = new PlayHT(config.apiUser, config.apiKey);
        voice = config.voice;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    @Override
    public void speak(Entity entity, String message) {
        LOGGER.info("Making a call to PlayHT: {}", message);

        SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
        if (audioManager == null) {
            LOGGER.warn("Simple VC was not setup properly; audio will not play.");
            return;
        }

        // try {
        //     handleStreamRequest(entity, audioManager, Files.newInputStream(Paths.get("villager.wav")), null);
        // } catch (IOException e) {
        //     LOGGER.error("Error loading villager.wav", e);
        // }
        long startTime = System.currentTimeMillis();
        new SpeechStreamRequest()
                .text(message)
                .outputFormat(OutputFormat.WAV)
                .voice(voice)
                .voiceEngine(VoiceEngine.PLAYHT2)
                .send(playHT).handle((in, e) -> {
                    LOGGER.info("Recieved response from PlayHT in {}ms", System.currentTimeMillis() - startTime);
                    handleStreamRequest(entity, audioManager, in, e);
                    return null;
                });
    }
    

    private void handleStreamRequest(Entity entity, SpeechAudioManager audioManager, InputStream in, Throwable e) {
        if (e != null) {
            LOGGER.error("Error getting text-to-speech", e);
            return;
        }
        
        audioManager.playAudioFromEntity(entity, in);
    }
}
