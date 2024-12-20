package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatMessages;
import net.minecraft.nbt.NbtCompound;

@Deprecated
public record LiteralMessage(ChatMessage message) implements Message {

    public static final MessageType<LiteralMessage> TYPE = new MessageType<>() {
        @Override
        public LiteralMessage fromNbt(NbtCompound nbt) {
            return new LiteralMessage(ChatMessages.chatMessageFromNbt(nbt));
        }
    };

    @Override
    public ChatMessage toChatMessage(ChatHistoryComponent history) {
        return message;
    }

    @Override
    public NbtCompound toNbt() {
        return ChatMessages.toNbt(message);
    }

    @Override
    public MessageType<?> getType() {
        return TYPE;
    }
}
