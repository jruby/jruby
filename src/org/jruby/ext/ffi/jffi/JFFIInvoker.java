
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Address;
import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Function;
import com.kenai.jffi.Library;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JFFIInvoker extends org.jruby.ext.ffi.Invoker {
    private static final Map<DynamicMethod, Library> libraryRefMap
            = Collections.synchronizedMap(new WeakHashMap<DynamicMethod, Library>());
    private final Library library;
    private final Function function;
    private final NativeType returnType;
    private final NativeParam[] parameterTypes;
    private final int parameterCount;
    private final CallingConvention convention;
    
    public JFFIInvoker(Ruby runtime, String libraryName, String functionName, NativeType returnType, NativeParam[] parameterTypes, String convention) {
        super(runtime, parameterTypes.length);
        try {
            this.library = LibraryCache.open(libraryName, Library.LAZY);
        } catch (UnsatisfiedLinkError ex) {
            throw runtime.newLoadError("Could not open library " + libraryName);
        }
        Address address = library.findSymbol(functionName);
        if (address == null) {
            throw runtime.newLoadError("Could find function " + functionName);
        }
        final com.kenai.jffi.Type jffiReturnType = getFFIType(returnType);
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            jffiParamTypes[i] = getFFIType(parameterTypes[i]);
        }
        function = new Function(address, jffiReturnType, jffiParamTypes);
        this.parameterTypes = new NativeParam[parameterTypes.length];
        System.arraycopy(parameterTypes, 0, this.parameterTypes, 0, parameterTypes.length);
        this.parameterCount = parameterTypes.length;
        this.returnType = returnType;
        this.convention = "stdcall".equals(convention)
                ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
    }


    @Override
    public IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        libraryRefMap.put(dm, library);
        return dm;
    }
    private static final com.kenai.jffi.Type getFFIType(NativeParam type) {

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
        } else if (type instanceof CallbackInfo) {
            return com.kenai.jffi.Type.POINTER;
        } else {
            throw new IllegalArgumentException("Unknown type " + type);
        }
    }
}
