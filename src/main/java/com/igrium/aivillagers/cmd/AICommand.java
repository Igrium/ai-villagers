package com.igrium.aivillagers.cmd;

import com.aallam.openai.api.chat.ChatMessage;
import com.igrium.aivillagers.chat.ChatHistoryComponent;
import com.igrium.aivillagers.gpt.ChatMessages;
import com.igrium.aivillagers.util.VillagerCounterComponent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

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
        ).then(
            literal("villagercount").then(
                literal("get").executes(AICommand::getCounter)
            ).then(
                literal("set").then(
                    argument("index", IntegerArgumentType.integer(0)).executes(AICommand::setCounter)
                )
            )
        ));

    }

    private static int doAiHistory(CommandContext<ServerCommandSource> context, boolean direct) throws CommandSyntaxException {
        Entity ent = EntityArgumentType.getEntity(context, "target");
        ChatHistoryComponent history = getHistory(ent);

        List<ChatMessage> messages = history.getChatHistory();
        if (messages.isEmpty()) {
            throw NO_HISTORY.create();
        }
        int i = 0;

        for (var message : messages) {
            if (direct) {
                context.getSource().sendFeedback(
                        () -> NbtHelper.toPrettyPrintedText(ChatMessages.toNbt(message)), false);
            } else {
                context.getSource().sendFeedback(
                        () -> Text.literal("\n" + ChatMessages.toSimpleString(message)), false);
            }
            i++;
        }

        return i;
    }

    private static int clearAiHistory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity ent = EntityArgumentType.getEntity(context, "target");
        ChatHistoryComponent history = getHistory(ent);

        List<ChatMessage> messages = history.getChatHistory();
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

    private static int getCounter(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        VillagerCounterComponent counter = VillagerCounterComponent.get(context.getSource().getServer());
        context.getSource().sendFeedback(() -> Text.literal("The next villager spawned will be " + counter.getNextName()), false);
        return counter.getCurrentIndex();
    }

    private static int setCounter(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int index = IntegerArgumentType.getInteger(context, "index");
        VillagerCounterComponent counter = VillagerCounterComponent.get(context.getSource().getServer());
        counter.setCurrentIndex(index);
        context.getSource().sendFeedback(() -> Text.literal("The next villager spawned will be " + counter.getNextName() + ". May cause duplicates!"), false);
        return 0;
    }
}
