package com.igrium.aivillagers.util;

import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An arguably silly class that queues items to be returned by an iterator in a thread-safe manner.
 * If the queue is empty, the iterator will block until it's filled unless the queue has been marked "complete"
 */
public class ConcurrentIteratorQueue<T> implements Iterator<T> {
    private volatile boolean complete;
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    public void add(T val) {
        if (complete) {
            throw new IllegalStateException("Queue has been marked as complete!");
        }
        queue.add(val);
        this.notify();
    }

    public int size() {
        return queue.size();
    }

    @Override
    public boolean hasNext() {
        return !complete || !queue.isEmpty();
    }

    public void setComplete() {
        this.complete = true;
    }

    @Override
    public T next() {
        T val = queue.poll();
        if (val == null) {
            if (complete) {
                LoggerFactory.getLogger(getClass())
                        .warn("A race condition has caused ConcurrentIteratorQueue#next to be called with no more items. Null will be returned.");
                return null;
            }
            try {
                this.wait();
                return next();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        } else {
            return val;
        }
    }
}
