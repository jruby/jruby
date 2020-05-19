package org.jruby.ir.operands;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/**
 * This represents a non-temporary variable used in a closure
 * and defined in this or a parent closure.
 */
public class ClosureLocalVariable extends LocalVariable {
    public ClosureLocalVariable(RubySymbol name, int scopeDepth, int location) {
        super(name, scopeDepth, location);
    }

    public ClosureLocalVariable(RubySymbol name, int scopeDepth, int location, boolean isOuterScopeVar) {
        super(name, scopeDepth, location, isOuterScopeVar);
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
        // FIXME: LocalVariable simply returns this .. why not here?
        return new ClosureLocalVariable(name, scopeDepth, offset, isOuterScopeVar);
    }

    public LocalVariable cloneForDepth(int n) {
        return n > scopeDepth ? new ClosureLocalVariable(name, n, offset) : new ClosureLocalVariable(name, n, offset, isOuterScopeVar);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClosureLocalVariable(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
    }

    @Override
    public String toString() {
        return name + "(" + scopeDepth + ":" + offset + ":local=" + !isOuterScopeVar() + ")";
    }
}
