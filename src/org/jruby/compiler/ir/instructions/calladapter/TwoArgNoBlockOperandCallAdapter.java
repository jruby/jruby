package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class TwoArgNoBlockOperandCallAdapter extends CallAdapter {
    private final Operand arg1;
    private final Operand arg2;
        
    public TwoArgNoBlockOperandCallAdapter(CallSite callSite, Operand[] args) {        
        super(callSite);
        
        assert args.length == 2;
                
        this.arg1 = args[0];
        this.arg2 = args[1];
    }

    @Override
    public Object call(InterpreterContext interp, ThreadContext context, IRubyObject self, IRubyObject receiver) {
        IRubyObject value1 = (IRubyObject) arg1.retrieve(interp, context, self);
        IRubyObject value2 = (IRubyObject) arg2.retrieve(interp, context, self);        
        return callSite.call(context, self, receiver, value1, value2);
    }
}
