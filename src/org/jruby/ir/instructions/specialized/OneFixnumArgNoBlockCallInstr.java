package org.jruby.ir.instructions.specialized;

import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.operands.Fixnum;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class OneFixnumArgNoBlockCallInstr extends CallInstr {
    private final long arg1;
    
    public OneFixnumArgNoBlockCallInstr(CallInstr call) {
        super(call);
        
        assert getCallArgs().length == 1;
        
        this.arg1 = ((Fixnum) getCallArgs()[0]).value;
    }
    
    @Override
    public String toString() {
        return super.toString() + "{1F}";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope dynamicScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, dynamicScope, temp);
        
        return getCallSite().call(context, self, object, arg1);
    }
}
