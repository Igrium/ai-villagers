package com.igrium.aivillagers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A "handle" that wraps AI manager calls for a particular villager.
 */
public interface VillagerAIHandle {

    /**
     * Get the villager entity this handle belongs to.
     * @return Villager entity.
     */
    public Entity getEntity();

    /**
     * Called when a player attempts to speak to this villager.
     * @param player The player that spoke.
     * @param message What was said.
     */
    public void onSpokenTo(ServerPlayerEntity player, String message);

    public void onDamage(DamageSource source, float amount);

    /**
     * Force the villager to speak using text-to-speech.
     * @param message Message to speak.
     */
    public void speak(String message);
}
