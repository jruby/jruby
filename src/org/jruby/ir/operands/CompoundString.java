package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.List;
import java.util.Map;

// This represents a compound string in Ruby
// Ex: - "Hi " + "there"
//     - "Hi #{name}"
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this string operand could get converted to calls
// that appends the components of the compound string into a single string object
public class CompoundString extends Operand {
    final private List<Operand> pieces;
    final private Encoding encoding;

    public CompoundString(List<Operand> pieces, Encoding encoding) {
        this.pieces = pieces;
        this.encoding = encoding;
    }

    public CompoundString(List<Operand> pieces) {
        this(pieces, null);
    }

    @Override
    public boolean hasKnownValue() {
        if (pieces != null) {
            for (Operand o : pieces) {
                if (!o.hasKnownValue()) return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "CompoundString:" + (encoding == null? "" : encoding) + (pieces == null ? "[]" : java.util.Arrays.toString(pieces.toArray()));
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        List<Operand> newPieces = new java.util.ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.getSimplifiedOperand(valueMap, force));
        }

        return new CompoundString(newPieces, encoding);
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
        if (hasKnownValue()) return this;

        List<Operand> newPieces = new java.util.ArrayList<Operand>();
        for (Operand p : pieces) {
            newPieces.add(p.cloneForInlining(ii));
        }

        return new CompoundString(newPieces, encoding);
    }

    // SSS FIXME: Buggy?
    String retrieveJavaString(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StringBuilder buf = new StringBuilder();

        for (Operand p : pieces) {
            buf.append(p.retrieve(context, self, currDynScope, temp));
        }

        return buf.toString();
    }

    public boolean isSameEncoding(StringLiteral str) {
        return str.bytelist.getEncoding() == encoding;
    }

    public RubyString[] retrievePieces(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        RubyString[] strings = new RubyString[pieces.size()];
        int i = 0;
        for (Operand p : pieces) {
            strings[i++] = (RubyString)p.retrieve(context, self, currDynScope, temp);
        }
        return strings;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: Doesn't work in all cases.  See example below
        //
        //    s = "x\234\355\301\001\001\000\000\000\200\220\376\257\356\b\n#{"\000" * 31}\030\200\000\000\001"
        //    s.length prints 70 instead of 52
        //
        // return context.getRuntime().newString(retrieveJavaString(interp, context, self));

        boolean is1_9 = context.runtime.is1_9();
        ByteList bytes = new ByteList();
        if (is1_9) bytes.setEncoding(encoding);
        RubyString str = RubyString.newStringShared(context.getRuntime(), bytes, StringSupport.CR_7BIT);
        for (Operand p : pieces) {
            if ((p instanceof StringLiteral) && (!is1_9 || isSameEncoding((StringLiteral)p))) {
                str.getByteList().append(((StringLiteral)p).bytelist);
            } else {
               IRubyObject pval = (IRubyObject)p.retrieve(context, self, currDynScope, temp);
               if (is1_9) str.append19(pval);
               else str.append(pval);
            }
        }

        return str;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CompoundString(this);
    }
}
