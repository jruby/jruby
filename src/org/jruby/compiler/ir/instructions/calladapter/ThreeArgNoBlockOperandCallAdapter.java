package org.jruby.compiler.ir.instructions.calladapter;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
/**
 */
public class ThreeArgNoBlockOperandCallAdapter extends CallAdapter {
    private final Operand arg1;
    private final Operand arg2;
    private final Operand arg3;
        
    public ThreeArgNoBlockOperandCallAdapter(CallSite callSite, Operand[] args) {        
        super(callSite);
        
        assert args.length == 3;
                
        this.arg1 = args[0];
        this.arg2 = args[1];
        this.arg3 = args[2];
    }

    @Override
    public Object call(ThreadContext context, IRubyObject self, IRubyObject receiver, Object[] temp) {
        IRubyObject value1 = (IRubyObject) arg1.retrieve(context, self, temp);
        IRubyObject value2 = (IRubyObject) arg2.retrieve(context, self, temp);        
        IRubyObject value3 = (IRubyObject) arg3.retrieve(context, self, temp);        
        return callSite.call(context, self, receiver, value1, value2, value3);
    }    
}
