package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class GET_ARRAY_Instr extends OneOperandInstr
{
    public final int     _index;
    public final boolean _all;  // If true, returns the rest of the array starting at the index

    public GET_ARRAY_Instr(Variable dest, Operand array, int index, boolean getRestOfArray)
    {
        super(Operation.GET_ARRAY, dest, array);
        _index = index;
        _all   = getRestOfArray; 
    }

    public String toString() { return "\t" + _result + " = " + _arg + "[" + _index + (_all ? ":END" : "") + "] (GET_ARRAY)"; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        Operand array = valueMap.get(_arg);
        if (array == null)
            array = _arg;

        return _arg.fetchCompileTimeArrayElement(_index, _all);
    }
}
