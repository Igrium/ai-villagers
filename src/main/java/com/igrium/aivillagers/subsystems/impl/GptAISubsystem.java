package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.gpt.GptClient;
import com.igrium.aivillagers.gpt.VillagerAIInterface;
import com.igrium.aivillagers.gpt.VillagerAIInterfaceImpl;
import com.igrium.aivillagers.subsystems.AISubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GptAISubsystem implements AISubsystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(GptAISubsystem.class);

    public static final SubsystemType<GptAISubsystem> TYPE = SubsystemType.create(GptAISubsystem::new, GptAIConfig.class);

    public static final class GptAIConfig {
        public String apiKey;
        public String model = "gpt-4o";
    }

    private final AIManager aiManager;
    private final GptAIConfig config;
    private final GptClient client;

    public GptAISubsystem(AIManager aiManager, GptAIConfig config) {
        this.aiManager = aiManager;
        this.config = config;

        VillagerAIInterface aiInterface = new VillagerAIInterfaceImpl();
        this.client = new GptClient(aiInterface, this, config.apiKey);
    }

    public GptAIConfig getConfig() {
        return config;
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void onSpokenTo(Entity villager, ServerPlayerEntity player, String message) {
        client.sendMessage(villager, player, message).whenComplete((msg, e) -> {
           if (e != null) {
               LOGGER.error("Error getting message from OpenAI", e);
           } else {
               LOGGER.info(msg);
           }
        });
    }

    /**
     * Called when the language model has determined it's time for the villager to speak.
     *
     * @param villager The villager in question.
     * @param target   Who they're speaking to.
     * @param message  What to say.
     */
    public void doSpeak(Entity villager, Entity target, String message) {
        aiManager.getSpeechSubsystem().speak(villager, message);
    }

    @Override
    public void onDamage(Entity villager, DamageSource source, float amount) {

    }
}
