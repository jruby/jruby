package org.jruby.ir.operands;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

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
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ClosureLocalVariable)) return false;

        return hashCode() == obj.hashCode();
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof ClosureLocalVariable)) return 0;

        int a = hashCode();
        int b = arg0.hashCode();
        return a < b ? -1 : (a == b ? 0 : 1);
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return new ClosureLocalVariable((IRClosure) ii.getScope(), name, scopeDepth, offset);
    }

    public LocalVariable cloneForDepth(int n) {
        return new ClosureLocalVariable(definingScope, name, n, offset);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClosureLocalVariable(this);
    }

    @Override
    public String toString() {
        return "<" + name + "(" + scopeDepth + ":" + offset + ")>";
    }
}
