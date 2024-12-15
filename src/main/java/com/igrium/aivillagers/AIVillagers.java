package com.igrium.aivillagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.subsystems.impl.ChatListeningSubsystem;

public class AIVillagers implements ModInitializer {
    public static final String MOD_ID = "ai-villagers";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private AIManager aiManager;

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void onInitialize() {
        aiManager = new AIManager();
        ServerMessageEvents.CHAT_MESSAGE.register(ChatListeningSubsystem::onChatMessage);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((LivingEntity entity, DamageSource source, float baseDamageTaken,
                float damageTaken, boolean blocked) -> {
            if (entity instanceof VillagerEntity && damageTaken > 0 && !blocked) {
                aiManager.getAIHandle(entity).onDamage(source, damageTaken);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(aiManager::tick);
        

    }
}