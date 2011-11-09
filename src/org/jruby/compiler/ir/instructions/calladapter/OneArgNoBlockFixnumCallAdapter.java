package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Fixnum;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
class OneArgNoBlockFixnumCallAdapter extends CallAdapter {
    private long arg1;

    public OneArgNoBlockFixnumCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);
        
        assert args.length == 1;
        
        this.arg1 = ((Fixnum) args[0]).value.longValue();
    }

    @Override
    public Object call(InterpreterContext interp, ThreadContext context, IRubyObject self, IRubyObject receiver) {
        return (IRubyObject) callSite.call(context, self, receiver, arg1);
    }
}
