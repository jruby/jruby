package org.jruby.ir.instructions.calladapter;

import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneArgBlockOperandCallAdapter extends ClosureCallAdapter {
    private Operand arg1;

    public OneArgBlockOperandCallAdapter(CallSite callSite, Operand[] args, Operand closure) {
        super(callSite, closure);

        assert args.length == 1;

        arg1 = args[0];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        IRubyObject value1 = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        Block block = prepareBlock(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, value1, block);
    }
}
