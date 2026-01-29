
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallContext;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

abstract public class NativeInvoker extends DynamicMethod {
    protected final Arity arity;
    protected final com.kenai.jffi.Function function;
    private final int cbIndex;
    private final NativeCallbackFactory cbFactory;


    public NativeInvoker(RubyModule implementationClass, com.kenai.jffi.Function function, Signature signature) {
        super(implementationClass, Visibility.PUBLIC, "ffi"+function.getFunctionAddress());
        this.arity = Arity.fixed(signature.getParameterCount());
        this.function = function;

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
        this.cbIndex = cbIndex;
        this.cbFactory = cbFactory;
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Deprecated(since = "9.4.3.0") @Override
    public final Arity getArity() {
        return arity;
    }
    @Override
    public final boolean isNative() {
        return true;
    }

    CallContext getCallContext() {
        return function.getCallContext();
    }

    long getFunctionAddress() {
        return function.getFunctionAddress();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
                            RubyModule clazz, String name, IRubyObject[] args, Block block) {

        if (!block.isGiven() || cbIndex < 0) {
            arity.checkArity(context.runtime, args);
            return call(context, self, clazz, name, args);

        } else {
            Arity.checkArgumentCount(context, name, args,
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
                return call(context, self, clazz, name, params);
            } finally {
                cb.dispose();
            }
        }
    }
}
