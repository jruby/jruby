package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YieldInstr extends Instr {
    // SSS FIXME: Correct?  Where does closure arg come from?
    Operand yieldArg;
    public YieldInstr(Variable result, Operand arg) {
        super(Operation.YIELD, result);
        this.yieldArg = arg;
    }
   
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: This needs to be cloned!
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object resultValue;
        if (yieldArg == null) {
            resultValue = interp.getBlock().call(interp.getContext());
        } else {
            resultValue = interp.getBlock().yield(interp.getContext(), (IRubyObject)yieldArg.retrieve(interp));
        }
        getResult().store(interp, resultValue);
        return null;
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + yieldArg + ")";
    }

    public Operand[] getOperands() {
        return (yieldArg == null) ? new Operand[]{} : new Operand[] {yieldArg};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        if (yieldArg != null) yieldArg = yieldArg.getSimplifiedOperand(valueMap);
    }
}
