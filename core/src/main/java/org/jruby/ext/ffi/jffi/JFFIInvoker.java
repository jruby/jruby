
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.*;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

public class JFFIInvoker extends org.jruby.ext.ffi.AbstractInvoker {
    private final Function function;
    private final Type returnType;
    private final Type[] parameterTypes;
    private final CallingConvention convention;
    private final IRubyObject enums;
    
    public static RubyClass createInvokerClass(ThreadContext context, RubyModule FFI) {
        return FFI.defineClassUnder(context, "Invoker", FFI.getClass("AbstractInvoker"), NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, AbstractInvoker.class, JFFIInvoker.class).
                defineConstants(context, JFFIInvoker.class);
    }

    JFFIInvoker(Ruby runtime, long address, Type returnType, Type[] parameterTypes, CallingConvention convention) {
        this(runtime, runtime.getModule("FFI").getClass("Invoker"),
                new CodeMemoryIO(runtime, address),
                returnType, parameterTypes, convention, null);
    }

    JFFIInvoker(Ruby runtime, RubyClass klass, MemoryIO fptr,
            Type returnType, Type[] parameterTypes, CallingConvention convention, IRubyObject enums) {
        super(runtime, klass, parameterTypes.length, fptr);

        final com.kenai.jffi.Type jffiReturnType = FFIUtil.getFFIType(returnType);
        if (jffiReturnType == null) throw argumentError(runtime.getCurrentContext(), "Invalid return type " + returnType);
        
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            if ((jffiParamTypes[i] = FFIUtil.getFFIType(parameterTypes[i])) == null) {
                throw argumentError(runtime.getCurrentContext(), "Invalid parameter type " + parameterTypes[i]);
            }
        }
        
        function = new Function(fptr.address(), jffiReturnType, jffiParamTypes);
        this.parameterTypes = (Type[]) parameterTypes.clone();
        this.returnType = returnType;
        this.convention = convention;
        this.enums = enums;
        // Wire up Function#call(*args) to use the super-fast native invokers
        getSingletonClass().addMethod("call", createDynamicMethod(getSingletonClass()));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 4)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args) {

        if (!(args[0] instanceof Pointer)) {
            throw typeError(context, "Invalid function address ", args[0], " (expected FFI::Pointer)");
        }
        
        if (!(args[1] instanceof RubyArray)) {
            throw typeError(context, "Invalid parameter array ", args[1], " (expected Array)");
        }

        if (!(args[2] instanceof Type)) throw typeError(context, "Invalid return type " + args[2]);

        Pointer ptr = (Pointer) args[0];
        RubyArray paramTypes = (RubyArray) args[1];
        Type returnType = (Type) args[2];

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        if (args[3] instanceof RubyHash) {
            RubyHash options = (RubyHash) args[3];
            convention = options.fastARef(asSymbol(context, "convention")).asJavaString();
            enums = options.fastARef(asSymbol(context, "enums"));
            if (enums != null && !enums.isNil() && !(enums instanceof RubyHash || enums instanceof Enums)) {
                throw typeError(context, "wrong type for options[:enum] ", enums, " (expected Hash or Enums)");
            }
        } else {
            convention = args[3].asJavaString();
        }

        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            IRubyObject type = paramTypes.entry(i);
            if (!(type instanceof Type te)) throw argumentError(context, "Invalid parameter type");
            parameterTypes[i] = te;
        }

        return new JFFIInvoker(context.runtime, (RubyClass) recv, ptr.getMemoryIO(), returnType, parameterTypes,
                "stdcall".equals(convention) ? CallingConvention.STDCALL : CallingConvention.DEFAULT, enums);
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        return MethodFactory.createDynamicMethod(getRuntime(), module, function,
                    returnType, parameterTypes, convention, enums, false);
    }
    
}
