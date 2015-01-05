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
    public ZeroOperandArgNoBlockCallInstr(Variable result, String name, Operand receiver, Operand[] args) {
        super(Operation.CALL_0O, CallType.NORMAL, result, name, receiver, args, null);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ZeroOperandArgNoBlockCallInstr(ii.getRenamedVariable(result), getName(), receiver.cloneForInlining(ii),
                cloneCallArgs(ii));
    }

    @Override
    public String toString() {
        return super.toString() + "{0O}";
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currScope, dynamicScope, temp);

        return getCallSite().call(context, self, object);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZeroOperandArgNoBlockCallInstr(this);
    }
}
