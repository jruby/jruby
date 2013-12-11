
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
import org.jruby.ext.ffi.*;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="FFI::Function", parent="FFI::Pointer")
public final class Function extends org.jruby.ext.ffi.AbstractInvoker {
    
    private final com.kenai.jffi.Function function;
    private final NativeFunctionInfo functionInfo;
    private final IRubyObject enums;
    private final boolean saveError;
    private volatile boolean autorelease = true;

    public static RubyClass createFunctionClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Function",
                module.getClass("Pointer"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractInvoker.class);
        result.defineAnnotatedMethods(Function.class);
        result.defineAnnotatedConstants(Function.class);

        return result;
    }
    
    Function(Ruby runtime, RubyClass klass, MemoryIO address,
            Type returnType, Type[] parameterTypes, CallingConvention convention,
            IRubyObject enums, boolean saveError) {
        super(runtime, klass, parameterTypes.length, address);

        this.functionInfo = new NativeFunctionInfo(runtime, returnType, parameterTypes, convention);

        function = new com.kenai.jffi.Function(address.address(),
                functionInfo.jffiReturnType, functionInfo.jffiParameterTypes, functionInfo.convention, saveError);
        
        this.enums = enums;
        this.saveError = saveError;
        // Wire up Function#call(*args) to use the super-fast native invokers
        getSingletonClass().addMethod("call", createDynamicMethod(getSingletonClass()));
    }

    Function(Ruby runtime, RubyClass klass, MemoryIO address,
            NativeFunctionInfo functionInfo, IRubyObject enums) {
        super(runtime, klass, functionInfo.parameterTypes.length, address);

        this.functionInfo = functionInfo;

        function = new com.kenai.jffi.Function(address.address(),
                functionInfo.jffiReturnType, functionInfo.jffiParameterTypes, functionInfo.convention);
        this.enums = enums;
        this.saveError = true;
        // Wire up Function#call(*args) to use the super-fast native invokers
        getSingletonClass().addMethod("call", createDynamicMethod(getSingletonClass()));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 2, optional = 2)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        MemoryIO fptr = null;
        RubyHash options = null;
        Object proc = null;
        int optionsIndex = 2;

        Type returnType = org.jruby.ext.ffi.Util.findType(context, args[0]);

        if (!(args[1] instanceof RubyArray)) {
            throw context.runtime.newTypeError("Invalid parameter array "
                    + args[1].getMetaClass().getName() + " (expected Array)");
        }

        RubyArray paramTypes = (RubyArray) args[1];
        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = org.jruby.ext.ffi.Util.findType(context, paramTypes.entry(i));
        }

        if (args.length > 2 && args[2] instanceof Pointer) {
            fptr = new CodeMemoryIO(context.runtime, (Pointer) args[2]);
            optionsIndex = 3;
        } else if (args.length > 2 && (args[2] instanceof RubyProc || args[2].respondsTo("call"))) {
            proc = args[2];
            optionsIndex = 3;
        } else if (block.isGiven()) {
            proc = block;
            optionsIndex = 2;
        } else {
            throw context.runtime.newTypeError("Invalid function address "
                    + args[0].getMetaClass().getName() + " (expected FFI::Pointer)");
        }    

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        boolean saveError = true;
        if (args.length > optionsIndex && args[optionsIndex] instanceof RubyHash) {
            options = (RubyHash) args[optionsIndex];

            IRubyObject rbConvention = options.fastARef(context.runtime.newSymbol("convention"));
            if (rbConvention != null && !rbConvention.isNil()) {
                convention = rbConvention.asJavaString();
            }

            IRubyObject rbSaveErrno = options.fastARef(context.runtime.newSymbol("save_errno"));
            if (rbSaveErrno != null && !rbSaveErrno.isNil()) {
                saveError = rbSaveErrno.isTrue();
            }

            enums = options.fastARef(context.runtime.newSymbol("enums"));
            if (enums != null && !enums.isNil() && !(enums instanceof RubyHash)) {
                throw context.runtime.newTypeError("wrong type for options[:enum] "
                        + enums.getMetaClass().getName() + " (expected Hash)");

            }
        }

        CallingConvention callConvention = "stdcall".equals(convention)
                        ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        if (fptr == null && proc != null) {
            fptr = CallbackManager.getInstance().newClosure(context.runtime,
                    returnType, parameterTypes, proc, callConvention);
        }
        return new Function(context.runtime, (RubyClass) recv, fptr,
                    returnType, parameterTypes, callConvention, enums, saveError);
    }

    @JRubyMethod(name = "free")
    public final IRubyObject free(ThreadContext context) {
        if (getMemoryIO() instanceof AllocatedDirectMemoryIO) {
            ((AllocatedDirectMemoryIO) getMemoryIO()).free();
        } else {
            throw context.runtime.newRuntimeError("cannot free non-allocated function");
        }
        
        // Replace memory object with one that throws an exception on any access
        setMemoryIO(new FreedMemoryIO(context.runtime));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        if (autorelease != release.isTrue() && getMemoryIO() instanceof AllocatedDirectMemoryIO) {
            ((AllocatedDirectMemoryIO) getMemoryIO()).setAutoRelease(autorelease = release.isTrue());
        }

        return context.runtime.getNil();
    }

    @JRubyMethod(name = { "autorelease?", "autorelease" })
    public final IRubyObject autorelease_p(ThreadContext context) {
        return context.runtime.newBoolean(autorelease);
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        return MethodFactory.createDynamicMethod(getRuntime(), module, function,
                    functionInfo.returnType, functionInfo.parameterTypes, functionInfo.convention, enums, !saveError);
    }
    
}
