package org.jruby.ext.ffi.jffi;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class JITNativeInvoker0 extends JITNativeInvoker {

    public JITNativeInvoker0() {
        super(0);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1) {
        throw context.getRuntime().newArgumentError(1, arity);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        throw context.getRuntime().newArgumentError(2, arity);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        throw context.getRuntime().newArgumentError(3, arity);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        throw context.getRuntime().newArgumentError(4, arity);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        throw context.getRuntime().newArgumentError(5, arity);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, 
            IRubyObject arg5, IRubyObject arg6) {
        throw context.getRuntime().newArgumentError(6, arity);
    }
}
