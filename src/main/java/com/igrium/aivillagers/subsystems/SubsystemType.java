package com.igrium.aivillagers.subsystems;

import java.util.function.BiFunction;
import java.util.function.Function;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.igrium.aivillagers.AIManager;

public abstract class SubsystemType<T extends Subsystem> {

    public abstract T create(AIManager aiManager, JsonObject config);

    public static <T extends Subsystem> SubsystemType<T> createSimple(BiFunction<AIManager, JsonObject, T> factory) {
        return new SimpleSubsystemType<>(factory);
    }

    public static <T extends Subsystem, C> SubsystemType<T> create(BiFunction<AIManager, C, T> factory, Class<C> configClass) {
        return new JsonSubsystemType<>(factory, configClass);
    }

    public static <T extends Subsystem> SubsystemType<T> create(Function<AIManager, T> factory) {
        return new NoConfigSubsystemType<>(factory);
    }
    

    private static class SimpleSubsystemType<T extends Subsystem> extends SubsystemType<T> {

        final BiFunction<AIManager, JsonObject, T> factory;

        SimpleSubsystemType(BiFunction<AIManager, JsonObject, T> factory) {
            this.factory = factory;
        }

        @Override
        public T create(AIManager aiManager, JsonObject config) {
            return factory.apply(aiManager, config);
        }

    }

    private static class JsonSubsystemType<T extends Subsystem, C> extends SubsystemType<T> {
        static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        final Class<C> configClass;
        final BiFunction<AIManager, C, T> factory;

        JsonSubsystemType(BiFunction<AIManager, C, T> factory, Class<C> configClass) {
            this.configClass = configClass;
            this.factory = factory;
        }

        @Override
        public T create(AIManager aiManager, JsonObject config) {
            return factory.apply(aiManager, GSON.fromJson(config, configClass));
        }
    }

    private static class NoConfigSubsystemType<T extends Subsystem> extends SubsystemType<T> {
        final Function<AIManager, T> factory;

        NoConfigSubsystemType(Function<AIManager, T> factory) {
            this.factory = factory;
        }

        @Override
        public T create(AIManager aiManager, JsonObject config) {
            return factory.apply(aiManager);
        }
    }
}