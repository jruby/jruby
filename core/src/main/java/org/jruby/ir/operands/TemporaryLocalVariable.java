package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A set of variables which are only used in a particular scope and never
 * visible to Ruby itself.
 */
public class TemporaryLocalVariable extends TemporaryVariable {
    public final int offset;

    public TemporaryLocalVariable(int offset) {
        super();

        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.LOCAL;
    }

    @Override
    public String getName() {
        return getPrefix() + offset;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof TemporaryLocalVariable)) return false;

        return super.equals(other) && getOffset() == ((TemporaryLocalVariable) other).getOffset();
    }

    public String getPrefix() {
        return "%v_";
    }

    @Override
    public Variable clone(InlinerInfo ii) {
        return new TemporaryLocalVariable(offset);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: When AddLocalVarLoadStoreInstructions pass is not enabled, we don't need this check.
        // We only need these because Ruby code can have local vars used before being defined.
        //
        //    a = 1 if always-false
        //    p a   # should print nil since a is not defined on the else path
        //
        // Now, when locals are promoted to temps, this local-var behavior gets transferred to tmp-vars as well!
        //
        // If can canonicalize Ruby code to get rid of use-before-defs, we can get rid of the null checks
        // both here and in DynamicScope var lookups.  To be done later.
        //
        // I dont like this at all.  This feels ugly!
        Object o = temp[offset];
        return o == null ? context.nil : o;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.TemporaryLocalVariable(this);
    }
}
