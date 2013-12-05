package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class ZeroOperandArgNoBlockCallInstr extends CallInstr {
    public ZeroOperandArgNoBlockCallInstr(CallInstr call) {
        super(Operation.CALL_0O, call);
    }

    @Override
    public String toString() {
        return super.toString() + "{0O}";
    }

    public Operand getReceiver() {
        return receiver;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);

        return getCallSite().call(context, self, object);
    }
}
