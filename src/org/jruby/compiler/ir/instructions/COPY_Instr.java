package org.jruby.compiler.ir.instructions;

// This is of the form:

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

//   d = s
public class COPY_Instr extends OneOperandInstr 
{
    public COPY_Instr(Variable d, Operand s)
    {
        super(Operation.COPY, d, s);
    }

    public String toString() { return "\t" + _result + " = " + _arg; }

    // Copy propagation
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        return valueMap.get(_arg);
    }
}
