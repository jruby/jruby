package org.jruby.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyLocalJumpError;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;

// Encapsulates exceptions to be thrown at runtime
public class IRException extends Operand {
    private String exceptionType;
    
    protected IRException(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public static final IRException RETRY_LocalJumpError = new IRException("LocalJumpError: retry outside of rescue not supported");
    public static final IRException NEXT_LocalJumpError = new IRException("LocalJumpError: unexpected next");
    public static final IRException BREAK_LocalJumpError = new IRException("LocalJumpError: unexpected break");
    public static final IRException RETURN_LocalJumpError = new IRException("LocalJumpError: unexpected return");
    public static final IRException REDO_LocalJumpError = new IRException("LocalJumpError: unexpected redo");

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("LocalJumpError:");
        if (this == NEXT_LocalJumpError) result.append("NEXT");
        else if (this == BREAK_LocalJumpError) result.append("BREAK");
        else if (this == RETURN_LocalJumpError) result.append("RETURN");
        else if (this == REDO_LocalJumpError) result.append("REDO");
        else if (this == RETRY_LocalJumpError) result.append("RETRY");
        return result.toString();
    }

    public RuntimeException getException(Ruby runtime) { 
        if (this == NEXT_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.NEXT, null, "unexpected next");
        else if (this == BREAK_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, null, "unexpected break");
        else if (this == RETURN_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETURN, null, "unexpected return");
        else if (this == REDO_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, null, "unexpected redo");
        else if (this == RETRY_LocalJumpError) return runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETRY, null, "retry outside of rescue not supported");
        else throw new RuntimeException("Unhandled case in operands/IRException.java");
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.IRException(this);
    }
}
