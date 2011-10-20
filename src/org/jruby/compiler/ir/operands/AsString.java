package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AsString extends Operand {
    Operand source; 

    public AsString(Operand source) {
        this.source = source;
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return ((IRubyObject)source.retrieve(interp, context, self)).asString();
    }

    @Override
    public String toString() {
        return "#{" + source + "}";
    }
}
