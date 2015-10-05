package org.jruby.ir.instructions.specialized;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneFloatArgNoBlockCallInstr extends CallInstr {
    private final double flote;

    public OneFloatArgNoBlockCallInstr(CallType callType, Variable result, String name, Operand receiver, Operand[] args,
                                       boolean potentiallyRefined) {
        super(Operation.CALL_1D, callType, result, name, receiver, args, null, potentiallyRefined);

        assert args.length == 1;

        this.flote = ((Float) args[0]).value;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneFloatArgNoBlockCallInstr(getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), isPotentiallyRefined());
    }

    public double getFloatArg() {
        return flote;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, flote);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OneFloatArgNoBlockCallInstr(this);
    }
}
