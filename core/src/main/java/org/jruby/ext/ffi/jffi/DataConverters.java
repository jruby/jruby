package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.*;
import org.jruby.api.Access;
import org.jruby.ext.ffi.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.util.WeakIdentityHashMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;


/**
 *
 */
public class DataConverters {
    @SuppressWarnings("unchecked")
    private static final Map<IRubyObject, NativeDataConverter> enumConverters = Collections.synchronizedMap(new WeakIdentityHashMap());

    @Deprecated
    static boolean isEnumConversionRequired(Type type, RubyHash enums) {
        if (type instanceof Type.Builtin && enums != null && !enums.isEmpty()) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
//                case LONGDOUBLE:
                    return true;
                    
                default:
                    return false;
            }
        }
        return false;
    }

    static boolean isEnumConversionRequired(Type type, Enums enums) {
        if (type instanceof Type.Builtin && enums != null && !enums.isEmpty()) {
            switch (type.getNativeType()) {
                case CHAR:
                case UCHAR:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                case LONG:
                case ULONG:
                case LONG_LONG:
                case ULONG_LONG:
//                case LONGDOUBLE:
                    return true;
                    
                default:
                    return false;
            }
        }
        return false;
    }

    static NativeDataConverter getResultConverter(Type type) {
        if (type instanceof Type.Builtin) {
            return null;
        
        } else if (type instanceof MappedType) {
            return new MappedDataConverter((MappedType) type);
        
        } else if (type instanceof CallbackInfo) {
            return new CallbackDataConverter((CallbackInfo) type);
        }
        
        return null;
    }


    static NativeDataConverter getParameterConverter(Type type) {
        if (type instanceof MappedType) {
            return new MappedDataConverter((MappedType) type);

        } else if (type instanceof CallbackInfo) {
            return new CallbackDataConverter((CallbackInfo) type);
        }

        return null;
    }

    @Deprecated
    static NativeDataConverter getParameterConverter(Type type, RubyHash enums) {
        if (isEnumConversionRequired(type, enums)) {
            NativeDataConverter converter = enumConverters.get(enums);
            if (converter != null) {
                return converter;
            }
            enumConverters.put(enums, converter = new IntOrEnumConverter(NativeType.INT, enums));
            return converter;
        
        } else {
            return getParameterConverter(type);
        }
    }

    static NativeDataConverter getParameterConverter(Type type, Enums enums) {
        if (isEnumConversionRequired(type, enums)) {
            NativeDataConverter converter = enumConverters.get(enums);
            if (converter != null) {
                return converter;
            }
            enumConverters.put(enums, converter = new IntOrEnumConverter(NativeType.INT, enums));
            return converter;
        
        } else {
            return getParameterConverter(type);
        }
    }

    public static final class IntOrEnumConverter extends NativeDataConverter {
        private final NativeType nativeType;
        private final IRubyObject enums;
        private volatile IdentityHashMap<RubySymbol, RubyInteger> symbolToValue = new IdentityHashMap<RubySymbol, RubyInteger>();

        public IntOrEnumConverter(NativeType nativeType, IRubyObject enums) {
            this.nativeType = nativeType;
            this.enums = enums;
        }

        @Override
        public NativeType nativeType() {
            return nativeType;
        }

        @Override
        public IRubyObject fromNative(ThreadContext context, IRubyObject obj) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public IRubyObject toNative(ThreadContext context, IRubyObject obj) {
            // Fast path - handle fixnums quickly
            if (obj instanceof RubyFixnum) {
                return obj;
            }

            return lookupOrConvert(context, obj);
        }

        IRubyObject lookupOrConvert(ThreadContext context, IRubyObject obj) {
            if (obj instanceof RubySymbol) {
                IRubyObject value;
                if ((value = symbolToValue.get(obj)) != null) {
                    return value;
                }

                return lookupAndCacheValue(context, obj);

            } else {
                return obj.convertToInteger();
            }
        }

        private synchronized IRubyObject lookupAndCacheValue(ThreadContext context, IRubyObject obj) {
            IRubyObject value = enums instanceof Enums ? ((Enums)enums).mapSymbol(context, obj) : ((RubyHash)enums).fastARef(obj);
            if (value.isNil() || !(value instanceof RubyInteger integer)) {
                throw argumentError(context, "invalid enum value, " + obj.inspect(context));
            }

            IdentityHashMap<RubySymbol, RubyInteger> s2v = new IdentityHashMap<RubySymbol, RubyInteger>(symbolToValue);
            s2v.put((RubySymbol) obj, integer);
            this.symbolToValue = new IdentityHashMap<>(s2v);

            return value;
        }
    }
    
    public static final class MappedDataConverter extends NativeDataConverter {
        private final MappedType converter;

        public MappedDataConverter(MappedType converter) {
            super(converter.isReferenceRequired(), converter.isPostInvokeRequired());
            this.converter = converter;
        }
        
        public NativeType nativeType() {
            return converter.getRealType().getNativeType();
        }

        public IRubyObject fromNative(ThreadContext context, IRubyObject obj) {
            return converter.fromNative(context, obj);
        }

        public IRubyObject toNative(ThreadContext context, IRubyObject obj) {
            return converter.toNative(context, obj);
        }
    }
    
    public static final class CallbackDataConverter extends NativeDataConverter {
        private final CachingCallSite callSite = new FunctionalCachingCallSite("call");
        private final NativeCallbackFactory callbackFactory;
        private final NativeFunctionInfo functionInfo;

        public CallbackDataConverter(CallbackInfo cbInfo) {
            this.callbackFactory = CallbackManager.getInstance().getCallbackFactory(cbInfo.getRuntime(), cbInfo);
            this.functionInfo = new NativeFunctionInfo(cbInfo.getRuntime(),
                    cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                    cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
        }
        
        public NativeType nativeType() {
            return NativeType.POINTER;
        }

        public IRubyObject fromNative(ThreadContext context, IRubyObject obj) {
            if (obj instanceof Pointer ptr) {
                if (ptr.getAddress() == 0) return context.nil;

                return new org.jruby.ext.ffi.jffi.Function(context.runtime, Access.getClass(context, "FFI", "Function"),
                        new CodeMemoryIO(context.runtime, ptr), functionInfo, null);
            }

            throw typeError(context, "internal error: non-pointer");
        }

        public IRubyObject toNative(ThreadContext context, IRubyObject obj) {
            if (obj instanceof Pointer || obj.isNil()) return obj;
            if (obj instanceof RubyObject rubyObj) return callbackFactory.getCallback(rubyObj, callSite);

            throw typeError(context, "wrong argument type.  Expected callable object");
        }
    }
    
    public static final class ChainedDataConverter extends NativeDataConverter {
        private final NativeDataConverter upper;
        private final NativeDataConverter lower;

        public ChainedDataConverter(NativeDataConverter first, NativeDataConverter second) {
            super(first.isReferenceRequired() || second.isReferenceRequired(), first.isPostInvokeRequired() || second.isPostInvokeRequired());
            this.upper = first;
            this.lower = second;
        }
        
        public NativeType nativeType() {
            return lower.nativeType();
        }

        public IRubyObject fromNative(ThreadContext context, IRubyObject obj) {
            return upper.fromNative(context, lower.fromNative(context, obj));
        }

        public IRubyObject toNative(ThreadContext context, IRubyObject obj) {
            return lower.toNative(context, upper.toNative(context, obj));
        }
    }
}
