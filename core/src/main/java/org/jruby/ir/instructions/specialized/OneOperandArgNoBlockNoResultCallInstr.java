package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class OneOperandArgNoBlockNoResultCallInstr extends NoResultCallInstr {
    public OneOperandArgNoBlockNoResultCallInstr(NoResultCallInstr call) {
        super(Operation.NORESULT_CALL_1O, call);
    }

    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }

    public Operand getReceiver() {
        return receiver;
    }

    public Operand getArg1() {
        return getCallArgs()[0];
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getCallArgs()[0].retrieve(context, self, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1);
    }
}
