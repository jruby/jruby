package org.jruby.compiler.ir.operands;

import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * For argument processing.  If an opt arg does not exist we will return
 * this so instrs can reason about non-existent arguments.
 */
public class UndefinedValue extends Operand {
    public static final Operand UNDEFINED = new UndefinedValue();
    
    private UndefinedValue() {}

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return this;
    }
}
