package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
// that appends the components of the compound string into a single string object
public class CompoundString extends Operand {

    final public List<Operand> pieces;

    public CompoundString(List<Operand> pieces) {
        this.pieces = pieces;
    }

    @Override
    public boolean isConstant() {
        if (pieces != null) {
            for (Operand o : pieces) {
                if (!o.isConstant()) return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "COMPOUND_STRING" + (pieces == null ? "" : java.util.Arrays.toString(pieces.toArray()));
    }

    @Override
    public boolean isNonAtomicValue() {
        return true;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
        int i = 0;
        for (Operand p : pieces) {
            pieces.set(i, p.getSimplifiedOperand(valueMap));
            i++;
        }

        return this;
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        for (Operand o : pieces) {
            o.addUsedVariables(l);
        }
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        if (isConstant()) return this;

        List<Operand> newPieces = new java.util.ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.cloneForInlining(ii));
        }

        return new CompoundString(newPieces);
    }

    @Override
    public Object retrieve(InterpreterContext interp) {
        StringBuilder buf = new StringBuilder();

        for (Operand p : pieces) {
            buf.append(p.retrieve(interp));
        }

        return interp.getRuntime().newString(buf.toString());
    }
}
