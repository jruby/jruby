package org.jruby.compiler.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyLocalJumpError;

// Encapsulates exceptions to be thrown at runtime
public class IRException extends Constant {
    private String exceptionType;
    protected IRException(String exceptionType) { this.exceptionType = exceptionType; }

    public static final IRException RETRY_LocalJumpError = new IRException("LocalJumpError: retry outside of rescue not supported");
    public static final IRException NEXT_LocalJumpError = new IRException("LocalJumpError: unexpected next");
    public static final IRException BREAK_LocalJumpError = new IRException("LocalJumpError: unexpected break");
    public static final IRException RETURN_LocalJumpError = new IRException("LocalJumpError: unexpected return");
    public static final IRException REDO_LocalJumpError = new IRException("LocalJumpError: unexpected redo");

    @Override
    public String toString() {
        return exceptionType;
    }

    public RuntimeException getException(Ruby runtime) { 
        if (this == NEXT_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.NEXT, null, "unexpected next");
        else if (this == BREAK_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, null, "unexpected break");
        else if (this == RETURN_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETURN, null, "unexpected return");
        else if (this == REDO_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, null, "unexpected redo");
        else if (this == RETRY_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, null, "retry outside of rescue not supported");
        else throw new RuntimeException("Unhandled case in operands/IRException.java");
    }
}
