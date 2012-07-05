package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.*;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.*;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import java.util.Arrays;

/**
 *
 */
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

    Signature getSignature() {
        return signature;
    }

    CallContext getCallContext() {
        return callContext;
    }

    long getFunctionAddress() {
        return functionAddress;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        throw context.getRuntime().newArgumentError(0, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1) {
        throw context.getRuntime().newArgumentError(1, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2) {

        throw context.getRuntime().newArgumentError(2, arity);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        throw context.getRuntime().newArgumentError(3, arity);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self,
                                    RubyModule clazz, String name, IRubyObject[] args) {
        if (args.length != arity) {
            throw context.getRuntime().newArgumentError(args.length, arity);
        }

        switch (args.length) {
            case 0:
                return call(context, self, clazz, name);

            case 1:
                return call(context, self, clazz, name, args[0]);

            case 2:
                return call(context, self, clazz, name, args[0], args[1]);

            case 3:
                return call(context, self, clazz, name, args[0], args[1], args[2]);

            default:
                throw context.getRuntime().newArgumentError("too many arguments: " + args.length);
        }
    }
}
