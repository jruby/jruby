
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::VariadicInvoker", parent = "Object")
public class VariadicInvoker extends RubyObject {
    private final CallingConvention convention;
    private final Pointer address;
    private final FunctionInvoker functionInvoker;
    private final com.kenai.jffi.Type returnType;

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
    private VariadicInvoker(Ruby runtime, IRubyObject klazz, Pointer address,
            FunctionInvoker functionInvoker, com.kenai.jffi.Type returnType,
            CallingConvention convention) {
        super(runtime, (RubyClass) klazz);
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

    @JRubyMethod(name = { "__new" }, meta = true, required = 3, optional = 1)
    public static VariadicInvoker newInvoker(ThreadContext context, IRubyObject klass, IRubyObject[] args) {

        if (!(args[0] instanceof Pointer)) {
            throw context.runtime.newTypeError(args[0], context.runtime.getFFI().pointerClass);
        }

        final Pointer address = (Pointer) args[0];
        
        if (!(args[1] instanceof Type)) {
            throw context.runtime.newTypeError("invalid return type");
        }
        
        final Type returnType = (Type) args[1];
        final CallingConvention conv = "stdcall".equals(args[2].toString())
                ? CallingConvention.STDCALL : CallingConvention.DEFAULT;

        FunctionInvoker functionInvoker = DefaultMethodFactory.getFunctionInvoker(returnType);
        return new VariadicInvoker(context.runtime, klass, address,
                functionInvoker, FFIUtil.getFFIType(returnType), conv);
    }

    @JRubyMethod(name = { "invoke" })
    public IRubyObject invoke(ThreadContext context, IRubyObject typesArg, IRubyObject paramsArg) {
        IRubyObject[] types = ((RubyArray) typesArg).toJavaArrayMaybeUnsafe();
        IRubyObject[] params = ((RubyArray) paramsArg).toJavaArrayMaybeUnsafe();
        com.kenai.jffi.Type[] ffiParamTypes = new com.kenai.jffi.Type[types.length];
        ParameterMarshaller[] marshallers = new ParameterMarshaller[types.length];

        for (int i = 0; i < types.length; ++i) {
            Type type = (Type) types[i];
            switch (NativeType.valueOf(type)) {
                case CHAR:
                case SHORT:
                case INT:
                    ffiParamTypes[i] = com.kenai.jffi.Type.SINT32;
                    marshallers[i] = DefaultMethodFactory.getMarshaller(NativeType.INT);
                    break;
                case UCHAR:
                case USHORT:
                case UINT:
                    ffiParamTypes[i] = com.kenai.jffi.Type.UINT32;
                    marshallers[i] = DefaultMethodFactory.getMarshaller(NativeType.UINT);
                    break;
                case FLOAT:
                case DOUBLE:
                    ffiParamTypes[i] = com.kenai.jffi.Type.DOUBLE;
                    marshallers[i] = DefaultMethodFactory.getMarshaller(NativeType.DOUBLE);
                    break;
                default:
                    ffiParamTypes[i] = FFIUtil.getFFIType(type);
                    marshallers[i] = DefaultMethodFactory.getMarshaller((Type) types[i], CallingConvention.DEFAULT, null);
                    break;
            }
        }

        Invocation invocation = new Invocation(context);
        Function function = new Function(address.getAddress(), returnType, ffiParamTypes, convention);
        try {
            HeapInvocationBuffer args = new HeapInvocationBuffer(function);
            for (int i = 0; i < marshallers.length; ++i) {
                marshallers[i].marshal(invocation, args, params[i]);
            }

            return functionInvoker.invoke(context, function, args);
        } finally {
            invocation.finish();
        }
    }
}
