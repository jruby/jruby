
package org.jruby.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * A general purpose reference tracker & reaper utility class.
 */
public final class ReferenceReaper {
    public final ReferenceQueue referenceQueue = new ReferenceQueue();
    private final Thread reaperThread;
    
    private ReferenceReaper() {
        reaperThread = new Thread(reaper, "ReferenceReaper");
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    private static final class SingletonHolder {
        private static final ReferenceReaper INSTANCE = new ReferenceReaper();
    }
    public static final ReferenceReaper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final Runnable reaper = new Runnable() {

        public void run() {
            for ( ; ; ) {
                try {
                    Reference r = referenceQueue.remove();
                    try {
                        if (r instanceof Runnable) {
                            ((Runnable) r).run();
                        }
                    } finally {
                        r.clear();
                    }
                } catch (InterruptedException ex) {
                    break;
                } catch (Throwable t) {
                    continue;
                }
            }
        }
    };
}
