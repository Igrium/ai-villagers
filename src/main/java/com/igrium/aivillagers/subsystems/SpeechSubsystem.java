package com.igrium.aivillagers.subsystems;

import net.minecraft.entity.Entity;

/**
 * The subsystem that allows villagers to speak using text-to-speech.
 */
public interface SpeechSubsystem extends Subsystem {
    /**
     * Force a villager to speak.
     * @param entity The villager entity.
     * @param message The message.
     */
    public void speak(Entity entity, String message);
}
