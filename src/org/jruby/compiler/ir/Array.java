package org.jruby.compiler.ir;

import java.util.List;

// Represents an array [_, _, .., _] in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this array operand could get converted to calls
// that actually build a Ruby object
public class Array extends Operand
{
    final public List<Operand> _elts;

	 public Array() { _elts = null; }

	 public Array(Operand[] elts) { _elts = (List<Operand>)java.util.Arrays.asList(elts); }

    public Array(List<Operand> elts) { _elts = elts; }

    public boolean isConstant() 
    {
		 if (_elts != null) {
			 for (Operand o: _elts)
				 if (!o.isConstant())
					 return false;
		 }

       return true;
    }

    public boolean isBlank() { return _elts == null || _elts.size() == 0; }
}
