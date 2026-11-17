package org.jruby.ir.instructions.specialized;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgNoBlockCallInstr extends CallInstr {
    // specific instr constructor
    public OneOperandArgNoBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name,
                                         Operand receiver, Operand[] args, int flags, boolean isPotentiallyRefined) {
        this(scope, Operation.CALL_1O, callType, result, name, receiver, args, flags, isPotentiallyRefined);
    }

    // normal constructor
    public OneOperandArgNoBlockCallInstr(IRScope scope, Operation op, CallType callType, Variable result,
                                         RubySymbol name, Operand receiver, Operand[] args, int flags,
                                         boolean isPotentiallyRefined) {
        super(scope, op, callType, result, name, receiver, args, NullBlock.INSTANCE, flags, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneOperandArgNoBlockCallInstr(ii.getScope(), Operation.CALL_1O, getCallType(),
                ii.getRenamedVariable(result), getName(), getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                getFlags(), isPotentiallyRefined());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getArg1().retrieve(context, self, currScope, dynamicScope, temp);

        IRRuntimeHelpers.setCallInfo(context, getFlags());

        return getCallSite().call(context, self, object, arg1);
    }
}
