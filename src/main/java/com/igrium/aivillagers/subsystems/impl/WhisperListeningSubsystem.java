package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.SpeechVCPlugin;
import com.igrium.aivillagers.listening.WhisperClient;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.voice.VoiceListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A listening subsystem that calls into OpenAI's Whisper.
 */
public class WhisperListeningSubsystem implements ListeningSubsystem {

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
                new VoiceListener.Builder(plugin.getApi(), () -> onStartedTalking(p)).build());

        listener.consumeVoicePacket(data);
    }

    private final AtomicInteger testFileIndex = new AtomicInteger();

    protected OutputStream onStartedTalking(ServerPlayerEntity player) {
        AIVillagers.LOGGER.info("{} is talking.", player.getName().getString());
        return whisperClient.handleVoiceCapture(player);
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

}
