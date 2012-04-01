package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.ext.ffi.*;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
abstract public class JITNativeInvoker extends NativeInvoker {
    protected static final Invoker invoker = Invoker.getInstance();
    protected final NativeInvoker fallbackInvoker;
    protected final com.kenai.jffi.Function function;
    protected final com.kenai.jffi.CallContext callContext;
    protected final long functionAddress;
    protected final Signature signature;
    protected final int arity;
    protected final NativeDataConverter resultConverter;
    protected final NativeDataConverter parameterConverter0;
    protected final NativeDataConverter parameterConverter1;
    protected final NativeDataConverter parameterConverter2;
    protected final NativeDataConverter parameterConverter3;
    protected final NativeDataConverter parameterConverter4;
    protected final NativeDataConverter parameterConverter5;
    protected final ObjectParameterInfo parameterInfo0;
    protected final ObjectParameterInfo parameterInfo1;
    protected final ObjectParameterInfo parameterInfo2;
    protected final ObjectParameterInfo parameterInfo3;
    protected final ObjectParameterInfo parameterInfo4;
    protected final ObjectParameterInfo parameterInfo5;

    public JITNativeInvoker(com.kenai.jffi.Function function, Signature signature, NativeInvoker fallbackInvoker) {
        this.arity = signature.getParameterCount();
        this.function = function;
        this.callContext = function.getCallContext();
        this.functionAddress = function.getFunctionAddress();
        this.signature = signature;
        this.fallbackInvoker = fallbackInvoker;

        // Get any result and parameter converters needed
        resultConverter = DataConverters.getResultConverter(signature.getResultType());
        parameterConverter0 = getParameterConverter(signature, 0);
        parameterConverter1 = getParameterConverter(signature, 1);
        parameterConverter2 = getParameterConverter(signature, 2);
        parameterConverter3 = getParameterConverter(signature, 3);
        parameterConverter4 = getParameterConverter(signature, 4);
        parameterConverter5 = getParameterConverter(signature, 5);
        parameterInfo0 = getParameterInfo(signature, 0);
        parameterInfo1 = getParameterInfo(signature, 1);
        parameterInfo2 = getParameterInfo(signature, 2);
        parameterInfo3 = getParameterInfo(signature, 3);
        parameterInfo4 = getParameterInfo(signature, 4);
        parameterInfo5 = getParameterInfo(signature, 5);
    }

    private static NativeDataConverter getParameterConverter(Signature signature, int i) {
        return signature.getParameterCount() > i
            ? DataConverters.getParameterConverter(signature.getParameterType(i), signature.getEnums()) : null;
    }

    private static ObjectParameterInfo getParameterInfo(Signature signature, int i) {
        if (signature.getParameterCount() <= i) {
            return null;
        }

        Type type = signature.getParameterType(i);
        int flags = 0;
        NativeType nativeType  = type instanceof MappedType
                ? ((MappedType) type).getRealType().getNativeType() : type.getNativeType();

        switch (nativeType) {
            case BUFFER_IN:
            case STRING:
            case TRANSIENT_STRING:
                flags |= ObjectParameterInfo.IN | ObjectParameterInfo.NULTERMINATE;
                break;

            case BUFFER_OUT:
                flags |= ObjectParameterInfo.OUT | ObjectParameterInfo.CLEAR;
                break;

            case POINTER:
            case BUFFER_INOUT:
                flags |= ObjectParameterInfo.IN | ObjectParameterInfo.OUT | ObjectParameterInfo.CLEAR | ObjectParameterInfo.NULTERMINATE;
                break;

            default:
                return null;
        }

        return ObjectParameterInfo.create(i, ObjectParameterInfo.ARRAY, ObjectParameterInfo.BYTE, flags);
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
