package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class EQQ_Instr extends TwoOperandInstr {
    public EQQ_Instr(Variable result, Operand v1, Operand v2) {
        super(Operation.EQQ, result, v1, v2);
    }
}
