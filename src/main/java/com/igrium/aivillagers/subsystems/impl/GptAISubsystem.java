package com.igrium.aivillagers.subsystems.impl;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.AISubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.aivillagers.subsystems.SpeechSubsystem.SpeechStream;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.chat.Chat;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class GptAISubsystem implements AISubsystem {

    public static final SubsystemType<GptAISubsystem> TYPE = SubsystemType.create(GptAISubsystem::new, GptAIConfig.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(GptAISubsystem.class);

    public static final class GptAIConfig {
        public String baseUrl = "https://api.openai.com";
        public String apiKey;
        public String model = "gpt-4o-mini";
        public int maxCompletionTokens = 200;
    }

    private final SimpleOpenAI openAI;
    private final AIManager aiManager;
    private final GptAIConfig config;

    public GptAISubsystem(AIManager aiManager, GptAIConfig config) {
        this.aiManager = aiManager;
        this.openAI = SimpleOpenAI.builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .build();
        this.config = config;
    }

    public SimpleOpenAI getOpenAI() {
        return openAI;
    }
    public AIManager getAiManager() {
        return aiManager;
    }

    @Override
    public void onSpokenTo(Entity villager, ServerPlayerEntity player, String message) {
        ChatRequest request = ChatRequest.builder()
                .model(config.model)
                .message(SystemMessage.of("You are a Minecraft villager."))
                .message(UserMessage.of(message))
                .maxCompletionTokens(config.maxCompletionTokens)
                .build();

        openAI.chatCompletions().createStream(request)
                .thenAccept(res -> handleGptResponse(villager, player, res))
                .exceptionally(e -> {
                    LOGGER.error("Error accessing OpenAI", e);
                    aiManager.getSpeechSubsystem().speak(villager, "Error accessing OpenAI!");
                    return null;
                });
    }

    protected void handleGptResponse(Entity villager, ServerPlayerEntity player, Stream<Chat> stream) {
        try (SpeechStream speech = aiManager.getSpeechSubsystem().openSpeechStream(villager)) {
            stream.filter(resp -> resp.getChoices().size() > 0 && resp.firstContent() != null)
                    .map(resp -> resp.firstContent())
                    .forEach(speech::acceptToken);
        }
    }

    @Override
    public void onDamage(Entity villager, DamageSource source, float amount) {
        onSpokenTo(villager, null, "I have punched you for " + amount + " damage");
    }
    
}
