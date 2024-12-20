package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatMessagesKt;
import net.minecraft.nbt.NbtCompound;

public record LiteralMessage(ChatMessage message) implements Message {

    public static final MessageType<LiteralMessage> TYPE = new MessageType<>() {
        @Override
        public LiteralMessage fromNbt(NbtCompound nbt) {
            return new LiteralMessage(ChatMessagesKt.chatMessageFromNbt(nbt));
        }
    };

    @Override
    public ChatMessage toChatMessage(ChatHistoryComponent history) {
        return message;
    }

    @Override
    public NbtCompound toNbt() {
        return ChatMessagesKt.toNbt(message);
    }

    @Override
    public MessageType<?> getType() {
        return TYPE;
    }
}
