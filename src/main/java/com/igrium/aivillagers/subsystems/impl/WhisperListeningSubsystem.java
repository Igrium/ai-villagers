package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.SpeechVCPlugin;
import com.igrium.aivillagers.listening.WhisperClient;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.util.AudioUtils;
import com.igrium.aivillagers.voice.CallbackEncoder;
import com.igrium.aivillagers.voice.GatedEncoder;
import com.igrium.aivillagers.voice.VoiceListener;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * A listening subsystem that calls into OpenAI's Whisper.
 */
public class WhisperListeningSubsystem implements ListeningSubsystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhisperListeningSubsystem.class);

    public static final SubsystemType<WhisperListeningSubsystem> TYPE = SubsystemType.create(WhisperListeningSubsystem::new, WhisperConfig.class);

    public static final class WhisperConfig {
        public String apiKey;
    }

    private final WhisperClient whisperClient;

    public WhisperListeningSubsystem(AIManager aiManager, WhisperConfig config) {
        whisperClient = new WhisperClient(config.apiKey, this::onProcessSpeech);
    }

    private final Map<ServerPlayerEntity, VoiceListener> voiceListeners = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void enableListening(Entity villager) {

    }

    @Override
    public void disableListening(Entity villager) {

    }

    @Override
    public void onMicPacket(SpeechVCPlugin plugin, ServerPlayerEntity player, short[] data) {
        var listener = voiceListeners.computeIfAbsent(player, p ->
                new VoiceListener.Builder(plugin.getApi(), api -> onStartedTalking(api, p))
                        .build());

        listener.consumeVoicePacket(data);
    }

    protected Mp3Encoder onStartedTalking(VoicechatApi api, ServerPlayerEntity player) {
        AIVillagers.LOGGER.info("{} is talking.", player.getName().getString());
        return cancelingThresholdEncoder(api, whisperClient.handleVoiceCapture(player));
//        return debugThresholdEncoder(api, () -> whisperClient.handleVoiceCapture(player).getStream());
    }

    protected void onProcessSpeech(ServerPlayerEntity player, String message) {
        LoggerFactory.getLogger(getClass()).info("{}: {}", player.getName().getString(), message);
    }

    private int tickNum = 0;

    @Override
    public void tick(MinecraftServer server) {

        if (tickNum % 2 == 0) {
            Util.getMainWorkerExecutor().execute(() -> {
                for (var l : voiceListeners.values()) {
                    l.tick();
                }
            });
        }
        tickNum++;
    }


    private Mp3Encoder debugThresholdEncoder(VoicechatApi api, Supplier<OutputStream> out) {
        return new GatedEncoder(() -> AudioUtils.createGenericMp3Encoder(api, out.get()), (buffer, sum) -> {
            double rms = Math.sqrt((double) sum / buffer.size());
            LOGGER.info("Average RMS: {}", rms);
            return rms > 100;
        });
    }

    private Mp3Encoder cancelingThresholdEncoder(VoicechatApi api, WhisperClient.RequestHandle handle) {
        handle.setUseResult(false);
        int threshold = 100;
        return new CallbackEncoder(AudioUtils.createGenericMp3Encoder(api, handle.getStream()), (samples, buffer, sum) -> {
            if (handle.getUseResult()) return;
            if (sum / buffer.size() > threshold * threshold) {
                LOGGER.info("Encoder hit threshold!");
                handle.setUseResult(true);
            }
        });
    }
}
