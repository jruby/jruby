/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class TwoArgNoBlockConstantCallAdapter extends CallAdapter {
    private final Operand arg1;
    private IRubyObject constant1 = null;
    private final Operand arg2;
    private IRubyObject constant2 = null;
    
    public TwoArgNoBlockConstantCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);

        assert args.length == 2;
        
        this.arg1 = args[0];
        this.arg2 = args[1];
    }

    @Override
    public Label call(InterpreterContext interp, ThreadContext context, Operand result, IRubyObject self, IRubyObject receiver) {
        if (constant1 == null) generateConstants(interp, context, self);
        IRubyObject returnValue = (IRubyObject) callSite.call(context, self, receiver, constant1, constant2);
        result.store(interp, context, self, returnValue);
        return null;
    }

    private void generateConstants(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        constant1 = (IRubyObject) arg1.retrieve(interp, context, self);
        constant2 = (IRubyObject) arg2.retrieve(interp, context, self);        
    }
}
