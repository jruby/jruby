package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

public class IRBreakJump extends RuntimeException implements Unrescuable {
    public DynamicScope scopeToReturnTo;
    public IRubyObject breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    private IRBreakJump() {}

    public static IRBreakJump create(DynamicScope scopeToReturnTo, IRubyObject rv) {
        IRBreakJump bj = new IRBreakJump();
        bj.scopeToReturnTo = scopeToReturnTo;
        bj.breakValue = rv;
        bj.caughtByLambda = false;
        bj.breakInEval = false;
        return bj;
    }

    @Override
    public Throwable fillInStackTrace() {
        if (Options.JUMP_BACKTRACE.load()) {
            return super.fillInStackTrace();
        }

        return this;
    }
}
