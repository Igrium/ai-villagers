package com.igrium.aivillagers;

import com.igrium.aivillagers.subsystems.*;

import net.minecraft.server.MinecraftServer;

import java.util.List;

public class AIManager {

    private final ListeningSubsystem listeningSubsystem;
    private final AISubsystem aiSubsystem;
    private final SpeechSubsystem speechSubsystem;

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

    public void onServerStart(MinecraftServer server) {
        listeningSubsystem.onServerStart(server);
        aiSubsystem.onServerStart(server);
        speechSubsystem.onServerStart(server);
    }

    public void onServerStop(MinecraftServer server) {
        listeningSubsystem.onServerStop(server);
        aiSubsystem.onServerStop(server);
        speechSubsystem.onServerStop(server);
    }
}
