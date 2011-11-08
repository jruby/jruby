package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneArgNoBlockConstantCallAdapter extends CallAdapter {
    private final Operand arg1;
    private IRubyObject constant1 = null;
    
    public OneArgNoBlockConstantCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);
        
        assert args.length == 1;
        
        this.arg1 = args[0];
    }

    @Override
    public Label call(InterpreterContext interp, ThreadContext context, Operand result, IRubyObject self, IRubyObject receiver) {
        if (constant1 == null) constant1 = (IRubyObject) arg1.retrieve(interp, context, self);
        result.store(interp, context, self, callSite.call(context, self, receiver, constant1));
        return null;
    }
}
