package com.igrium.test;

import java.io.BufferedReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String apiKey = "";
    public String apiUser = "";

    public String toJson() {
        return GSON.toJson(this);
    }

    public static TestConfig fromJson(Reader reader) {
        return GSON.fromJson(reader, TestConfig.class);
    }

    public static TestConfig fromFile(Path file) {
        try(BufferedReader reader = Files.newBufferedReader(file)) {
            return fromJson(reader);
        } catch (Exception e) {
            LoggerFactory.getLogger(TestConfig.class).error("Error reading config", e);
            return new TestConfig();
        }
    }
}
