/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author enebo
 */
public class LocalVariable extends Variable {
    protected String name;
    protected int scopeDepth;
    protected int location;

    // FIXME: We should resolve to an index into an array but localvariable has no allocator
    public LocalVariable(String name, int scopeDepth, int location) {
        this.name = name;
        this.scopeDepth = scopeDepth;
        this.location = location;
    }

    public int getScopeDepth() {
        return scopeDepth;
    }

    public int getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return isSelf() ? name : name + "(" + scopeDepth + ":" + location + ")";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean isSelf() {
		  return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LocalVariable)) return false;

        return name.equals(((LocalVariable) obj).name);
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof LocalVariable)) return 0;

        return name.compareTo(((LocalVariable) arg0).name);
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return interp.getLocalVariable(context, scopeDepth, location);
    }

    @Override
    public Object store(InterpreterContext interp, ThreadContext context, IRubyObject self, Object value) {
        return interp.setLocalVariable(scopeDepth, location, value);
    }

    // SSS FIXME: Better name than this?
    public LocalVariable cloneForDepth(int n) {
        return new LocalVariable(name, n, location);
    }
}
