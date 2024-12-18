
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
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

@JRubyClass(name="FFI::Function", parent="FFI::Pointer")
public final class Function extends org.jruby.ext.ffi.AbstractInvoker {
    
    private final com.kenai.jffi.Function function;
    private final NativeFunctionInfo functionInfo;
    private final IRubyObject enums;
    private final boolean saveError;
    private volatile boolean autorelease = true;

    public static RubyClass createFunctionClass(ThreadContext context, RubyModule FFI) {
        return FFI.defineClassUnder(context, "Function", FFI.getClass(context, "Pointer"), NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, AbstractInvoker.class, Function.class).
                defineConstants(context, Function.class);
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
        var singleton = singletonClass(getRuntime().getCurrentContext());
        // Wire up Function#call(*args) to use the super-fast native invokers
        singleton.addMethod("call", createDynamicMethod(singleton));
    }

    Function(Ruby runtime, RubyClass klass, MemoryIO address,
            NativeFunctionInfo functionInfo, IRubyObject enums) {
        super(runtime, klass, functionInfo.parameterTypes.length, address);

        this.functionInfo = functionInfo;

        function = new com.kenai.jffi.Function(address.address(),
                functionInfo.jffiReturnType, functionInfo.jffiParameterTypes, functionInfo.convention);
        this.enums = enums;
        this.saveError = true;
        var singleton = singletonClass(getRuntime().getCurrentContext());
        // Wire up Function#call(*args) to use the super-fast native invokers
        singleton.addMethod("call", createDynamicMethod(singleton));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 2, optional = 2, checkArity = false)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 2, 4);
        MemoryIO fptr = null;
        Object proc = null;
        int optionsIndex;

        if (!(args[1] instanceof RubyArray)) throw typeError(context, "Invalid parameter array ", args[1], " (expected Array)");

        RubyArray paramTypes = (RubyArray) args[1];
        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameterTypes[i] = org.jruby.ext.ffi.Util.findType(context, paramTypes.entry(i));
        }

        if (argc > 2 && args[2] instanceof Pointer) {
            fptr = new CodeMemoryIO(context.runtime, (Pointer) args[2]);
            optionsIndex = 3;
        } else if (argc > 2 && (args[2] instanceof RubyProc || args[2].respondsTo("call"))) {
            proc = args[2];
            optionsIndex = 3;
        } else if (block.isGiven()) {
            proc = block;
            optionsIndex = 2;
        } else {
            throw typeError(context, "Invalid function address ", args[0], " (expected FFI::Pointer)");
        }

        Type returnType = org.jruby.ext.ffi.Util.findType(context, args[0]);

        // Get the convention from the options hash
        String convention = "default";
        IRubyObject enums = null;
        boolean saveError = true;
        if (argc > optionsIndex && args[optionsIndex] instanceof RubyHash options) {
            IRubyObject rbConvention = options.fastARef(asSymbol(context, "convention"));
            if (rbConvention != null && !rbConvention.isNil()) convention = rbConvention.asJavaString();

            IRubyObject rbSaveErrno = options.fastARef(asSymbol(context, "save_errno"));
            if (rbSaveErrno != null && !rbSaveErrno.isNil()) saveError = rbSaveErrno.isTrue();

            enums = options.fastARef(asSymbol(context, "enums"));
            if (enums != null && !enums.isNil() && !(enums instanceof RubyHash || enums instanceof Enums)) {
                throw typeError(context, "wrong type for options[:enum] ", enums, " (expected Hash or Enums)");
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
        if (!(getMemoryIO() instanceof AllocatedDirectMemoryIO mio)) throw runtimeError(context, "cannot free non-allocated function");
        mio.free();
        // Replace memory object with one that throws an exception on any access
        setMemoryIO(new FreedMemoryIO(context.runtime));
        return context.nil;
    }

    @JRubyMethod(name = "autorelease=")
    public final IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        if (autorelease != release.isTrue() && getMemoryIO() instanceof AllocatedDirectMemoryIO amio) {
           amio.setAutoRelease(autorelease = release.isTrue());
        }

        return context.nil;
    }

    @JRubyMethod(name = { "autorelease?", "autorelease" })
    public final IRubyObject autorelease_p(ThreadContext context) {
        return asBoolean(context, autorelease);
    }

    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        return MethodFactory.createDynamicMethod(getRuntime(), module, function,
                    functionInfo.returnType, functionInfo.parameterTypes, functionInfo.convention, enums, !saveError);
    }
    
}
