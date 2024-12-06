package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.javasupport.Java;

/**
* Created by headius on 2/26/15.
*/
final class ClassInitializer extends Initializer {

    ClassInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyClass initialize(final RubyModule proxy) {
        final RubyClass proxyClass = (RubyClass) proxy;

        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);

        // set parent to either package module or outer class
        final RubyModule parent;
        final Class<?> enclosingClass = javaClass.getEnclosingClass();
        if ( enclosingClass != null ) {
            parent = Java.getProxyClass(runtime, enclosingClass);
        } else {
            parent = Java.getJavaPackageModule(runtime, javaClass.getPackage());
        }
        proxy.setParent(parent);

        // set the Java class name and package
        proxy.baseName(javaClass.isMemberClass() ?
                javaClass.getSimpleName() :
                inferBaseNameFromJavaName(enclosingClass)); // Anonymous of Local class
        proxyClass.getName(); // trigger calculateName()

        final MethodGatherer state = new MethodGatherer(runtime, javaClass.getSuperclass());

        state.initialize(javaClass, proxy);

        return proxyClass;
    }

    private String inferBaseNameFromJavaName(Class<?> enclosingClass) {
        String baseName;
        baseName = javaClass.getSimpleName(); // returns "" for anonymous
        if ( enclosingClass != null ) {
            // instead of an empty name anonymous classes will have a "conforming"
            // although not valid (by Ruby semantics) RubyClass name e.g. :
            // 'Java::JavaUtilConcurrent::TimeUnit::1' for $1 anonymous enum class
            // NOTE: if this turns out suitable shall do the same for method etc.
            final String className = javaClass.getName();
            final int length = className.length();
            final int offset = enclosingClass.getName().length();
            if ( length > offset && className.charAt(offset) != '$' ) {
                baseName = className.substring( offset );
            } else if ( length > offset + 1 ) { // skip '$'
                baseName = className.substring( offset + 1 );
            }
        }
        return baseName;
    }

}
