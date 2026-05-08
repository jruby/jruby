package org.jruby.ir.instructions.specialized;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Float;
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

public class OneFloatArgNoBlockCallInstr extends CallInstr {
    private final double flote;

    // normal constructor
    public OneFloatArgNoBlockCallInstr(IRScope scope, CallType callType, Variable result, RubySymbol name,
                                       Operand receiver, Operand[] args, int flags, boolean potentiallyRefined) {
        super(scope, Operation.CALL_1D, callType, result, name, receiver, args, NullBlock.INSTANCE, flags, potentiallyRefined);

        assert args.length == 1;

        this.flote = ((Float) args[0]).value;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneFloatArgNoBlockCallInstr(ii.getScope(), getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), getFlags(), isPotentiallyRefined()
        );
    }

    public double getFloatArg() {
        return flote;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);

        IRRuntimeHelpers.setCallInfo(context, getFlags());

        return getCallSite().call(context, self, object, flote);
    }
}
