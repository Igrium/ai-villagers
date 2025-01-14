package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.speech.ElevenLabsSpeechClient;
import com.igrium.aivillagers.subsystems.SubsystemType;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class ElevenLabsSpeechSubsystem extends Text2SpeechSubsystem {

    public static SubsystemType<ElevenLabsSpeechSubsystem> TYPE = SubsystemType
            .create(ElevenLabsSpeechSubsystem::new, ElevenLabsSpeechSubsystemConfig.class);

    public static final class ElevenLabsSpeechSubsystemConfig {
        public String apiKey;
        public String voiceId;
        public @Nullable String baseURL;
    }

    private final AIManager aiManager;

    private final ElevenLabsSpeechClient client;

    public ElevenLabsSpeechSubsystem(AIManager aiManager, ElevenLabsSpeechSubsystemConfig config) {
        this.aiManager = aiManager;
        client = new ElevenLabsSpeechClient(
                config.apiKey, config.voiceId, config.baseURL != null ? config.baseURL : "https://api.elevenlabs.io");
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message) {
        return client.streamTTS(message).thenApply(SpeechAudioManager::getAudioInputStream);
    }
}
