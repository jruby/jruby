package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

public class IRBreakJump extends IRJump implements Unrescuable {
    public DynamicScope scopeToReturnTo;
    public IRubyObject breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    private IRBreakJump(DynamicScope scopeToReturnTo, IRubyObject rv) {
        this.scopeToReturnTo = scopeToReturnTo;
        this.breakValue = rv;
        this.caughtByLambda = false;
        this.breakInEval = false;
    }

    public static IRBreakJump create(DynamicScope scopeToReturnTo, IRubyObject rv) {
        return new IRBreakJump(scopeToReturnTo, rv);
    }
}
