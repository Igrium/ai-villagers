package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.VillagerAIHandle;
import com.igrium.aivillagers.subsystems.AISubsystem;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * An AI subsystem that simply repeats what it's told.
 */
public class DummyAISubsystem implements AISubsystem {

    @Override
    public void onSpokenTo(VillagerAIHandle villager, ServerPlayerEntity player, String message) {
        villager.speak(message);
    }
    
    @Override
    public void onDamage(VillagerAIHandle villager, DamageSource source, float amount) {
        villager.speak("Ouch. I took " + amount / 2f + " hearts.");
    }
}
