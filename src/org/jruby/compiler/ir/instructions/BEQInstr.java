package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends BranchInstr {
    public BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BEQInstr(getArg1().cloneForInlining(ii), 
                getArg2().cloneForInlining(ii), ii.getRenamedLabel(getJumpTarget()));
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block) {
        Operand arg1 = getArg1();
        Operand arg2 = getArg2();
        Object value1 = arg1.retrieve(interp, context, self);
        
        if (arg2 instanceof BooleanLiteral) {
            boolean v1True  = ((IRubyObject)value1).isTrue();
            boolean arg2True = ((BooleanLiteral)arg2).isTrue();
            return (v1True && arg2True) || (!v1True && !arg2True) ? getJumpTarget() : null;
        } else {
            Object value2 = arg2.retrieve(interp, context, self);
            boolean eql = ((arg2 == Nil.NIL) || (arg2 == UndefinedValue.UNDEFINED)) ?
                    value1 == value2 : ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue();
            return eql ? getJumpTarget() : null;
        }
    }
}
