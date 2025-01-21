package com.igrium.aivillagers.util;

import com.igrium.aivillagers.AIVillagers;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ErrorSound implements IdentifiableResourceReloadListener {

    private byte[] data = new byte[0];

    public byte[] getData() {
        return data;
    }

    public AudioInputStream getInputStream() {
        try {
            return AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException("Somehow, reading audio data from a byte array failed:", e);
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("ai-villagers:error_sound");
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.runAsync(() -> {

            var opt = manager.getResource(Identifier.of("ai-villagers:sounds/error.wav"));
            if (opt.isPresent()) {
                try (var in = opt.get().getInputStream()) {
                    data = in.readAllBytes();
                    AIVillagers.LOGGER.info("Loaded funny error sound.");
                } catch (Exception e) {
                    AIVillagers.LOGGER.error("Error loading error sound: ", e);
                }
            } else {
                data = new byte[0];
                AIVillagers.LOGGER.warn("No sound found at ai-villagers:sounds/error.wav");
            }
        }, prepareExecutor).thenCompose(synchronizer::whenPrepared);
    }
}
