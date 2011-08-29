package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
public class SValue extends Operand {
    Operand array;

    public SValue(Operand array) {
        this.array = array;
    }

    @Override
    public boolean isConstant() {
        return array.isConstant();
    }

    @Override
    public String toString() {
        return "SValue(" + array + ")";
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        array = array.getSimplifiedOperand(valueMap);
        if (array instanceof Array) {
            Array a = (Array) array;
            return (a.elts.length == 1) ? a.elts[0] : a;
        }
        else {
            return this;
        }
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }
 
    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
        return isConstant() ? this : new SValue(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object val = array.retrieve(interp, context, self);
        
        if (val instanceof RubyArray) {
            int n = ((RubyArray) val).getLength();
            
            if (n == 0) return Nil.NIL.retrieve(interp, context, self); // SSS FIXME: interp.getRuntime().getNil();
            if (n == 1) return ((RubyArray) val).entry(0);
            
            return val;
        }

        return Nil.NIL.retrieve(interp, context, self); // SSS FIXME: interp.getRuntime().getNil();
    }
}
