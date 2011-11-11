package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Adapter for: foo(), recv.foo() 
 */
public class NoArgNoBlockOperandCallAdapter extends CallAdapter {
    public NoArgNoBlockOperandCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);
    }

    @Override
    public Object call(InterpreterContext interp, ThreadContext context, IRubyObject self, IRubyObject receiver, Object[] temp) {
        return callSite.call(context, self, receiver);
    }
}
