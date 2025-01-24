package com.igrium.aivillagers;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private VoicechatServerApi serverApi;

    private SpeechAudioManager audioManager;

    @Override
    public void initialize(VoicechatApi api) {
        instance = this;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
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

    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        for (var decoder : decoders.values()) {
            decoder.close();
        }
        decoders.clear();
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        var decoder = decoders.remove(event.getPlayerUuid());
        if (decoder != null) {
            decoder.close();
        }
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null)
            return;

        if (!(senderConnection.getPlayer().getPlayer() instanceof ServerPlayerEntity player)) {
            AIVillagers.LOGGER.warn("Received microphone packets from non-player.");
            return;
        }

        byte[] opus = event.getPacket().getOpusEncodedData();
        short[] decoded;
        if (opus.length != 0) {
            var decoder = decoders.computeIfAbsent(player.getUuid(), p -> serverApi.createDecoder());
            decoded = decoder.decode(event.getPacket().getOpusEncodedData());
        } else {
            decoded = new short[0];
        }
        AIVillagers.getInstance().getAiManager().getListeningSubsystem().onMicPacket(this, player, decoded);
    }

    public VoicechatServerApi getApi() {
        return serverApi;
    }

    public VoicechatServerApi getServerApi() {
        return serverApi;
    }

    public SpeechAudioManager getAudioManager() {
        return audioManager;
    }


}
