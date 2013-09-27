package org.jruby.ir.instructions.calladapter;

import org.jruby.ir.operands.Operand;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneArgNoBlockConstantCallAdapter extends CallAdapter {
    private final Operand arg1;
    private IRubyObject constant1 = null;

    public OneArgNoBlockConstantCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);

        assert args.length == 1;

        this.arg1 = args[0];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        if (constant1 == null) constant1 = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, constant1);
    }
}
