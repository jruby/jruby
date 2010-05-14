package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class YIELD_Instr extends MultiOperandInstr
{
    // SSS FIXME: Correct?  Where does closure arg come from?
    public YIELD_Instr(Variable result, Operand[] args)
    {
        super(Operation.YIELD, result, args);
    }
   
    public boolean isRubyInternalsCall() { return false; }
    public boolean isStaticCallTarget()  { return false; }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Attempt to clone YIELD instruction.  We should never get here!");
    }
}
