package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ClosureReturnInstr extends Instr {
    private Operand returnValue;

    public ClosureReturnInstr(Operand rv) {
        super(Operation.CLOSURE_RETURN);
        this.returnValue = rv;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    public Operand getReturnValue() {
        return returnValue;
    }

    public Operand[] getOperands() {
        return new Operand[]{returnValue};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        returnValue = returnValue.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + returnValue + ")";
    }

    @Interp
    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block, Object exception) {
        return returnValue.retrieve(interp, context, self);
    }
}
