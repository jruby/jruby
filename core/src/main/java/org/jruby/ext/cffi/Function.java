package org.jruby.ext.cffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Function extends RubyObject implements NativeAddress {
    private final CallContext callContext;
    private final NativeAddress nativeAddress;
    
    public Function(Ruby runtime, RubyClass metaClass, CallContext callContext, NativeAddress nativeAddress) {
        super(runtime, metaClass);
        this.callContext = callContext;
        this.nativeAddress = nativeAddress;
    }

    @JRubyMethod(name = "new", module = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject self, IRubyObject callContext, IRubyObject address) {
        try {
            return new Function(context.runtime, (RubyClass) self, (CallContext) callContext, (NativeAddress) address);

        } catch (ClassCastException cce) {
            if (!(callContext instanceof CallContext)) throw context.runtime.newTypeError(callContext, CallContext.getClass(context.runtime));
            if (!(address instanceof NativeAddress)) throw context.runtime.newTypeError(address, "native address");
            throw context.runtime.newRuntimeError("unexpected error");
        }
    }

    @Override
    public long address() {
        return nativeAddress.address();
    }
    
    CallContext getCallContext() {
        return callContext;
    }

    @JRubyMethod(name = "attach")
    public IRubyObject attach(ThreadContext context, IRubyObject obj, IRubyObject methodName) {
        NativeMethod method = new NativeMethod(obj.getSingletonClass(), this);
        method.setName(methodName.asJavaString());
        obj.getSingletonClass().addMethod(methodName.asJavaString(), method);
        if (obj instanceof RubyModule) {
            ((RubyModule) obj).addMethod(methodName.asJavaString(), method);
        }

        return this;
    }
}
