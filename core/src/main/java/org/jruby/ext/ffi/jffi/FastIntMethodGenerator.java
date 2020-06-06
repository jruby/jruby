package org.jruby.ext.ffi.jffi;


import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
import org.jruby.ext.ffi.NativeType;
import com.kenai.jffi.Platform;

/**
 *
 */
final class FastIntMethodGenerator extends AbstractNumericMethodGenerator {
    private static final int MAX_PARAMETERS = getMaximumFastIntParameters();

    private static final String[] signatures = buildSignatures(int.class, MAX_PARAMETERS);

    private static final String[] methodNames = {
        "invokeI0", "invokeI1", "invokeI2", "invokeI3", "invokeI4", "invokeI5", "invokeI6"
    };

    String getInvokerMethodName(JITSignature signature) {

        final int parameterCount = signature.getParameterCount();

        if (parameterCount <= MAX_PARAMETERS && parameterCount <= methodNames.length) {
            return methodNames[parameterCount];

        } else {
            throw new IllegalArgumentException("invalid fast-int parameter count: " + parameterCount);
        }
    }

    String getInvokerSignature(int parameterCount) {
        if (parameterCount <= MAX_PARAMETERS && parameterCount <= signatures.length) {
            return signatures[parameterCount];
        }
        throw new IllegalArgumentException("invalid fast-int parameter count: " + parameterCount);
    }

    final Class getInvokerIntType() {
        return int.class;
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

        if (!platform.getCPU().equals(Platform.CPU.I386) && !platform.getCPU().equals(Platform.CPU.X86_64)) {
            return false;
        }

        for (int i = 0; i < parameterCount; i++) {
            if (!isFastIntParameter(platform, signature.getParameterType(i))) {
                return false;
            }
        }

        return isFastIntResult(platform, signature.getResultType());
    }


    final static int getMaximumFastIntParameters() {
        try {
            com.kenai.jffi.Invoker.class.getDeclaredMethod("invokeI6", CallContext.class, long.class,
                    int.class, int.class, int.class, int.class, int.class, int.class);
            return 6;
        } catch (Throwable t) {
            return -1;
        }
    }


    private static boolean isFastIntType(Platform platform, NativeType type) {
        switch (type) {
            case BOOL:
            case CHAR:
            case UCHAR:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
                return true;
                
            case LONG:
            case ULONG:
                return platform.longSize() == 32;
                
            default:
                return false;
        }
    }


    static boolean isFastIntResult(Platform platform, NativeType type) {
        switch (type) {
            case VOID:
                return true;

            case POINTER:
            case STRING:
            case TRANSIENT_STRING:
                return platform.addressSize() == 32;

            default:
                return isFastIntType(platform, type);
        }
    }

    static boolean isFastIntParameter(Platform platform, NativeType type) {
        switch (type) {
            case POINTER:
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
            case STRING:
            case TRANSIENT_STRING:
                return platform.addressSize() == 32;
            default:
                return isFastIntType(platform, type);
        }
    }
}
