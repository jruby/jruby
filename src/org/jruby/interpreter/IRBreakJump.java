package org.jruby.interpreter;

import org.jruby.compiler.ir.IRExecutionScope;

public class IRBreakJump extends RuntimeException {
    public final IRExecutionScope scopeToReturnTo;
    public final Object breakValue;

    public IRBreakJump(IRExecutionScope s, Object rv) {
        this.scopeToReturnTo = s;
        this.breakValue = rv;
    }
}
