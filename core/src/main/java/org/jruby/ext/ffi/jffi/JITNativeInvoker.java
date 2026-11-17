package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.*;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.cli.Options;

import java.util.Arrays;

import static org.jruby.api.Error.argumentError;

abstract public class JITNativeInvoker extends NativeInvoker {
    protected static final Invoker invoker = Invoker.getInstance();
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
    protected final CachingCallSite parameterCallSite0;
    protected final CachingCallSite parameterCallSite1;
    protected final CachingCallSite parameterCallSite2;
    protected final CachingCallSite parameterCallSite3;
    protected final CachingCallSite parameterCallSite4;
    protected final CachingCallSite parameterCallSite5;
    protected final CachingCallSite parameterCallSite6;

    public JITNativeInvoker(RubyModule implementationClass, com.kenai.jffi.Function function, Signature signature) {
        super(implementationClass, function, signature);
        this.arity = signature.getParameterCount();
        this.function = function;
        this.callContext = function.getCallContext();
        this.functionAddress = function.getFunctionAddress();
        this.signature = signature;

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
        parameterCallSite0 = getParameterCallSite(signature, 0);
        parameterCallSite1 = getParameterCallSite(signature, 1);
        parameterCallSite2 = getParameterCallSite(signature, 2);
        parameterCallSite3 = getParameterCallSite(signature, 3);
        parameterCallSite4 = getParameterCallSite(signature, 4);
        parameterCallSite5 = getParameterCallSite(signature, 5);
        parameterCallSite6 = getParameterCallSite(signature, 6);
    }

    @SuppressWarnings("deprecation")
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
    private static CachingCallSite getParameterCallSite(Signature signature, int parameterIndex) {
        if (signature.getParameterCount() <= parameterIndex) {
            return null;
        }

        Type type = signature.getParameterType(parameterIndex);
        NativeType nativeType  = type instanceof MappedType
                ? ((MappedType) type).getRealType().getNativeType() : type.getNativeType();

        switch (nativeType) {
            case STRING:
            case TRANSIENT_STRING:
                return new FunctionalCachingCallSite("to_str");

            case POINTER:
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                return new FunctionalCachingCallSite("to_ptr");

            default:
                return null;
        }
    }

    CallContext getCallContext() {
        return callContext;
    }

    long getFunctionAddress() {
        return functionAddress;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        throw argumentError(context, 0, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1) {
        throw argumentError(context, 1, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2) {

        throw argumentError(context, 2, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        throw argumentError(context, 3, arity);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        throw argumentError(context, 4, arity);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        throw argumentError(context, 5, arity);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4,
                            IRubyObject arg5, IRubyObject arg6) {
        throw argumentError(context, 6, arity);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self,
                                    RubyModule clazz, String name, IRubyObject[] args) {
        if (args.length != arity) throw argumentError(context,  args.length, arity);

        return switch (args.length) {
            case 0 -> call(context, self, clazz, name);
            case 1 -> call(context, self, clazz, name, args[0]);
            case 2 -> call(context, self, clazz, name, args[0], args[1]);
            case 3 -> call(context, self, clazz, name, args[0], args[1], args[2]);
            case 4 -> call(context, self, clazz, name, args[0], args[1], args[2], args[3]);
            case 5 -> call(context, self, clazz, name, args[0], args[1], args[2], args[3], args[4]);
            case 6 -> call(context, self, clazz, name, args[0], args[1], args[2], args[3], args[4], args[5]);
            default -> throw argumentError(context, "too many arguments: " + args.length);
        };
    }
}
