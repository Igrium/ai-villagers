package com.igrium.elevenlabs;

public class ElevenLabsException extends RuntimeException {
    private final String url;
    private final int statusCode;

    public ElevenLabsException(String url, int statusCode, String message) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
    }

    public ElevenLabsException(String url, int statusCode) {
        this(url, statusCode, "ElevenLabs returned status code " + statusCode + ". URL: " + url);
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
