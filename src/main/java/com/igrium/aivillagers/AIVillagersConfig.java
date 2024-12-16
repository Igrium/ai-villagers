package com.igrium.aivillagers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class AIVillagersConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
}
