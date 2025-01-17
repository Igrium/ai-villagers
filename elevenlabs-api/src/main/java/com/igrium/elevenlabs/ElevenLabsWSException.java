package com.igrium.elevenlabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElevenLabsWSException extends RuntimeException {
    @NotNull
    private final String error;
    private final int code;

    public ElevenLabsWSException(@NotNull String error, int code) {
        super(error + " (" + code + ")");
        this.error = error;
        this.code = code;
    }

    public ElevenLabsWSException(@NotNull String error, int code, @NotNull String message) {
        super(error + ": " + message + " (" + code + ")");
        this.error = error;
        this.code = code;
    }

    public @NotNull String getError() {
        return error;
    }

    public int getCode() {
        return code;
    }
}
