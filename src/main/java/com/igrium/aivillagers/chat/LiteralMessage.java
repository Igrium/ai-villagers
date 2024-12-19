package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import com.igrium.aivillagers.gpt.ChatMessagesKt;
import net.minecraft.nbt.NbtCompound;

public class LiteralMessage implements Message {

    public static final MessageType<LiteralMessage> TYPE = new MessageType<>() {
        @Override
        public LiteralMessage fromNbt(NbtCompound nbt) {
            return new LiteralMessage(ChatMessagesKt.chatMessageFromNbt(nbt));
        }
    };

    private final ChatMessage message;

    public LiteralMessage(ChatMessage message) {
        this.message = message;
    }

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
