package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class PushBindingInstr extends Instr {
    private IRScope scope;   // Scope for which frame is needed

    public PushBindingInstr(IRScope scope) {
        super(Operation.PUSH_BINDING);
        this.scope = scope;
    }
    
    public IRScope getScope() {
        return scope;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // The frame will now be allocated in the caller's scope
        return new PushBindingInstr(ii.getInlineHostScope());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushBindingInstr(this);
    }
}
