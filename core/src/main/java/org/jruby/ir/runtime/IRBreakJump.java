package org.jruby.ir.runtime;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.IRScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBreakJump extends RuntimeException implements Unrescuable {
    public DynamicScope scopeToReturnTo;
    public IRubyObject breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    private IRBreakJump() {}

    // See https://jira.codehaus.org/browse/JRUBY-6523
    // Dont use static threadlocals because they leak classloaders.
    // Instead, use soft/weak references so that GC can collect these.

    private static ThreadLocal<Reference<IRBreakJump>> threadLocalBJ = new ThreadLocal<Reference<IRBreakJump>>();

    public static IRBreakJump create(DynamicScope scopeToReturnTo, IRubyObject rv) {
        IRBreakJump bj;
        Reference<IRBreakJump> bjRef = threadLocalBJ.get();
        if (bjRef != null) {
            bj = bjRef.get();
        } else {
            bj = new IRBreakJump();
            threadLocalBJ.set(new SoftReference<IRBreakJump>(bj));
        }
        bj.scopeToReturnTo = scopeToReturnTo;
        bj.breakValue = rv;
        bj.caughtByLambda = false;
        bj.breakInEval = false;
        return bj;
    }
}
