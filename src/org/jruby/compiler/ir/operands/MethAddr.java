package org.jruby.compiler.ir.operands;

// Placeholder class for method address

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MethAddr extends Reference {
    public static final MethAddr NO_METHOD = new MethAddr("");
    public static final MethAddr UNKNOWN_SUPER_TARGET  = new MethAddr("-unknown-super-target-");

    public MethAddr(String name) {
        super(name);
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MethAddr) && ((MethAddr)o).getName().equals(getName());
    }
}
