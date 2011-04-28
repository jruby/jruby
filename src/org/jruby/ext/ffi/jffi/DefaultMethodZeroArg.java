
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethodZeroArg extends DefaultMethod {
    
    public DefaultMethodZeroArg(RubyModule implementationClass, Function function, 
            FunctionInvoker functionInvoker, Signature signature) {
        super(implementationClass, function, functionInvoker, new ParameterMarshaller[0], signature);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        
        return getNativeInvoker().invoke(context);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return getNativeInvoker().invoke(context);
    }

}
