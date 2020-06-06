package org.jruby.ir.instructions.specialized;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

public class OneArgOperandAttrAssignInstr extends AttrAssignInstr {
    // clone constructor
    protected OneArgOperandAttrAssignInstr(IRScope scope, CallType callType, RubySymbol name, Operand receiver,
                              Operand[] args, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, callType, name, receiver, args, potentiallyRefined, callSite, callSiteId);
    }

    // normal constructor
    public OneArgOperandAttrAssignInstr(IRScope scope, Operand obj, RubySymbol attr, Operand[] args, boolean isPotentiallyRefined) {
        super(scope, obj, attr, args, null, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new OneArgOperandAttrAssignInstr(ii.getScope(), getCallType(), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject value = (IRubyObject) getArg1().retrieve(context, self, currScope, dynamicScope, temp);

        callSite.call(context, self, object, value);

        return null;
    }
}
