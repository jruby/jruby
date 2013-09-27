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
public class TemporaryVariable extends Variable {
    public final int offset;
    String name;

    public TemporaryVariable(int offset) {
        this.offset = offset;
        this.name = getPrefix() + offset;
    }

	 // Used for temporary variables like %current_module, %_arg_array
    public TemporaryVariable(String name, int offset) {
        this.offset = offset;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TemporaryVariable)) return false;

        return name.equals(((TemporaryVariable) obj).name);
    }

    public int compareTo(Object other) {
        if (!(other instanceof TemporaryVariable)) return 0;

        return name.compareTo(((TemporaryVariable) other).name);
    }

    protected String getPrefix() {
        return "%v_";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Variable cloneForCloningClosure(InlinerInfo ii) {
        return new TemporaryVariable(name, offset);
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
        visitor.TemporaryVariable(this);
    }
}
