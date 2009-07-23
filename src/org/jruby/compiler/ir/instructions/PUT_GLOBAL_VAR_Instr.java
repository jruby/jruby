package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.GlobalVariable;
import org.jruby.compiler.ir.operands.Operand;

public class PUT_GLOBAL_VAR_Instr extends PUT_Instr
{
    public PUT_GLOBAL_VAR_Instr(String varName, Operand value)
    {
        super(Operation.PUT_GLOBAL_VAR, new GlobalVariable(varName), null, value);
    }
}
