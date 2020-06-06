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
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgBlockCallInstr extends CallInstr {
    // clone constructor
    public OneOperandArgBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name,
                                       Operand receiver, Operand[] args, Operand closure, boolean isPotentiallyRefined,
                                       CallSite callSite, long callSiteId) {
        super(scope, Operation.CALL_1OB, callType, result, name, receiver, args, closure, isPotentiallyRefined, callSite, callSiteId);
    }

    // normal constructor
    public OneOperandArgBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name, Operand receiver, Operand[] args,
                                       Operand closure, boolean isPotentiallyRefined) {
        super(scope, Operation.CALL_1OB, callType, result, name, receiver, args, closure, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneOperandArgBlockCallInstr(ii.getScope(), getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii), isPotentiallyRefined(),
                getCallSite(), getCallSiteId());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        // NOTE: This logic shouod always match the CALL_10B logic in InterpreterEngine.processCall
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getArg1().retrieve(context, self, currScope, dynamicScope, temp);
        Block preparedBlock = prepareBlock(context, self, currScope, dynamicScope, temp);

        if (hasLiteralClosure()) {
            return getCallSite().callIter(context, self, object, arg1, preparedBlock);
        }

        return getCallSite().call(context, self, object, arg1, preparedBlock);
    }
}
