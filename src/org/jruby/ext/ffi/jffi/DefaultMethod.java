
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.Function;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.internal.runtime.methods.CacheableMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

public class DefaultMethod extends DynamicMethod implements CacheableMethod {
    protected final Signature signature;
    private final NativeInvoker defaultInvoker;
    private volatile NativeInvoker compiledInvoker;
    private JITHandle jitHandle;
    protected final Arity arity;
    protected final Function function;


    public DefaultMethod(RubyModule implementationClass, Function function,
                         Signature signature, NativeInvoker defaultInvoker) {
        super(implementationClass, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone);
        this.arity = Arity.fixed(signature.getParameterCount());
        this.function = function;
        this.defaultInvoker = defaultInvoker;
        this.signature = signature;
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Override
    public final Arity getArity() {
        return arity;
    }

    @Override
    public final boolean isNative() {
        return true;
    }

    public DynamicMethod getMethodForCaching() {
        return compiledInvoker != null ? compiledInvoker : this;
    }

    Signature getSignature() {
        return signature;
    }

    CallContext getCallContext() {
        return function.getCallContext();
    }

    long getFunctionAddress() {
        return function.getFunctionAddress();
    }

    protected final NativeInvoker getNativeInvoker() {
        return compiledInvoker != null ? compiledInvoker : tryCompilation();
    }

    private synchronized JITHandle getJITHandle() {
        if (jitHandle == null) {
            jitHandle = JITCompiler.getInstance().getHandle(signature);
        }
        return jitHandle;
    }

    private synchronized NativeInvoker tryCompilation() {

        if (compiledInvoker != null) {
            return compiledInvoker;
        }

        NativeInvoker invoker = getJITHandle().compile(getImplementationClass(), function, signature);
        if (invoker != null) {
            compiledInvoker = invoker;
            getImplementationClass().invalidateCacheDescendants();
            return compiledInvoker;
        }
        
        //
        // Once compilation has failed, always fallback to the default invoker
        //
        if (getJITHandle().compilationFailed()) {
            compiledInvoker = defaultInvoker;
            getImplementationClass().invalidateCacheDescendants();
        }
        
        return defaultInvoker;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
                            RubyModule clazz, String name, IRubyObject[] args) {
        return getNativeInvoker().call(context, self, clazz, name, args);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
            RubyModule clazz, String name, IRubyObject[] args, Block block) {
        return getNativeInvoker().call(context, self, clazz, name, args, block);
    }

    public final NativeInvoker forceCompilation() {
        NativeInvoker invoker = null;
        while (!getJITHandle().compilationFailed() && (invoker = getJITHandle().compile(getImplementationClass(), function, signature)) == null)
            ;
        return invoker;
    }
}
