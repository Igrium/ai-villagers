package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.village.VillagerProfession;

/**
 * Represents the initial prompt for the LLM. Automatically added at the beginning of the chat history for each villager.
 */
@Deprecated
public class InitialPromptMessage implements Message {

    public static final MessageType<InitialPromptMessage> TYPE = new MessageType<>() {

        @Override
        public InitialPromptMessage fromNbt(NbtCompound nbt) {
            return new InitialPromptMessage();
        }
    };

    @Override
    public ChatMessage toChatMessage(ChatHistoryComponent history) {
        PromptManager promptManager = PromptManager.getInstance();
        String prompt = promptManager.getBasePrompt();

        return ChatMessage.Companion.System(promptManager.applyTemplate(history, prompt), null);
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
