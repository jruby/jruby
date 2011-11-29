package org.jruby.interpreter;

import org.jruby.compiler.ir.IRScope;

public class IRBreakJump extends RuntimeException {
    public final IRScope scopeToReturnTo;
    public final Object breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    public IRBreakJump(IRScope s, Object rv) {
        this.scopeToReturnTo = s;
        this.breakValue = rv;
        this.caughtByLambda = false;
        this.breakInEval = false;
    }
}
