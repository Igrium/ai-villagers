package com.igrium.playht;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.igrium.playht.util.HttpException;

public final class SpeechStreamRequest {

    private static final Gson GSON = new Gson();

    public static enum OutputFormat {
        @SerializedName("mp3")
        MP3,
        @SerializedName("mulaw")
        MULAW,
        @SerializedName("wav")
        WAV,
        @SerializedName("ogg")
        OGG,
        @SerializedName("flac")
        FLAC
    }

    public static enum VoiceEngine {
        PLAY3_MINI("Play3.0-mini"),
        PLAY_DIALOG("PlayDialog"),
        PLAYHT2_TURBO("PlayHT2.0-turbo"),
        PLAYHT2("PlayHT2.0"),
        PLAYHT1("PlayHT1.0");

        private VoiceEngine(String name) {
            this.voiceName = name;
        }

        final String voiceName;
    }

    @SerializedName("text")
    private String text;

    public SpeechStreamRequest text(String text) {
        this.text = text;
        return this;
    }

    @SerializedName("voice")
    private String voice = "s3://voice-cloning-zero-shot/d9ff78ba-d016-47f6-b0ef-dd630f59414e/female-cs/manifest.json";

    public SpeechStreamRequest voice(String voice) {
        this.voice = voice;
        return this;
    }

    @SerializedName("output_format")
    private OutputFormat outputFormat = OutputFormat.MP3;

    public SpeechStreamRequest outputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }

    @SerializedName("speed")
    private float speed = 1;

    public SpeechStreamRequest speed(float speed) {
        this.speed = speed;
        return this;
    }

    @SerializedName("sample_rate")
    private int sampleRate = 24000;

    public SpeechStreamRequest sampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    @SerializedName("seed")
    private Integer seed;

    public SpeechStreamRequest seed(int seed) {
        this.seed = seed;
        return this;
    }

    public SpeechStreamRequest randomSeed() {
        this.seed = null;
        return this;
    }

    @SerializedName("temperature")
    private Float temerature;

    public SpeechStreamRequest temerature(float temerature) {
        this.temerature = temerature;
        return this;
    }

    public SpeechStreamRequest randomTemperature() {
        this.temerature = null;
        return this;
    }

    @SerializedName("voice_engine")
    private String voiceEngine = "PlayDialog";

    public SpeechStreamRequest voiceEngine(String voiceEngine) {
        this.voiceEngine = voiceEngine;
        return this;
    }

    public SpeechStreamRequest voiceEngine(VoiceEngine voiceEngine) {
        this.voiceEngine = voiceEngine.voiceName;
        return this;
    }

    @SerializedName("emotion")
    private String emotion = null;

    public SpeechStreamRequest emotion(String emotion) {
        this.emotion = emotion;
        return this;
    }

    @SerializedName("voice_guidance")
    private Float voiceGuidance = null;

    public SpeechStreamRequest voiceGuidance(float guidance) {
        this.voiceGuidance = guidance;
        return this;
    }
    
    public SpeechStreamRequest defaultVoiceGuidance() {
        this.voiceGuidance = null;
        return this;
    }

    @SerializedName("text_guidance")
    private Float textGuidance = null;

    public SpeechStreamRequest textGuidance(float guidance) {
        this.textGuidance = guidance;
        return this;
    }

    public SpeechStreamRequest defaultTextGuidance() {
        this.textGuidance = null;
        return this;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Send this speech stream request to the server.
     * 
     * @param <T>         Body response type.
     * @param playHT      PlayHT instance.
     * @param bodyHandler The body handler to use.
     * @return A future that completes once the response has been recieved.
     */
    public <T> CompletableFuture<T> send(PlayHT playHT, BodyHandler<T> bodyHandler) {
        HttpRequest req = HttpRequest.newBuilder(playHT.getBaseUrl().resolve("/api/v2/tts/stream"))
                .header("AUTHORIZATION", playHT.getCredentials().apiKey())
                .header("X-USER-ID", playHT.getCredentials().apiUser())
                .header("content-type", "application/json")
                .POST(BodyPublishers.ofString(toJson()))
                .build();

        return playHT.getHttpClient().sendAsync(req, bodyHandler).thenApply(res -> {
            if (res.statusCode() != 200) {
                throw new HttpException(res.statusCode(), res.uri());
            }
            return res.body();
        });
    }

    /**
     * Send this speech stream request to the server and recieve an input stream.
     * 
     * @param playHT PlayHT instance.
     * @return A future that completes with an input stream containing the audio.
     */
    public CompletableFuture<InputStream> send(PlayHT playHT) {
        return send(playHT, BodyHandlers.ofInputStream());
    }
}
