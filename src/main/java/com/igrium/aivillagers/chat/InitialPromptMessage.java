package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.village.VillagerProfession;

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
        PromptManager promptManager = PromptManager.getInstance();
        String prompt = promptManager.getBasePrompt() + " "
                + promptManager.getProfessionPrompt(getOriginalProfession(history), false);

        return ChatMessage.Companion.System(promptManager.applyTemplate(history, prompt), null);
    }

    private VillagerProfession getOriginalProfession(ChatHistoryComponent history) {
        return history.getOriginalProfession().orElse(VillagerProfession.NONE);
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
