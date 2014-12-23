package org.jruby.ir.instructions.specialized;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.operands.Float;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class OneFloatArgNoBlockCallInstr extends CallInstr {
    private final double flote;

    public OneFloatArgNoBlockCallInstr(CallInstr call) {
        super(Operation.CALL_1D, call);

        assert getCallArgs().length == 1;

        this.flote = ((Float) getCallArgs()[0]).value;
    }

    @Override
    public String toString() {
        return super.toString() + "{1F}";
    }

    public double getFloatArg() {
        return flote;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currScope, dynamicScope, temp);
        return getCallSite().call(context, self, object, flote);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.OneFloatArgNoBlockCallInstr(this);
    }
}
