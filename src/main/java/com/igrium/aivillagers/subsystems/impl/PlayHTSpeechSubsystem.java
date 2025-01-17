package com.igrium.aivillagers.subsystems.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class PlayHTSpeechSubsystem extends Text2SpeechSubsystem {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static class PlayHTConfig {
        public String apiKey = "";
        public String apiUser = "";
        public String voice = "s3://voice-cloning-zero-shot/9d10e3be-833b-4868-8a6a-67d91233344d/original/manifest.json";
    }

    public static final SubsystemType<PlayHTSpeechSubsystem> TYPE = SubsystemType.create(PlayHTSpeechSubsystem::new, PlayHTConfig.class);

    private final PlayHT playHT;
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
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message, String prevText) {
        return new SpeechStreamRequest()
                .text(message)
                .outputFormat(OutputFormat.WAV)
                .voice(voice)
                .voiceEngine(VoiceEngine.PLAYHT2)
                .send(playHT).thenApply(
                        SpeechAudioManager::getAudioInputStream
                );
    }
}
