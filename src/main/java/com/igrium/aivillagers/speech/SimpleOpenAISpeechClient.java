package com.igrium.aivillagers.speech;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.igrium.playht.util.HttpException;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * An extremely simple OpenAI speech client because the main OpenAI client doesn't support streaming.
 */
public class SimpleOpenAISpeechClient {

    public static final class SpeechRequest {

        @SerializedName("model")
        private String modelId = "tts-1";

        @SerializedName("input")
        private String input = "";

        @SerializedName("voice")
        private String voice = null;

        @SerializedName("response_format")
        private String responseFormat = null;

        @SerializedName("speed")
        private Double speed = null;

        public SpeechRequest setModelId(String modelId) {
            this.modelId = Objects.requireNonNull(modelId);
            return this;
        }

        public SpeechRequest setInput(String input) {
            this.input = Objects.requireNonNull(input);
            return this;
        }

        public SpeechRequest setVoice(@Nullable String voice) {
            this.voice = voice;
            return this;
        }

        public SpeechRequest setResponseFormat(@Nullable String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public SpeechRequest setSpeed(double speed) {
            this.speed = speed;
            return this;
        }
    }

    private final HttpClient httpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final String apiKey;
    private final URI baseUrl;

    public SimpleOpenAISpeechClient(String apiKey, URI baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public SimpleOpenAISpeechClient(String apiKey) {
        this(apiKey, URI.create("https://api.openai.com/v1"));
    }

    public String getApiKey() {
        return apiKey;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    private final Gson GSON = new Gson();

    public CompletableFuture<InputStream> send(SpeechRequest request) {
        String json = GSON.toJson(request);
        var req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/audio/speech"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream()).thenApply(res -> {
            if (res.statusCode() == 200) {
                return res.body();
            } else {
                throw new HttpException(res.statusCode(), res.uri());
            }
        });
    }
}
