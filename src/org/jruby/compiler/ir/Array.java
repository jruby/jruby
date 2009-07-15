package org.jruby.compiler.ir;

import java.util.List;

// Represents an array [_, _, .., _] in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this array operand could get converted to calls
// that actually build a Ruby object
public class Array extends Operand
{
    final public Operand[] _elts;

    public Array() { _elts = null; }

    public Array(Operand[] elts) { _elts = elts; }

    public Array(List<Operand> elts) { this(elts.toArray(new Operand[elts.size()])); }

    public boolean isBlank() { return _elts == null || _elts.length == 0; }

    public String toString() { return "Array:[" + (isBlank() ? "" : java.util.Arrays.toString(_elts)) + "]"; }

// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() 
    {
       if (_elts != null) {
          for (Operand o: _elts)
             if (!o.isConstant())
                return false;
       }

       return true;
    }

    public boolean isCompoundValue() { return true; }

    public boolean inCollapsedForm()
    {
       if (_elts != null) {
          for (Operand o: _elts)
             if (o.isCompoundValue())
                return false;
       }

       return true;
    }

    public Operand fetchCompileTimeArrayElement(int argIndex)
    {
        // If the array is constant and is in collapsed form, fetch the array element
        // else return null meaning we couldn't figure out the array value at compile time
        return (isConstant() && inCollapsedForm()) ? ((argIndex < _elts.length) ? _elts[argIndex] : Nil.NIL) : null;
    }

    public Operand toArray() { return this; }
}
