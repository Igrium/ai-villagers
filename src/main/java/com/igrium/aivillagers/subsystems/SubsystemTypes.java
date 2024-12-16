package com.igrium.aivillagers.subsystems;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.impl.ChatListeningSubsystem;
import com.igrium.aivillagers.subsystems.impl.ChatSpeechSubsystem;
import com.igrium.aivillagers.subsystems.impl.DummyAISubsystem;

public class SubsystemTypes {
    public static final BiMap<String, SubsystemType<? extends ListeningSubsystem>> LISTENING_REGISTRY = HashBiMap.create();
    public static final BiMap<String, SubsystemType<? extends AISubsystem>> AI_REGISTRY = HashBiMap.create();
    public static final BiMap<String, SubsystemType<? extends SpeechSubsystem>> SPEECH_REGISTRY = HashBiMap.create();

    public static ListeningSubsystem getListening(AIManager aiManager, JsonObject config) throws IllegalArgumentException  {
        return getSubsystem(aiManager, config, LISTENING_REGISTRY);
    }

    public static AISubsystem getAI(AIManager aiManager, JsonObject config) throws IllegalArgumentException {
        return getSubsystem(aiManager, config, AI_REGISTRY);
    }

    public static SpeechSubsystem getSpeech(AIManager aiManager, JsonObject config) throws IllegalArgumentException {
        return getSubsystem(aiManager, config, SPEECH_REGISTRY);
    }

    private static <T> T getSubsystem(AIManager aiManager, JsonObject config,
            BiMap<String, ? extends SubsystemType<? extends T>> registry) throws IllegalArgumentException {
        String typeName = config.get("type").getAsString();
        var type = registry.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Unknown subsystem type: " + typeName);
        }
        return type.create(aiManager, config);
    }

    public static void registerDefaults() {
        LISTENING_REGISTRY.put("chat", ChatListeningSubsystem.TYPE);

        AI_REGISTRY.put("dummy", DummyAISubsystem.TYPE);

        SPEECH_REGISTRY.put("chat", ChatSpeechSubsystem.TYPE);
    }
}
