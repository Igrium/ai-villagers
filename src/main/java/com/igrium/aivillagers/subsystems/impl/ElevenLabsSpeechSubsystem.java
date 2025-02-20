package com.igrium.aivillagers.subsystems.impl;

import com.google.gson.annotations.SerializedName;
import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.com.igrium.elevenlabs.ElevenLabsWSConnection;
import com.igrium.aivillagers.speech.ElevenLabsSpeechClient;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.elevenlabs.requests.OutputFormat;
import com.igrium.elevenlabs.requests.VoiceSettings;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.BufferedInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ElevenLabsSpeechSubsystem extends Text2SpeechSubsystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElevenLabsSpeechSubsystem.class);
    public static SubsystemType<ElevenLabsSpeechSubsystem> TYPE = SubsystemType
            .create(ElevenLabsSpeechSubsystem::new, ElevenLabsSpeechSubsystemConfig.class);

    public static final class ElevenLabsSpeechSubsystemConfig {
        public String apiKey;
        public String voiceId;
        public @Nullable String baseURL;
        public @Nullable String modelId = "eleven_multilingual_v2";
        public @Nullable ElevenLabsVoiceSettings voiceSettings;
    }

    /**
     * This is really dumb that I need a dedicated representation of this for config.
     */
    public static final class ElevenLabsVoiceSettings {
        @Nullable
        @SerializedName("stability")
        public Double stability;

        @Nullable
        @SerializedName("similarity_boost")
        public Double similarityBoost;

        @Nullable
        @SerializedName("style")
        public Double style;

        @Nullable
        @SerializedName("use_speaker_boost")
        public Boolean useSpeakerBoost;

        public VoiceSettings toVoiceSettings() {
            return new VoiceSettings(stability, similarityBoost, style, useSpeakerBoost);
        }
    }

    private final AIManager aiManager;

    private final ElevenLabsSpeechClient client;

    @Nullable
    private final VoiceSettings voiceSettings;

    @Nullable
    private String modelId;

    public ElevenLabsSpeechSubsystem(AIManager aiManager, ElevenLabsSpeechSubsystemConfig config) {
        this.aiManager = aiManager;
        client = new ElevenLabsSpeechClient(
                config.apiKey, config.voiceId, config.baseURL != null ? config.baseURL : "https://api.elevenlabs.io");

        if (config.voiceSettings != null) {
            this.voiceSettings = config.voiceSettings.toVoiceSettings();
        } else {
            this.voiceSettings = null;
        }
        this.modelId = config.modelId;
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    private static final OutputFormat FORMAT = OutputFormat.PCM_22050;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(22500, 16, 1, true, false);

    private final Path debugOutput = Paths.get("debugAudio");

    @Override
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message, String prevText) {
        return client.streamTTS(message)
//                .thenApply(in -> MirrorInputStream.createDebugMirror(in, debugOutput))
                .thenApply(in -> new AudioInputStream(new BufferedInputStream(in), AUDIO_FORMAT, Integer.MAX_VALUE));
    }

    private void playErrorSound(Entity entity) {
        SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
        if (audioManager == null) {
            LOGGER.error("Simple VC was not setup properly; audio will not play.");
            return;
        }
        audioManager.playAudioFromEntity(entity, AIVillagers.getInstance().getErrorSound().getInputStream());
    }

    @Override
    public SpeechStream openSpeechStream(Entity entity) {
        ElevenLabsSpeechStream stream = new ElevenLabsSpeechStream();
        client.openWSConnection(FORMAT, voiceSettings, modelId).thenAccept(ws -> {
            stream.setConnection(ws);

            AudioInputStream in = new AudioInputStream(ws.getInputStream(), AUDIO_FORMAT, Integer.MAX_VALUE);
            SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
            if (audioManager == null) {
                LOGGER.error("Simple VC was not setup properly; audio will not play.");
                return;
            }
            ws.setOnError((e) -> {
                LOGGER.error("Error communicating with ElevenLabs: ", e);
                playErrorSound(entity);
            });

            // Wait until we have audio to try to play it.
            ws.getOnReceiveFirstData().thenRun(() -> audioManager.playAudioFromEntity(entity, in));

        }).exceptionally(e -> {
            LOGGER.error("Error establishing connection with Eleven Labs: ", e);
            playErrorSound(entity);
            return null;
        });
        return stream;
    }

    private static class ElevenLabsSpeechStream implements SpeechStream {

        private static final String SPLIT = "[.,?!;:—\\-\\[\\](){}]";

        @Nullable
        ElevenLabsWSConnection connection;

        final Queue<String> cache = new ConcurrentLinkedQueue<>();
        StringBuilder sentenceBuilder = new StringBuilder();

        public void setConnection(ElevenLabsWSConnection connection) {
            this.connection = connection;
            for (var str : cache) {
                connection.sendTTSText(str);
            }
        }

        @Override
        public synchronized void acceptToken(String token) {
            sentenceBuilder.append(token);
            if (token.matches(SPLIT)) {
                acceptSentence(sentenceBuilder.toString());
                sentenceBuilder = new StringBuilder();
            }
        }

        private void acceptSentence(String sentence) {
            if (connection != null)
                connection.sendTTSText(sentence);
            cache.add(sentence);
        }

        @Override
        public void close() {
            if (!sentenceBuilder.isEmpty()) {
                acceptSentence(sentenceBuilder.toString());
            }
            if (connection != null)
                connection.close();
        }
    }
}
