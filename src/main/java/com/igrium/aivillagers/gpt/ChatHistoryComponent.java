package com.igrium.aivillagers.gpt;

import java.util.*;
import java.util.stream.Stream;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.chat.ChangeProfessionMessage;
import com.igrium.aivillagers.chat.InitialPromptMessage;
import com.igrium.aivillagers.chat.Message;
import com.igrium.aivillagers.chat.MessageType;
import net.minecraft.entity.passive.VillagerEntity;
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
import org.slf4j.LoggerFactory;

/**
 * Keeps track of chat history.
 */
// TODO: Save messages in a way that keeps more of their metadata.
public class ChatHistoryComponent implements Component {

    public static final ComponentKey<ChatHistoryComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of("ai-villagers:chat_history"), ChatHistoryComponent.class);

    public static @NotNull ChatHistoryComponent get(Entity entity) {
        return KEY.get(entity);
    }

    public static @Nullable ChatHistoryComponent getNullable(Entity entity) {
        return KEY.getNullable(entity);
    }

    private final Entity entity;
    private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());

    @Nullable
    private VillagerProfession originalProfession;

    public ChatHistoryComponent(Entity entity) {
        this.entity = entity;
    }

    public List<Message> getMessageHistory() {
        return messageHistory;
    }

    /**
     * The original profession that this villager had before changing. If unset, current profession is used.
     */
    public Optional<VillagerProfession> getOriginalProfession() {
        return Optional.ofNullable(originalProfession);
    }

    /**
     * The original profession that this villager had before changing. If unset, current profession is used.
     */
    public void setOriginalProfession(@Nullable VillagerProfession originalProfession) {
        this.originalProfession = originalProfession;
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

    /**
     * Return the villager's profession at a given message index. If the given index contains a change
     * profession message, the new profession is returned.
     *
     * @param index Index to use.
     * @return The villager's profession at that point. <code>null</code> if it's not a villager.
     */
    public @Nullable VillagerProfession getProfessionAt(int index) {
        if (!(entity instanceof VillagerEntity villager)) return null;

        synchronized (messageHistory) {
            if (messageHistory.isEmpty()) return originalProfession;

            if (index >= messageHistory.size()) {
                index = messageHistory.size();
            }

            // Step through message history backwards until change profession message is encountered
            ListIterator<Message> iterator = messageHistory.listIterator(index);
            Message message;
            while (iterator.hasPrevious()) {
                message = iterator.previous();
                if (message instanceof ChangeProfessionMessage(VillagerProfession profession)) {
                    return profession;
                }
            }
        }

        var original = originalProfession;
        return original != null ? original : villager.getVillagerData().getProfession();
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
