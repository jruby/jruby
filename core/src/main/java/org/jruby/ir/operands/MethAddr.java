package org.jruby.ir.operands;

// Placeholder class for method address

import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MethAddr extends Reference {
    public static final MethAddr NO_METHOD = new MethAddr("");
    public static final MethAddr UNKNOWN_SUPER_TARGET  = new MethAddr("-unknown-super-target-");

    public MethAddr(String name) {
        super(name);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MethAddr) && ((MethAddr)o).getName().equals(getName());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MethAddr(this);
    }
    
    @Override
    public String toString() {
        return "'" + getName() + "'";
    }
}
