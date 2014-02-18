package org.jruby.ir.runtime;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import org.jruby.exceptions.Unrescuable;

public class IRReturnJump extends RuntimeException implements Unrescuable {
    public int methodToReturnFrom;
    public Object returnValue;

    private IRReturnJump() {}

    // See https://jira.codehaus.org/browse/JRUBY-6523
    // Dont use static threadlocals because they leak classloaders.
    // Instead, use soft/weak references so that GC can collect these.

    private static ThreadLocal<Reference<IRReturnJump>> threadLocalRJ = new ThreadLocal<Reference<IRReturnJump>>();

    public static IRReturnJump create(int scopeId, Object rv) {
        IRReturnJump rj;
        Reference<IRReturnJump> rjRef = threadLocalRJ.get();
        if (rjRef != null) {
            rj = rjRef.get();
        } else {
            rj = new IRReturnJump();
            threadLocalRJ.set(new SoftReference<IRReturnJump>(rj));
        }
        rj.methodToReturnFrom = scopeId;
        rj.returnValue = rv;
        return rj;
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
