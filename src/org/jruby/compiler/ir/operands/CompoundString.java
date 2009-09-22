package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
// that appends the components of the compound string into a single string object
public class CompoundString extends Operand
{
    final public List<Operand> _pieces;

    public CompoundString(List<Operand> pieces) { _pieces = pieces; }

    public boolean isConstant() 
    {
       if (_pieces != null) {
          for (Operand o: _pieces)
             if (!o.isConstant())
                return false;
       }

       return true;
    }

    public String toString() { 
       return "COMPOUND_STRING" + (_pieces == null ? "" : java.util.Arrays.toString(_pieces.toArray()));
    }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        int i = 0;
        for (Operand p: _pieces) {
           _pieces.set(i, p.getSimplifiedOperand(valueMap));
           i++;
        }

        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        for (Operand o: _pieces)
            o.addUsedVariables(l);
    }
}
