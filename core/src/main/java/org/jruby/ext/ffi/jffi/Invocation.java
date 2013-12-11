package org.jruby.ext.ffi.jffi;

import java.util.ArrayList;
import org.jruby.runtime.ThreadContext;

/**
 * An invocation session.
 * This provides post-invoke cleanup.
 */
final class Invocation {
    private final ThreadContext context;
    private final int postInvokeCount;
    private final int referenceCount;
    private ArrayList<Runnable> postInvokeList;
    private ArrayList<Object> references;

    public Invocation(ThreadContext context) {
        this(context, 0, 0);
    }

    
    Invocation(ThreadContext context, int postInvokeCount, int referenceCount) {
        this.context = context;
        this.postInvokeCount = postInvokeCount;
        this.referenceCount = referenceCount;
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
             postInvokeList = new ArrayList<Runnable>(postInvokeCount);
        }
        postInvokeList.add(postInvoke);
    }

    ThreadContext getThreadContext() {
        return context;
    }

    void addReference(Object ref) {
        if (references == null) {
             references = new ArrayList<Object>(referenceCount);
        }
        references.add(ref);

    }
}
