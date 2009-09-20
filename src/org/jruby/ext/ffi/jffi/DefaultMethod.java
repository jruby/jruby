
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethod extends JFFIDynamicMethod {
    private final ParameterMarshaller[] marshallers;
    protected final boolean needsInvocationSession;
    public DefaultMethod(RubyModule implementationClass, Function function,
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers) {
        super(implementationClass, Arity.fixed(marshallers.length), function, functionInvoker);
        this.marshallers = marshallers;
        boolean needsInvocation = false;
        for (ParameterMarshaller m : marshallers) {
            if (m.needsInvocationSession()) {
                needsInvocation = true;
                break;
            }
        }
        this.needsInvocationSession = needsInvocation;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context);
            try {
                for (int i = 0; i < args.length; ++i) {
                    marshallers[i].marshal(invocation, buffer, args[i]);
                }
                return functionInvoker.invoke(context.getRuntime(), function, buffer);
            } finally {
                invocation.finish();
            }
        } else {
            for (int i = 0; i < args.length; ++i) {
                marshallers[i].marshal(context, buffer, args[i]);
            }
            return functionInvoker.invoke(context.getRuntime(), function, buffer);
        }
    }
}
