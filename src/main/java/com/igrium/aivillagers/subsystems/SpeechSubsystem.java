package com.igrium.aivillagers.subsystems;

import java.io.Closeable;
import java.util.function.BiConsumer;

import com.igrium.aivillagers.subsystems.SpeechSubsystem.SpeechStream;

import net.minecraft.entity.Entity;

/**
 * The subsystem that allows villagers to speak using text-to-speech.
 */
public interface SpeechSubsystem extends Subsystem {

    /**
     * An interface that allows the speech subsystem to recieve text from the AI
     * subsystem one token at a time.
     */
    public static interface SpeechStream extends Closeable {
        public void acceptToken(String token);
        public void close(); // Speech streams are not allowed to throw IO exceptions.
    }

    /**
     * Force a villager to speak.
     * @param entity The villager entity.
     * @param message The message.
     */
    public void speak(Entity entity, String message);

    public default SpeechStream openSpeechStream(Entity entity) {
        return new DefaultSpeechStream(entity, this::speak);
    }
}

/**
 * A speech stream that just calls the speak function once it's closed.
 */
class DefaultSpeechStream implements SpeechStream {
    BiConsumer<Entity, String> speakFunction;
    Entity entity;

    StringBuffer buffer = new StringBuffer();

    DefaultSpeechStream(Entity entity, BiConsumer<Entity, String> speakFunction) {
        this.entity = entity;
        this.speakFunction = speakFunction;
    }

    @Override
    public void acceptToken(String token) {
        buffer.append(token);
    }

    @Override
    public void close() {
        speakFunction.accept(entity, buffer.toString());
    }
}
