/*
 *
 */
package org.jruby.ext.ffi.jffi;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruby.RubyModule;
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

    final NativeInvoker compile(RubyModule implementationClass, com.kenai.jffi.Function function, Signature signature, String methodName) {
        if (compilationFailed || (counter.incrementAndGet() < THRESHOLD && !"force".equalsIgnoreCase(Options.COMPILE_MODE.load()))) {
            return null;
        }

        Class<? extends NativeInvoker> compiledClass;
        synchronized (this) {
            if (compiledClassRef == null || (compiledClass = compiledClassRef.get()) == null) {
                compiledClass = newInvokerClass(jitSignature, methodName);
                if (compiledClass == null) {
                    compilationFailed = true;
                    return null;
                }
                compiler.registerClass(this, compiledClass);
                compiledClassRef = new WeakReference<Class<? extends NativeInvoker>>(compiledClass);
            }
        }

        try {
            Constructor<? extends NativeInvoker> cons = compiledClass.getDeclaredConstructor(RubyModule.class, com.kenai.jffi.Function.class, Signature.class);
            return cons.newInstance(implementationClass, function, signature);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    Class<? extends NativeInvoker> newInvokerClass(JITSignature jitSignature, String methodName) {

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

        return new AsmClassBuilder(generator, jitSignature, methodName).build();
    }
}
