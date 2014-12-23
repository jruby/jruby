package org.jruby.ir.instructions.specialized;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneOperandArgNoBlockCallInstr extends CallInstr {
    public OneOperandArgNoBlockCallInstr(CallInstr call) {
        super(Operation.CALL_1O, call);
    }

    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }

    public Operand getArg1() {
        return getCallArgs()[0];
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getCallArgs()[0].retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, arg1);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OneOperandArgNoBlockCallInstr(this);
    }
}
