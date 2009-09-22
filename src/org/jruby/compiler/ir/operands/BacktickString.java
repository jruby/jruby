package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// This represents a backtick string in Ruby
// Ex: `ls .`; `cp #{src} #{dst}`
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
public class BacktickString extends Operand
{
    final public List<Operand> _pieces;

    public BacktickString(Operand val) { _pieces = new ArrayList<Operand>(); _pieces.add(val); }
    public BacktickString(List<Operand> pieces) { _pieces = pieces; }

    public boolean isConstant() {
       for (Operand o: _pieces)
          if (!o.isConstant())
             return false;

       return true;
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
