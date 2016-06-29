package org.jruby.ir.instructions.specialized;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 6/8/16.
 */
public class TwoOperandArgNoBlockCallInstr  extends CallInstr  {
    public TwoOperandArgNoBlockCallInstr(CallType callType, Variable result, String name, Operand receiver,
                                         Operand[] args, boolean isPotentiallyRefined) {
        this(Operation.CALL_2O, callType, result, name, receiver, args, isPotentiallyRefined);
    }

    public TwoOperandArgNoBlockCallInstr(Operation op, CallType callType, Variable result, String name, Operand receiver,
                                         Operand[] args, boolean isPotentiallyRefined) {
        super(op, callType, result, name, receiver, args, null, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new TwoOperandArgNoBlockCallInstr(getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), isPotentiallyRefined());
    }

    public Operand getArg2() {
        return operands[2];
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getArg1().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg2 = (IRubyObject) getArg2().retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1, arg2);
    }
}
