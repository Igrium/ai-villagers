package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.debug.MirrorInputStream;
import com.igrium.aivillagers.speech.OpenAISpeechClient;
import com.igrium.aivillagers.speech.SimpleOpenAISpeechClient;
import com.igrium.aivillagers.subsystems.SubsystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class OpenAISpeechSubsystem extends Text2SpeechSubsystem {

    public static final SubsystemType<OpenAISpeechSubsystem> TYPE = SubsystemType
            .create(OpenAISpeechSubsystem::new, OpenAISpeechConfig.class);

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final class OpenAISpeechConfig {
        public String apiKey;
        public String voice = "echo";
    }

    private final AIManager aiManager;
    private final OpenAISpeechConfig config;
    private final SimpleOpenAISpeechClient client;

    public OpenAISpeechSubsystem(AIManager aiManager, OpenAISpeechConfig config) {
        this.aiManager = aiManager;
        this.config = config;
        this.client = new SimpleOpenAISpeechClient(config.apiKey);
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    private final Path debugOutput = Paths.get("debugAudio.wav");

    @Override
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message, String prevText) {
        var req = new SimpleOpenAISpeechClient.SpeechRequest()
                .setInput(message)
                .setVoice(config.voice)
                .setModelId("tts-1")
                .setResponseFormat("wav");

        return client.send(req)
//                .thenApply(in -> MirrorInputStream.createDebugMirror(in, debugOutput))
                .thenApply(SpeechAudioManager::getAudioInputStream);
    }
}
