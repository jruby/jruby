package org.jruby.ir.operands;

import org.jruby.RubyRange;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a range (1..5) or (a..b) in ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, this range operand could get converted to calls
// that actually build the Range object
public class Range extends Operand {
    final private Operand begin;
    final private Operand end;
    private boolean exclusive;

    public Range(Operand begin, Operand end, boolean exclusive) {
        this.begin = begin;
        this.end = end;
        this.exclusive = exclusive;
    }

    @Override
    public String toString() {
        return begin + (exclusive ? ".." : "...") + end;
    }

// ---------- These methods below are used during compile-time optimizations ------- 
    @Override
    public boolean hasKnownValue() {
        return begin.hasKnownValue() && end.hasKnownValue();
    }

    @Override
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) {
        if (!hasKnownValue()) return null;

        // SSS FIXME: Cannot optimize this without assuming that Range.to_ary method has not redefined.
        // So for now, return null!
        return null;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newBegin = begin.getSimplifiedOperand(valueMap, force);
        Operand newEnd = end.getSimplifiedOperand(valueMap, force);
        return (newBegin == begin && newEnd == end) ? this : new Range(newBegin, newEnd, exclusive);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        begin.addUsedVariables(l);
        end.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return hasKnownValue() ? this : new Range(begin.cloneForInlining(ii), end.cloneForInlining(ii), exclusive);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return RubyRange.newRange(context.getRuntime(), context,
                (IRubyObject) begin.retrieve(context, self, currDynScope, temp), 
                (IRubyObject) end.retrieve(context, self, currDynScope, temp), exclusive);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Range(this);
    }
}
