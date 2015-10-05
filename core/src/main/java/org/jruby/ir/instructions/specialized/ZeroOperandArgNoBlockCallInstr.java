package org.jruby.ir.instructions.specialized;

import org.jruby.ir.IRVisitor;
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

public class ZeroOperandArgNoBlockCallInstr extends CallInstr {
    public ZeroOperandArgNoBlockCallInstr(CallType callType, Variable result, String name, Operand receiver,
                                          Operand[] args, boolean isPotentiallyRefined) {
        super(Operation.CALL_0O, callType, result, name, receiver, args, null, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ZeroOperandArgNoBlockCallInstr(getCallType(), ii.getRenamedVariable(result), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), isPotentiallyRefined());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);

        return getCallSite().call(context, self, object);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZeroOperandArgNoBlockCallInstr(this);
    }
}
