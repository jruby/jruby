package org.jruby.compiler.ir;

public class BTRUE_Instr extends BRANCH_Instr {
    public BTRUE_Instr(Operand v1, Label jmpTarget)
    {
        super(Operation.BTRUE, v1, null, jmpTarget);
    }
}
