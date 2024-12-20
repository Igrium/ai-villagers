package com.igrium.aivillagers.chat;

import com.aallam.openai.api.chat.ChatMessage;
import net.minecraft.nbt.NbtCompound;

/**
 * A message that can have dynamic arguments.
 */
public interface Message {
    /**
     * Compute a literal chat message from this message.
     *
     * @return Chat message.
     */
    public ChatMessage toChatMessage(ChatHistoryComponent history);

    /**
     * Serialize this message to NBT.
     *
     * @return Serialized message.
     */
    public NbtCompound toNbt();

    /**
     * Get a singleton type class representing this class.
     * The type class contains the deserializer and the type ID.
     *
     * @return Message type singleton
     */
    public MessageType<?> getType();
}
