
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class DefaultMethod extends JFFIDynamicMethod {
    private final ParameterMarshaller[] marshallers;
    protected final boolean needsInvocationSession;
    protected final Signature signature;
    private final NativeInvoker defaultInvoker;
    private final int cbIndex;
    private final NativeCallbackFactory cbFactory;
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

        int cbIndex = -1;
        NativeCallbackFactory cbFactory = null;
        for (int i = 0; i < signature.getParameterCount(); ++i) {
            if (signature.getParameterType(i) instanceof CallbackInfo) {
                cbFactory = CallbackManager.getInstance().getCallbackFactory(implementationClass.getRuntime(),
                        (CallbackInfo) signature.getParameterType(i));
                cbIndex = i;
                break;
            }
        }
        this.cbFactory = cbFactory;
        this.cbIndex = cbIndex;

        this.needsInvocationSession = piCount > 0 || refCount > 0;
        this.compiledInvoker = null;
        this.jitHandle = null;
        this.defaultInvoker = new BufferNativeInvoker(function, functionInvoker, marshallers);
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


    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block) {

        if (!block.isGiven() || cbIndex < 0) {
            arity.checkArity(context.getRuntime(), args);
            return getNativeInvoker().invoke(context, args);

        } else {
            Arity.checkArgumentCount(context.getRuntime(), args,
                    arity.getValue() - 1, arity.getValue());

            IRubyObject[] params = new IRubyObject[arity.getValue()];
            for (int i = 0; i < cbIndex; i++) {
                params[i] = args[i];
            }

            NativeCallbackPointer cb;
            params[cbIndex] = cb = cbFactory.newCallback(block);

            for (int i = cbIndex + 1; i < params.length; i++) {
                params[i] = args[i - 1];
            }

            try {
                return getNativeInvoker().invoke(context, params);
            } finally {
                cb.dispose();
            }
        }
    }
}
