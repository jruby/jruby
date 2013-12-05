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
public class FourArgNoBlockConstantCallAdapter extends CallAdapter {
    private final Operand arg1;
    private IRubyObject constant1 = null;
    private final Operand arg2;
    private IRubyObject constant2 = null;
    private final Operand arg3;
    private IRubyObject constant3 = null;
    private final Operand arg4;
    private IRubyObject constant4 = null;

    public FourArgNoBlockConstantCallAdapter(CallSite callSite, Operand[] args) {
        super(callSite);

        assert args.length == 4;

        this.arg1 = args[0];
        this.arg2 = args[1];
        this.arg3 = args[2];
        this.arg4 = args[3];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, DynamicScope currDynScope, Object[] temp) {
        if (constant1 == null) generateConstants(context, self, currDynScope, temp);
        return callSite.call(context, self, receiver, constant1, constant2, constant3, constant4);
    }

    private void generateConstants(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        constant1 = (IRubyObject) arg1.retrieve(context, self, currDynScope, temp);
        constant2 = (IRubyObject) arg2.retrieve(context, self, currDynScope, temp);
        constant3 = (IRubyObject) arg3.retrieve(context, self, currDynScope, temp);
        constant4 = (IRubyObject) arg4.retrieve(context, self, currDynScope, temp);
    }
}
