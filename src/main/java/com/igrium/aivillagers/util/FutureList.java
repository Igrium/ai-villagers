package com.igrium.aivillagers.util;


import com.mojang.datafixers.util.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FutureList<T> {
    private final List<CompletableFuture<T>> futures = new ArrayList<>();
    private final Consumer<List<Either<T, Throwable>>> onComplete;

    public FutureList(Consumer<List<Either<T, Throwable>>> onComplete) {
        this.onComplete = onComplete;
    }

    public synchronized void submit(CompletableFuture<T> function) {
        futures.add(function);
        function.whenComplete((v, t) -> checkCompletion());
    }

    private void checkCompletion() {
        List<Either<T, Throwable>> l;
        synchronized (this) {
            for (var future : futures) {
                if (!future.isDone())
                    return;
            }
            l = futures.stream().map(FutureList::getResult).toList();
            futures.clear();
        }
        onComplete.accept(l);
    }

    private static <T> Either<T, Throwable> getResult(CompletableFuture<T> future) {
        if (!future.isDone())
            throw new IllegalStateException("Future is not done!");

        if (future.isCompletedExceptionally()) {
            return Either.right(future.exceptionNow());
        } else {
            return Either.left(future.getNow(null));
        }
    }
}
