package org.jruby.ext.ffi.jna;

import java.util.ArrayList;

/**
 * An invocation sesseion.
 * This provides post-invoke cleanup.
 */
final class Invocation {

    private final ArrayList<Runnable> postInvokeList = new ArrayList<Runnable>();

    void finish() {
        for (Runnable r : postInvokeList) {
            r.run();
        }
    }

    void addPostInvoke(Runnable postInvoke) {
        postInvokeList.add(postInvoke);
    }
}
