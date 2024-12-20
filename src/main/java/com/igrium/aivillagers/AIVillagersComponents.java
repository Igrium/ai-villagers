package com.igrium.aivillagers;

import com.igrium.aivillagers.util.VillagerCounterComponent;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

import com.igrium.aivillagers.chat.ChatHistoryComponent;

import net.minecraft.entity.passive.MerchantEntity;
import org.ladysnake.cca.api.v3.level.LevelComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.level.LevelComponentInitializer;

public class AIVillagersComponents implements EntityComponentInitializer, LevelComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(MerchantEntity.class, ChatHistoryComponent.KEY, ChatHistoryComponent::new);
    }

    @Override
    public void registerLevelComponentFactories(LevelComponentFactoryRegistry registry) {
        registry.register(VillagerCounterComponent.KEY, VillagerCounterComponent::new);
    }
}
