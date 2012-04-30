package org.jruby.ir.operands;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;

/**
 * This represents a variable used in a closure that is
 * local to the closure and is not defined in any ancestor lexical scope
 */
public class ClosureLocalVariable extends LocalVariable {
    final public IRClosure definingScope;

    public ClosureLocalVariable(IRClosure scope, String name, int scopeDepth, int location) {
        super(name, scopeDepth, location);
        this.definingScope = scope;
    }

    @Override
    public String toString() {
        return "<" + name + "(" + scopeDepth + ":" + offset + ")>";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ClosureLocalVariable)) return false;

        return name.equals(((LocalVariable) obj).name);
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof ClosureLocalVariable)) return 0;

        return name.compareTo(((LocalVariable) arg0).name);
    }

    @Override
    public Variable cloneForCloningClosure(InlinerInfo ii) {
        return new ClosureLocalVariable(ii.getClonedClosure(), name, scopeDepth, offset);
    }

    // SSS FIXME: Better name than this?
    public LocalVariable cloneForDepth(int n) {
        return new ClosureLocalVariable(definingScope, name, n, offset);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClosureLocalVariable(this);
    }
}
