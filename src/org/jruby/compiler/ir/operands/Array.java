package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IR_Class;

// Represents an array [_, _, .., _] in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this array operand could get converted to calls
// that actually build a Ruby object
public class Array extends Operand
{
    public final Operand[] _elts;

    public Array() { _elts = new Operand[0]; }

    public Array(List<Operand> elts) { this(elts.toArray(new Operand[elts.size()])); }

    public Array(Operand[] elts) { _elts = (elts == null) ? new Operand[0] : elts; }

    public boolean isBlank() { return _elts.length == 0; }

    public String toString() { return "Array:" + (isBlank() ? "" : java.util.Arrays.toString(_elts)); }

// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() 
    {
        for (Operand o: _elts)
            if (!o.isConstant())
                return false;

       return true;
    }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        for (int i = 0; i < _elts.length; i++) {
            _elts[i] = _elts[i].getSimplifiedOperand(valueMap);
        }

        return this;
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        if (!getSubArray) {
            return (argIndex < _elts.length) ? _elts[argIndex] : Nil.NIL;
        }
        else {
            if (argIndex < _elts.length) {
                Operand[] newElts = new Operand[_elts.length-argIndex]; 
                System.arraycopy(_elts, argIndex, newElts, 0, newElts.length);
                return new Array(newElts);
            }
            else {
                return new Array();
            }
        }
    }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("Array"); }

    public Operand toArray() { return this; }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        for (Operand o: _elts)
            o.addUsedVariables(l);
    }
}
