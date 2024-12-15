package com.igrium.aivillagers.subsystems;

import net.minecraft.server.MinecraftServer;

public interface Subsystem {
    public default void tick(MinecraftServer server) {
    };
}
