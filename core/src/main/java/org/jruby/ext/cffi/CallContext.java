package org.jruby.ext.cffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CallContext extends RubyObject {
    private final Type returnType;
    private final Type[] parameterTypes;
    private final CallingConvention callingConvention;
    
    CallContext(Ruby runtime, RubyClass metaClass, Type returnType, Type[] parameterTypes, CallingConvention callingConvention) {
        super(runtime, metaClass);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.callingConvention = callingConvention;
    }
    
    @JRubyMethod(name = "new", module = true, required = 2, optional = 1)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        try {
            RubyArray parameters = args[1].convertToArray();
            Type[] parameterTypes = new Type[parameters.getLength()];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[i] = (Type) parameters.get(i);
            }
            
            CallingConvention callingConvention = CallingConvention.DEFAULT;
            if (args.length > 2) {
                IRubyObject convention = args[2].convertToHash().fastARef(RubySymbol.newSymbol(context.runtime, "convention"));
                if ("stdcall".equals(convention.asJavaString())) {
                    callingConvention = CallingConvention.STDCALL;
                }
            }
    
            return new CallContext(context.runtime, (RubyClass) self, (Type) args[0], parameterTypes, callingConvention);
        
        } catch (ClassCastException cce) {
            throw badTypes(context.runtime, args[0], args[1].convertToArray().toJavaArrayMaybeUnsafe());
        }
    }
    
    private static RaiseException badTypes(Ruby runtime, IRubyObject returnType, IRubyObject[] parameterTypes) {

        if (!(returnType instanceof Type)) {
            return runtime.newTypeError("wrong argument type "
                    + returnType.getMetaClass().getName() + " (expected JRuby::CFFI::Type)");
        }

        for (IRubyObject obj : parameterTypes) {
            if (!(obj instanceof Type)) {
                return runtime.newTypeError("wrong argument type "
                        + obj.getMetaClass().getName() + " (expected array of JRuby::CFFI::Type)");
            }
        }
        return runtime.newRuntimeError("unexpected error");
    }

    @JRubyMethod(name = "to_s")
    public final IRubyObject to_s(ThreadContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("#<").append(getMetaClass().getRealClass().getName()).append(" params=[");
        for (int i = 0; i < parameterTypes.length; ++i) {
            sb.append(parameterTypes[i].toString().toLowerCase());
            if (i < (parameterTypes.length - 1)) {
                sb.append(", ");
            }
        }
        sb.append("], return=").append(returnType.toString().toLowerCase()).append(", convention=").append(callingConvention()).append(">");
        return context.runtime.newString(sb.toString());
    }
    
    
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallContext[parameters=[");
        for (int i = 0; i < parameterTypes.length; ++i) {
            sb.append(parameterTypes[i].toString().toLowerCase());
            if (i < (parameterTypes.length - 1)) {
                sb.append(", ");
            }
        }
        sb.append("] return=").append(returnType.toString().toLowerCase()).append("]");
        return sb.toString();
    }


    @JRubyMethod
    public final IRubyObject result_type(ThreadContext context) {
        return returnType;
    }

    @JRubyMethod
    public final IRubyObject param_types(ThreadContext context) {
        return RubyArray.newArray(context.runtime, parameterTypes);
    }

    /**
     * Gets the native return type the callback should return
     *
     * @return The native return type
     */
    final Type getReturnType() {
        return returnType;
    }

    /**
     * Gets the ruby parameter types of the callback
     *
     * @return An array of the parameter types
     */
    final Type[] getParameterTypes() {
        return parameterTypes;
    }
    
    final int getParameterCount() {
        return parameterTypes.length;
    }
    
    final Type getParameterType(int idx) {
        return parameterTypes[idx];
    }

    final CallingConvention callingConvention() {
        return callingConvention;
    }
    
    static RubyClass getClass(Ruby runtime) {
        return JRubyCFFILibrary.getModule(runtime).getClass("CallContext");
    }
}
