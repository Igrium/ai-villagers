package com.igrium.aivillagers;

import com.igrium.aivillagers.util.AudioUtils;
import com.igrium.aivillagers.voice.VoiceCapture;
import com.igrium.aivillagers.voice.VoiceListener;
import com.igrium.aivillagers.voice.VoiceWriter;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class SpeechVCPlugin implements VoicechatPlugin {

    public static final String VILLAGER_CATEGORY = "villager";

    private static SpeechVCPlugin instance;

    public static SpeechVCPlugin getInstance() {
        return instance;
    }

    @Override
    public String getPluginId() {
        return "ai-villagers";
    }

    private VoicechatApi api;
    private VoicechatServerApi serverApi;

    private SpeechAudioManager audioManager;

    @Nullable
    private OpusDecoder decoder;

    @Override
    public void initialize(VoicechatApi api) {
        instance = this;
        this.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        ServerTickEvents.START_SERVER_TICK.register(this::tick);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        audioManager = new SpeechAudioManager(this);

        serverApi.volumeCategoryBuilder()
                .setId(VILLAGER_CATEGORY)
                .setName("Villagers")
                .setDescription("Villager text-to-speech")
                .build();
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
    }

    /**
     * Represents a consumer that gets notified when a player starts streaming. Only one of these may exist at a time,
     * and <b>it must consume the entire input stream or else the voice chat system will freeze.</b>
     */
    public interface MicStreamConsumer {
        void onPlayerSpeaking(SpeechVCPlugin plugin, ServerPlayerEntity player, InputStream in);
    }

    final AtomicInteger testFileIndex = new AtomicInteger();

    private void consumeDefaultMicStream(SpeechVCPlugin plugin, ServerPlayerEntity player, InputStream in) {
        AIVillagers.LOGGER.info("{} is talking.", player.getName().getString());
        Path testFile = FabricLoader.getInstance().getGameDir().resolve("testaudio/" + testFileIndex.getAndIncrement() + ".mp3");
        try {
            Files.createDirectories(testFile.getParent());
            try(var out = new BufferedOutputStream(Files.newOutputStream(testFile))) {
                in.transferTo(out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AIVillagers.LOGGER.info("Saved snippet to {}", testFile);

    }

    private final Map<ServerPlayerEntity, VoiceListener> voiceListeners = Collections.synchronizedMap(new WeakHashMap<>());

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null)
            return;

        if (!(senderConnection.getPlayer().getPlayer() instanceof ServerPlayerEntity player)) {
            AIVillagers.LOGGER.warn("Received microphone packets from non-player.");
            return;
        }

        if (decoder == null) {
            decoder = event.getVoicechat().createDecoder();
        }

        byte[] opus = event.getPacket().getOpusEncodedData();
        short[] decoded;
        if (opus.length != 0) {
            assert decoder != null;
            // TODO: do we need one decoder per player?
            decoded = decoder.decode(event.getPacket().getOpusEncodedData());
        } else {
            decoded = new short[0];
        }
        AIVillagers.getInstance().getAiManager().getListeningSubsystem().onMicPacket(player, decoded);

        var listener = voiceListeners.computeIfAbsent(player, p -> new VoiceListener(api, 300,
                in -> Util.getIoWorkerExecutor().execute(() -> {
                    consumeDefaultMicStream(this, player, in);
                })));

        listener.consumeVoicePacket(decoded);
    }

    int tickNum = 0;
    private void tick(MinecraftServer server) {
        if (tickNum % 2 == 0) {
            Util.getMainWorkerExecutor().execute(() -> {
                for (var l : voiceListeners.values()) {
                    l.tick();
                }
            });

        }
        tickNum++;
    }

    public VoicechatApi getApi() {
        return api;
    }

    public VoicechatServerApi getServerApi() {
        return serverApi;
    }

    public SpeechAudioManager getAudioManager() {
        return audioManager;
    }


}
