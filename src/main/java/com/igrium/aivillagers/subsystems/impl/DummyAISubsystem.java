package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.AISubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * An AI subsystem that simply repeats what it's told.
 */
public class DummyAISubsystem implements AISubsystem {

    public static final SubsystemType<DummyAISubsystem> TYPE = SubsystemType.create(DummyAISubsystem::new);

    private final AIManager aiManager;

    public DummyAISubsystem(AIManager aiManager) {
        this.aiManager = aiManager;
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void onSpokenTo(Entity villager, ServerPlayerEntity player, String message) {
        aiManager.getSpeechSubsystem().speak(villager, message);
    }
    
    @Override
    public void onDamage(Entity villager, DamageSource source, float amount) {
        aiManager.getSpeechSubsystem().speak(villager, "Ouch. I took " + amount / 2f
                + " hearts. You're so mean.");
    }
}
