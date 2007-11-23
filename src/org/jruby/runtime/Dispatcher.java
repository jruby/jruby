package org.jruby.runtime;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class Dispatcher {
    private static class DefaultDispatcher extends Dispatcher {
        public DefaultDispatcher() {
            switchTable = new byte[0];
        }
        
        public IRubyObject callMethod(ThreadContext context, IRubyObject self, RubyClass rubyclass, int methodIndex, String name,
                IRubyObject[] args, CallType callType, Block block) {
            try {
                return RuntimeHelpers.invokeAs(context, rubyclass, self, name, args, callType, block);
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
    };
    
    public static final Dispatcher DEFAULT_DISPATCHER = new DefaultDispatcher();
    
    protected byte[] switchTable;
    
    public abstract IRubyObject callMethod(ThreadContext context, IRubyObject self, RubyClass rubyclass, int methodIndex, String name,
            IRubyObject[] args, CallType callType, Block block);
    
    public void clearIndex(int index) {
        if (index >= switchTable.length) return;
        
        switchTable[index] = 0;
    }
}
