package com.igrium.aivillagers.subsystems;

import net.minecraft.entity.Entity;

/**
 * Responsible for listening to what players are saying and calling the other
 * subsystems.
 */
public interface ListeningSubsystem extends Subsystem {
    public void enableListening(Entity villager);
    public void disableListening(Entity villager);
}
