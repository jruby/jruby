package org.jruby.ir.runtime;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import org.jruby.ir.IRScope;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBreakJump extends RuntimeException {
    public int scopeIdToReturnTo;
    public IRubyObject breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    private IRBreakJump() {}

    // See https://jira.codehaus.org/browse/JRUBY-6523
    // Dont use static threadlocals because they leak classloaders.
    // Instead, use soft/weak references so that GC can collect these.

    private static ThreadLocal<Reference<IRBreakJump>> threadLocalBJ = new ThreadLocal<Reference<IRBreakJump>>();

    public static IRBreakJump create(int scopeIdToReturnTo, IRubyObject rv) {
        IRBreakJump bj;
        Reference<IRBreakJump> bjRef = threadLocalBJ.get();
        if (bjRef != null) {
            bj = bjRef.get();
        } else {
            bj = new IRBreakJump();
            threadLocalBJ.set(new SoftReference<IRBreakJump>(bj));
        }
        bj.scopeIdToReturnTo = scopeIdToReturnTo;
        bj.breakValue = rv;
        bj.caughtByLambda = false;
        bj.breakInEval = false;
        return bj;
    }
}
