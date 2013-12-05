/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.calladapter;

import org.jruby.ir.operands.Operand;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class OneArgNoBlockOperandCallAdapter extends CallAdapter {
    private final Operand arg1;

    public OneArgNoBlockOperandCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);

        assert args.length == 1;

        this.arg1 = args[0];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        IRubyObject value1 = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, value1);
    }
}
