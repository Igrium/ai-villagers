package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

public record ChangeProfessionMessage(VillagerProfession profession) implements Message {

    public static final MessageType<ChangeProfessionMessage> TYPE = new MessageType<ChangeProfessionMessage>() {
        @Override
        public ChangeProfessionMessage fromNbt(NbtCompound nbt) {
            var profession = Registries.VILLAGER_PROFESSION.get(
                    Identifier.tryParse(
                            nbt.getString("profession")));
            return new ChangeProfessionMessage(profession);
        }
    };

    @Override
    public ChatMessage toChatMessage(ChatHistoryComponent history) {
        String message = PromptManager.getInstance().getProfessionPrompt(profession, true);
        return ChatMessage.Companion.System(message, null);
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("profession", Registries.VILLAGER_PROFESSION.getId(profession).toString());
        return nbt;
    }

    @Override
    public MessageType<?> getType() {
        return TYPE;
    }
}
