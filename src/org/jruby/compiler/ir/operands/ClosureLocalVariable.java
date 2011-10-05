package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.interpreter.InterpreterContext;

/**
 * This represents a variable used in a closure that is
 * local to the closure and is not defined in any ancestor lexical scope
 */
public class ClosureLocalVariable extends LocalVariable {
    public final IRClosure definingScope;
    public ClosureLocalVariable(IRClosure scope, String name, int scopeDepth, int location) {
        super(name, scopeDepth, location);
        this.definingScope = scope;
    }

    @Override
    public String toString() {
        return "<" + name + "(" + scopeDepth + ":" + location + ")>";
    }

    // SSS FIXME: a = "<v>"; b = ClosureLocalVariable("v"); a.hashCode() == b.hashCode() but a.equals(b) == false
    // Strictly speaking, this is inconsistent.  But, as long as we are not comparing strings and local variables, we are okay.
    @Override
    public int hashCode() {
        return toString().hashCode();
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
    public LocalVariable clone() {
        return new ClosureLocalVariable(definingScope, name, scopeDepth, location);
    }
}
