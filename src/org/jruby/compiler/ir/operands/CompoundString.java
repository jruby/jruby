package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.RubyBasicObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;

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

    // SSS FIXME: Buggy?
    String retrieveJavaString(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        StringBuilder buf = new StringBuilder();

        for (Operand p : pieces) {
            buf.append(p.retrieve(interp, context, self));
        }

        return buf.toString();
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // SSS FIXME: Doesn't work in all cases.  See example below
        //
        //    s = "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"
        //    s.length prints 70 instead of 52
        //
        // return context.getRuntime().newString(retrieveJavaString(interp, context, self));

        ByteList bytes = new ByteList();
        //if (is19()) bytes.setEncoding(encoding);
        RubyString str = RubyString.newStringShared(context.getRuntime(), bytes, StringSupport.CR_7BIT);
        for (Operand p : pieces) {
            if (p instanceof StringLiteral) {
                str.getByteList().append(((StringLiteral)p)._bl_value);
            } else {
                str.append((IRubyObject)p.retrieve(interp, context, self));
            }
        }

        return str;
    }
}
