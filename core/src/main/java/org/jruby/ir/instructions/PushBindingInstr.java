package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PushBindingInstr extends Instr {
    private IRScope scope;   // Scope for which frame is needed

    public PushBindingInstr(IRScope scope) {
        super(Operation.PUSH_BINDING);
        this.scope = scope;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case CLOSURE_INLINE:
            case METHOD_INLINE:
                // FIXME: Is this correct??
                // Why do we need to push a scope?
                // PopBinding doesn't seem to be popping on inlining
                return new PushBindingInstr(ii.getInlineHostScope());
            default:
                return new PushBindingInstr(scope);
        }
    }

    @Override
    public String toString() {
        return "" + getOperation() + "(" + scope + ")";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBindingInstr(this);
    }
}
