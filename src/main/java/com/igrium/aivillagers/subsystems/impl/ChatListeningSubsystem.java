package com.igrium.aivillagers.subsystems.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.util.PlayerUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * An implementation of the listening subsystem that uses the in-game chat.
 */
public class ChatListeningSubsystem implements ListeningSubsystem {

    public static final SubsystemType<ChatListeningSubsystem> TYPE = SubsystemType.create(ChatListeningSubsystem::new);
    
    private static record CachedMessage(String content, ServerPlayerEntity sender) {};

    /**
     * Allow individual instances to listen to events with this hack.
     */
    private static final Set<ChatListeningSubsystem> instances = Collections.newSetFromMap(new WeakHashMap<>());

    private final Set<Entity> listeningEntities = Collections.newSetFromMap(new WeakHashMap<>());
    private final Queue<CachedMessage> messageCache = new LinkedList<>();

    private final AIManager aiManager;
    
    public ChatListeningSubsystem(AIManager aiManager) {
        this.aiManager = aiManager;
        instances.add(this);
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void tick(MinecraftServer server) {
        CachedMessage message;
        while ((message = messageCache.poll()) != null) {
            handleMessage(message);
        }
    }

    private void handleMessage(CachedMessage message) {
        HitResult target = PlayerUtils.findCrosshairTarget(message.sender(), 6, 6);
        if (target instanceof EntityHitResult entHit) {
            Entity ent = entHit.getEntity();
            if (ent instanceof VillagerEntity) {
                aiManager.getAiSubsystem().onSpokenTo(ent, message.sender(), message.content());
            }
        }
    }

    @Override
    public void enableListening(Entity villager) {
        listeningEntities.add(villager);
    }

    @Override
    public void disableListening(Entity villager) {
        listeningEntities.remove(villager);
    }

    @Override
    public void onMicPacket(ServerPlayerEntity player, short[] data) {

    }

    public static void onChatMessage(SignedMessage message, ServerPlayerEntity sender, Parameters params) {
        for (var instance : instances) {
            instance.messageCache.add(new CachedMessage(message.getContent().getString(), sender));
        }
    }
}
