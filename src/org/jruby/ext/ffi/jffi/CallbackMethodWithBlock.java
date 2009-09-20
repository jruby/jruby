
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A DynamicMethod that has a callback argument as the last parameter.  It will
 * treat any block as the last argument.
 */
final class CallbackMethodWithBlock extends DynamicMethod {
    private final ParameterMarshaller[] marshallers;
    private final Function function;
    private final FunctionInvoker functionInvoker;
    private final int cbindex;
    
    public CallbackMethodWithBlock(RubyModule implementationClass, Function function, 
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers, int cbindex) {
        super(implementationClass, Visibility.PUBLIC, CallConfiguration.FrameFullScopeFull);
        this.function = function;
        this.functionInvoker = functionInvoker;
        this.marshallers = marshallers;
        this.cbindex = cbindex;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block) {
        boolean blockGiven = block.isGiven();
        Arity.checkArgumentCount(context.getRuntime(), args,
                marshallers.length - (blockGiven ? 1 : 0), marshallers.length);
        
        Invocation invocation = new Invocation(context);
        try {
            HeapInvocationBuffer buffer = new HeapInvocationBuffer(function);

            if (!blockGiven) {
                for (int i = 0; i < args.length; ++i) {
                    marshallers[i].marshal(invocation, buffer, args[i]);
                }
            } else {
                for (int i = 0; i < cbindex; ++i) {
                    marshallers[i].marshal(invocation, buffer, args[i]);
                }
                ((CallbackMarshaller)marshallers[cbindex]).marshal(invocation, buffer, block);
                for (int i = cbindex + 1; i < marshallers.length; ++i) {
                    marshallers[i].marshal(invocation, buffer, args[i - 1]);
                }
            }
            return functionInvoker.invoke(context.getRuntime(), function, buffer);
        } finally {
            invocation.finish();
        }
    }
    @Override
    public DynamicMethod dup() {
        return this;
    }
    @Override
    public Arity getArity() {
        return Arity.fixed(marshallers.length);
    }
    @Override
    public boolean isNative() {
        return true;
    }
}
