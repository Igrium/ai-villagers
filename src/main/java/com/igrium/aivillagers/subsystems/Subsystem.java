package com.igrium.aivillagers.subsystems;

import net.minecraft.server.MinecraftServer;

public interface Subsystem {
    default void tick(MinecraftServer server) {
    }

    default void onServerStart(MinecraftServer server) {
    }

    default void onServerStop(MinecraftServer server) {
    }
}
