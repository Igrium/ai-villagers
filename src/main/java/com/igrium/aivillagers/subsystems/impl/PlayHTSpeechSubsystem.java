package com.igrium.aivillagers.subsystems.impl;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.aivillagers.AIManager;
import com.igrium.aivillagers.subsystems.SpeechSubsystem;
import com.igrium.aivillagers.subsystems.SubsystemType;
import com.igrium.playht.PlayHT;
import com.igrium.playht.SpeechStreamRequest;
import com.igrium.playht.SpeechStreamRequest.OutputFormat;
import com.igrium.playht.SpeechStreamRequest.VoiceEngine;

import net.minecraft.entity.Entity;

public class PlayHTSpeechSubsystem implements SpeechSubsystem {

    protected static final Logger LOGGER = LoggerFactory.getLogger(PlayHTSpeechSubsystem.class);

    public static class PlayHTConfig {
        public String apiKey = "";
        public String apiUser = "";
        public String voice = "s3://voice-cloning-zero-shot/9d10e3be-833b-4868-8a6a-67d91233344d/original/manifest.json";
    }

    public static final SubsystemType<PlayHTSpeechSubsystem> TYPE = SubsystemType.create(PlayHTSpeechSubsystem::new, PlayHTConfig.class);

    private PlayHT playHT;
    private String voice;

    public PlayHTSpeechSubsystem(AIManager aiManager, PlayHTConfig config) {
        playHT = new PlayHT(config.apiUser, config.apiKey);
        voice = config.voice;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    @Override
    public void speak(Entity entity, String message) {
        LOGGER.info("Making a call to PlayHT: {}", message);

        Path outPath = Paths.get("audio.ogg");

        new SpeechStreamRequest()
                .text(message)
                .outputFormat(OutputFormat.OGG)
                .voice(voice)
                .voiceEngine(VoiceEngine.PLAYHT2)
                .send(playHT).handle((in, e) -> {
                    if (e != null) {
                        LOGGER.error("Error getting text-to-speech", e);
                        return null;
                    }
                    LOGGER.info("Recieving data from PlayHT");
                    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outPath))) {
                        in.transferTo(out);
                    } catch (Exception ex) {
                        LOGGER.error("Error writing to file", ex);
                    }
                    LOGGER.info("Wrote to " + outPath.toAbsolutePath());
                    return null;
                });
    }
    
}
