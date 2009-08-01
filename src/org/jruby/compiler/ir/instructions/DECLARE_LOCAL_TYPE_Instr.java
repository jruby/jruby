package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class DECLARE_LOCAL_TYPE_Instr extends NoOperandInstr
{
    int     _argIndex;
    String  _typeName;

    public DECLARE_LOCAL_TYPE_Instr(Variable dest, int index, String type)
    {
        super(Operation.DECLARE_TYPE, dest);
        _argIndex = index;
        _typeName = type;
    }

    public String toString() { return super.toString() + "(" + _argIndex + ":" + _typeName  + ")"; }
}
