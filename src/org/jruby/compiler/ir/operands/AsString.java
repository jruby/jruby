package org.jruby.compiler.ir.operands;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AsString extends Operand {
    Operand source; 

    public AsString(Operand source) {
        this.source = source;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, Object[] temp) {
        return ((IRubyObject)source.retrieve(context, self, temp)).asString();
    }

    @Override
    public String toString() {
        return "#{" + source + "}";
    }
}
