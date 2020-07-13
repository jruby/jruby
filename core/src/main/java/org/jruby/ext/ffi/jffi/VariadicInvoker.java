
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.Enums;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

@JRubyClass(name = "FFI::VariadicInvoker", parent = "Object")
public class VariadicInvoker extends RubyObject {
    private final CallingConvention convention;
    private final Pointer address;
    private final FunctionInvoker functionInvoker;
    private final com.kenai.jffi.Type returnType;
    private final IRubyObject enums;
    private final boolean saveError;
    private static final java.util.Locale LOCALE = java.util.Locale.ENGLISH;


    public static RubyClass createVariadicInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("VariadicInvoker",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(VariadicInvoker.class);
        result.defineAnnotatedConstants(VariadicInvoker.class);

        return result;
    }

    private VariadicInvoker(Ruby runtime, IRubyObject klazz, Pointer address,
            FunctionInvoker functionInvoker, com.kenai.jffi.Type returnType,
            CallingConvention convention, IRubyObject enums, boolean saveError) {
        super(runtime, (RubyClass) klazz);
        this.address = address;
        this.functionInvoker = functionInvoker;
        this.returnType = returnType;
        this.convention = convention;
        this.enums = enums;
        this.saveError = saveError;
    }

    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     *
     * @return The <tt>Arity</tt> of the native function.
     */
    public final Arity getArity() {
        return Arity.OPTIONAL;
    }

    @JRubyMethod(name = { "new" }, meta = true, required = 4)
    public static VariadicInvoker newInstance(ThreadContext context, IRubyObject klass, IRubyObject[] args) {
        IRubyObject rbFunction = args[0];
        IRubyObject rbParameterTypes = args[1];
        IRubyObject rbReturnType = args[2];
        RubyHash options = (RubyHash) args[3];

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        boolean saveError = true;
        IRubyObject typeMap = null;

        IRubyObject rbConvention = options.fastARef(context.runtime.newSymbol("convention"));
        if (rbConvention != null && !rbConvention.isNil()) {
            convention = rbConvention.asJavaString();
        }

        IRubyObject rbSaveErrno = options.fastARef(context.runtime.newSymbol("save_errno"));
        if (rbSaveErrno != null && !rbSaveErrno.isNil()) {
            saveError = rbSaveErrno.isTrue();
        }

        enums = options.fastARef(context.runtime.newSymbol("enums"));
        if (enums != null && !enums.isNil() && !(enums instanceof RubyHash || enums instanceof Enums)) {
            throw context.runtime.newTypeError("wrong type for options[:enum] "
                + enums.getMetaClass().getName() + " (expected Hash or Enums)");

        }
        typeMap = options.fastARef(context.runtime.newSymbol("type_map"));
        if (typeMap != null && !typeMap.isNil() && !(typeMap instanceof RubyHash)) {
            throw context.runtime.newTypeError("wrong type for options[:type_map] "
                    + typeMap.getMetaClass().getName() + " (expected Hash)");

        }

        final Type returnType = org.jruby.ext.ffi.Util.findType(context, rbReturnType, typeMap);

        if (!(rbParameterTypes instanceof RubyArray)) {
            throw context.runtime.newTypeError("Invalid parameter array "
                    + rbParameterTypes.getMetaClass().getName() + " (expected Array)");
        }

        if (!(rbFunction instanceof Pointer)) {
            throw context.runtime.newTypeError(rbFunction, context.runtime.getFFI().pointerClass);
        }
        final Pointer address = (Pointer) rbFunction;

        CallingConvention callConvention = "stdcall".equals(convention)
                        ? CallingConvention.STDCALL : CallingConvention.DEFAULT;

        RubyArray paramTypes = (RubyArray) rbParameterTypes;
        RubyArray fixed = RubyArray.newArray(context.runtime);
        for (int i = 0; i < paramTypes.getLength(); ++i) {
            Type type = (Type)paramTypes.entry(i);
            if (type.getNativeType() != org.jruby.ext.ffi.NativeType.VARARGS)
                fixed.append(type);
        }

        FunctionInvoker functionInvoker = DefaultMethodFactory.getFunctionInvoker(returnType);

        VariadicInvoker varInvoker = new VariadicInvoker(context.runtime, klass, address, functionInvoker, FFIUtil.getFFIType(returnType), callConvention, enums, saveError);

        varInvoker.setInstanceVariable("@fixed", fixed);
        varInvoker.setInstanceVariable("@type_map", typeMap);

        return varInvoker;
    }

    @JRubyMethod(name = { "invoke" })
    public IRubyObject invoke(ThreadContext context, IRubyObject typesArg, IRubyObject paramsArg) {
        IRubyObject[] types = ((RubyArray) typesArg).toJavaArrayMaybeUnsafe();
        IRubyObject[] params = ((RubyArray) paramsArg).toJavaArrayMaybeUnsafe();
        com.kenai.jffi.Type[] ffiParamTypes = new com.kenai.jffi.Type[types.length];
        ParameterMarshaller[] marshallers = new ParameterMarshaller[types.length];
        RubyClass builtinClass = Type.getTypeClass(context.getRuntime()).getClass("Builtin");

        for (int i = 0; i < types.length; ++i) {
            Type type = (Type) types[i];
            switch (NativeType.valueOf(type)) {
                case CHAR:
                case SHORT:
                case INT:
                    ffiParamTypes[i] = com.kenai.jffi.Type.SINT32;
                    marshallers[i] = DefaultMethodFactory.getMarshaller((Type)builtinClass.getConstant(NativeType.INT.name().toUpperCase(LOCALE)), convention, enums);
                    break;
                case UCHAR:
                case USHORT:
                case UINT:
                    ffiParamTypes[i] = com.kenai.jffi.Type.UINT32;
                    marshallers[i] = DefaultMethodFactory.getMarshaller((Type)builtinClass.getConstant(NativeType.UINT.name().toUpperCase(LOCALE)), convention, enums);
                    break;
                case FLOAT:
                case DOUBLE:
                    ffiParamTypes[i] = com.kenai.jffi.Type.DOUBLE;
                    marshallers[i] = DefaultMethodFactory.getMarshaller((Type)builtinClass.getConstant(NativeType.DOUBLE.name().toUpperCase(LOCALE)), convention, enums);
                    break;
                default:
                    ffiParamTypes[i] = FFIUtil.getFFIType(type);
                    marshallers[i] = DefaultMethodFactory.getMarshaller((Type) types[i], convention, enums);
                    break;
            }
        }

        Invocation invocation = new Invocation(context);
        Function function = new Function(address.getAddress(), returnType, ffiParamTypes, convention, saveError);
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
