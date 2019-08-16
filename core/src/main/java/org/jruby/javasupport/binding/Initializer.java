package org.jruby.javasupport.binding;

import com.headius.backport9.modules.Modules;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.StringSupport.startsWith;

/**
* Created by headius on 2/26/15.
*/
public abstract class Initializer {

    static final Logger LOG = LoggerFactory.getLogger(Initializer.class);

    public static final boolean DEBUG_SCALA = false;

    protected final Ruby runtime;
    protected final JavaSupport javaSupport;
    protected final Class javaClass;

    public static final String METHOD_MANGLE = "__method";

    private static final Map<String, String> SCALA_OPERATORS;

    // TODO: other reserved names?
    private static Map<String, AssignedName> newReservedNamesMap(final int size) {
        HashMap<String, AssignedName> RESERVED_NAMES = new HashMap<>(size + 4, 1);
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
        return RESERVED_NAMES;
    }

    protected static final Map<String, AssignedName> STATIC_RESERVED_NAMES;
    static {
        STATIC_RESERVED_NAMES = newReservedNamesMap(1);
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }
    protected static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES;
    static {
        INSTANCE_RESERVED_NAMES = newReservedNamesMap(2);
        // only possible for "getClass" to be an instance method in Java
        INSTANCE_RESERVED_NAMES.put("class", new AssignedName("class", Priority.RESERVED));
        // "initialize" has meaning only for an instance (as opposed to a class)
        INSTANCE_RESERVED_NAMES.put("initialize", new AssignedName("initialize", Priority.RESERVED));
    }

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
        flagAsJavaProxy(proxy); return proxy;
    }

    private static void flagAsJavaProxy(final RubyModule proxy) {
        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);
    }

    protected static String fixScalaNames(final String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }
        return s;
    }

    static {
        HashMap<String, String> scalaOperators = new HashMap<>(24, 1);
        scalaOperators.put("\\$plus", "+");
        scalaOperators.put("\\$minus", "-");
        scalaOperators.put("\\$colon", ":");
        scalaOperators.put("\\$div", "/");
        scalaOperators.put("\\$eq", "=");
        scalaOperators.put("\\$less", "<");
        scalaOperators.put("\\$greater", ">");
        scalaOperators.put("\\$bslash", "\\\\");
        scalaOperators.put("\\$hash", "#");
        scalaOperators.put("\\$times", "*");
        scalaOperators.put("\\$bang", "!");
        scalaOperators.put("\\$at", "@");
        scalaOperators.put("\\$percent", "%");
        scalaOperators.put("\\$up", "^");
        scalaOperators.put("\\$amp", "&");
        scalaOperators.put("\\$tilde", "~");
        scalaOperators.put("\\$qmark", "?");
        scalaOperators.put("\\$bar", "|");
        SCALA_OPERATORS = Collections.unmodifiableMap(scalaOperators);
    }

    static final ClassValue<Boolean> IS_SCALA = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            boolean scalaAnno = false;
            for (Annotation anno : type.getDeclaredAnnotations()) {
                Package pkg = anno.annotationType().getPackage();
                if (pkg != null && pkg.getName() != null && pkg.getName().startsWith("scala.")) {
                    scalaAnno = true;
                    break;
                }
            }
            return scalaAnno;
        }
    };

    private static void setJavaClassFor(final Class<?> javaClass, final RubyModule proxy) {
        proxy.setInstanceVariable("@java_class", proxy.getRuntime().getJavaSupport().getJavaClassFromCache(javaClass));
        proxy.dataWrapStruct(javaClass);
    }

    public abstract RubyModule initialize(RubyModule proxy);

    public static final ClassValue<Method[]> DECLARED_METHODS = new ClassValue<Method[]>() {
        @Override
        public Method[] computeValue(Class cls) {
            return cls.getDeclaredMethods();
        }
    };

}
