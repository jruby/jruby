package org.jruby.compiler.ir;

public class PUT_GLOBAL_VAR_Instr extends PUT_Instr
{
    public PUT_GLOBAL_VAR_Instr(String varName, Operand value)
    {
        super(Operation.PUT_GLOBAL_VAR, new GlobalVariable(varName), null, value);
    }
}
