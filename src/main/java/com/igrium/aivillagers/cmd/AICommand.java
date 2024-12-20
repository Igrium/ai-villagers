package com.igrium.aivillagers.cmd;

import static net.minecraft.server.command.CommandManager.*;

import com.igrium.aivillagers.chat.Message;
import com.igrium.aivillagers.chat.MessageType;
import com.igrium.aivillagers.chat.ChatHistoryComponent;
import com.igrium.aivillagers.gpt.ChatMessagesKt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AICommand {

    private static final SimpleCommandExceptionType NO_HISTORY
            = new SimpleCommandExceptionType(Text.literal("This entity does not have an AI chat history."));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {

        dispatcher.register(literal("ai").then(
            literal("history").then(
                literal("get").then(
                    argument("target", EntityArgumentType.entity()).executes(ctx -> doAiHistory(ctx, false)).then(
                        literal("direct").executes(ctx -> doAiHistory(ctx, true))
                    )
                )
            ).then(
                literal("clear").then(
                    argument("target", EntityArgumentType.entity()).executes(AICommand::clearAiHistory)
                )
            )
        ));

    }


    private static int doAiHistory(CommandContext<ServerCommandSource> context, boolean direct) throws CommandSyntaxException {
        Entity ent = EntityArgumentType.getEntity(context, "target");
        ChatHistoryComponent history = getHistory(ent);

        List<Message> messages = history.getMessageHistory();
        if (messages.isEmpty()) {
            throw NO_HISTORY.create();
        }
        int i = 0;
        
        List<Message> fullMessages = new ArrayList<>(messages.size() + 1);
        fullMessages.add(history.getInitialPrompt());
        synchronized (messages) {
            fullMessages.addAll(messages);
        }
        for (var message : fullMessages) {
            if (direct) {
                context.getSource().sendFeedback(
                        () -> NbtHelper.toPrettyPrintedText(MessageType.saveMessage(message)), false);
            } else {
                context.getSource().sendFeedback(
                        () -> Text.literal(ChatMessagesKt.toSimpleString(message, history) + "\n"), false);
            }
            i++;
        }

        return i;
    }

    private static int clearAiHistory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity ent = EntityArgumentType.getEntity(context, "target");
        ChatHistoryComponent history = getHistory(ent);

        List<Message> messages = history.getMessageHistory();
        int numMessages = messages.size();
        if (numMessages == 0) {
            throw NO_HISTORY.create();
        }

        messages.clear();
        context.getSource().sendFeedback(
                () -> Text.literal("Cleared " + numMessages + " messages from ").append(ent.getDisplayName()), false);

        return numMessages;
    }

    private static ChatHistoryComponent getHistory(Entity entity) throws CommandSyntaxException {
        ChatHistoryComponent history = ChatHistoryComponent.getNullable(entity);
        if (history == null) {
            throw NO_HISTORY.create();
        }
        return history;
    }
}
