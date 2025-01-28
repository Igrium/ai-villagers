package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.SpeechVCPlugin;
import com.igrium.aivillagers.listening.ConversationTracker;
import com.igrium.aivillagers.listening.PlayerIntentTracker;
import com.igrium.aivillagers.listening.WhisperClient;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.util.AudioUtils;
import com.igrium.aivillagers.util.FutureList;
import com.igrium.aivillagers.voice.CallbackEncoder;
import com.igrium.aivillagers.voice.VoiceListener;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A listening subsystem that calls into OpenAI's Whisper.
 */
public class WhisperListeningSubsystem implements ListeningSubsystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhisperListeningSubsystem.class);

    public static final SubsystemType<WhisperListeningSubsystem> TYPE = SubsystemType.create(WhisperListeningSubsystem::new, WhisperConfig.class);

    public static final class WhisperConfig {
        public String apiKey;
    }

    private final AIManager aiManager;
    private final WhisperClient whisperClient;

    public WhisperListeningSubsystem(AIManager aiManager, WhisperConfig config) {
        this.aiManager = aiManager;
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

    private final Map<ServerPlayerEntity, FutureList<String>> futureLists = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * <b>Only access player trackers on the server thread!!</b>
     */
    private final Map<ServerPlayerEntity, ConversationTracker> convoTrackers = new WeakHashMap<>();

    protected Mp3Encoder onStartedTalking(VoicechatApi api, ServerPlayerEntity player) {
        var writer = whisperClient.sendTranscriptionRequest();

        // Create a new intent tracker if we don't already have one.
        Objects.requireNonNull(player.getServer()).execute(() -> {
            convoTrackers.computeIfAbsent(player, ConversationTracker::new).startTalking(false);
        });

        var futureList = futureLists.computeIfAbsent(player, p -> new FutureList<>(list -> {
            // Collect all segments and form into string
            StringBuilder builder = new StringBuilder();
            for (var e : list) {
                if (e.left().isPresent() && !e.left().get().isBlank()) {
                    builder.append(e.left().get()).append(' ');
                }
                if (e.right().isPresent()) {
                    LOGGER.error("Error receiving text data from OpenAI:", e.right().get());
                }
            }
            onProcessSpeech(p, builder.toString());
        }));

        AIVillagers.LOGGER.info("{} is talking.", player.getName().getString());

        MutableBoolean shouldCancel = new MutableBoolean(true);
        int threshold = 100;

        // Every mic packet, we check that the RMS is above the threshold and permit future to complete.
        Mp3Encoder encoder = new CallbackEncoder(AudioUtils.createGenericMp3Encoder(api, writer.getOutput()), (samples, buffer, sum) -> {
            if (shouldCancel.isFalse()) return;
            if (sum / buffer.size() > threshold * threshold) {
                shouldCancel.setFalse();
            }
        });

        // If we never passed the RMS threshold, drop the response when it comes back.
        futureList.submit(writer.getFuture().thenApplyAsync(res -> shouldCancel.isTrue() ? "" : res));

        return encoder;
    }

    /**
     * Called when the queue of transcription jobs is emptied for a player,
     * and they all have been merged into a single message.
     *
     * @param player  The player in question.
     * @param message The combined message from the player.
     */
    protected void onProcessSpeech(ServerPlayerEntity player, String message) {
        if (message.isBlank()) return;
        LOGGER.info("{}: {}", player.getName().getString(), message);

        // Intent tracker should only be accessed on the server thread
        Objects.requireNonNull(player.getServer()).execute(() -> {
            ConversationTracker tracker = convoTrackers.get(player);
            if (tracker == null) {
                LOGGER.warn("Player didn't have a conversation tracker when onProcessSpeech was called!");
                return;
            }

            Entity target = tracker.stopTalking();
            if (target == null) {
                LOGGER.warn("Player didn't have likely speech target");
                return;
            }

            aiManager.getAiSubsystem().onSpokenTo(target, player, message);
        });

//        HitResult target = PlayerUtils.findCrosshairTarget(player, 6, 6);
//        if (target instanceof EntityHitResult entHit) {
//            Entity ent = entHit.getEntity();
//            if (ent instanceof VillagerEntity) {
//                aiManager.getAiSubsystem().onSpokenTo(ent, player, message);
//            }
//        }
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

        for (var tracker : convoTrackers.values()) {
            tracker.tick();
        }
    }

}
