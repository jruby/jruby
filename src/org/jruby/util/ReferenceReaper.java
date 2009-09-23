
package org.jruby.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * A general purpose reference tracker & reaper utility class.
 */
public final class ReferenceReaper {
    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    private final Thread reaperThread;
    
    private ReferenceReaper() {
        reaperThread = new Thread(reaper);
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    private static final class SingletonHolder {
        private static final ReferenceReaper INSTANCE = new ReferenceReaper();
    }
    public static final ReferenceReaper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static abstract class Weak<T> extends java.lang.ref.WeakReference<T> implements Runnable {
        public Weak(T referent) {
            super(referent, ReferenceReaper.getInstance().referenceQueue);
        }
    }

    public static abstract class Soft<T> extends java.lang.ref.SoftReference<T> implements Runnable {
        public Soft(T referent) {
            super(referent, ReferenceReaper.getInstance().referenceQueue);
        }
    }

    public static abstract class Phantom<T> extends java.lang.ref.PhantomReference<T> implements Runnable {
        public Phantom(T referent) {
            super(referent, ReferenceReaper.getInstance().referenceQueue);
        }
    }

    private final Runnable reaper = new Runnable() {

        public void run() {
            for ( ; ; ) {
                try {
                    Reference r = referenceQueue.remove();
                    if (r instanceof Runnable) {
                        ((Runnable) r).run();
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
