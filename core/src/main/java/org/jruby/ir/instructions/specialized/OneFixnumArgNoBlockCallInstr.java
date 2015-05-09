package org.jruby.ir.instructions.specialized;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneFixnumArgNoBlockCallInstr extends CallInstr {
    private final long fixNum;

    public OneFixnumArgNoBlockCallInstr(CallType callType, Variable result, String name, Operand receiver, Operand[] args, boolean potentiallyRefined) {
        super(Operation.CALL_1F, callType, result, name, receiver, args, null, potentiallyRefined);

        assert args.length == 1;

        this.fixNum = ((Fixnum) args[0]).value;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneFixnumArgNoBlockCallInstr(getCallType(), ii.getRenamedVariable(result), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), isPotentiallyRefined());
    }

    public long getFixnumArg() {
        return fixNum;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, fixNum);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OneFixnumArgNoBlockCallInstr(this);
    }
}
