package org.jruby.compiler.ir;

public class GET_GLOBAL_VAR_Instr extends OneOperandInstr
{
    public GET_GLOBAL_VAR_Instr(Variable dest, String gvarName)
    {
        super(Operation.GET_GLOBAL_VAR, dest, new GlobalVariable(gvarName));
    }
}
