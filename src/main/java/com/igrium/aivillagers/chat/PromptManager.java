package com.igrium.aivillagers.chat;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.igrium.aivillagers.AIVillagers;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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
    public record ProfessionPrompt(@NotNull String initial, @NotNull String switched) {
        public ProfessionPrompt(String prompt) {
            this(prompt, prompt);
        }
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

    private final Prompts prompts = new Prompts();

    public Prompts getPrompts() {
        return prompts;
    }

    public String applyTemplate(ChatHistoryComponent chatHistoryComponent, String message) {
        return prompts.applyTemplate(chatHistoryComponent, message);
    }

    // DATAPACK LOADING
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static class PromptJson {
        public String basePrompt = "";
        public final Map<String, ProfessionPrompt> professions = new HashMap<>();
        public ProfessionPrompt professionFallback = new ProfessionPrompt("", "");

        public String nameChange = "";

        public void append(PromptJson other) {
            if (other.basePrompt != null) {
                this.basePrompt = other.basePrompt;
            }
            this.professions.putAll(other.professions);
            if (other.professionFallback != null) {
                this.professionFallback = other.professionFallback;
            }
        }

        public void apply(PromptManager manager) {
            var prompts = manager.getPrompts();

            // Resolve villager professions out of IDs.
            Map<VillagerProfession, ProfessionPrompt> profPrompts = new HashMap<>(professions.size());
            for (var entry : professions.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (!Registries.VILLAGER_PROFESSION.containsId(id)) {
                    AIVillagers.LOGGER.warn("Unknown villager profession: {}", entry.getKey());
                    continue;
                }

                VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(id);
                profPrompts.put(profession, entry.getValue());
            }

            prompts.setInitialPrompt(basePrompt);

            prompts.getProfessionPrompts().clear();
            prompts.getProfessionPrompts().putAll(profPrompts);
            prompts.setFallbackProfessionPrompt(professionFallback);

            prompts.setNameChangePrompt(nameChange);

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
