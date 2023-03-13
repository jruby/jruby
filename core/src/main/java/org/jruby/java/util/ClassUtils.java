package org.jruby.java.util;

import org.jruby.RubyModule;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility functions for working with Java classes and their Ruby proxies.
 */
public class ClassUtils {
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    public static boolean assignable(Class<?> target, Class<?> from) {
        if ( target.isPrimitive() ) target = CodegenUtils.getBoxType(target);
        else if ( from == Void.TYPE || target.isAssignableFrom(from) ) {
            return true;
        }
        if ( from.isPrimitive() ) from = CodegenUtils.getBoxType(from);

        if ( target.isAssignableFrom(from) ) return true;

        if ( Number.class.isAssignableFrom(target) ) {
            if ( Number.class.isAssignableFrom(from) ) {
                return true;
            }
            if ( from == Character.class ) {
                return true;
            }
        }
        else if ( target == Character.class ) {
            if ( Number.class.isAssignableFrom(from) ) {
                return true;
            }
        }
        return false;
    }

    public static Class<?>[] getArgumentTypes(final ThreadContext context, final IRubyObject[] args, final int offset) {
        final int length = args.length; // offset == 0 || 1
        if ( length == offset ) return EMPTY_CLASS_ARRAY;
        final Class<?>[] argumentTypes = new Class[length - offset];
        for ( int i = offset; i < length; i++ ) {
            argumentTypes[ i - offset ] = Java.resolveClassType(context, args[i]);
        }
        return argumentTypes;
    }

    public static AccessibleObject getMatchingCallable(Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        if ( methodName.length() == 6 && "<init>".equals(methodName) ) {
            return getMatchingConstructor(javaClass, argumentTypes);
        }
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods
        return getMatchingDeclaredMethod(javaClass, methodName, argumentTypes);
    }

    public static Constructor getMatchingConstructor(final Class<?> javaClass, final Class<?>[] argumentTypes) {
        try {
            return javaClass.getConstructor(argumentTypes);
        }
        catch (NoSuchMethodException e) {
            final int argLength = argumentTypes.length;
            // Java reflection does not allow retrieving constructors like methods
            Search: for (Constructor<?> ctor : javaClass.getConstructors()) {
                final Class<?>[] ctorTypes = ctor.getParameterTypes();
                final int ctorLength = ctorTypes.length;

                if ( ctorLength != argLength ) continue Search;
                // for zero args case we can stop searching
                if ( ctorLength == 0 && argLength == 0 ) {
                    return ctor;
                }

                boolean found = true;
                TypeScan: for ( int i = 0; i < argLength; i++ ) {
                    //if ( i >= ctorLength ) found = false;
                    if ( ctorTypes[i].isAssignableFrom(argumentTypes[i]) ) {
                        found = true; // continue TypeScan;
                    } else {
                        continue Search; // not-found
                    }
                }

                // if we get here, we found a matching method, use it
                // TODO: choose narrowest method by continuing to search
                if ( found ) return ctor;
            }
        }
        return null; // no matching ctor found
    }

    public static Method getMatchingDeclaredMethod(Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods.  also, the getDeclared calls may throw SecurityException if
        // we're running under a restrictive security policy.
        try {
            return javaClass.getDeclaredMethod(methodName, argumentTypes);
        }
        catch (NoSuchMethodException e) {
            // search through all declared methods to find a closest match
            MethodSearch: for ( Method method : javaClass.getDeclaredMethods() ) {
                if ( method.getName().equals(methodName) ) {
                    Class<?>[] targetTypes = method.getParameterTypes();

                    // for zero args case we can stop searching
                    if (targetTypes.length == 0 && argumentTypes.length == 0) {
                        return method;
                    }

                    TypeScan: for (int i = 0; i < argumentTypes.length; i++) {
                        if (i >= targetTypes.length) continue MethodSearch;

                        if (targetTypes[i].isAssignableFrom(argumentTypes[i])) {
                            continue TypeScan;
                        } else {
                            continue MethodSearch;
                        }
                    }

                    // if we get here, we found a matching method, use it
                    // TODO: choose narrowest method by continuing to search
                    return method;
                }
            }
        }
        // no matching method found
        return null;
    }

    public static Field[] getDeclaredFields(final Class<?> clazz) {
        try {
            return clazz.getDeclaredFields();
        }
        catch (SecurityException e) {
            return getFields(clazz);
        }
    }

    public static Field[] getFields(final Class<?> clazz) {
        try {
            return clazz.getFields();
        }
        catch (SecurityException e) { return new Field[0]; }
    }

    public static Class<?>[] getDeclaredClasses(Class<?> clazz) {
        try {
            return clazz.getDeclaredClasses();
        }
        catch (SecurityException e) {
            return new Class<?>[0];
        }
        catch (NoClassDefFoundError cnfe) {
            // This is a Scala-specific hack, since Scala uses peculiar
            // naming conventions and class attributes that confuse Java's
            // reflection logic and cause a blow up in getDeclaredClasses.
            // See http://lampsvn.epfl.ch/trac/scala/ticket/2749
            return new Class<?>[0];
        }
    }

    public static String getSimpleName(Class<?> clazz) {
 		if (clazz.isArray()) {
 			return getSimpleName(clazz.getComponentType()) + "[]";
 		}

 		String className = clazz.getName();
 		int len = className.length();
        int i = className.lastIndexOf('$');
 		if (i != -1) {
            do {
 				i++;
 			} while (i < len && Character.isDigit(className.charAt(i)));
 			return className.substring(i);
 		}

 		return className.substring(className.lastIndexOf('.') + 1);
 	}

    public static Constructor[] getConstructors(final Class<?> clazz) {
        try {
            return clazz.getConstructors();
        }
        catch (SecurityException e) { return new Constructor[0]; }
    }

    public static boolean isJavaClassProxyType(ThreadContext context, RubyModule c) {
        return JavaUtil.getJavaClass(c, null) != null;
    }
}
