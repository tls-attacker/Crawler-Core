/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.util;

import com.mongodb.lang.NonNull;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class CancellableFuture<V> implements RunnableFuture<V> {

    private final AtomicReference<V> result = new AtomicReference<>();
    private final RunnableFuture<V> innerFuture;
    private final Semaphore resultWritten = new Semaphore(0);

    public CancellableFuture(Callable<V> callable) {
        innerFuture =
                new FutureTask<>(
                        () -> {
                            V res = callable.call();
                            result.set(res);
                            resultWritten.release();
                            return res;
                        });
    }

    public CancellableFuture(Runnable runnable, V res) {
        innerFuture =
                new FutureTask<>(
                        () -> {
                            runnable.run();
                            result.set(res);
                            resultWritten.release();
                            return res;
                        });
    }

    @Override
    public boolean cancel(boolean b) {
        return innerFuture.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return innerFuture.get();
        } catch (CancellationException e) {
            resultWritten.acquire();
            return result.get();
        }
    }

    @Override
    public V get(long l, @NonNull TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return innerFuture.get(l, timeUnit);
        } catch (CancellationException e) {
            if (resultWritten.tryAcquire(l, timeUnit)) {
                return result.get();
            }
            throw new TimeoutException("Timeout while waiting for cancelled result");
        }
    }

    @Override
    public void run() {
        innerFuture.run();
    }
}
