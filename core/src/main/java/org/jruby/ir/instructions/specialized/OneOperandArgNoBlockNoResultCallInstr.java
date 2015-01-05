package org.jruby.ir.instructions.specialized;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgNoBlockNoResultCallInstr extends NoResultCallInstr {
    public OneOperandArgNoBlockNoResultCallInstr(CallType callType, String name, Operand receiver, Operand[] args, Operand closure) {
        super(Operation.NORESULT_CALL_1O, callType, name, receiver, args, closure);
    }

    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }

    public Operand getArg1() {
        return getCallArgs()[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneOperandArgNoBlockNoResultCallInstr(getCallType(), getName(), receiver.cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getCallArgs()[0].retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1);
    }
}
