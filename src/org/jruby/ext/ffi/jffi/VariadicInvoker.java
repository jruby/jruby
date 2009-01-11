
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.Library;
import com.kenai.jffi.Type;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::VariadicInvoker", parent = "Object")
public class VariadicInvoker extends RubyObject {
    private final CallingConvention convention;
    private final Library library;
    private final long address;
    private final FunctionInvoker functionInvoker;
    private final Type returnType;

    public static RubyClass createVariadicInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("VariadicInvoker",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(VariadicInvoker.class);
        result.defineAnnotatedConstants(VariadicInvoker.class);

        return result;
    }
    /**
     * Creates a new <tt>Invoker</tt> instance.
     * @param arity
     */
    private VariadicInvoker(Ruby runtime, IRubyObject klazz, Library library, long address,
            FunctionInvoker functionInvoker, Type returnType,
            CallingConvention convention) {
        super(runtime, (RubyClass) klazz);
        this.library = library;
        this.address = address;
        this.functionInvoker = functionInvoker;
        this.returnType = returnType;
        this.convention = convention;
    }
    
    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     *
     * @return The <tt>Arity</tt> of the native function.
     */
    public final Arity getArity() {
        return Arity.OPTIONAL;
    }

    @JRubyMethod(name = { "__new" }, meta = true, required = 4)
    public static VariadicInvoker newInvoker(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        CallingConvention conv = "stdcall".equals(args[3].toString())
                ? CallingConvention.STDCALL : CallingConvention.STDCALL;
        Library library;
        long address;
        try {
            library = args[0] instanceof DynamicLibrary 
                    ? ((DynamicLibrary) args[0]).getNativeLibrary() 
                    : Library.getCachedInstance(args[0].toString(), Library.LAZY);
            address = args[1] instanceof DynamicLibrary.Symbol
                    ? ((DynamicLibrary.Symbol) args[1]).getAddress()
                    : library.getSymbolAddress(args[1].toString());
        } catch (UnsatisfiedLinkError ex) {
            throw context.getRuntime().newLoadError(ex.getMessage());
        }
        NativeType returnType = NativeType.valueOf(Util.int32Value(args[2]));
        FunctionInvoker functionInvoker = DefaultMethodFactory.getFunctionInvoker(returnType);
        return new VariadicInvoker(recv.getRuntime(), recv, library, address,
                functionInvoker, getFFIType(returnType), conv);
    }

    @JRubyMethod(name = { "invoke" })
    public IRubyObject invoke(ThreadContext context, IRubyObject typesArg, IRubyObject paramsArg) {
        IRubyObject[] types = ((RubyArray) typesArg).toJavaArrayMaybeUnsafe();
        IRubyObject[] params = ((RubyArray) paramsArg).toJavaArrayMaybeUnsafe();
        Type[] ffiParamTypes = new Type[types.length];
        NativeType[] paramTypes = new NativeType[types.length];
        for (int i = 0; i < types.length; ++i) {
            NativeType t = NativeType.valueOf(Util.int32Value(types[i]));
            switch (t) {
                case INT8:
                case INT16:
                case INT32:
                    paramTypes[i] = NativeType.INT32;
                    break;
                case UINT8:
                case UINT16:
                case UINT32:
                    paramTypes[i] = NativeType.UINT32;
                    break;
                case FLOAT32:
                case FLOAT64:
                    paramTypes[i] = NativeType.FLOAT64;
                    break;
                default:
                    paramTypes[i] = t;
            }
            ffiParamTypes[i] = getFFIType(paramTypes[i]);
        }
        Invocation invocation = new Invocation(context);
        Function function = new Function(address, returnType, ffiParamTypes, convention);
        HeapInvocationBuffer args = new HeapInvocationBuffer(function);
        for (int i = 0; i < types.length; ++i) {
            DefaultMethodFactory.getMarshaller(paramTypes[i]).marshal(invocation, args, params[i]);
        }
        return functionInvoker.invoke(context.getRuntime(), function, args);
    }
    private static final Type getFFIType(NativeParam type) {
        if (type instanceof NativeType) switch ((NativeType) type) {
            case VOID: return com.kenai.jffi.Type.VOID;
            case INT8: return com.kenai.jffi.Type.SINT8;
            case UINT8: return com.kenai.jffi.Type.UINT8;
            case INT16: return com.kenai.jffi.Type.SINT16;
            case UINT16: return com.kenai.jffi.Type.UINT16;
            case INT32: return com.kenai.jffi.Type.SINT32;
            case UINT32: return com.kenai.jffi.Type.UINT32;
            case INT64: return com.kenai.jffi.Type.SINT64;
            case UINT64: return com.kenai.jffi.Type.UINT64;
            case LONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.SINT32
                        : com.kenai.jffi.Type.SINT64;
            case ULONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.UINT32
                        : com.kenai.jffi.Type.UINT64;
            case FLOAT32: return com.kenai.jffi.Type.FLOAT;
            case FLOAT64: return com.kenai.jffi.Type.DOUBLE;
            case POINTER: return com.kenai.jffi.Type.POINTER;
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                return com.kenai.jffi.Type.POINTER;
            case STRING: return com.kenai.jffi.Type.POINTER;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
//        } else if (type instanceof CallbackInfo) {
//            return com.kenai.jffi.Type.POINTER;
        } else {
            throw new IllegalArgumentException("Unknown type " + type);
        }
    }
}
