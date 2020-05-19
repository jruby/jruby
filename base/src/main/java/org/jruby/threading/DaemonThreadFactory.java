package org.jruby.threading;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A ThreadFactory for when we're using pooled threads; we want to create
 * the threads with daemon = true so they don't keep us from shutting down.
 */
public class DaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger count = new AtomicInteger(1);
    private final String name;
    private final int priority;

    public DaemonThreadFactory(String name) {
        this.name = name;
        this.priority = Thread.NORM_PRIORITY;
    }

    public DaemonThreadFactory(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public DaemonThreadFactory() {
        this.name = "JRubyWorker";
        this.priority = Thread.NORM_PRIORITY;
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name + "-" + count.getAndIncrement());
        thread.setDaemon(true);
        thread.setPriority(priority);

        return thread;
    }
}