package com.igrium.playht;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

public class PlayHT {
    public static record Credentials(String apiUser, String apiKey) {};

    private final HttpClient httpClient;
    private final Credentials credentials;

    private URI baseUrl;

    public PlayHT(Credentials credentials, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.credentials = credentials;
        try {
            baseUrl = new URI("https://api.play.ht");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public PlayHT(Credentials credentials) {
        this(credentials, HttpClient.newHttpClient());
    }

    public PlayHT(String apiUser, String apiKey, HttpClient httpClient) {
        this(new Credentials(apiUser, apiKey), httpClient);
    }

    public PlayHT(String apiUser, String apiKey) {
        this(new Credentials(apiUser, apiKey));
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public HttpClient getHttpClient() {
        return httpClient;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    
}
