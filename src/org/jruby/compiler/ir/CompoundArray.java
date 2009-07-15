package org.jruby.compiler.ir;

import java.util.List;

// This represents a concatenation of an array and a splat
// Ex: a = 1,2,3,*[5,6,7]
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this will get built into an actual array object
public class CompoundArray extends Operand
{
    final public Operand _a1;
    final public Operand _a2;

    private Operand _simplifiedValue;

    public CompoundArray(Operand a1, Operand a2) { _a1 = a1; _a2 = a2; }

    public boolean isConstant() { return _a1.isConstant() && _a2.isConstant(); }

    public String toString() { return _a1 + ", *" + _a2; }

    public Operand getSimplifiedValue()
    {
        if ((_a1 instanceof Array) && (_a2 instanceof Array)) {
            // SSS FIXME: Move this code to some utils area .. or probably there is already a method for this in some jruby utils class
            // Holy cow!  Just to append two darned arrays!
            Operand[] a1Elts = ((Array)_a1)._elts;
            Operand[] a2Elts = ((Array)_a2)._elts;
            Operand[] newElts = new Operand[a1Elts.length + a2Elts.length];
            System.arraycopy(a1Elts, 0, newElts, 0, a1Elts.length);
            System.arraycopy(a2Elts, 0, newElts, a1Elts.length, a2Elts.length);
            return new Array(newElts);
        }
        else {
            return this;
        }
    }

    // SSS FIXME: Premature optimization of GET_ARRAY ... make this part of a pass of peephole optimization!
    public Operand fetchCompileTimeArrayElement(int argIndex)
    {
        // SSS FIXME: This is not the right approach -- we'll need to reset this value on each opt. pass.
        if (_simplifiedValue == null)
            _simplifiedValue = getSimplifiedValue();

        return (_simplifiedValue == this) ? null : _simplifiedValue.fetchCompileTimeArrayElement(argIndex);
    }
}
