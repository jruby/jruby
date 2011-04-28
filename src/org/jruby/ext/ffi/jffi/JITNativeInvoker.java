package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Invoker;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class JITNativeInvoker extends NativeInvoker {
    protected static final Invoker invoker = Invoker.getInstance();
    protected final int arity;

    public JITNativeInvoker(int arity) {
        this.arity = arity;
    }
    
    abstract public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4);
    
    abstract public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5);
    
    abstract public IRubyObject invoke(ThreadContext context, IRubyObject arg1, 
            IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, 
            IRubyObject arg5, IRubyObject arg6);
    
    public final IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        if (args.length != arity) {
            throw context.getRuntime().newArgumentError(args.length, arity);
        }
        
        // This will never be called in a hot path anyway, so just use a switch statement
        switch (args.length) {
            case 0:
                return invoke(context);
            case 1:
                return invoke(context, args[0]);
            case 2:
                return invoke(context, args[0], args[1]);
            case 3:
                return invoke(context, args[0], args[1], args[2]);
            case 4:
                return invoke(context, args[0], args[1], args[2], args[3]);
            case 5:
                return invoke(context, args[0], args[1], args[2], args[3], args[4]);
            case 6:
                return invoke(context, args[0], args[1], args[2], args[3], args[4], args[5]);
            default:
                throw context.getRuntime().newArgumentError("too many arguments: " + args.length);
        }
    }
    
    
}
