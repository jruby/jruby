
package org.jruby.compiler;

import org.jruby.Ruby;
import org.jruby.internal.runtime.methods.CallbackMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.callback.CompiledReflectionCallback;

/**
 * Provides some services for Ruby code that is compiled to JVM bytecode.
 */
public final class ByteCodeRuntime {
    private static CompiledClassLoader classLoader =
            new CompiledClassLoader(); // FIXME: one per runtime

    public static void registerMethod(Ruby runtime, String className, String methodName, int arity) {
        Callback callback = new CompiledReflectionCallback(runtime, className, methodName, arity, classLoader);
        CallbackMethod method = new CallbackMethod(callback, Visibility.PUBLIC);
        runtime.getRubyClass().addMethod(methodName, method);
    }





    public static void addClass(String name, byte[] javaClass) {
        classLoader.loadClass(name, javaClass);
    }

    private static class CompiledClassLoader extends ClassLoader {
        public Class loadClass(String name, byte[] javaClass) {
            Class cl = defineClass(name,
                                   javaClass,
                                   0,
                                   javaClass.length);
            resolveClass(cl);
            return cl;
        }
    }
}
