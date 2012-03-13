package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Symbol extends Reference {
    public Symbol(String name) {
        super(name);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return context.getRuntime().newSymbol(getName());
    }

    @Override
    public String toString() {
        return ":" + getName();
    }

    @Override
    public void compile(JVM jvm) {
        jvm.method().push(getName());
    }
}
