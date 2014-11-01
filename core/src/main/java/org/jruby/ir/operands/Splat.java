package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand implements DepthCloneable {
    final private Operand array;
    final public boolean unsplatArgs;

    public Splat(Operand array, boolean unsplatArgs) {
        super(OperandType.SPLAT);
        this.array = array;
        this.unsplatArgs = unsplatArgs;
    }

    public Splat(Operand array) {
        this(array, false);
    }

    @Override
    public String toString() {
        return (unsplatArgs ? "*(unsplat)" : "*") + array;
    }

    @Override
    public boolean hasKnownValue() {
        return false; /*_array.isConstant();*/
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newArray = array.getSimplifiedOperand(valueMap, force);
        return (newArray == array) ? this : new Splat(newArray, unsplatArgs);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }

    /** When fixing up splats in nested closure we need to tweak the operand if it is a LocalVariable */
    public Operand cloneForDepth(int n) {
        return array instanceof LocalVariable ? new Splat(((LocalVariable) array).cloneForDepth(n), unsplatArgs) : this;
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return hasKnownValue() ? this : new Splat(array.cloneForInlining(ii), unsplatArgs);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        IRubyObject arrayVal = (IRubyObject) array.retrieve(context, self, currScope, currDynScope, temp);
        // SSS FIXME: Some way to specialize this code?
        return Helpers.irSplat(context, arrayVal);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Splat(this);
    }
}
