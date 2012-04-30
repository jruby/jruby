/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author enebo
 */
public class LocalVariable extends Variable {
    protected String name;
    protected int scopeDepth;
    protected int offset;

    // FIXME: We should resolve to an index into an array but localvariable has no allocator
    public LocalVariable(String name, int scopeDepth, int location) {
        this.name = name;
        this.scopeDepth = scopeDepth;
        this.offset = location;
    }

    public int getScopeDepth() {
        return scopeDepth;
    }

    public int getLocation() {
        return offset;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return isSelf() ? name : name + "(" + scopeDepth + ":" + offset + ")";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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


    public boolean isSelf() {
        return false;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        IRubyObject value = currDynScope.getValue(offset, scopeDepth);
        if (value == null) value = context.nil;
        return value;
    }

    @Override
    public Variable cloneForCloningClosure(InlinerInfo ii) {
        return new LocalVariable(name, scopeDepth, offset);
    }

    // SSS FIXME: Better name than this?
    public LocalVariable cloneForDepth(int n) {
        return new LocalVariable(name, n, offset);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.LocalVariable(this);
    }
}
