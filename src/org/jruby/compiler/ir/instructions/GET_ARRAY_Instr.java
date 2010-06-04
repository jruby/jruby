package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This is an internal ruby array generated during multiple assignment expressions.
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

    public String toString() { return "\t" + result + " = " + argument + "[" + _index + (_all ? ":END" : "") + "] (GET_ARRAY)"; }

    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap)
    {
        simplifyOperands(valueMap);
        Operand val = argument.getValue(valueMap);
        return val.fetchCompileTimeArrayElement(_index, _all);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GET_ARRAY_Instr(ii.getRenamedVariable(result), argument.cloneForInlining(ii), _index, _all);
    }
}
