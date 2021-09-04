package org.jruby.ext.ffi.jffi;

import jnr.ffi.util.ref.FinalizableReferenceQueue;

/**
 *
 */
class NativeFinalizer {
    private final FinalizableReferenceQueue finalizerQueue = new FinalizableReferenceQueue();
    
    private static final class SingletonHolder {
        private static final NativeFinalizer INSTANCE = new NativeFinalizer();
    }
    
    public static NativeFinalizer getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public FinalizableReferenceQueue getFinalizerQueue() {
        return finalizerQueue;
    }
}
