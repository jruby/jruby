package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends BranchInstr {
    public BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BEQInstr(operand1.cloneForInlining(ii), operand2.cloneForInlining(ii), ii.getRenamedLabel(target));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Operand[] args = getOperands();
        Object value1 = args[0].retrieve(interp);
        Object value2 = args[1].retrieve(interp);

//        System.out.println("VALUE1: " + value1 + ", VALUE2: " + value2);
        // FIXME: Obviously inefficient

        return (value1 == value2) ? target : null;
    }
}
