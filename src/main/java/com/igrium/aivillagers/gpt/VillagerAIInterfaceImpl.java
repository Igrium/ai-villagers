package com.igrium.aivillagers.gpt;

import com.aallam.openai.api.chat.ChatMessage;
import net.minecraft.entity.Entity;

import java.util.List;

public class VillagerAIInterfaceImpl implements VillagerAIInterface {
    @Override
    public List<ChatMessage> getHistory(Entity villager) {
        return ChatHistoryComponent.get(villager).getMessageHistory();
    }
}
