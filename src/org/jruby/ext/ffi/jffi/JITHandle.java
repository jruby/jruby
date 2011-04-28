/*
 *
 */
package org.jruby.ext.ffi.jffi;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.ext.ffi.Type;

/**
 *
 */
final class JITHandle {

    private static final int THRESHOLD = Integer.getInteger("jruby.ffi.compile.threshold", 10);
    private final JITSignature jitSignature;
    private volatile boolean compilationFailed = false;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile Class<? extends NativeInvoker> compiledClass = null;

    JITHandle(JITSignature signature, boolean compilationFailed) {
        this.jitSignature = signature;
        this.compilationFailed = compilationFailed;
    }

    final boolean compilationFailed() {
        return compilationFailed;
    }

    final NativeInvoker compile(com.kenai.jffi.Function function, Signature signature) {
        if (compilationFailed || counter.incrementAndGet() < THRESHOLD) {
            return null;
        }

        synchronized (this) {
            if (compiledClass == null) {
                compiledClass = newInvokerClass(jitSignature);
                if (compiledClass == null) {
                    compilationFailed = true;
                    return null;
                }
            }
        }

        // Get any result and parameter converters needed
        NativeDataConverter resultConverter = DataConverters.getResultConverter(signature.getResultType());
        NativeDataConverter[] parameterConverters = new NativeDataConverter[signature.getParameterCount()];
        for (int i = 0; i < parameterConverters.length; i++) {
            Type parameterType = signature.getParameterType(i);
            parameterConverters[i] = DataConverters.getParameterConverter(parameterType, signature.getEnums());
        }

        try {
            Constructor<? extends NativeInvoker> cons = compiledClass.getDeclaredConstructor(com.kenai.jffi.Function.class, NativeDataConverter.class, NativeDataConverter[].class, NativeInvoker.class);
            return cons.newInstance(function, resultConverter, parameterConverters,
                    createFallbackInvoker(function, jitSignature));
        } catch (Throwable t) {
            return null;
        }
    }

    Class<? extends NativeInvoker> newInvokerClass(JITSignature jitSignature) {

        JITMethodGenerator generator = null;
        JITMethodGenerator[] generators = {
            new FastIntMethodGenerator(),
            new FastLongMethodGenerator(),
            new FastNumericMethodGenerator(),};

        for (int i = 0; i < generators.length; i++) {
            if (generators[i].isSupported(jitSignature)) {
                generator = generators[i];
                break;
            }
        }

        if (generator == null) {
            return null;
        }

        return new AsmClassBuilder(generator, jitSignature).build();
    }
    
    
    static NativeInvoker createFallbackInvoker(com.kenai.jffi.Function function, JITSignature signature) {
        
        ParameterMarshaller[] parameterMarshallers = new ParameterMarshaller[signature.getParameterCount()];
        for (int i = 0; i < parameterMarshallers.length; i++) {
            parameterMarshallers[i] = DefaultMethodFactory.getMarshaller(signature.getParameterType(i));
        }
        
        return new BufferNativeInvoker(function, 
                DefaultMethodFactory.getFunctionInvoker(signature.getResultType()), 
                parameterMarshallers);
    }
}
