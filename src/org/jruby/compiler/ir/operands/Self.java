package org.jruby.compiler.ir.operands;

import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Self extends LocalVariable {
    public static final Self SELF = new Self();

    private Self() {
        super("%self", 0, 0);
    }

    public boolean isSelf() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;

    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return self;
    }

    @Override
    public LocalVariable clone() {
        return this;
    }
}
