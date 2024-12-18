package com.igrium.aivillagers.util;

import com.igrium.aivillagers.gpt.VillagerAIInterface;
import kotlinx.serialization.json.JsonObject;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A java-compatible version of a tool call implementation.
 */
public interface ToolCallImplementation {
    /**
     * Execute this tool call.
     *
     * @param aiInterface The AI interface to use.
     * @param villager    The villager executing the call.
     * @param target      The player the villager is talking to, if any.
     * @param params      Tool call params.
     * @return A future that completes with the tool call result.
     */
    CompletableFuture<String> execute(@NotNull VillagerAIInterface aiInterface,
                                      @NotNull Entity villager,
                                      @Nullable Entity target,
                                      @NotNull JsonObject params);
}
