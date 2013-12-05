package org.jruby.ir.instructions.calladapter;

import org.jruby.ir.operands.Operand;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
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
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        return callSite.call(context, self, receiver);
    }
}
