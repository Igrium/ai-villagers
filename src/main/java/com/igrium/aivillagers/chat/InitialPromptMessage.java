package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import net.minecraft.nbt.NbtCompound;

/**
 * Represents the initial prompt for the LLM. Automatically added at the beginning of the chat history for each villager.
 */
public class InitialPromptMessage implements Message {

    public static final MessageType<InitialPromptMessage> TYPE = new MessageType<>() {

        @Override
        public InitialPromptMessage fromNbt(NbtCompound nbt) {
            return new InitialPromptMessage();
        }
    };

    private static final String PROMPT = "You are a Minecraft villager like the ones from Villager News. Villagers speak casually, are somewhat stubborn, and charge exorbitant prices. Short phrases";

    @Override
    public ChatMessage toChatMessage(ChatHistoryComponent history) {
        return ChatMessage.Companion.System("You are a minecraft villager.", null);
    }

    @Override
    public NbtCompound toNbt() {
        return new NbtCompound();
    }

    @Override
    public MessageType<?> getType() {
        return TYPE;
    }
}
