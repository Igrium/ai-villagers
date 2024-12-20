package com.igrium.aivillagers.chat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.NbtCompound;

@Deprecated
public abstract class MessageType<T extends Message> {
    public static final BiMap<String, MessageType<?>> REGISTRY = HashBiMap.create();

    /**
     * Load a message from NBT.
     * @param nbt NBT tag to load from.
     * @return parsed message.
     * @throws IllegalArgumentException If the message type was not found.
     */
    public static Message loadMessage(NbtCompound nbt) throws IllegalArgumentException {
        String id = nbt.getString("type");
        MessageType<?> type = REGISTRY.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown message type: " + id);
        }
        return type.fromNbt(nbt);
    }

    /**
     * Save a message to NBT, properly installing its type tag.
     * @param message Message to save.
     * @return Serialized NBT.
     */
    public static NbtCompound saveMessage(Message message) {
        String id = message.getType().getId();
        NbtCompound nbt = message.toNbt();
        nbt.putString("type", id);
        return nbt;
    }

    public static void registerDefaults() {
        REGISTRY.put("literal", LiteralMessage.TYPE);
        REGISTRY.put("initialPrompt", InitialPromptMessage.TYPE);
        REGISTRY.put("changeProfession", ChangeProfessionMessage.TYPE);
    }

    // ---------

    public abstract T fromNbt(NbtCompound nbt);

    public final String getId() {
        return REGISTRY.inverse().get(this);
    }
}
