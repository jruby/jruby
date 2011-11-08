package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ManyArgBlockOperandCallAdapter extends ClosureCallAdapter {
    private Operand[] args;
    
    public ManyArgBlockOperandCallAdapter(CallSite callSite, Operand[] args, Operand closure) {
        super(callSite, closure);
        
        this.args = args;
    }

    @Override
    public Label call(InterpreterContext interp, ThreadContext context, Operand result, IRubyObject self, IRubyObject receiver) {
        IRubyObject[] values = prepareArguments(interp, context, self, args);
        Block block = prepareBlock(interp, context, self);

        try {
            result.store(interp, context, self, callSite.call(context, self, receiver, values, block));
        } finally {
            block.escape();
        }
        
        return null;
    }

    protected IRubyObject[] prepareArguments(InterpreterContext interp, ThreadContext context, IRubyObject self, Operand[] args) {
        IRubyObject[] newArgs = new IRubyObject[args.length];

        for (int i = 0; i < args.length; i++) {
            newArgs[i] = (IRubyObject) args[i].retrieve(interp, context, self);
        }

        return newArgs;
    }
}
