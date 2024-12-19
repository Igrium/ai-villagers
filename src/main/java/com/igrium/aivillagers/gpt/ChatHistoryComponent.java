package com.igrium.aivillagers.gpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aallam.openai.api.chat.ChatMessage;
import com.aallam.openai.api.chat.ChatMessageBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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

    public static final ComponentKey<ChatHistoryComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of("ai-villagers:chat_history"), ChatHistoryComponent.class);

    public static ChatHistoryComponent get(Entity entity) {
        return KEY.get(entity);
    }
    
    private final Entity entity;
    private final List<ChatMessage> messageHistory = Collections.synchronizedList(new ArrayList<>());

    public ChatHistoryComponent(Entity entity) {
        this.entity = entity;
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void readFromNbt(NbtCompound tag, WrapperLookup registryLookup) {
        synchronized (messageHistory) {
            messageHistory.clear();
            for (var nbt : tag.getList("history", NbtElement.COMPOUND_TYPE)) {
                messageHistory.add(ChatMessagesKt.chatMessageFromNbt((NbtCompound) nbt));
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, WrapperLookup registryLookup) {
        NbtList history = new NbtList();
        synchronized (messageHistory) {
            for (var chat : messageHistory) {
                history.add(ChatMessagesKt.toNbt(chat));
            }
        }
        tag.put("history", history);
    }


}
