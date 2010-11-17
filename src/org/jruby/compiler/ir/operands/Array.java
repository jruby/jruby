package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IRClass;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents an array [_, _, .., _] in ruby
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this array operand could get converted to calls
// that actually build a Ruby object
public class Array extends Operand {
    public final Operand[] elts;

    public Array() {
        elts = new Operand[0];
    }

    public Array(List<Operand> elts) {
        this(elts.toArray(new Operand[elts.size()]));
    }

    public Array(Operand[] elts) {
        this.elts = elts == null ? new Operand[0] : elts;
    }

    public boolean isBlank() {
        return elts.length == 0;
    }

    @Override
    public String toString() {
        return "Array:" + (isBlank() ? "" : java.util.Arrays.toString(elts));
    }

// ---------- These methods below are used during compile-time optimizations ------- 
    @Override
    public boolean isConstant() {
        for (Operand o : elts) {
            if (!o.isConstant()) return false;
        }

        return true;
    }

    @Override
    public boolean isNonAtomicValue() { 
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        for (int i = 0; i < elts.length; i++) {
            elts[i] = elts[i].getSimplifiedOperand(valueMap);
        }

        return this;
    }

    @Override
    public Operand fetchCompileTimeArrayElement(int argIndex, boolean getSubArray) {
        if (!getSubArray) return argIndex < elts.length ? elts[argIndex] : Nil.NIL;

        if (argIndex < elts.length) {
            Operand[] newElts = new Operand[elts.length - argIndex];
            System.arraycopy(elts, argIndex, newElts, 0, newElts.length);

            return new Array(newElts);
        }

        return new Array();
    }

    @Override
    public IRClass getTargetClass() {
        return IRClass.getCoreClass("Array");
    }

    public Operand toArray() {
        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (Operand o: elts) {
            o.addUsedVariables(l);
        }
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
        if (isConstant()) return this;

        Operand[] newElts = new Operand[elts.length];
        for (int i = 0; i < elts.length; i++) {
            newElts[i] = elts[i].cloneForInlining(ii);
        }

        return new Array(newElts);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        IRubyObject[] elements = new IRubyObject[elts.length];

        for (int i = 0; i < elements.length; i++) {
            elements[i] = (IRubyObject) elts[i].retrieve(interp);
        }

        return interp.getRuntime().newArray(elements);
    }
}
