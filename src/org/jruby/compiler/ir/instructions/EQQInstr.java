package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends TwoOperandInstr {
    public EQQInstr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, v1, v2);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new EQQInstr(ii.getRenamedVariable(result), operand1.cloneForInlining(ii), operand2.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        IRubyObject receiver = (IRubyObject) getOperand1().retrieve(interp);
        IRubyObject value = (IRubyObject) getOperand2().retrieve(interp);
        getResult().store(interp, receiver.callMethod(interp.getContext(), "===", value));
        
        return null;
    }


}
