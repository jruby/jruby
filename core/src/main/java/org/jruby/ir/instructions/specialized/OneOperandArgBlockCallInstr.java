package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgBlockCallInstr extends CallInstr {
    public OneOperandArgBlockCallInstr(CallInstr call) {
        super(Operation.CALL_1OB, call);
    }

    @Override
    public String toString() {
        return super.toString() + "{1OB}";
    }

    public Operand getReceiver() {
        return receiver;
    }

    public Operand getArg1() {
        return getCallArgs()[0];
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getCallArgs()[0].retrieve(context, self, dynamicScope, temp);
        Block preparedBlock = prepareBlock(context, self, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1, preparedBlock);
    }
}
