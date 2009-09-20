
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethodThreeArg extends DefaultMethod {
    private final ParameterMarshaller m1, m2, m3;
    
    public DefaultMethodThreeArg(RubyModule implementationClass, Function function,
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers) {
        super(implementationClass, function, functionInvoker, marshallers);
        m1 = marshallers[0];
        m2 = marshallers[1];
        m3 = marshallers[2];
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context);
            try {
                m1.marshal(invocation, buffer, arg1);
                m2.marshal(invocation, buffer, arg2);
                m3.marshal(invocation, buffer, arg3);
                return functionInvoker.invoke(context.getRuntime(), function, buffer);
            } finally {
                invocation.finish();
            }
        } else {
            m1.marshal(context, buffer, arg1);
            m2.marshal(context, buffer, arg2);
            m3.marshal(context, buffer, arg3);
            return functionInvoker.invoke(context.getRuntime(), function, buffer);
        }
    }

}
