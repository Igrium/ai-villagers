package com.igrium.openai;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.playht.PlayHT;
import com.igrium.playht.SpeechStreamRequest;
import com.igrium.playht.SpeechStreamRequest.OutputFormat;
import com.igrium.playht.SpeechStreamRequest.VoiceEngine;
import com.igrium.test.TestConfig;

public class OpenAITestApp {

    public static final Logger LOGGER = LoggerFactory.getLogger("Test App");
    public static void main(String[] args) {
        TestConfig config = TestConfig.fromFile(Paths.get("config.json"));

        PlayHT playHT = new PlayHT(config.apiUser, config.apiKey);

        Path outPath = Paths.get("audio.mp3");
        
        new SpeechStreamRequest()
                .text("This is a test for Text-to-speech!")
                .outputFormat(OutputFormat.MP3)
                .voice("s3://voice-cloning-zero-shot/9d10e3be-833b-4868-8a6a-67d91233344d/original/manifest.json")
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
                }).join();

        LOGGER.info("Making call to PlayHT");
    }
}
