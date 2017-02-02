package com.android.inputmethod.keyboard.emojifast;

import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Created by sepehr on 2/1/17.
 */
public class ListenableFutureTask<V> extends FutureTask<V> {

    private final List<FutureTaskListener<V>> listeners = new LinkedList<>();

    @Nullable
    private final Object identifier;

    public ListenableFutureTask(Callable<V> callable) {
        this(callable, null);
    }

    public ListenableFutureTask(Callable<V> callable, @Nullable Object identifier) {
        super(callable);
        this.identifier = identifier;
    }

    public ListenableFutureTask(final V result) {
        this(result, null);
    }

    public ListenableFutureTask(final V result, @Nullable Object identifier) {
        super(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return result;
            }
        });
        this.identifier = identifier;
        this.run();
    }

    public synchronized void addListener(FutureTaskListener<V> listener) {
        if (this.isDone()) {
            callback(listener);
        } else {
            this.listeners.add(listener);
        }
    }

    public synchronized void removeListener(FutureTaskListener<V> listener) {
        this.listeners.remove(listener);
    }

    @Override
    protected synchronized void done() {
        callback();
    }

    private void callback() {
        for (FutureTaskListener<V> listener : listeners) {
            callback(listener);
        }
    }

    private void callback(FutureTaskListener<V> listener) {
        if (listener != null) {
            try {
                listener.onSuccess(get());
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            } catch (ExecutionException e) {
                listener.onFailure(e);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof ListenableFutureTask && this.identifier != null) {
            return identifier.equals(other);
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        if (identifier != null) return identifier.hashCode();
        else                    return super.hashCode();
    }
}
