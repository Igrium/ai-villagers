package com.igrium.aivillagers.chat;

import java.util.*;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.gpt.ChatMessages;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;

/**
 * Keeps track of chat history.
 */
// TODO: Save messages in a way that keeps more of their metadata.
public class ChatHistoryComponent implements Component {

    public static final ComponentKey<ChatHistoryComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of("ai-villagers:chat_history"), ChatHistoryComponent.class);

    public static @NotNull ChatHistoryComponent get(Entity entity) {
        return KEY.get(entity);
    }

    public static @Nullable ChatHistoryComponent getNullable(Entity entity) {
        return KEY.getNullable(entity);
    }

    private final Entity entity;
    private final List<ChatMessage> chatHistory = Collections.synchronizedList(new ArrayList<>());

    @Nullable
    private VillagerProfession originalProfession;

    public ChatHistoryComponent(Entity entity) {
        this.entity = entity;
    }

    public List<ChatMessage> getChatHistory() {
        return chatHistory;
    }

    /**
     * Get this villager's chat history. If there's no initial prompt, calculate it and add it to the beginning of the list.
     * @return Mutable list of chat messages.
     */
    public List<ChatMessage> prepareChatHistory() {
        synchronized (chatHistory) {
            if (chatHistory.isEmpty()) {
                chatHistory.add(PromptManager.getInstance().getPrompts().getInitialPromptMsg(this));
            }
            return chatHistory;
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt, WrapperLookup wrapperLookup) {
        NbtList list = new NbtList();
        synchronized (chatHistory) {
            for (var msg : chatHistory) {
                list.add(ChatMessages.toNbt(msg));
            }
        }
        nbt.put("messages", list);
    }

    @Override
    public void readFromNbt(NbtCompound nbt, WrapperLookup wrapperLookup) {
        NbtList list = nbt.getList("messages", NbtElement.COMPOUND_TYPE);
        synchronized (chatHistory) {
            chatHistory.clear();
            for (var msg : list) {
                chatHistory.add(ChatMessages.chatMessageFromNbt((NbtCompound) msg));
            }
        }
    }


    public Entity getEntity() {
        return entity;
    }

}
