package com.nikoskatsanos.bayleaf.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BayLeafThreadFactory implements ThreadFactory {

    private final String name;
    private final boolean daemon;

    private final AtomicInteger count = new AtomicInteger(0);

    public BayLeafThreadFactory(String name) {
        this(name, true);
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(r, String.format("%s-%d", this.name, this.count.getAndIncrement()));
        t.setDaemon(this.daemon);
        return t;
    }
}
