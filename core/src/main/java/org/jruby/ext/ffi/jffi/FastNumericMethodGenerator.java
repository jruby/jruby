package org.jruby.ext.ffi.jffi;


import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Platform;
import org.jruby.ext.ffi.NativeType;

/**
 *
 */
final class FastNumericMethodGenerator extends AbstractNumericMethodGenerator {
    private static final int MAX_PARAMETERS = getMaximumFastNumericParameters();

    private static final String[] signatures = buildSignatures(long.class, MAX_PARAMETERS);

    private static final String[] methodNames = {
        "invokeN0", "invokeN1", "invokeN2", "invokeN3", "invokeN4", "invokeN5", "invokeN6"
    };

    String getInvokerMethodName(JITSignature signature) {

        final int parameterCount = signature.getParameterCount();

        if (parameterCount <= MAX_PARAMETERS && parameterCount <= methodNames.length) {
            return methodNames[parameterCount];

        } else {
            throw new IllegalArgumentException("invalid fast-long parameter count: " + parameterCount);
        }
    }

    String getInvokerSignature(int parameterCount) {
        if (parameterCount <= MAX_PARAMETERS && parameterCount <= signatures.length) {
            return signatures[parameterCount];
        }
        throw new IllegalArgumentException("invalid fast-long parameter count: " + parameterCount);
    }

    final Class getInvokerIntType() {
        return long.class;
    }

    public boolean isSupported(JITSignature signature) {
        final int parameterCount = signature.getParameterCount();

        if (!signature.getCallingConvention().equals(CallingConvention.DEFAULT) || parameterCount > MAX_PARAMETERS) {
            return false;
        }

        final Platform platform = Platform.getPlatform();

        if (platform.getOS().equals(Platform.OS.WINDOWS)) {
            return false;
        }

        // Only supported on amd64 arches
        if (!platform.getCPU().equals(Platform.CPU.I386) && !platform.getCPU().equals(Platform.CPU.X86_64)) {
            return false;
        }

        for (int i = 0; i < parameterCount; i++) {
            if (!isFastNumericParameter(platform, signature.getParameterType(i))) {
                return false;
            }
        }

        return isFastNumericResult(platform, signature.getResultType());
    }


    final static int getMaximumFastNumericParameters() {
        try {
            com.kenai.jffi.Invoker.class.getDeclaredMethod("invokeN6", CallContext.class, long.class,
                    long.class, long.class, long.class, long.class, long.class, long.class);
            return 6;
        } catch (Throwable t) {
            return -1;
        }
    }


    private static boolean isFastNumericType(Platform platform, NativeType type) {
        switch (type) {
            case BOOL:
            case CHAR:
            case UCHAR:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
            case LONG:
            case ULONG:
            case LONG_LONG:
            case ULONG_LONG:
            case FLOAT:
            case DOUBLE:
                return true;

            default:
                return false;
        }
    }


    static boolean isFastNumericResult(Platform platform, NativeType type) {
        switch (type) {
            case VOID:
            case POINTER:
            case STRING:
            case TRANSIENT_STRING:
                return true;

            default:
                return isFastNumericType(platform, type);
        }
    }

    static boolean isFastNumericParameter(Platform platform, NativeType type) {
        switch (type) {
            case POINTER:
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
            case STRING:
            case TRANSIENT_STRING:
                return true;
            
            default:
                return isFastNumericType(platform, type);
        }
    }
}
