package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class JITNativeInvoker6 extends JITNativeInvoker {
    
    public JITNativeInvoker6(com.kenai.jffi.Function function, Signature signature, NativeInvoker fallbackInvoker) {
        super(function, signature, fallbackInvoker);
    }
    
    public final IRubyObject invoke(ThreadContext context) {
        throw context.getRuntime().newArgumentError(0, arity);
    }
    
    public final IRubyObject invoke(ThreadContext context, IRubyObject arg1) {
        throw context.getRuntime().newArgumentError(1, arity);
    }

    public final IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        throw context.getRuntime().newArgumentError(2, arity);
    }

    public final IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        throw context.getRuntime().newArgumentError(3, arity);
    }
    
    public final IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        throw context.getRuntime().newArgumentError(4, arity);
    }
    
    public final IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        throw context.getRuntime().newArgumentError(5, arity);
    }
}
