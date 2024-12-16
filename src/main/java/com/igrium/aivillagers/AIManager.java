package com.igrium.aivillagers;

import java.util.WeakHashMap;

import com.igrium.aivillagers.subsystems.AISubsystem;
import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemTypes;
import com.igrium.aivillagers.subsystems.impl.ChatListeningSubsystem;
import com.igrium.aivillagers.subsystems.impl.ChatSpeechSubsystem;
import com.igrium.aivillagers.subsystems.impl.DummyAISubsystem;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class AIManager {

    private ListeningSubsystem listeningSubsystem = new ChatListeningSubsystem(this);
    private AISubsystem aiSubsystem = new DummyAISubsystem();
    private SpeechSubsystem speechSubsystem = new ChatSpeechSubsystem(this, new ChatSpeechSubsystem.Config());

    public ListeningSubsystem getListeningSubsystem() {
        return listeningSubsystem;
    }

    public AISubsystem getAiSubsystem() {
        return aiSubsystem;
    }

    public SpeechSubsystem getSpeechSubsystem() {
        return speechSubsystem;
    }

    public AIManager(AIVillagersConfig config) {
        listeningSubsystem = SubsystemTypes.getListening(this, config.listening);
        aiSubsystem = SubsystemTypes.getAI(this, config.ai);
        speechSubsystem = SubsystemTypes.getSpeech(this, config.speech);
    }

    public void tick(MinecraftServer server) {
        listeningSubsystem.tick(server);
        aiSubsystem.tick(server);
        speechSubsystem.tick(server);
    }

    private final WeakHashMap<Entity, VillagerAIHandleImpl> aiHandles = new WeakHashMap<>();

    /**
     * Get or create an AI handle for a given villager.
     * @param villager Villager entity.
     * @return The AI handle.
     */
    public VillagerAIHandle getAIHandle(Entity villager) {
        return aiHandles.computeIfAbsent(villager, VillagerAIHandleImpl::new);
    }

    private class VillagerAIHandleImpl implements VillagerAIHandle {

        final Entity entity;

        VillagerAIHandleImpl(Entity entity) {
            this.entity = entity;
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        @Override
        public void onSpokenTo(ServerPlayerEntity player, String message) {
            aiSubsystem.onSpokenTo(this, player, message);
        }

        
        @Override
        public void onDamage(DamageSource source, float amount) {
            aiSubsystem.onDamage(this, source, amount);
        }

        @Override
        public void speak(String message) {
            speechSubsystem.speak(entity, message);
        }

    }
}
