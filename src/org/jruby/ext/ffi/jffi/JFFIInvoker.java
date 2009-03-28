
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.Library;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JFFIInvoker extends org.jruby.ext.ffi.AbstractInvoker {
    /**
     * Reference map to keep libraries open for as long as there is a method mapped
     * to that library.
     */
    private static final Map<DynamicMethod, Object> libraryRefMap
            = Collections.synchronizedMap(new WeakHashMap<DynamicMethod, Object>());
    private final Object handle;
    private final Function function;
    private final Type returnType;
    private final Type[] parameterTypes;
    private final int parameterCount;
    private final CallingConvention convention;
    private final RubyModule callModule;
    private final DynamicMethod callMethod;
    
    public static RubyClass createInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Invoker",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractInvoker.class);
        result.defineAnnotatedMethods(JFFIInvoker.class);
        result.defineAnnotatedConstants(JFFIInvoker.class);

        return result;
    }
    
    JFFIInvoker(Ruby runtime, String libraryName, String functionName, Type returnType, Type[] parameterTypes, String convention) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Invoker"), Library.getCachedInstance(libraryName, Library.LAZY),
                Library.getCachedInstance(libraryName, Library.LAZY).getSymbolAddress(functionName),
                returnType, parameterTypes, convention);
    }
    JFFIInvoker(Ruby runtime, long address, Type returnType, Type[] parameterTypes) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Invoker"), null, address,
                returnType, parameterTypes, "default");
    }
    JFFIInvoker(Ruby runtime, RubyClass klass, Object handle, long address, Type returnType, Type[] parameterTypes, String convention) {
        super(runtime, klass, parameterTypes.length);

        final com.kenai.jffi.Type jffiReturnType = getFFIType(returnType);
        if (jffiReturnType == null) {
            throw runtime.newArgumentError("Invalid return type " + returnType);
        }
        
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            if ((jffiParamTypes[i] = getFFIType(parameterTypes[i])) == null) {
                throw runtime.newArgumentError("Invalid parameter type " + parameterTypes[i]);
            }
        }

        this.handle = handle;
        function = new Function(address, jffiReturnType, jffiParamTypes);
        this.parameterTypes = new Type[parameterTypes.length];
        System.arraycopy(parameterTypes, 0, this.parameterTypes, 0, parameterTypes.length);
        this.parameterCount = parameterTypes.length;
        this.returnType = returnType;
        this.convention = "stdcall".equals(convention)
                ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        this.callModule = RubyModule.newModule(runtime);
        this.callModule.addModuleFunction("call", callMethod = createDynamicMethod(callModule));
    }
    
    @JRubyMethod(name = { "new" }, meta = true, required = 4)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args) {

        if (!(args[0] instanceof BasePointer)) {
            throw context.getRuntime().newArgumentError("Invalid function address");
        }
        
        if (!(args[1] instanceof RubyArray)) {
            throw context.getRuntime().newArgumentError("Invalid parameter types array");
        }

        if (!(args[2] instanceof Type)) {
            throw context.getRuntime().newArgumentError("Invalid return type");
        }
        BasePointer ptr = (BasePointer) args[0];
        RubyArray paramTypes = (RubyArray) args[1];
        Type returnType = (Type) args[2];
        String convention = args[3].toString();

        Type[] parameterTypes = new Type[paramTypes.size()];
        for (int i = 0; i < parameterTypes.length; ++i) {
            IRubyObject type = paramTypes.entry(i);
            if (!(type instanceof Type)) {
                throw context.getRuntime().newArgumentError("Invalid parameter type");
            }
            parameterTypes[i] = (Type) paramTypes.entry(i);
        }
        
        return new JFFIInvoker(context.getRuntime(), (RubyClass) recv, ptr,
                ptr.getAddress(), (Type) returnType, parameterTypes, convention);
    }

    /**
     * Invokes the native function with the supplied ruby arguments.
     * @param rubyArgs The ruby arguments to pass to the native function.
     * @return The return value from the native function, as a ruby object.
     */
    @JRubyMethod(name= { "invoke", "call", "call0", "call1", "call2", "call3" }, rest = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        return callMethod.call(context, callModule, callModule.getSingletonClass(), "call", args, Block.NULL_BLOCK);
    }
    
    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        DynamicMethod dm;
        if (convention == CallingConvention.DEFAULT
            && FastIntMethodFactory.getFactory().isFastIntMethod(returnType, parameterTypes)) {
            dm = FastIntMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes);
        } else if (convention == CallingConvention.DEFAULT
            && FastLongMethodFactory.getFactory().isFastLongMethod(returnType, parameterTypes)) {
            dm = FastLongMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes);
        } else {
            dm = DefaultMethodFactory.getFactory().createMethod(module,
                    function, returnType, parameterTypes, convention);
        }
        libraryRefMap.put(dm, handle);
        return dm;
    }

    static final com.kenai.jffi.Type getFFIType(Type type) {

        if (type instanceof Type.Builtin || type instanceof CallbackInfo) {
            return FFIUtil.getFFIType(type.getNativeType());
        } else {
            return null;
        }
    }
}
