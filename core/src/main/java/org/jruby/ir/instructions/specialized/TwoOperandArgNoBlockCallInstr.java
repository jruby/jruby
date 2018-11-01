package org.jruby.ir.instructions.specialized;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
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

public class TwoOperandArgNoBlockCallInstr  extends CallInstr  {
    public TwoOperandArgNoBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                         Operand[] args, boolean isPotentiallyRefined) {
        this(scope, Operation.CALL_2O, callType, result, name, receiver, args, isPotentiallyRefined);
    }

    public TwoOperandArgNoBlockCallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                         Operand[] args, boolean isPotentiallyRefined) {
        super(scope, op, callType, result, name, receiver, args, null, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new TwoOperandArgNoBlockCallInstr(ii.getScope(), getCallType(), ii.getRenamedVariable(result), getName(),
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
