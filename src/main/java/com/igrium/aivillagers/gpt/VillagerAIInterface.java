package com.igrium.aivillagers.gpt;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.AIManager;
import net.minecraft.entity.Entity;

import java.util.List;

/**
 * Function implementations for what villagers can do.
 */
public interface VillagerAIInterface {
    public List<ChatMessage> getHistory(Entity villager);
}
