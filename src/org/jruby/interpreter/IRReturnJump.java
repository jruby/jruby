package org.jruby.interpreter;

import org.jruby.compiler.ir.IRMethod;

public class IRReturnJump extends RuntimeException {
    public final IRMethod methodToReturnFrom;
    public final Object returnValue;

    public IRReturnJump(IRMethod m, Object rv) {
        this.methodToReturnFrom = m;
        this.returnValue = rv;
    }
}
