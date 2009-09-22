package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IR_Class;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the Range object
public class Range extends Operand
{
    Operand _begin;
    Operand _end;

    public Range(Operand b, Operand e) { _begin = b; _end = e; }

    public String toString() { return "(" + _begin + ".." + _end + "):Range"; }

// ---------- These methods below are used during compile-time optimizations ------- 
    public boolean isConstant() 
    {
        return _begin.isConstant() && _end.isConstant();
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        if (!isConstant())
            return null;

        // SSS FIXME: Cannot optimize this without assuming that Range.to_ary method has not redefined.
        // So for now, return null!
        return null;
    }

    public IR_Class getTargetClass() { return IR_Class.getCoreClass("Range"); }

    public boolean isNonAtomicValue() { return true; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _begin = _begin.getSimplifiedOperand(valueMap);
        _end = _end.getSimplifiedOperand(valueMap);
        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _begin.addUsedVariables(l);
        _end.addUsedVariables(l);
    }
}
