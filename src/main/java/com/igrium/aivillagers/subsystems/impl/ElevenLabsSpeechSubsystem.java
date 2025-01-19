package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.SpeechAudioManager;
import com.igrium.aivillagers.com.igrium.elevenlabs.ElevenLabsWSConnection;
import com.igrium.aivillagers.speech.ElevenLabsSpeechClient;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.elevenlabs.requests.OutputFormat;
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
import java.util.concurrent.CompletableFuture;

public class ElevenLabsSpeechSubsystem extends Text2SpeechSubsystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElevenLabsSpeechSubsystem.class);
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
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(22500, 16, 1, true, false);

    private final Path debugOutput = Paths.get("debugAudio");

    @Override
    protected CompletableFuture<AudioInputStream> doTextToSpeech(String message, String prevText) {
        return client.streamTTS(message)
//                .thenApply(in -> MirrorInputStream.createDebugMirror(in, debugOutput))
                .thenApply(in -> new AudioInputStream(new BufferedInputStream(in), AUDIO_FORMAT, Integer.MAX_VALUE));
    }

//    @Override
//    public SpeechStream openSpeechStream(Entity entity) {
//        ElevenLabsSpeechStream stream = new ElevenLabsSpeechStream();
//        client.openWSConnection(FORMAT).thenAccept(ws -> {
//            stream.setConnection(ws);
//
//            AudioInputStream in = new AudioInputStream(ws.getInputStream(), AUDIO_FORMAT, Integer.MAX_VALUE);
//            SpeechAudioManager audioManager = SpeechAudioManager.getInstance();
//            if (audioManager == null) {
//                LOGGER.error("Simple VC was not setup properly; audio will not play.");
//                return;
//            }
//
//            audioManager.playAudioFromEntity(entity, in);
//        }).exceptionally(e -> {
//            LOGGER.error("Error establishing connection with Eleven Labs: ", e);
//            return null;
//        });
//        return stream;
//    }

    private static class ElevenLabsSpeechStream implements SpeechStream {

        @Nullable
        ElevenLabsWSConnection connection;

        final List<String> cache = new LinkedList<>();

        public void setConnection(ElevenLabsWSConnection connection) {
            this.connection = connection;
            for (var str : cache) {
                connection.sendTTSText(str);
            }
        }

        @Override
        public void acceptToken(String token) {
            if (connection != null)
                connection.sendTTSText(token);
            cache.add(token);
        }

        @Override
        public void close() {
            if (connection != null)
                connection.close();
        }
    }
}
