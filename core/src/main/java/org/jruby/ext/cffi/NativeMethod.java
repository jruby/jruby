
package org.jruby.ext.cffi;

import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.Ruby;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
final class NativeMethod extends DynamicMethod {
    protected final Arity arity;
    protected final Function function;
    private final com.kenai.jffi.CallContext jffiContext;


    public NativeMethod(RubyModule implementationClass, Function function) {
        super(implementationClass, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone);
        this.arity = Arity.fixed(function.getCallContext().getParameterCount());
        this.function = function;
        this.jffiContext = buildNativeCallContext(function.getCallContext());
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Override
    public final Arity getArity() {
        return arity;
    }
    @Override
    public final boolean isNative() {
        return true;
    }

    CallContext getCallContext() {
        return function.getCallContext();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self,
                            RubyModule clazz, String name, IRubyObject[] args, Block block) {

        arity.checkArity(context.runtime, args);
        HeapInvocationBuffer invocationBuffer = new HeapInvocationBuffer(jffiContext);
        CallContext callContext = function.getCallContext();
        List<IRubyObject> mappedParams = null;
        
        for (int i = 0; i < callContext.getParameterCount(); i++) {
            Type parameterType = callContext.getParameterType(i);
            if (parameterType instanceof Type.Builtin) {
                toNative(context.runtime, invocationBuffer, parameterType, args[i]);
            
            } else if (parameterType instanceof MappedType) {
                if (mappedParams == null) mappedParams = new ArrayList<IRubyObject>();
                Type realType = parameterType;
                IRubyObject value = args[i];
                do {
                    MappedType mappedType = (MappedType) realType;
                    mappedParams.add(value = mappedType.toNative(context, value));
                    realType = mappedType.getRealType();
                } while (realType instanceof MappedType);
                toNative(context.runtime, invocationBuffer, realType, value);
            }
        }
        
        if (callContext.getReturnType() instanceof Type.Builtin) {
            return invoke(context.runtime, jffiContext, invocationBuffer, callContext.getReturnType().nativeType(), function.address());
        
        } else if (callContext.getReturnType() instanceof MappedType) {
            MappedType mappedType = (MappedType) callContext.getReturnType();
            return mappedType.fromNative(context, invoke(context.runtime, jffiContext, invocationBuffer,
                    mappedType.getRealType().nativeType(), function.address()));
        }
        
        throw context.runtime.newRuntimeError("unsupported return type");
    }
    
    private static void toNative(Ruby runtime, HeapInvocationBuffer invocationBuffer, Type parameterType, IRubyObject value) {
        switch (parameterType.nativeType()) {
            case SCHAR:
                invocationBuffer.putByte(Util.int8Value(value));
                break;
            
            case UCHAR:
                invocationBuffer.putByte(Util.uint8Value(value));
                break;

            case SSHORT:
                invocationBuffer.putShort(Util.int16Value(value));
                break;
            
            case USHORT:
                invocationBuffer.putShort(Util.uint16Value(value));
                break;

            case SINT:
                invocationBuffer.putInt(Util.int32Value(value));
                break;
            
            case UINT:
                invocationBuffer.putInt((int) Util.uint32Value(value));
                break;

            case SLONG:
                if (parameterType.size() == 4) {
                    invocationBuffer.putInt(Util.int32Value(value));
                } else {
                    invocationBuffer.putLong(Util.longValue(value));
                }
                break;

            case ULONG:
                if (parameterType.size() == 4) {
                    invocationBuffer.putInt((int) Util.uint32Value(value));
                } else {
                    invocationBuffer.putLong(Util.longValue(value));
                }
                break;

            case SLONG_LONG:
            case ULONG_LONG:
                invocationBuffer.putLong(Util.longValue(value));
                break;
            
            case FLOAT:
                invocationBuffer.putFloat((float) RubyFloat.num2dbl(value));
                break;
            
            case DOUBLE:
                invocationBuffer.putDouble((float) RubyFloat.num2dbl(value));
                break;
            
            case POINTER:
                if (value instanceof NativeAddress) {
                    invocationBuffer.putAddress(((NativeAddress) value).address());
                
                } else if (value.isNil()) {
                    invocationBuffer.putAddress(0);

                } else if (value instanceof RubyInteger) {
                    invocationBuffer.putAddress(RubyNumeric.num2long(value));
                
                } else {
                    throw runtime.newTypeError(value, "native address");
                }
                break;

            default:
                throw runtime.newTypeError("unsupported parameter type " + parameterType);
        }
    }
    
    private static IRubyObject invoke(Ruby runtime, com.kenai.jffi.CallContext jffiContext, HeapInvocationBuffer invocationBuffer, NativeType nativeType, long address) {
        com.kenai.jffi.Invoker invoker = com.kenai.jffi.Invoker.getInstance();
        switch (nativeType) {
            case VOID:
                invoker.invokeInt(jffiContext, address, invocationBuffer);
                return runtime.getNil();
            
            case BOOL:
                return runtime.newBoolean(invoker.invokeInt(jffiContext, address, invocationBuffer) != 0);
            
            case SCHAR:
                return Util.newSigned8(runtime, (byte) invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case UCHAR:
                return Util.newUnsigned8(runtime, (byte) invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case SSHORT:
                return Util.newSigned16(runtime, (short) invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case USHORT:
                return Util.newUnsigned16(runtime, (short) invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case SINT:
                return Util.newSigned32(runtime, invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case UINT:
                return Util.newUnsigned32(runtime, invoker.invokeInt(jffiContext, address, invocationBuffer));
            
            case SLONG:
                return jffiContext.getReturnType().size() == 4
                        ? Util.newSigned32(runtime, invoker.invokeInt(jffiContext, address, invocationBuffer))
                        : Util.newSigned64(runtime, invoker.invokeLong(jffiContext, address, invocationBuffer));
                
            case ULONG:
                return jffiContext.getReturnType().size() == 4
                        ? Util.newUnsigned32(runtime, invoker.invokeInt(jffiContext, address, invocationBuffer))
                        : Util.newUnsigned64(runtime, invoker.invokeLong(jffiContext, address, invocationBuffer));
            
            case SLONG_LONG:
                return Util.newSigned64(runtime, invoker.invokeLong(jffiContext, address, invocationBuffer));
            
            case ULONG_LONG:
                return Util.newUnsigned64(runtime, invoker.invokeLong(jffiContext, address, invocationBuffer));
            
            case FLOAT:
                return RubyFloat.newFloat(runtime, invoker.invokeFloat(jffiContext, address, invocationBuffer));
            
            case DOUBLE:
                return RubyFloat.newFloat(runtime, invoker.invokeDouble(jffiContext, address, invocationBuffer));
            
            case POINTER:
                return Pointer.newPointer(runtime, invoker.invokeAddress(jffiContext, address, invocationBuffer));
            
            default:
                throw runtime.newTypeError("unsupported return type: " + nativeType);
        }
    }
    
    private static com.kenai.jffi.CallContext buildNativeCallContext(CallContext callContext) {
        com.kenai.jffi.Type jffiReturnType;
        if (callContext.getReturnType() instanceof Type.Builtin) {
            jffiReturnType = Type.jffiType(callContext.getReturnType().nativeType());
        } else if (callContext.getReturnType() instanceof MappedType) {
            jffiReturnType = Type.jffiType(((MappedType) callContext.getReturnType()).getRealType().nativeType());
        } else {
            throw callContext.getRuntime().newTypeError("unsupported return type: " + callContext.getReturnType());
        }

        com.kenai.jffi.Type[] jffiParameterTypes = new com.kenai.jffi.Type[callContext.getParameterCount()];
        for (int i = 0; i < callContext.getParameterCount(); i++) {
            Type parameterType = callContext.getParameterType(i);
            if (parameterType instanceof Type.Builtin) {
                jffiParameterTypes[i] = Type.jffiType(parameterType.nativeType());
            
            } else if (parameterType instanceof MappedType) {
                Type realType = parameterType;
                do {
                    realType = ((MappedType) realType).getRealType();
                } while (realType instanceof MappedType);
                jffiParameterTypes[i] = Type.jffiType(realType.nativeType());
            
            } else {
                throw callContext.getRuntime().newTypeError("unsupported parameter type: " + parameterType);
            }
        }

        return com.kenai.jffi.CallContext.getCallContext(jffiReturnType, jffiParameterTypes,
                callContext.callingConvention(), true);
    }
}
