
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import org.jruby.Ruby;

/**
 * Holds information for creating JFFI functions
 */
public class NativeFunctionInfo {
    public final org.jruby.ext.ffi.Type returnType;
    public final org.jruby.ext.ffi.Type[] parameterTypes;
    public final com.kenai.jffi.Type jffiReturnType;
    public final com.kenai.jffi.Type[] jffiParameterTypes;
    public final com.kenai.jffi.CallingConvention convention;

    public NativeFunctionInfo(Ruby runtime, org.jruby.ext.ffi.Type returnType,
            org.jruby.ext.ffi.Type[] parameterTypes, CallingConvention convention) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.jffiReturnType = FFIUtil.getFFIType(returnType);
        if (jffiReturnType == null) {
            throw runtime.newTypeError("invalid FFI return type: " + returnType);
        }

        this.jffiParameterTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            jffiParameterTypes[i] = FFIUtil.getFFIType(parameterTypes[i]);
            if (jffiParameterTypes[i] == null) {
                throw runtime.newTypeError("invalid FFI parameter type: " + parameterTypes[i]);
            }
        }

        this.convention = convention;
    }




}
