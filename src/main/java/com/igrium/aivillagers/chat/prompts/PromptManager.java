package com.igrium.aivillagers.chat.prompts;

import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PromptManager {
    /**
     * Shortcut for <code>AIVillagers.getInstance().getPromptManager()</code>
     *
     * @return Prompt manager instance
     */
    public static PromptManager getInstance() {
        return AIVillagers.getInstance().getPromptManager();
    }

    /**
     * A prompt used to establish a villager profession.
     *
     * @param initial  The prompt to use when a villager has spawned with a profession.
     * @param switched The prompt to use when a villager has switched to a profession.
     */
    public record ProfessionPrompt(String initial, String switched) {
        public ProfessionPrompt(String prompt) {
            this(prompt, prompt);
        }
    }

    private String basePrompt;
    private final Map<VillagerProfession, ProfessionPrompt> professionPrompts = new ConcurrentHashMap<>();
    private ProfessionPrompt fallbackProfessionPrompt = new ProfessionPrompt(
            "You are a <profession>, but you don't know how to do your job.",
            "You are now a <profession>, but you don't know how to do your job."
    );


    public String getBasePrompt() {
        return basePrompt;
    }

    public ProfessionPrompt getFallbackProfessionPrompt() {
        return fallbackProfessionPrompt;
    }

    public void setFallbackProfessionPrompt(ProfessionPrompt fallbackProfessionPrompt) {
        this.fallbackProfessionPrompt = fallbackProfessionPrompt;
    }

    public Map<VillagerProfession, ProfessionPrompt> getProfessionPrompts() {
        return professionPrompts;
    }

    public ProfessionPrompt getProfessionPrompt(VillagerProfession profession) {
        return professionPrompts.getOrDefault(profession, fallbackProfessionPrompt);
    }

    public String getProfessionPrompt(VillagerProfession profession, boolean switched) {
        ProfessionPrompt prompt = professionPrompts.getOrDefault(profession, fallbackProfessionPrompt);
        String message = switched ? prompt.switched : prompt.initial;
        return message.replace("<profession>", profession.id());
    }

    /**
     * Apply all the relevant values from a string template to a prompt.
     *
     * @param chatHistory The chat history of the villager this is for.
     * @param message     The prompt.
     * @return The prompt with all the values applied.
     */
    public String applyTemplate(@NotNull ChatHistoryComponent chatHistory, String message) {
        VillagerEntity villager = null;
        if (chatHistory.getEntity() instanceof VillagerEntity v) {
            villager = v;
        }

        Language language = Language.getInstance();
        ST st = new ST(message);

        st.add("entity", language.get(chatHistory.getEntity().getType().getTranslationKey(), "villager"));

        VillagerProfession prof = villager != null ? villager.getVillagerData().getProfession()
                : chatHistory.getOriginalProfession().orElse(VillagerProfession.NONE);
        st.add("profession", language.get(prof.id(), "unemployed"));

        return st.render();
    }
}
