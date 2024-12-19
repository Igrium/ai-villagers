package com.igrium.aivillagers.gpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.chat.InitialPromptMessage;
import com.igrium.aivillagers.chat.Message;
import com.igrium.aivillagers.chat.MessageType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

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
    private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());

    public ChatHistoryComponent(Entity entity) {
        this.entity = entity;
    }

    public List<Message> getMessageHistory() {
        return messageHistory;
    }

    private final InitialPromptMessage initialPrompt = new InitialPromptMessage();

    /**
     * Compile a list of chat messages from the message history.
     * @return Immutable chat message list.
     */
    public List<ChatMessage> getChatHistory() {
        synchronized (messageHistory) {
            return Stream.concat(
                            Stream.of(initialPrompt.toChatMessage(this)),
                            messageHistory.stream().map(msg -> msg.toChatMessage(this))).
                    toList();
        }
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void readFromNbt(NbtCompound tag, WrapperLookup registryLookup) {
        try {
            synchronized (messageHistory) {
                messageHistory.clear();
                for (var nbt : tag.getList("history", NbtElement.COMPOUND_TYPE)) {
                    messageHistory.add(MessageType.loadMessage((NbtCompound) nbt));
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error loading chat history for " + entity, e);
        }

    }

    @Override
    public void writeToNbt(NbtCompound tag, WrapperLookup registryLookup) {
        NbtList history = new NbtList();
        synchronized (messageHistory) {
            for (var message : messageHistory) {
//                history.add(ChatMessagesKt.toNbt(chat));
                history.add(MessageType.saveMessage(message));
            }
        }
        tag.put("history", history);
    }


}
