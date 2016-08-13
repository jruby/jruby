package org.jruby.java.addons;

import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author kares
 */
public abstract class ClassJavaAddons {

    // Get the native (or reified) (a la become_java!) class for this Ruby class.
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, final IRubyObject self) {
        RubyClass current = (RubyClass) self;
        do {
            Class<?> reified = current.getReifiedClass();
            if ( reified != null ) {
                // TODO: java_class is used for different things with Java proxy modules/classes
                // returning a JavaClass here would break stuff - simply needs to get through ...
                // return JavaClass.get(context.runtime, reified);
                return Java.getInstance(context.runtime, reified);
            }
            current = current.getSuperClass();
        }
        while ( current != null );
        return context.nil;
    }

}