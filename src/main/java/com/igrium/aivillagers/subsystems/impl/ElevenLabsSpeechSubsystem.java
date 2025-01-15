package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.speech.ElevenLabsSpeechClient;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.debug.MirrorInputStream;
import com.igrium.elevenlabs.requests.OutputFormat;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final OutputFormat FORMAT = OutputFormat.PCM_22050;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(22.05f, 16, 1, true, false);

    private final Path debugOutput = Paths.get("debugAudio");

    @Override
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message) {
        return client.streamTTS(message)
                .thenApply(in -> MirrorInputStream.createDebugMirror(in, debugOutput))
                .thenApply(in -> new AudioInputStream(in, AUDIO_FORMAT, Integer.MAX_VALUE));
    }

}
