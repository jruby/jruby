package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class WrappedIRClosure extends Constant {
    private final IRClosure closure;

    public WrappedIRClosure(IRClosure scope) {
        this.closure = scope;
    }

    public IRClosure getClosure() {
        return closure;
    }

    @Override
    public String toString() {
        return closure.toString();
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self, Object[] temp) {
        BlockBody body = closure.getBlockBody();
        closure.getStaticScope().determineModule();
        Binding binding = context.currentBinding(self, context.getCurrentScope());

        return new Block(body, binding);
    }
}
