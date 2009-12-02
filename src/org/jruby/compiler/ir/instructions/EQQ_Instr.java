package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQ_Instr extends TwoOperandInstr {
    public EQQ_Instr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, v1, v2);
    }
}
