package com.igrium.aivillagers;

import com.igrium.aivillagers.gpt.PromptManager;
import com.igrium.aivillagers.cmd.AICommand;
import com.igrium.aivillagers.util.ErrorSound;
import com.igrium.aivillagers.util.VillagerCounterComponent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.subsystems.SubsystemTypes;
import com.igrium.aivillagers.subsystems.impl.ChatListeningSubsystem;

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

    private PromptManager promptManager;
    public PromptManager getPromptManager() {
        return promptManager;
    }

    private final ErrorSound errorSound = new ErrorSound();

    public ErrorSound getErrorSound() {
        return errorSound;
    }

    @Override
    public void onInitialize() {
        instance = this;
        SubsystemTypes.registerDefaults();

        config = AIVillagersConfig.loadConfig();
        try {
            aiManager = new AIManager(config);
        } catch (Exception e) {
            throw new CrashException(CrashReport.create(e, "Error initializing AI manager."));
        }

        promptManager = new PromptManager();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(promptManager);

        CommandRegistrationCallback.EVENT.register(AICommand::register);

        ServerMessageEvents.CHAT_MESSAGE.register(ChatListeningSubsystem::onChatMessage);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((LivingEntity entity, DamageSource source, float baseDamageTaken,
                float damageTaken, boolean blocked) -> {
            if (entity instanceof VillagerEntity && damageTaken > 0 && !blocked) {
                aiManager.getAiSubsystem().onDamage(entity, source, damageTaken);
            }
        });

        // Villager counting
        ServerEntityEvents.ENTITY_LOAD.register((ent, world) -> {
            if (ent instanceof MerchantEntity && !ent.hasCustomName()) {
                VillagerCounterComponent counter = VillagerCounterComponent.get(world.getServer());
                ent.setCustomName(Text.literal(counter.getNextNameAndIncrement()));
                ent.setCustomNameVisible(false);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(aiManager::tick);
        ServerLifecycleEvents.SERVER_STARTING.register(aiManager::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(aiManager::onServerStop);

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(errorSound);
    }
}