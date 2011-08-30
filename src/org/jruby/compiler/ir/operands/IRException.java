package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyLocalJumpError;

// Encapsulates exceptions to be thrown at runtime
public class IRException extends Constant {
    private String exceptionType;
    protected IRException(String exceptionType) { this.exceptionType = exceptionType; }

    public static final IRException NEXT_LocalJumpError = new IRException("LocalJumpError: unexpected next");
    public static final IRException BREAK_LocalJumpError = new IRException("LocalJumpError: unexpected break");

    @Override
    public String toString() {
        return exceptionType;
    }

    public RuntimeException getException(Ruby runtime) { 
        if (this == NEXT_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.NEXT, null, "unexpected next");
        else if (this == BREAK_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, null, "unexpected break");
        else throw new RuntimeException("Unhandled case in operands/IRException.java");
    }
}
