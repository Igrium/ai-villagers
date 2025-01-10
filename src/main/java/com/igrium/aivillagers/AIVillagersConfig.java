package com.igrium.aivillagers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import net.fabricmc.loader.api.FabricLoader;

public class AIVillagersConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setStrictness(Strictness.LENIENT)
            .create();

    private static JsonObject defaultSubsystem(String name) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", name);
        return obj;
    }

    public JsonObject listening = defaultSubsystem("chat");
    public JsonObject ai = defaultSubsystem("dummy");
    public JsonObject speech = defaultSubsystem("chat");

    public static AIVillagersConfig load(Path file) {
        try(BufferedReader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, AIVillagersConfig.class);
        } catch (Exception e) {
            AIVillagers.LOGGER.error("Error loading AI Villagers config", e);
        }

        AIVillagersConfig config = new AIVillagersConfig();
        try(BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(GSON.toJson(config));
        } catch (Exception e) {
            AIVillagers.LOGGER.error("Error saving AI Villagers config", e);
        }

        return config;
    }

    /**
     * Attempt to load the AI villagers config.
     * First look for a jsonc file, then look for a json file if it doesn't exist.
     * @return AI villages config object.
     */
    public static AIVillagersConfig loadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path file = configDir.resolve("ai-villagers.jsonc");
        if (!Files.isRegularFile(file)) {
            file = configDir.resolve("ai-villagers.json");
        }
        return load(file);
    }
}
