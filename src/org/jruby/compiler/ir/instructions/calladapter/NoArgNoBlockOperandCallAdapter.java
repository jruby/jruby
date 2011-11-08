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
 * Adapter for: foo(), recv.foo() 
 */
public class NoArgNoBlockOperandCallAdapter extends CallAdapter {
    public NoArgNoBlockOperandCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);
    }

    @Override
    public Label call(InterpreterContext interp, ThreadContext context, Operand result, IRubyObject self, IRubyObject receiver) {
        result.store(interp, context, self, callSite.call(context, self, receiver));
        return null;
    }
}
