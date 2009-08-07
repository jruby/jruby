package org.jruby.compiler.ir.instructions;

// This is of the form:
//   d = s

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class COPY_Instr extends OneOperandInstr 
{
    public COPY_Instr(Variable d, Operand s)
    {
        super(Operation.COPY, d, s);
    }

    public String toString() { return "\t" + _result + " = " + _arg; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        simplifyOperands(valueMap);
        return _arg;
/**
        simplifyOperands(valueMap);
        // Since compound values get left in place and don't get replaced,
        // and we want to pass along the simplified value, look it up!
        return _arg instanceof Variable ? valueMap.get(_arg) : _arg;
**/
    }
}
