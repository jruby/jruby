package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Symbol extends Reference {
    public Symbol(String name) {
        super(name);
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return context.getRuntime().newSymbol(getName());
    }

    @Override
    public String toString() {
        return ":" + getName();
    }
}
