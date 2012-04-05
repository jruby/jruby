
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

final class DefaultMethodOneArg extends DefaultMethod {
    
    public DefaultMethodOneArg(RubyModule implementationClass, Function function,
                               Signature signature, NativeInvoker defaultInvoker) {
        super(implementationClass, function, signature, defaultInvoker);
    }
    

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, 
            String name, IRubyObject arg1) {
        
        return getNativeInvoker().invoke(context, arg1);
    }
}
