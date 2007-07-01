package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class Dispatcher {
    private static class DefaultDispatcher extends Dispatcher {
        public DefaultDispatcher() {
            switchTable = new byte[0];
        }
        
        public IRubyObject callMethod(ThreadContext context, IRubyObject self, RubyModule rubyclass, int methodIndex, String name,
                IRubyObject[] args, CallType callType, Block block) {
            return self.callMethod(context, rubyclass, name, args, callType, block);
        }
    };
    
    public static final Dispatcher DEFAULT_DISPATCHER = new DefaultDispatcher();
    
    protected byte[] switchTable;
    
    public abstract IRubyObject callMethod(ThreadContext context, IRubyObject self, RubyModule rubyclass, int methodIndex, String name,
            IRubyObject[] args, CallType callType, Block block);
    
    public void clearIndex(int index) {
        if (index >= switchTable.length) return;
        
        switchTable[index] = 0;
    }
}
