
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethodFiveArg extends DefaultMethod {
    
    public DefaultMethodFiveArg(RubyModule implementationClass, Function function,
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers,
            Signature signature) {
        super(implementationClass, function, functionInvoker, marshallers, signature);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        return getNativeInvoker().invoke(context, arg1, arg2, arg3, arg4, arg5);
    }
    
}
