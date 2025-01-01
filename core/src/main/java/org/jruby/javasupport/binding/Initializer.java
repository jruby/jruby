package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaSupport;
import org.jruby.runtime.ThreadContext;
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

    @Deprecated(since = "10.0")
    public static RubyModule setupProxyClass(Ruby runtime, final Class<?> javaClass, RubyClass proxy) {
        return setupProxyClass(runtime.getCurrentContext(), javaClass, proxy);
    }

    public static RubyModule setupProxyClass(ThreadContext context, final Class<?> javaClass, RubyClass proxy) {
        setJavaClassFor(context, javaClass, proxy);

        proxy.reifiedClass((Class) javaClass);

        if ( javaClass.isArray() ) {
            flagAsJavaProxy(context, proxy); return proxy;
        }

        if ( javaClass.isPrimitive() ) {
            final RubyClass proxySingleton = proxy.singletonClass(context);
            proxySingleton.undefineMethod("new"); // remove ConcreteJavaProxy class method 'new'
            if ( javaClass == Void.TYPE ) {
                // special treatment ... while Java::int[4] is OK Java::void[2] is NOT!
                proxySingleton.undefineMethod("[]"); // from JavaProxy
                proxySingleton.undefineMethod("new_array"); // from JavaProxy
            }
            flagAsJavaProxy(context, proxy); return proxy;
        }

        proxy = new ClassInitializer(context.runtime, javaClass).initialize(context, proxy);
        flagAsJavaProxy(context, proxy); return proxy;
    }

    @Deprecated(since = "10.0")
    public static RubyModule setupProxyModule(Ruby runtime, final Class<?> javaClass, RubyModule proxy) {
        return setupProxyModule(runtime.getCurrentContext(), javaClass, proxy);
    }

    public static RubyModule setupProxyModule(ThreadContext context, final Class<?> javaClass, RubyModule proxy) {
        setJavaClassFor(context, javaClass, proxy);

        assert javaClass.isInterface();

        proxy = new InterfaceInitializer(context.runtime, javaClass).initialize(proxy);
        flagAsJavaProxy(context, proxy);
        return proxy;
    }

    private static void flagAsJavaProxy(ThreadContext context, final RubyModule proxy) {
        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.singletonClass(context).setJavaProxy(true);
    }

    private static void setJavaClassFor(ThreadContext context, final Class<?> javaClass, final RubyModule proxy) {
        JavaProxy.setJavaClass(context, proxy, javaClass);
        proxy.dataWrapStruct(javaClass);
    }

    public RubyModule initialize(RubyModule proxy) {
        return initialize(proxy.getCurrentContext(), proxy);
    }


    public abstract RubyModule initialize(ThreadContext context, RubyModule proxy);

    @Deprecated
    public static final ClassValue<Method[]> DECLARED_METHODS = MethodGatherer.DECLARED_METHODS;

}
