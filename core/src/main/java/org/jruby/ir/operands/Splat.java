package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand {
    final private Operand array;

    public Splat(Operand array) {
        this.array = array;
    }

    @Override
    public boolean hasKnownValue() {
        return false; /*_array.isConstant();*/
    }

    @Override
    public String toString() {
        return "*" + array;
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newArray = array.getSimplifiedOperand(valueMap, force);
        /*
         * SSS FIXME:  Cannot convert this to an Array operand!
         *
        if (_array instanceof Variable) {
        _array = ((Variable)_array).getValue(valueMap);
        }
         */
        return (newArray == array) ? this : new Splat(newArray);
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
        return hasKnownValue() ? this : new Splat(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        IRubyObject arrayVal = (IRubyObject) array.retrieve(context, self, currDynScope, temp);
        // SSS FIXME: Some way to specialize this code?
        return Helpers.irSplat(context, arrayVal);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Splat(this);
    }
}
