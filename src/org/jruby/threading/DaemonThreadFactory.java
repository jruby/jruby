package org.jruby.threading;

import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory for when we're using pooled threads; we want to create
 * the threads with daemon = true so they don't keep us from shutting down.
 */
public class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);

        return thread;
    }
}