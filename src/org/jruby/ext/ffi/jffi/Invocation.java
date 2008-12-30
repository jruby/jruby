package org.jruby.ext.ffi.jffi;

import java.util.ArrayList;
import org.jruby.runtime.ThreadContext;

/**
 * An invocation session.
 * This provides post-invoke cleanup.
 */
final class Invocation {
    private final ThreadContext context;
    private ArrayList<Runnable> postInvokeList;
    Invocation(ThreadContext context) {
        this.context = context;
    }
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

    ThreadContext getThreadContext() {
        return context;
    }
}
