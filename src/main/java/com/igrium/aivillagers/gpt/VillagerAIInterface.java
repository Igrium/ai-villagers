package com.igrium.aivillagers.gpt;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.chat.Message;
import net.minecraft.entity.Entity;

import java.util.List;

/**
 * Function implementations for what villagers can do.
 */
public interface VillagerAIInterface {
    public List<ChatMessage> getChatHistory(Entity villager);
    public List<Message> getMessageHistory(Entity villager);

    public String offerTrade(Entity villager, String sellItem, int sellAmount, String buyItem, int buyAmount);
}
