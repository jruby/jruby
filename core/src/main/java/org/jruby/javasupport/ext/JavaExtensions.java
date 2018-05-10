package org.jruby.javasupport.ext;

import org.jruby.Ruby;
import org.jruby.RubyModule;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Lazy Java class extensions initialization.
 *
 * @note Internal API
 * @author kares
 */
public class JavaExtensions {

    private static final boolean IMMEDIATE = false;

    private static final JavaExtensions INSTANCE = new JavaExtensions();

    public static JavaExtensions getInstance() { return INSTANCE; }

    static void put(final Ruby runtime, Class javaClass, Consumer<RubyModule> proxyClass) {
        if (IMMEDIATE) {
            proxyClass.accept( org.jruby.javasupport.Java.getProxyClass(runtime, javaClass) );
            return;
        }
        Object previous = INSTANCE.definitions.put(javaClass, proxyClass);
        assert previous == null;
    }

    private JavaExtensions() { /* hidden */ }

    final Map<Class, Consumer<RubyModule>> definitions = new WeakHashMap<>();

    public void define(final Class javaClass, final RubyModule proxyClass) {
        definitions.getOrDefault(javaClass, NOOP).accept(proxyClass);
    }

    private static final Consumer<RubyModule> NOOP = (noop) -> { /* no extensions */ };

}
