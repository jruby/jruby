package org.jruby.ir.operands;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/**
 * This represents a non-temporary variable used in a closure
 * and defined in this or a parent closure.
 */
public class ClosureLocalVariable extends LocalVariable {
    // Note that we cannot use (scopeDepth > 0) check to detect this.
    // When a dyn-scope is eliminated for a leaf scope, depths for all
    // closure local vars are decremented by 1 => a non-local variable
    // can have scope depth 0.
    //
    // Can only transition in one direction (from true to false)
    private boolean definedLocally;

    public ClosureLocalVariable(String name, int scopeDepth, int location) {
        super(name, scopeDepth, location);
        this.definedLocally = true;
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

    public boolean isDefinedLocally() {
        return definedLocally;
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        ClosureLocalVariable lv = new ClosureLocalVariable(name, scopeDepth, offset);
        lv.definedLocally = definedLocally;
        return lv;
    }

    public LocalVariable cloneForDepth(int n) {
        ClosureLocalVariable lv = new ClosureLocalVariable(name, n, offset);
        if (definedLocally && n > 0) {
            lv.definedLocally = false;
        }
        return lv;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClosureLocalVariable(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(definedLocally);
    }

    @Override
    public String toString() {
        return "<" + name + "(" + scopeDepth + ":" + offset + ":local=" + definedLocally + ")>";
    }
}
