package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class Dispatcher {
    private static class DefaultDispatcher extends Dispatcher {
        public DefaultDispatcher() {
            switchTable = new byte[0];
        }
        
        public IRubyObject callMethod(ThreadContext context, Object selfObject, RubyModule rubyclass, int methodIndex, String name,
                IRubyObject[] args, CallType callType, Block block) {
            IRubyObject self = (IRubyObject)selfObject;
            
            try {
                assert args != null;
                DynamicMethod method = null;
                method = rubyclass.searchMethod(name);


                if (method.isUndefined() || (!name.equals("method_missing") && !method.isCallableFrom(context.getFrameSelf(), callType))) {
                    return RubyObject.callMethodMissing(context, self, method, name, args, context.getFrameSelf(), callType, block);
                }

                return method.call(context, self, rubyclass, name, args, block);
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
    };
    
    public static final Dispatcher DEFAULT_DISPATCHER = new DefaultDispatcher();
    
    protected byte[] switchTable;
    
    public abstract IRubyObject callMethod(ThreadContext context, Object self, RubyModule rubyclass, int methodIndex, String name,
            IRubyObject[] args, CallType callType, Block block);
    
    public void clearIndex(int index) {
        if (index >= switchTable.length) return;
        
        switchTable[index] = 0;
    }
}
