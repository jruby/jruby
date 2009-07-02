package org.jruby.compiler.ir;

public class EQQ_Instr extends TwoOperandInstr {
    public EQQ_Instr(Operand result, Operation receiver, Operation argument) {
        super(Operation.EQQ, result, receiver, argument);
    }
}
