package com.igrium.aivillagers.gpt;

import java.util.ArrayList;
import java.util.List;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import io.github.sashirestela.openai.domain.chat.ChatMessage;
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
    private List<ChatMessage> messageHistory = new ArrayList<>();

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
        
    }

    @Override
    public void writeToNbt(NbtCompound tag, WrapperLookup registryLookup) {
        // NbtList messages = new NbtList();
        // for (var message : messageHistory) {
        //     if (message instanceof UserMessage userMessage) {
        //         messages.add(NbtString.of(userMessage.getContent().toString()));
        //     } else if (message instanceof ResponseMessage responseMessage) {
        //         messages.add(NbtString.of(responseMessage.getContent()));
        //     } else if (message instanceof AssistantMessage assistantMessage) {
        //         messages.add(NbtString.of(assistantMessage.getContent().toString()));
        //     }
        //     // messages.add(NbtString.of(message.))
        // }
    }
    
}
