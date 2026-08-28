package org.jruby.javasupport.ext;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.util.cli.Options;

import java.util.function.Consumer;

/**
 * Lazy Java class extensions initialization.
 *
 * <p>Note: Internal API</p>
 * @author kares
 */
public class JavaExtensions {

    private static final boolean LAZY = Options.JI_LOAD_LAZY.load();

    private JavaExtensions() { /* hidden */ }

    static void put(final Ruby runtime, Class javaClass, Consumer<RubyModule> proxyClass) {
        if (!LAZY) {
            proxyClass.accept( org.jruby.javasupport.Java.getProxyClass(runtime.getCurrentContext(), javaClass) );
            return;
        }
        Object previous = runtime.getJavaExtensionDefinitions().put(javaClass, proxyClass);
        assert previous == null;
    }

    public static void define(final Ruby runtime, final Class javaClass, final RubyModule proxyClass) {
        runtime.getJavaExtensionDefinitions().getOrDefault(javaClass, NOOP).accept(proxyClass);
    }

    private static final Consumer<RubyModule> NOOP = (noop) -> { /* no extensions */ };

}
