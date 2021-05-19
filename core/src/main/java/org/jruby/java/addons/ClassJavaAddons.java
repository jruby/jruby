package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * @author kares
 */
public abstract class ClassJavaAddons {

    // Get the native (or reified) (a la become_java!) class for this Ruby class.
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, final IRubyObject self) {
        Class reifiedClass = RubyClass.nearestReifiedClass((RubyClass) self);
        if ( reifiedClass == null ) return context.nil;
        return asJavaClass(context.runtime, reifiedClass);
    }

    private static JavaProxy asJavaClass(final Ruby runtime, final Class<?> reifiedClass) {
        return (JavaProxy) Java.getInstance(runtime, reifiedClass);
    }

    @JRubyMethod(name = "become_java!", required = 0)
    public static IRubyObject become_java(ThreadContext context, final IRubyObject self) {
        return becomeJava(context, (RubyClass) self, null, true);
    }

    @JRubyMethod(name = "become_java!", required = 1, optional = 1)
    public static IRubyObject become_java(ThreadContext context, final IRubyObject self, final IRubyObject[] args) {
        final RubyClass klass = (RubyClass) self;

        String dumpDir = null; boolean useChildLoader = true;
        if ( args[0] instanceof RubyString ) {
            dumpDir = args[0].asJavaString();
            if ( args.length > 1 ) useChildLoader = args[1].isTrue();
        }
        else {
            useChildLoader = args[0].isTrue();
            // ~ compatibility with previous (.rb) args handling :
            if ( args.length > 1 && args[1] instanceof RubyString ) {
                dumpDir = args[1].asJavaString();
            }
        }

        return becomeJava(context, klass, dumpDir, useChildLoader);
    }

    private static IRubyObject becomeJava(final ThreadContext context, final RubyClass klass,
        final String dumpDir, final boolean useChildLoader) {

        klass.reifyWithAncestors(dumpDir, useChildLoader);

        Class<?> reifiedClass = klass.getReifiedClass();
        if (reifiedClass == null) { // java proxies can't be reified, but they deserve field accessors too
            reifiedClass = JavaProxyClass.getProxyClass(context.runtime, klass).getJavaClass();
        }
        if (reifiedClass == null) {
            throw context.runtime.newTypeError("requested class " + klass.getName() + " was not reifiable");
        }
        generateFieldAccessors(context, klass, reifiedClass);
        return asJavaClass(context.runtime, reifiedClass);
    }

    private static void generateFieldAccessors(ThreadContext context, final RubyClass klass, final Class<?> javaClass) {
        for ( String name : getJavaFieldNames(klass) ) {
            Field field;
            try {
                field = javaClass.getDeclaredField(name);
            }
            catch (NoSuchFieldException e) {
                throw context.runtime.newRuntimeError("no field: '" + name + "' in reified class for " + klass.getName());
            }
            JavaProxy.installField(context, name, field, klass);
        }
    }

    private static Set<String> getJavaFieldNames(final RubyClass klass) {
        return klass.getFieldSignatures().keySet();
    }

}