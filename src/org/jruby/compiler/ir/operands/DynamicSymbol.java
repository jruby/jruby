package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DynamicSymbol extends DynamicReference {
    public DynamicSymbol(CompoundString s) { super(s); }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return context.getRuntime().newSymbol(((IRubyObject)_refName.retrieve(interp, context, self)).asJavaString());
    }

	public String toString() {
        return ":" + _refName.toString();
	}
}
