
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.FreedMemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="FFI::Function", parent="FFI::Pointer")
public final class Function extends org.jruby.ext.ffi.AbstractInvoker {
    
    private final com.kenai.jffi.Function function;
    private final Type returnType;
    private final Type[] parameterTypes;
    private final CallingConvention convention;
    private final IRubyObject enums;
    private volatile boolean autorelease = true;

    public static RubyClass createFunctionClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Function",
                module.fastGetClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractInvoker.class);
        result.defineAnnotatedMethods(Function.class);
        result.defineAnnotatedConstants(Function.class);

        return result;
    }
    
    Function(Ruby runtime, RubyClass klass, DirectMemoryIO address,
            Type returnType, Type[] parameterTypes, CallingConvention convention, IRubyObject enums) {
        super(runtime, klass, parameterTypes.length, address);

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

        function = new com.kenai.jffi.Function(address.getAddress(), jffiReturnType, jffiParamTypes);
        this.parameterTypes = (Type[]) parameterTypes.clone();
        this.returnType = returnType;
        this.convention = convention;
        this.enums = enums;
        // Wire up Function#call(*args) to use the super-fast native invokers
        getSingletonClass().addMethod("call", createDynamicMethod(getSingletonClass()));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 2, optional = 2)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        DirectMemoryIO fptr = null;
        RubyHash options = null;
        Object proc = null;
        int optionsIndex = 2;

        Type returnType = FFIUtil.resolveType(context, args[0]);

        if (!(args[1] instanceof RubyArray)) {
            throw context.getRuntime().newTypeError("Invalid parameter array "
                    + args[1].getMetaClass().getName() + " (expected Array)");
        }

        RubyArray paramTypes = (RubyArray) args[1];
        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = FFIUtil.resolveType(context, paramTypes.entry(i));
        }

        if (args.length > 2 && args[2] instanceof Pointer) {
            fptr = new CodeMemoryIO(context.getRuntime(), (Pointer) args[2]);
            optionsIndex = 3;
        } else if (args.length > 2 && (args[2] instanceof RubyProc || args[2].respondsTo("call"))) {
            proc = args[2];
            optionsIndex = 3;
        } else if (block.isGiven()) {
            proc = block;
            optionsIndex = 2;
        } else {
            throw context.getRuntime().newTypeError("Invalid function address "
                    + args[0].getMetaClass().getName() + " (expected FFI::Pointer)");
        }    

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        if (args.length > optionsIndex && args[optionsIndex] instanceof RubyHash) {
            options = (RubyHash) args[optionsIndex];
            convention = options.fastARef(context.getRuntime().newSymbol("convention")).asJavaString();
            enums = options.fastARef(context.getRuntime().newSymbol("enums"));
            if (enums != null && !enums.isNil() && !(enums instanceof RubyHash)) {
                throw context.getRuntime().newTypeError("wrong type for options[:enum] "
                        + enums.getMetaClass().getName() + " (expected Hash)");

            }
        }
        CallingConvention callConvention = "stdcall".equals(convention)
                        ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        if (fptr == null && proc != null) {
            fptr = CallbackManager.getInstance().newClosure(context.getRuntime(),
                    returnType, parameterTypes, proc, callConvention);
        }
        return new Function(context.getRuntime(), (RubyClass) recv, fptr,
                    returnType, parameterTypes, callConvention, enums);
    }

    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        if (getMemoryIO() instanceof AllocatedDirectMemoryIO) {
            ((AllocatedDirectMemoryIO) getMemoryIO()).free();
        } else {
            throw context.getRuntime().newRuntimeError("cannot free non-allocated function");
        }
        
        // Replace memory object with one that throws an exception on any access
        setMemoryIO(new FreedMemoryIO(context.getRuntime()));
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        if (autorelease != release.isTrue() && getMemoryIO() instanceof AllocatedDirectMemoryIO) {
            ((AllocatedDirectMemoryIO) getMemoryIO()).setAutoRelease(autorelease = release.isTrue());
        }
        
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = { "autorelease?", "autorelease" })
    public final IRubyObject autorelease_p(ThreadContext context) {
        return context.getRuntime().newBoolean(autorelease);
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        if (enums == null || enums.isNil()) {
            return MethodFactory.createDynamicMethod(getRuntime(), module, function,
                    returnType, parameterTypes, convention);
        } else {
            return DefaultMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes, convention, enums);
        }
    }
    
}
