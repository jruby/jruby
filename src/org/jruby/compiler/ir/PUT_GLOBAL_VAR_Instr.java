package org.jruby.compiler.ir;

public class PUT_GLOBAL_VAR_Instr extends TwoOperandInstr
{
    public PUT_GLOBAL_VAR_Instr(String varName, Object value)
    {
        super(Operation.PUT_GLOBAL_VAR, null, new GlobalVariable(varName), value);
    }
}
