package org.jruby.interpreter;

import org.jruby.compiler.ir.IRMethod;

public class IRBreakJump extends RuntimeException {
    public final IRMethod methodToReturnTo;
    public final Object breakValue;

    public IRBreakJump(IRMethod m, Object rv) {
        this.methodToReturnTo = m;
        this.breakValue = rv;
    }
}
