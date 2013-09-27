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

public class PushFrameInstr extends Instr {
    public PushFrameInstr() {
        super(Operation.PUSH_FRAME);
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PushFrameInstr(this);
    }
}
