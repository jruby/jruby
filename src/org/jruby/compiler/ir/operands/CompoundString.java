package org.jruby.compiler.ir.operands;

import java.util.List;

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
}
