package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

public class DynamicSymbol extends Operand {
    // SSS FIXME: Should this be Operand or CompoundString?
    // Can it happen that symbols are built out of other than compound strings?
    // Or can it happen during optimizations that this becomes a generic operand?
    final private CompoundString symbolName;

    public DynamicSymbol(CompoundString n) { symbolName = n; }

    public String toString() {
        return ":" + symbolName.toString();
    }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        CompoundString newSymbol = (CompoundString)symbolName.getSimplifiedOperand(valueMap, force);
        return symbolName == newSymbol ? this : new DynamicSymbol(newSymbol);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        symbolName.addUsedVariables(l);
    }

    public Operand cloneForInlining(InlinerInfo ii) {
        return symbolName.cloneForInlining(ii);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return context.runtime.newSymbol(((IRubyObject) symbolName.retrieve(context, self, currDynScope, temp)).asJavaString());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DynamicSymbol(this);
    }
}
