package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import java.util.Arrays;
import org.jruby.RubyHash;
import org.jruby.ext.ffi.Type;

/**
 * A native function signature
 */
final class Signature {
    private final Type resultType;
    private final Type[] parameterTypes;
    private final CallingConvention convention;
    private final boolean ignoreError;
    private final RubyHash enums;

    public Signature(Type resultType, Type[] parameterTypes, 
            CallingConvention convention, boolean ignoreError, RubyHash enums) {
        this.resultType = resultType;
        this.parameterTypes = (Type[]) parameterTypes.clone();
        this.convention = convention;
        this.ignoreError = ignoreError;
        this.enums = enums;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        }
        Signature rhs = (Signature) o;
        return resultType.equals(rhs.resultType) && convention.equals(rhs.convention)
                && ignoreError == rhs.ignoreError
                && Arrays.equals(parameterTypes, rhs.parameterTypes)
                && ((enums == null && rhs.enums == null) || enums.equals(rhs.enums))
                ;
    }

    @Override
    public int hashCode() {
        return resultType.hashCode() 
                ^ convention.hashCode() 
                ^ Boolean.valueOf(ignoreError).hashCode()
                ^ Arrays.hashCode(parameterTypes)
                ^ (enums == null ? 0 : enums.hashCode());
    }

    public CallingConvention getCallingConvention() {
        return convention;
    }

    public boolean isIgnoreError() {
        return ignoreError;
    }

    public int getParameterCount() {
        return parameterTypes.length;
    }
    
    public Type getParameterType(int parameterIndex) {
        return parameterTypes[parameterIndex];
    }

    public Type getResultType() {
        return resultType;
    }

    public RubyHash getEnums() {
        return enums;
    }
}
