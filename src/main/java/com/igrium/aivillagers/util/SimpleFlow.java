package com.igrium.aivillagers.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SimpleFlow<T> implements Closeable {
    private final List<Subscriber<T>> subscribers = new ArrayList<>();

    public void submit(T val) {
        for (var s : subscribers) {
            s.accept(val);
        }
    }

    public void subscribe(Subscriber<T> subscriber) {
        subscribers.add(subscriber);
    }

    public boolean unsubscribe(Subscriber<?> subscriber) {
        return subscribers.remove(subscriber);
    }

    @Override
    public void close() {
        for (var s : subscribers) {
            s.close();
        }
    }

    public CompletableFuture<Integer> collect(Consumer<T> consumer) {
        var s = new CollectingSubscriber<>(consumer);
        subscribe(s);
        return s.future;
    }

    public interface Subscriber<T> {
        void accept(T val);
        default void close() {}
    }

    private static class CollectingSubscriber<T> implements Subscriber<T> {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        int count = 0;

        final Consumer<T> consumer;

        private CollectingSubscriber(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(T val) {
            consumer.accept(val);
            count++;
        }

        @Override
        public void close() {
            future.complete(count);
        }
    }
}
