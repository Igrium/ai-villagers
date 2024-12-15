package com.igrium.aivillagers.subsystems.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;

import net.minecraft.entity.Entity;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * An implementation of the listening subsystem that uses the in-game chat.
 */
public class ChatListeningSubsystem implements ListeningSubsystem {

    private static record CachedMessage(String message, ServerPlayerEntity sender) {};

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
    }

    @Override
    public void enableListening(Entity villager) {
        listeningEntities.add(villager);
    }

    @Override
    public void disableListening(Entity villager) {
        listeningEntities.remove(villager);
    }

    public static void onChatMessage(SignedMessage message, ServerPlayerEntity sender, Parameters params) {
        for (var instance : instances) {
            instance.messageCache.add(new CachedMessage(message.getContent().getString(), sender));
        }
    }
}
