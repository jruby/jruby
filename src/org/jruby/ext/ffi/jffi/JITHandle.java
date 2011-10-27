/*
 *
 */
package org.jruby.ext.ffi.jffi;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import org.jruby.ext.ffi.Type;
import org.jruby.util.cli.Options;

/**
 *
 */
final class JITHandle {

    private static final int THRESHOLD = Options.FFI_COMPILE_THRESHOLD.load();
    private final JITSignature jitSignature;
    private volatile boolean compilationFailed = false;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final JITCompiler compiler;
    private Reference<Class<? extends NativeInvoker>> compiledClassRef = null;

    JITHandle(JITCompiler compiler, JITSignature signature, boolean compilationFailed) {
        this.compiler = compiler;
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

        Class<? extends NativeInvoker> compiledClass;
        synchronized (this) {
            if (compiledClassRef == null || (compiledClass = compiledClassRef.get()) == null) {
                compiledClass = newInvokerClass(jitSignature);
                if (compiledClass == null) {
                    compilationFailed = true;
                    return null;
                }
                compiler.registerClass(this, compiledClass);
                compiledClassRef = new WeakReference<Class<? extends NativeInvoker>>(compiledClass);
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
            Constructor<? extends NativeInvoker> cons = compiledClass.getDeclaredConstructor(com.kenai.jffi.Function.class, Signature.class, NativeInvoker.class);
            return cons.newInstance(function, signature, createFallbackInvoker(function, jitSignature));
        } catch (Throwable t) {
            throw new RuntimeException(t);
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
