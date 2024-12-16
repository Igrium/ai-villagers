package com.igrium.playht.util;

import java.net.URI;

public class HttpException extends RuntimeException {
    private final int statusCode;
    private final URI uri;

    public HttpException(int statusCode, URI uri) {
        super("HTTP request failed with status code " + statusCode + " (" + uri + ")");
        this.statusCode = statusCode;
        this.uri = uri; 
    }

    public HttpException(int statusCode, URI uri, String message) {
        super(message);
        this.statusCode = statusCode;
        this.uri = uri;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public URI getUri() {
        return uri;
    }
}
