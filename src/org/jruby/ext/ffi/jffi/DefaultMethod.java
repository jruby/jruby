
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethod extends JFFIDynamicMethod {
    private final ParameterMarshaller[] marshallers;
    protected final boolean needsInvocationSession;
    protected  final Signature signature;
    private final NativeInvoker defaultInvoker;
    private NativeInvoker compiledInvoker;
    private JITHandle jitHandle;
    

    public DefaultMethod(RubyModule implementationClass, Function function,
            FunctionInvoker functionInvoker, ParameterMarshaller[] marshallers,
            Signature signature) {
        super(implementationClass, Arity.fixed(marshallers.length), function, functionInvoker);
        this.marshallers = marshallers;
        this.signature = signature;

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
        this.needsInvocationSession = piCount > 0 || refCount > 0;
        this.compiledInvoker = null;
        this.jitHandle = null;
        this.defaultInvoker = new BufferNativeInvoker(function, functionInvoker, marshallers);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        arity.checkArity(context.getRuntime(), args);
        
        return getNativeInvoker().invoke(context, args);
    }
    
    protected final NativeInvoker getNativeInvoker() {
        return compiledInvoker != null ? compiledInvoker : tryCompilation();
    }
    
    private synchronized NativeInvoker tryCompilation() {
        if (compiledInvoker != null) {
            return compiledInvoker;
        }

        if (jitHandle == null) {
            jitHandle = JITCompiler.getInstance().getHandle(signature);
        }

        NativeInvoker invoker = jitHandle.compile(function, signature);
        if (invoker != null) {
            return compiledInvoker = invoker;
        }
        
        //
        // Once compilation has failed, always fallback to the default invoker
        //
        if (jitHandle.compilationFailed()) {
            compiledInvoker = defaultInvoker;
        }
        
        return defaultInvoker;
    }
}
