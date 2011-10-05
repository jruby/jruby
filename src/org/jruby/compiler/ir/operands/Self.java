
package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
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
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // SSS FIXME: Should we have a special case for self?
        //return interp.getLocalVariable(getName());
        return self;
    }

    @Override
    public Object store(InterpreterContext interp, ThreadContext context, IRubyObject self, Object value) {
        return self;
    }

    @Override
    public LocalVariable clone() {
        return this;
    }
}
