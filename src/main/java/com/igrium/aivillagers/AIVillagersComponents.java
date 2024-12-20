package com.igrium.aivillagers;

import com.igrium.aivillagers.util.VillagerCounterComponent;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

import com.igrium.aivillagers.chat.ChatHistoryComponent;

import net.minecraft.entity.passive.MerchantEntity;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryV2;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;

public class AIVillagersComponents implements EntityComponentInitializer, ScoreboardComponentInitializer {

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerFor(MerchantEntity.class, ChatHistoryComponent.KEY, ChatHistoryComponent::new);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(VillagerCounterComponent.KEY, VillagerCounterComponent::new);
    }
}
