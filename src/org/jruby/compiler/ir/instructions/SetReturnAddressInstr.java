package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is of the form:
//    v = LBL_..
// Used in rescue blocks to tell the ensure block where to return to after it is done doing its thing.
public class SetReturnAddressInstr extends Instr {
    private Label returnAddr;

    public SetReturnAddressInstr(Variable d, Label l) {
        super(Operation.SET_RETADDR, d);
        this.returnAddr = l;
    }

    public Label getReturnAddr() {
        return (Label) returnAddr;
    }

    public Operand[] getOperands() {
        return new Operand[]{returnAddr};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
    }

    @Override
    public String toString() {
        return "" + getResult() + " = " + returnAddr;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SetReturnAddressInstr(ii.getRenamedVariable(getResult()), ii.getRenamedLabel(returnAddr));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, returnAddr);
        
        return null;
    }
}
