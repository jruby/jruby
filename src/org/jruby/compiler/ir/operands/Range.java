package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;
import org.jruby.RubyRange;

import org.jruby.compiler.ir.IRClass;
import org.jruby.interpreter.InterpreterContext;
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
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        begin = begin.getSimplifiedOperand(valueMap);
        end = end.getSimplifiedOperand(valueMap);
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
    public Object retrieve(InterpreterContext interp) {
        return RubyRange.newRange(interp.getContext().getRuntime(), interp.getContext(),
                (IRubyObject) begin.retrieve(interp), (IRubyObject) end.retrieve(interp), exclusive);
    }
}
