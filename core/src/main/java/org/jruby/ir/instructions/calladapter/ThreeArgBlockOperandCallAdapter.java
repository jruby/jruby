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
public class ThreeArgBlockOperandCallAdapter extends ClosureCallAdapter {
    private Operand arg1;
    private Operand arg2;
    private Operand arg3;

    public ThreeArgBlockOperandCallAdapter(CallSite callSite, Operand[] args, Operand closure) {
        super(callSite, closure);

        assert args.length == 3;

        arg1 = args[0];
        arg2 = args[1];
        arg3 = args[2];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        IRubyObject value1 = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        IRubyObject value2 = (IRubyObject) arg2.retrieve(context, self, currDynScope, temp);
        IRubyObject value3 = (IRubyObject) arg3.retrieve(context, self, currDynScope, temp);
        Block block = prepareBlock(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, value1, value2, value3, block);
    }
}
