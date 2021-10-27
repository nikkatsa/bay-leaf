package com.nikoskatsanos.bayleaf.core.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * An {@link Executor} which executes submitted {@link Runnable} tasks in order of submission
 *
 * <p>
 * This {@link Executor} has an underlying {@link ExecutorService} which executes the submitted tasks in order of submission. The underlying {@link ExecutorService} can be any
 * implementation.
 *
 * An ordered executor is useful especially in situations that specific tasks that have something in common (i.e. a common key) need to be executed in order. In such a scenario a
 * common thread pool can be maintained and used for all {@link OrderedExecutor}s.
 * <pre>
 *     {@code
 *
 *     private ExecutorService commonPool = Executors.newFixedThreadPool(16);
 *
 *     private Map<T, OrderExecutor> executors = new HashMap<>();
 *
 *     public OrderExecutor hashFunction(final T key ) {
 *          return executors.computeIfAbsent( key, k -> new OrderExecutor( commonPool ) );
 *     }
 * </pre>
 * </p>
 */
public class OrderedExecutor implements Executor {

    private final Queue<Runnable> tasks = new ArrayDeque<>(64);

    private final ExecutorService delegate;

    private boolean awaitingExecution = false;

    public OrderedExecutor(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(final Runnable command) {
        synchronized (this.tasks) {
            this.tasks.offer(command);
            if (!this.awaitingExecution) {
                this.awaitingExecution = true;
                this.delegate.execute(this::run);
            }
        }
    }

    private void run() {
        final Runnable nextRunnable = this.tasks.poll();
        if (nextRunnable == null) {
            return;
        }
        try {
            nextRunnable.run();
        } finally {
            synchronized (this.tasks) {
                if (this.tasks.isEmpty()) {
                    this.awaitingExecution = false;
                } else {
                    this.awaitingExecution = true;
                    this.delegate.execute(this::run);
                }
            }
        }
    }
}
