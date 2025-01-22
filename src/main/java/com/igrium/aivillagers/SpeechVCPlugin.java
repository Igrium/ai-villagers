package com.igrium.aivillagers;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

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

    private void consumeDefaultMicStream(SpeechVCPlugin plugin, ServerPlayerEntity player, InputStream in) {
        try {
            in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MicStreamConsumer micStreamConsumer = this::consumeDefaultMicStream;

    public void setMicStreamConsumer(MicStreamConsumer micStreamConsumer) {
        this.micStreamConsumer = micStreamConsumer;
    }

    private static final int MP3_BITRATE = 320;

    private class VoiceWriterState {
        VoiceWriter writer;

        synchronized void onPacket(ServerPlayerEntity player, short[] packet) {
            if (writer == null) {
                var in = new PipedInputStream();
                PipedOutputStream out;
                try {
                    out = new PipedOutputStream(in);
                } catch (IOException e) {
                    // Shouldn't ever happen
                    throw new RuntimeException(e);
                }
                Mp3Encoder encoder = api.createMp3Encoder(SpeechAudioManager.FORMAT, MP3_BITRATE, 6, out);

                writer = new VoiceWriter(encoder, 200);
                writer.setOnClose(this::onWriterClose);
                AIVillagers.LOGGER.info("{} is talking.", player.getName().getString());
                Util.getMainWorkerExecutor()
                        .execute(() -> micStreamConsumer.onPlayerSpeaking(SpeechVCPlugin.this, player, in));

            }
            writer.addAudio(packet);
        }

        int tickNum = 0;

        synchronized void tick() {
            if (writer != null && tickNum % 2 == 0) {
                try {
                    writer.flush(true);
                } catch (IOException e) {
                    AIVillagers.LOGGER.error("Error processing voice data:", e);
                    onWriterClose();
                }
            }
            tickNum++;
        }

        synchronized void onWriterClose() {
            this.writer = null;
            AIVillagers.LOGGER.info("Player stopped talking");
        }
    }

    private final Map<ServerPlayerEntity, VoiceWriterState> voiceWriters = Collections.synchronizedMap(new WeakHashMap<>());

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


//        AIVillagers.LOGGER.info("Received packet from player {}", player.getName().getString());
//
        decoder.resetState();
        short[] decoded = decoder.decode(event.getPacket().getOpusEncodedData());
        voiceWriters.computeIfAbsent(player, p -> new VoiceWriterState()).onPacket(player, decoded);
    }

    private void tick(MinecraftServer server) {
        Util.getMainWorkerExecutor().execute(() -> {
            for (var writer : voiceWriters.values()) {
                writer.tick();
            }
        });
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
