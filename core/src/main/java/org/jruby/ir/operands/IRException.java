package org.jruby.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyLocalJumpError;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

import java.util.List;

// Encapsulates exceptions to be thrown at runtime
public class IRException extends Operand {
    private final RubyLocalJumpError.Reason type;

    protected IRException(RubyLocalJumpError.Reason type) {
        super(OperandType.IR_EXCEPTION);

        this.type = type;
    }

    public RubyLocalJumpError.Reason getType() {
        return type;
    }

    public static final IRException RETRY_LocalJumpError = new IRException(RubyLocalJumpError.Reason.RETRY);
    public static final IRException NEXT_LocalJumpError = new IRException(RubyLocalJumpError.Reason.NEXT);
    public static final IRException BREAK_LocalJumpError = new IRException(RubyLocalJumpError.Reason.BREAK);
    public static final IRException RETURN_LocalJumpError = new IRException(RubyLocalJumpError.Reason.RETURN);
    public static final IRException REDO_LocalJumpError = new IRException(RubyLocalJumpError.Reason.REDO);

    public static IRException getExceptionFromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal > RubyLocalJumpError.Reason.values().length) {
            throw new IllegalArgumentException("Invalid ordinal value for jump error: " + ordinal);
        }

        switch(RubyLocalJumpError.Reason.values()[ordinal]) {
            case RETRY: return RETRY_LocalJumpError;
            case NEXT: return NEXT_LocalJumpError;
            case BREAK: return BREAK_LocalJumpError;
            case RETURN: return RETURN_LocalJumpError;
            case REDO: return REDO_LocalJumpError;
        }

        return null; // not reached.
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public RuntimeException getException(Ruby runtime) {
        switch (getType()) {
            case NEXT: return runtime.newLocalJumpError(getType(), null, "unexpected next");
            case BREAK: return runtime.newLocalJumpError(getType(), null, "unexpected break");
            case RETURN: return runtime.newLocalJumpError(getType(), null, "unexpected return");
            case REDO: return runtime.newLocalJumpError(getType(), null, "unexpected redo");
            case RETRY: return runtime.newLocalJumpError(getType(), null, "retry outside of rescue not supported");
        }
        throw new RuntimeException("Unhandled case in operands/IRException.java");
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.IRException(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode((byte) getType().ordinal());
    }

    public static IRException decode(IRReaderDecoder d) {
        return getExceptionFromOrdinal(d.decodeByte());
    }
    @Override
    public String toString() {
        return "LocalJumpError:" + getType();
    }
}
