package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;
import org.jruby.RubyRange;

import org.jruby.compiler.ir.IRClass;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the Range object
public class Range extends Operand {
    Operand begin;
    Operand end;
    boolean exclusive;

    public Range(Operand begin, Operand end, boolean exclusive) {
        this.begin = begin;
        this.end = end;
        this.exclusive = exclusive;
    }

    @Override
    public String toString() {
        return "(" + begin + (exclusive ? ".." : "...") + end + "):Range";
    }

// ---------- These methods below are used during compile-time optimizations ------- 
    @Override
    public boolean isConstant() {
        return begin.isConstant() && end.isConstant();
    }

    @Override
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) {
        if (!isConstant()) return null;

        // SSS FIXME: Cannot optimize this without assuming that Range.to_ary method has not redefined.
        // So for now, return null!
        return null;
    }

    @Override
    public IRClass getTargetClass() {
        return IRClass.getCoreClass("Range");
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        begin = begin.getSimplifiedOperand(valueMap, force);
        end = end.getSimplifiedOperand(valueMap, force);
        // SSS FIXME: This operand is not immutable because of this
        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        begin.addUsedVariables(l);
        end.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return isConstant() ? this : new Range(begin.cloneForInlining(ii), end.cloneForInlining(ii), exclusive);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return RubyRange.newRange(context.getRuntime(), context,
                (IRubyObject) begin.retrieve(context, self, currDynScope, temp), 
                (IRubyObject) end.retrieve(context, self, currDynScope, temp), exclusive);
    }
}
