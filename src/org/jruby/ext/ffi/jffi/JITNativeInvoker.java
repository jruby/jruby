package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class JITNativeInvoker extends NativeInvoker {
    protected static final Invoker invoker = Invoker.getInstance();
    protected final NativeInvoker fallbackInvoker;
    protected final com.kenai.jffi.Function function;
    protected final Signature signature;
    protected final int arity;
    protected final NativeDataConverter resultConverter;
    protected final NativeDataConverter parameterConverter0;
    protected final NativeDataConverter parameterConverter1;
    protected final NativeDataConverter parameterConverter2;
    protected final NativeDataConverter parameterConverter3;
    protected final NativeDataConverter parameterConverter4;
    protected final NativeDataConverter parameterConverter5;

    public JITNativeInvoker(com.kenai.jffi.Function function, Signature signature, NativeInvoker fallbackInvoker) {
        this.arity = signature.getParameterCount();
        this.function = function;
        this.signature = signature;
        this.fallbackInvoker = fallbackInvoker;

        System.out.println("resolving result converters");
        // Get any result and parameter converters needed
        resultConverter = DataConverters.getResultConverter(signature.getResultType());
        parameterConverter0 = getParameterConverter(signature, 0);
        parameterConverter1 = getParameterConverter(signature, 1);
        parameterConverter2 = getParameterConverter(signature, 2);
        parameterConverter3 = getParameterConverter(signature, 3);
        parameterConverter4 = getParameterConverter(signature, 4);
        parameterConverter5 = getParameterConverter(signature, 5);
    }

    private static NativeDataConverter getParameterConverter(Signature signature, int i) {
        return signature.getParameterCount() > i
            ? DataConverters.getParameterConverter(signature.getParameterType(i), signature.getEnums()) : null;
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
