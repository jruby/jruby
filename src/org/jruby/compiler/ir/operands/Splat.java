package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;

import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand {
    private Operand array;

    public Splat(Operand array) {
        this.array = array;
    }

    @Override
    public boolean isConstant() {
        return false; /*_array.isConstant();*/ 
    }

    @Override
    public String toString() {
        return "*" + array;
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        /*
         * SSS FIXME:  Cannot convert this to an Array operand!
         *
        _array = _array.getSimplifiedOperand(valueMap);
        if (_array instanceof Variable) {
        _array = ((Variable)_array).getValue(valueMap);
        }
         */
        return this;
    }

    @Override
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) {
        return array.fetchCompileTimeArrayElement(argIndex, getSubArray);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return isConstant() ? this : new Splat(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return RuntimeHelpers.splatValue((IRubyObject) array.retrieve(interp, context, self));
    }
}
