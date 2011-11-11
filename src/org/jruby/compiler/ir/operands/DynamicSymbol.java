package org.jruby.compiler.ir.operands;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DynamicSymbol extends DynamicReference {
    public DynamicSymbol(CompoundString s) { super(s); }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, Object[] temp) {
        return context.getRuntime().newSymbol(((IRubyObject)_refName.retrieve(context, self, temp)).asJavaString());
    }

    public String toString() {
        return ":" + _refName.toString();
    }
}
