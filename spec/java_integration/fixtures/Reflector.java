package java_integration.fixtures;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class Reflector {

    public static Object invoke(final Object obj, final Method method) throws Exception {
        if ( method.isVarArgs() ) {
            final Class type = method.getParameterTypes()[0].getComponentType();
            Object array = Array.newInstance(type, 0);
            return method.invoke(obj, array);
        }
        return method.invoke(obj);
    }

    public static Object invoke(final Object obj, final Method method, Object arg) throws Exception {
        if ( method.isVarArgs() && ! arg.getClass().isArray() ) {
            final Class type = method.getParameterTypes()[0].getComponentType();
            Object[] array = (Object[]) Array.newInstance(type, 1);
            array[0] = arg;
            return method.invoke(obj, (Object) array);
        }
        return method.invoke(obj, arg);
    }

    public static Object invoke(final Object obj, final Class<?> klass, final String method) throws Exception {
        Method instanceMethod = klass.getMethod(method, (Class[]) null);
        return instanceMethod.invoke(obj, (Object[]) null);
    }

    public static Object invoke(final Object obj, final String method) throws Exception {
        return invoke(obj, obj.getClass(), method);
    }

    public static Object invokeMatch(final Object obj, final String method, final Object... args) throws Exception {
        final Method[] methods = obj.getClass().getMethods();
        ArrayList<Method> matchedMethods = new ArrayList<Method>();
        for ( Method m : methods ) {
            if ( method.equals(m.getName()) ) {
                if ( m.isVarArgs() ) matchedMethods.add(m);
                else {
                    if (m.getParameterTypes().length == args.length) {
                        matchedMethods.add(m);
                    }
                }
            }
        }
        if ( matchedMethods.isEmpty() ) {
            throw new IllegalArgumentException("no methods of name " + method + " matched in " + obj.getClass());
        }
        if ( matchedMethods.size() > 1 ) {
            throw new IllegalArgumentException("multiple methods matched for name " + method + " and arguments " + Arrays.toString(args));
        }
        return matchedMethods.get(0).invoke(obj, args);
    }

    public static Method resolveMethod(final Object obj, final String method, final Class... types) throws Exception {
        final Method[] methods = obj.getClass().getMethods();
        ArrayList<Method> matchedMethods = new ArrayList<Method>();
        for ( Method m : methods ) {
            if ( method.equals(m.getName()) ) {
                if ( Arrays.equals(m.getParameterTypes(), types) ) {
                    matchedMethods.add(m);
                }
            }
        }
        if ( matchedMethods.isEmpty() ) {
            throw new IllegalArgumentException("no methods of name " + method + " matched in " + obj.getClass());
        }
        if ( matchedMethods.size() > 1 ) {
            throw new IllegalArgumentException("multiple methods matched for name " + method + " and param types " + Arrays.toString(types));
        }
        return matchedMethods.get(0);
    }

    public static Collection<Method> findMethods(final Object obj, final String method) throws Exception {
        final Method[] methods = obj.getClass().getMethods();
        ArrayList<Method> matchedMethods = new ArrayList<Method>();
        for ( Method m : methods ) {
            if ( method.equals(m.getName()) ) {
                matchedMethods.add(m);
            }
        }
        return matchedMethods;
    }

    public static <A extends Annotation> A getDeclaredAnnotation(Class<?> clazz,
        Class<A> annotationType) {
        for (Annotation a : clazz.getDeclaredAnnotations()) {
            if (annotationType.isAssignableFrom(a.annotationType())) {
                return (A) a;
            }
        }
        return null;
    }
}

