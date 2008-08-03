package org.jruby.ext.ffi.jna;

import java.util.ArrayList;

/**
 * An invocation session.
 * This provides post-invoke cleanup.
 */
final class Invocation {
    private ArrayList<Runnable> postInvokeList;

    void finish() {
        if (postInvokeList != null) {
            for (Runnable r : postInvokeList) {
                r.run();
            }
        }
    }

    void addPostInvoke(Runnable postInvoke) {
        if (postInvokeList == null) {
             postInvokeList = new ArrayList<Runnable>();
        }
        postInvokeList.add(postInvoke);
    }
}
