package com.igrium.aivillagers;

import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

import com.igrium.aivillagers.chat.ChatHistoryComponent;

import net.minecraft.entity.passive.MerchantEntity;

public class AIVillagersComponents implements EntityComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(MerchantEntity.class, ChatHistoryComponent.KEY, ChatHistoryComponent::new);
    }
    
}
