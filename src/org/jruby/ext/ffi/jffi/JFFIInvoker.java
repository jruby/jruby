
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JFFIInvoker extends org.jruby.ext.ffi.AbstractInvoker {
    private final Function function;
    private final Type returnType;
    private final Type[] parameterTypes;
    private final CallingConvention convention;
    private final IRubyObject enums;
    
    public static RubyClass createInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Invoker",
                module.fastGetClass("AbstractInvoker"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractInvoker.class);
        result.defineAnnotatedMethods(JFFIInvoker.class);
        result.defineAnnotatedConstants(JFFIInvoker.class);

        return result;
    }
    
    
    JFFIInvoker(Ruby runtime, long address, Type returnType, Type[] parameterTypes) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Invoker"),
                new CodeMemoryIO(runtime, address),
                returnType, parameterTypes, "default", null);
    }

    JFFIInvoker(Ruby runtime, RubyClass klass, DirectMemoryIO fptr,
            Type returnType, Type[] parameterTypes, String convention, IRubyObject enums) {
        super(runtime, klass, parameterTypes.length, fptr);

        final com.kenai.jffi.Type jffiReturnType = FFIUtil.getFFIType(returnType);
        if (jffiReturnType == null) {
            throw runtime.newArgumentError("Invalid return type " + returnType);
        }
        
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            if ((jffiParamTypes[i] = FFIUtil.getFFIType(parameterTypes[i])) == null) {
                throw runtime.newArgumentError("Invalid parameter type " + parameterTypes[i]);
            }
        }
        
        function = new Function(fptr.getAddress(), jffiReturnType, jffiParamTypes);
        this.parameterTypes = new Type[parameterTypes.length];
        System.arraycopy(parameterTypes, 0, this.parameterTypes, 0, parameterTypes.length);
        this.returnType = returnType;
        this.convention = "stdcall".equals(convention)
                ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        this.enums = enums;
        // Wire up Function#call(*args) to use the super-fast native invokers
        getSingletonClass().addMethod("call", createDynamicMethod(getSingletonClass()));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 4)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args) {

        if (!(args[0] instanceof Pointer)) {
            throw context.getRuntime().newTypeError("Invalid function address "
                    + args[0].getMetaClass().getName() + " (expected FFI::Pointer)");
        }
        
        if (!(args[1] instanceof RubyArray)) {
            throw context.getRuntime().newTypeError("Invalid parameter array "
                    + args[1].getMetaClass().getName() + " (expected Array)");
        }

        if (!(args[2] instanceof Type)) {
            throw context.getRuntime().newTypeError("Invalid return type " + args[2]);
        }
        Pointer ptr = (Pointer) args[0];
        RubyArray paramTypes = (RubyArray) args[1];
        Type returnType = (Type) args[2];

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        if (args[3] instanceof RubyHash) {
            RubyHash options = (RubyHash) args[3];
            convention = options.fastARef(context.getRuntime().newSymbol("convention")).asJavaString();
            enums = options.fastARef(context.getRuntime().newSymbol("enums"));
            if (enums != null && !enums.isNil() && !(enums instanceof RubyHash)) {
                throw context.getRuntime().newTypeError("wrong type for options[:enum] "
                        + enums.getMetaClass().getName() + " (expected Hash)");

            }
        } else {
            convention = args[3].asJavaString();
        }

        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            IRubyObject type = paramTypes.entry(i);
            if (!(type instanceof Type)) {
                throw context.getRuntime().newArgumentError("Invalid parameter type");
            }
            parameterTypes[i] = (Type) paramTypes.entry(i);
        }
        DirectMemoryIO fptr = (DirectMemoryIO) ptr.getMemoryIO();
        return new JFFIInvoker(context.getRuntime(), (RubyClass) recv, fptr,
                (Type) returnType, parameterTypes, convention, enums);
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        return (enums == null || enums.isNil())
            ? MethodFactory.createDynamicMethod(getRuntime(), module, function,
                    returnType, parameterTypes, convention)
            : DefaultMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes, convention, enums);
    }
    
}
