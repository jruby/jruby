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
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ZeroOperandArgNoBlockCallInstr extends CallInstr {
    // clone constructor
    public ZeroOperandArgNoBlockCallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                          Operand[] args, boolean isPotentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, op, callType, result, name, receiver, args, null, isPotentiallyRefined, callSite, callSiteId);
    }

    // normal constructor
    protected ZeroOperandArgNoBlockCallInstr(IRScope scope, Operation op, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                          Operand[] args, boolean isPotentiallyRefined) {
        super(scope, op, callType, result, name, receiver, args, null, isPotentiallyRefined);
    }

    // normal constructor
    public ZeroOperandArgNoBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver,
                                          Operand[] args, boolean isPotentiallyRefined) {
        super(scope, Operation.CALL_0O, callType, result, name, receiver, args, null, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        ZeroOperandArgNoBlockCallInstr zeroOperandArgNoBlockCallInstr = new ZeroOperandArgNoBlockCallInstr(ii.getScope(), getOperation(), getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), isPotentiallyRefined(), getCallSite(), getCallSiteId());

        zeroOperandArgNoBlockCallInstr.setProcNew(procNew);

        return zeroOperandArgNoBlockCallInstr;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);

        return getCallSite().call(context, self, object);
    }
}
