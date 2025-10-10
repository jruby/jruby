package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBreakJump extends IRJump implements Unrescuable {
    public final DynamicScope scopeToReturnTo;
    public final IRubyObject breakValue;
    public boolean breakInEval;

    private IRBreakJump(DynamicScope scopeToReturnTo, IRubyObject rv, boolean breakInEval) {
        this.scopeToReturnTo = scopeToReturnTo;
        this.breakValue = rv;
        this.breakInEval = breakInEval;
    }

    @Deprecated(since = "9.1.9.0")
    public static IRBreakJump create(DynamicScope scopeToReturnTo, IRubyObject rv) {
        return new IRBreakJump(scopeToReturnTo, rv, false);
    }

    public static IRBreakJump create(DynamicScope scopeToReturnTo, IRubyObject rv, boolean breakInEval) {
        return new IRBreakJump(scopeToReturnTo, rv, breakInEval);
    }
}
