package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyProc;
import org.jruby.RubySymbol;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.MappedType;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class DataConverters {

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
            MappedType mappedType = (MappedType) type;
            return !mappedType.isPostInvokeRequired() && !mappedType.isReferenceRequired()
                    ? new MappedDataConverter(mappedType) : null;
        
        } else if (type instanceof CallbackInfo) {
            return new CallbackDataConverter((CallbackInfo) type);
        }
        
        return null;
    }
    
     
    static NativeDataConverter getParameterConverter(Type type, RubyHash enums) {
        if (type instanceof Type.Builtin && isEnumConversionRequired(type, enums)) {
            return new IntOrEnumConverter(type.getNativeType(), enums);
        
        } else if (type instanceof MappedType) {
            MappedType mappedType = (MappedType) type;
            return !mappedType.isPostInvokeRequired() && !mappedType.isReferenceRequired()
                    ? new MappedDataConverter(mappedType) : null;
        
        } else if (type instanceof CallbackInfo) {
            return new CallbackDataConverter((CallbackInfo) type);
        }
        
        return null;
    }
    
    public static final class IntOrEnumConverter extends NativeDataConverter {
        private final NativeType nativeType;
        private final RubyHash enums;

        public IntOrEnumConverter(NativeType nativeType, RubyHash enums) {
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
        
        private IRubyObject lookupOrConvert(ThreadContext context, IRubyObject obj) {
            if (obj instanceof RubySymbol) {
                IRubyObject value = enums.fastARef(obj);
                if (value.isNil() || !(value instanceof RubyInteger)) {
                    throw context.getRuntime().newArgumentError("invalid enum value, " + obj.inspect());
                }
                
                return value;
            
            } else {
                return obj.convertToInteger();
            }
        }
    }
    
    public static final class MappedDataConverter extends NativeDataConverter {
        private final MappedType converter;

        public MappedDataConverter(MappedType converter) {
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
        private final NativeFunctionInfo functionInfo;
        private final CallbackInfo cbInfo;

        public CallbackDataConverter(CallbackInfo cbInfo) {
            this.cbInfo = cbInfo;
            this.functionInfo = new NativeFunctionInfo(cbInfo.getRuntime(),
                    cbInfo.getReturnType(), cbInfo.getParameterTypes(),
                    cbInfo.isStdcall() ? CallingConvention.STDCALL : CallingConvention.DEFAULT);
        }
        
        public NativeType nativeType() {
            return NativeType.POINTER;
        }

        public IRubyObject fromNative(ThreadContext context, IRubyObject obj) {
            if (!(obj instanceof Pointer)) {
                throw context.getRuntime().newTypeError("internal error: non-pointer");
            }
            Pointer ptr = (Pointer) obj;
            if (ptr.getAddress() == 0) {
                return context.getRuntime().getNil();
            }
            return new org.jruby.ext.ffi.jffi.Function(context.getRuntime(),
                    context.getRuntime().fastGetModule("FFI").fastGetClass("Function"),
                    new CodeMemoryIO(context.getRuntime(), ptr), functionInfo, null);
        }

        public IRubyObject toNative(ThreadContext context, IRubyObject obj) {
            if (obj instanceof Pointer || obj.isNil()) {
                return obj;
            
            } else if (obj instanceof RubyProc || obj.respondsTo("call")) {
                return CallbackManager.getInstance().getCallback(context.getRuntime(), 
                    cbInfo, obj);
                
            } else {
                throw context.getRuntime().newTypeError("wrong argument type.  Expected callable object");
            }
        }
    }
    
    public static final class ChainedDataConverter extends NativeDataConverter {
        private final NativeDataConverter upper;
        private final NativeDataConverter lower;

        public ChainedDataConverter(NativeDataConverter first, NativeDataConverter second) {
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
