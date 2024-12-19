package com.igrium.aivillagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.subsystems.SubsystemTypes;
import com.igrium.aivillagers.subsystems.impl.ChatListeningSubsystem;
import com.igrium.openai.OpenAI;

public class AIVillagers implements ModInitializer {
    public static final String MOD_ID = "ai-villagers";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AIVillagers instance;

    public static AIVillagers getInstance() {
        return instance;
    }

    private AIManager aiManager;

    public AIManager getAiManager() {
        return aiManager;
    }

    private AIVillagersConfig config;

    public AIVillagersConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        instance = this;
        SubsystemTypes.registerDefaults();

        config = AIVillagersConfig.load(FabricLoader.getInstance().getConfigDir().resolve("ai_villagers.json"));
        try {
            aiManager = new AIManager(config);
        } catch (Exception e) {
            throw new CrashException(CrashReport.create(e, "Error initializing AI manager."));
        }
        

        ServerMessageEvents.CHAT_MESSAGE.register(ChatListeningSubsystem::onChatMessage);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((LivingEntity entity, DamageSource source, float baseDamageTaken,
                float damageTaken, boolean blocked) -> {
            if (entity instanceof VillagerEntity && damageTaken > 0 && !blocked) {
                aiManager.getAiSubsystem().onDamage(entity, source, damageTaken);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(aiManager::tick);
    }
}