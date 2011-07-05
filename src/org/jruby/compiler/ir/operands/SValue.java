package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.RubyArray;

// Represents a svalue node in Ruby code
//
// According to headius, svalue evaluates its value node and returns:
//  * nil if it does not evaluate to an array or if it evaluates to an empty array
//  * the first element if it evaluates to a one-element array
//  * the array if it evaluates to a >1 element array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class SValue extends Operand
{
    Operand _array;

    public SValue(Operand a) { _array = a; }

    public boolean isConstant() { return _array.isConstant(); }

    public String toString() { return "SValue(" + _array + ")"; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap)
    {
        _array = _array.getSimplifiedOperand(valueMap);
        if (_array instanceof Array) {
            Array a = (Array)_array;
            return (a.elts.length == 1) ? a.elts[0] : a;
        }
        else {
            return this;
        }
    }

    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray)
    {
        // SSS FIXME: This should never get called for constant svalues
        return null;
    }

    public boolean isNonAtomicValue() { return true; }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l)
    {
        _array.addUsedVariables(l);
    }
 
    public Operand cloneForInlining(InlinerInfo ii) { 
        return isConstant() ? this : new SValue(_array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        Object val = _array.retrieve(interp);
        if (val instanceof RubyArray) {
            int n = ((RubyArray)val).getLength();
            if (n == 0)
                return Nil.NIL.retrieve(interp); // SSS FIXME: interp.getRuntime().getNil();
            else if (n == 1)
                return ((RubyArray)val).entry(0);
            else
                return val;
        }
        else {
            return Nil.NIL.retrieve(interp); // SSS FIXME: interp.getRuntime().getNil();
        }
    }
}
