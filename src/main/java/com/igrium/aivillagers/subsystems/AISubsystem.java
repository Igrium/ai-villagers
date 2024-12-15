package com.igrium.aivillagers.subsystems;

import com.igrium.aivillagers.VillagerAIHandle;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface AISubsystem extends Subsystem {

    /**
     * Called when a villager has been spoken to by a player.
     * 
     * @param villager Villager that was spoken to.
     * @param player   The player that spoke.
     * @param message  What was said
     */
    public void onSpokenTo(VillagerAIHandle villager, ServerPlayerEntity player, String message);

    /**
     * Called when a villager has taken damage.
     * 
     * @param villager Villager.
     * @param source   The damage source.
     * @param amount   The amount of damage.
     */
    public void onDamage(VillagerAIHandle villager, DamageSource source, float amount);
}
