package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.subsystems.ListeningSubsystem;
import com.igrium.aivillagers.voice.VoiceCapture;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.util.function.Consumer;

/**
 * A listening subsystem that calls into OpenAI's Whisper.
 */
public class WhisperListeningSubsystem implements ListeningSubsystem {
    @Override
    public void enableListening(Entity villager) {

    }

    @Override
    public void disableListening(Entity villager) {

    }

    @Override
    public void onMicPacket(ServerPlayerEntity player, short[] data) {

    }

    @Override
    public void tick(MinecraftServer server) {
        ListeningSubsystem.super.tick(server);
    }

}
