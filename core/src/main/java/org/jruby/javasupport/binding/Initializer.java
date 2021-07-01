package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaSupport;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jruby.util.StringSupport.startsWith;

/**
* Created by headius on 2/26/15.
*/
public abstract class Initializer {

    static final Logger LOG = LoggerFactory.getLogger(Initializer.class);

    protected final Ruby runtime;
    protected final JavaSupport javaSupport;
    protected final Class javaClass;

    public Initializer(Ruby runtime, Class javaClass) {
        this.runtime = runtime;
        this.javaSupport = runtime.getJavaSupport();
        this.javaClass = javaClass;
    }

    public static RubyModule setupProxyClass(Ruby runtime, final Class<?> javaClass, RubyClass proxy) {
        setJavaClassFor(javaClass, proxy);

        proxy.setReifiedClass((Class) javaClass);

        if ( javaClass.isArray() ) {
            flagAsJavaProxy(proxy); return proxy;
        }

        if ( javaClass.isPrimitive() ) {
            final RubyClass proxySingleton = proxy.getSingletonClass();
            proxySingleton.undefineMethod("new"); // remove ConcreteJavaProxy class method 'new'
            if ( javaClass == Void.TYPE ) {
                // special treatment ... while Java::int[4] is OK Java::void[2] is NOT!
                proxySingleton.undefineMethod("[]"); // from JavaProxy
                proxySingleton.undefineMethod("new_array"); // from JavaProxy
            }
            flagAsJavaProxy(proxy); return proxy;
        }

        proxy = new ClassInitializer(runtime, javaClass).initialize(proxy);
        flagAsJavaProxy(proxy); return proxy;
    }

    public static RubyModule setupProxyModule(Ruby runtime, final Class<?> javaClass, RubyModule proxy) {
        setJavaClassFor(javaClass, proxy);

        assert javaClass.isInterface();

        proxy = new InterfaceInitializer(runtime, javaClass).initialize(proxy);
        flagAsJavaProxy(proxy);
        return proxy;
    }

    private static void flagAsJavaProxy(final RubyModule proxy) {
        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);
    }

    private static void setJavaClassFor(final Class<?> javaClass, final RubyModule proxy) {
        JavaProxy.setJavaClass(proxy, javaClass);
        proxy.dataWrapStruct(javaClass);
    }

    public abstract RubyModule initialize(RubyModule proxy);

    @Deprecated
    public static final ClassValue<Method[]> DECLARED_METHODS = MethodGatherer.DECLARED_METHODS;

}
