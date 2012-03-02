package org.jruby.compiler.ir.instructions.specialized;

import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneOperandArgNoBlockCallInstr extends CallInstr {
    public OneOperandArgNoBlockCallInstr(CallInstr call) {
        super(call);
    }
    
    @Override
    public String toString() {
        return super.toString() + "{1O}";
    }    
    
    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        IRubyObject arg1 = (IRubyObject) getCallArgs()[0].retrieve(context, self, dynamicScope, temp);
        
        return getCallSite().call(context, self, object, arg1);
    }    
}
