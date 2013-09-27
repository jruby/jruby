package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRMethod;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PopBindingInstr extends Instr {
    public PopBindingInstr() {
        super(Operation.POP_BINDING);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PopBindingInstr();
    }

    @Override
    public String toString() {
        return "" + getOperation();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PopBindingInstr(this);
    }
}
