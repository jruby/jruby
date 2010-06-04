package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.Jump;
import org.jruby.runtime.builtin.IRubyObject;

public class BEQInstr extends BranchInstr {
    private Jump jumpTarget;

    public BEQInstr(Operand v1, Operand v2, Label jmpTarget) {
        super(Operation.BEQ, v1, v2, jmpTarget);
        
        jumpTarget = new Jump(jmpTarget);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BEQInstr(_arg1.cloneForInlining(ii), _arg2.cloneForInlining(ii), ii.getRenamedLabel(target));
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        Operand[] args = getOperands();
        Object value1 = args[0].retrieve(interp);
        Object value2 = args[1].retrieve(interp);

//        System.out.println("VALUE1: " + value1 + ", VALUE2: " + value2);
        // FIXME: Obviously inefficient

        if (value1 == value2) throw jumpTarget;
    }
}
