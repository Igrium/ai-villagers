package com.igrium.aivillagers.chat;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.igrium.aivillagers.AIVillagers;
import com.igrium.aivillagers.gpt.ChatHistoryComponent;
import com.igrium.aivillagers.util.VillagerUtils;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PromptManager implements SimpleSynchronousResourceReloadListener {

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
    @JsonAdapter(ProfessionPromptSerializer.class)
    public record ProfessionPrompt(String initial, String switched) {
    }

    private static class ProfessionPromptSerializer implements JsonSerializer<ProfessionPrompt>, JsonDeserializer<ProfessionPrompt> {

        @Override
        public ProfessionPrompt deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json instanceof JsonPrimitive) {
                return new ProfessionPrompt(json.getAsString(), json.getAsString());
            } else if (json instanceof JsonObject obj) {
                String initial = obj.get("initial").getAsString();

                String switched = null;
                JsonElement switchedElm = obj.get("switched");
                if (switchedElm != null) {
                    switched = switchedElm.getAsString();
                }

                return new ProfessionPrompt(initial, switched != null ? switched : initial);
            } else {
                throw new IllegalArgumentException("Unsupported json type: " + json.getClass().getSimpleName());
            }
        }

        @Override
        public JsonElement serialize(ProfessionPrompt src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("initial", src.initial);
            obj.addProperty("switched", src.switched);
            return null;
        }
    }

    private String basePrompt;
    private final Map<VillagerProfession, ProfessionPrompt> professionPrompts = new ConcurrentHashMap<>();
    private ProfessionPrompt fallbackProfessionPrompt = new ProfessionPrompt(
            "You are a <profession>.",
            "You are now a <profession>."
    );

    /**
     * Get the templated base prompt that's used for defining the villager's identity.
     * @return Templated base prompt.
     */
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
        return message.replace("<profession>", VillagerUtils.getProfessionName(profession));
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
        st.add("profession", VillagerUtils.getProfessionName(prof));

        return st.render();
    }

    // DATAPACK LOADING

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class PromptJson {
        public String basePrompt = "";
        public final Map<String, ProfessionPrompt> professions = new HashMap<>();
        public String fallback = "";

        public void append(PromptJson other) {
            if (other.basePrompt != null) {
                this.basePrompt = other.basePrompt;
            }
            this.professions.putAll(other.professions);
            if (other.fallback != null) {
                this.fallback = other.fallback;
            }
        }

        public void apply(PromptManager manager) {
            Map<VillagerProfession, ProfessionPrompt> prompts = new HashMap<>(professions.size());
            for (var entry : professions.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (!Registries.VILLAGER_PROFESSION.containsId(id)) {
                    AIVillagers.LOGGER.warn("Unknown villager profession: {}", entry.getKey());
                    continue;
                }

                VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(id);
                prompts.put(profession, entry.getValue());
            }
            manager.professionPrompts.clear();
            manager.professionPrompts.putAll(prompts);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("ai-villagers:prompts");
    }

    @Override
    public void reload(ResourceManager manager) {
        AIVillagers.LOGGER.info("Loading AI prompts");
        PromptJson promptJson = new PromptJson();

        for (var resource : manager.getAllResources(Identifier.of("ai-villagers:prompts.json"))) {
            try (Reader reader = resource.getReader()) {
                var json = GSON.fromJson(reader, PromptJson.class);
                promptJson.append(json);
            } catch (Exception e) {
                AIVillagers.LOGGER.error("Error reading prompt json from: " + resource.getPackId(), e);
            }
        }
        promptJson.apply(this);
    }

}
