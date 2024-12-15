package com.igrium.aivillagers.subsystems.impl;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;

import net.minecraft.entity.Entity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChatSpeechSubsystem implements SpeechSubsystem {

    private static final int RANGE = 20;

    private final AIManager aiManager;

    public ChatSpeechSubsystem(AIManager aiManager) {
        this.aiManager = aiManager;
    }

    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void speak(Entity villager, String message) {
        Parameters messageParams = MessageType.params(MessageType.SAY_COMMAND, villager);
        int rangeSquared = RANGE * RANGE;
        SentMessage sentMessage = SentMessage.of(SignedMessage.ofUnsigned(message));

        for (var player : villager.getWorld().getPlayers()) {
            if (player.getPos().squaredDistanceTo(villager.getPos()) <= rangeSquared) {
                ((ServerPlayerEntity) player).sendChatMessage(sentMessage, false, messageParams);
            }
        }
    }
    
}
