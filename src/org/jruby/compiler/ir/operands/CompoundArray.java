package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

// This represents a concatenation of an array and a splat
// Ex: a = 1,2,3,*[5,6,7]
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this will get built into an actual array object
public class CompoundArray extends Operand
{
    Operand _a1;
    Operand _a2;

    public CompoundArray(Operand a1, Operand a2) { _a1 = a1; _a2 = a2; }

    public boolean isConstant() { return _a1.isConstant() && _a2.isConstant(); }

    public String toString() { return _a1 + ", *" + _a2; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _a1 = _a1.getSimplifiedOperand(valueMap);
        _a2 = _a2.getSimplifiedOperand(valueMap);

        // For simplification, get the target value, even if compound
        Operand p1 = _a1;
        if (p1 instanceof Variable)
            p1 = ((Variable)p1).getValue(valueMap);

        // For simplification, get the target value, even if compound
        Operand p2 = _a2;
        if (p2 instanceof Variable)
            p2 = ((Variable)p2).getValue(valueMap);

        if ((p1 instanceof Array) && (p2 instanceof Array)) {
            // SSS FIXME: Move this code to some utils area .. or probably there is already a method for this in some jruby utils class
            // Holy cow!  Just to append two darned arrays!
            Operand[] p1Elts = ((Array)p1)._elts;
            Operand[] p2Elts = ((Array)p2)._elts;
            Operand[] newElts = new Operand[p1Elts.length + p2Elts.length];
            System.arraycopy(p1Elts, 0, newElts, 0, p1Elts.length);
            System.arraycopy(p2Elts, 0, newElts, p1Elts.length, p2Elts.length);
            return new Array(newElts);
        }
        else {
            return this;
        }
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        // SSS FIXME: For constant arrays, we should never get here!
        return null;
    }

    public boolean isNonAtomicValue() { return true; }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _a1.addUsedVariables(l);
        _a2.addUsedVariables(l);
    }
}
