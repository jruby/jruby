
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
    protected final int postInvokeCount;
    protected final int referenceCount;

    public DefaultMethod(RubyModule implementationClass, Function function,
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers) {
        super(implementationClass, Arity.fixed(marshallers.length), function, functionInvoker);
        this.marshallers = marshallers;

        int piCount = 0;
        int refCount = 0;
        for (ParameterMarshaller m : marshallers) {
            if (m.requiresPostInvoke()) {
                ++piCount;
            }

            if (m.requiresReference()) {
                ++refCount;
            }
        }
        this.postInvokeCount = piCount;
        this.referenceCount = refCount;
        this.needsInvocationSession = piCount > 0 || refCount > 0;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);
        if (needsInvocationSession) {
            Invocation invocation = new Invocation(context, postInvokeCount, referenceCount);
            try {
                for (int i = 0; i < args.length; ++i) {
                    marshallers[i].marshal(invocation, buffer, args[i]);
                }
                return functionInvoker.invoke(context, function, buffer);
            } finally {
                invocation.finish();
            }
        } else {
            for (int i = 0; i < args.length; ++i) {
                marshallers[i].marshal(context, buffer, args[i]);
            }
            return functionInvoker.invoke(context, function, buffer);
        }
    }
}
