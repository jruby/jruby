package org.jruby.ir.instructions.specialized;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgNoBlockNoResultCallInstr extends NoResultCallInstr {
    // clone constructor
    protected OneOperandArgNoBlockNoResultCallInstr(IRScope scope, CallType callType, RubySymbol name, Operand receiver,
                              Operand[] args, Operand closure, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, Operation.NORESULT_CALL_1O, callType, name, receiver, args, closure, potentiallyRefined, callSite, callSiteId);
    }

    // normal constructor
    public OneOperandArgNoBlockNoResultCallInstr(IRScope scope, CallType callType, RubySymbol name, Operand receiver, Operand[] args,
                                                 Operand closure, boolean isPotentiallyRefined) {
        super(scope, Operation.NORESULT_CALL_1O, callType, name, receiver, args, closure, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneOperandArgNoBlockNoResultCallInstr(ii.getScope(), getCallType(), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii),
                isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getArg1().retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1);
    }
}
