package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class NoArgBlockOperandCallAdapter extends ClosureCallAdapter {
    public NoArgBlockOperandCallAdapter(CallSite callSite, Operand[] args, Operand closure) {
        super(callSite, closure);
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        Block block = prepareBlock(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, block);
    }
}
