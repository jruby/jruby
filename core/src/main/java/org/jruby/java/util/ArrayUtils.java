package org.jruby.java.util;

import java.lang.reflect.Array;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.javasupport.JavaArray;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A collection of utilities for manipulating Java arrays.
 */
public class ArrayUtils {
    public static IRubyObject arefDirect(Ruby runtime, Object array, JavaUtil.JavaConverter javaConverter, int intIndex) {
        try {
            return JavaUtil.convertJavaArrayElementToRuby(runtime, javaConverter, array, intIndex);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError(
                    "index out of bounds for java array (" + intIndex +
                            " for length " + Array.getLength(array) + ")");
        }
    }

    public static IRubyObject concatArraysDirect(ThreadContext context, Object original, Object additional) {
        int oldLength = Array.getLength(original);
        int addLength = Array.getLength(additional);

        ArrayJavaProxy proxy = newProxiedArray(context.runtime, original.getClass().getComponentType(), oldLength + addLength);
        Object newArray = proxy.getObject();

        System.arraycopy(original, 0, newArray, 0, oldLength);
        System.arraycopy(additional, 0, newArray, oldLength, addLength);

        return proxy;
    }

    public static ArrayJavaProxy newProxiedArray(Ruby runtime, Class<?> componentType, int size) {
        return newProxiedArray(runtime, componentType, JavaUtil.getJavaConverter(componentType), size);
    }

    public static ArrayJavaProxy newProxiedArray(Ruby runtime, Class<?> componentType, JavaUtil.JavaConverter converter, int size) {
        final Object array = Array.newInstance(componentType, size);
        RubyClass proxyClass = JavaClass.get(runtime, array.getClass()).getProxyClass();
        return new ArrayJavaProxy(runtime, proxyClass, array, converter);
    }

    public static IRubyObject emptyJavaArrayDirect(ThreadContext context, Class componentType) {
        Ruby runtime = context.runtime;
        return newProxiedArray(runtime, componentType, 0);
    }

    public static IRubyObject javaArraySubarrayDirect(ThreadContext context, Object fromArray, int index, int size) {
        int actualLength = Array.getLength(fromArray);
        if (index >= actualLength) {
            return context.runtime.getNil();
        } else {
            if (index + size > actualLength) {
                size = actualLength - index;
            }

            ArrayJavaProxy proxy = ArrayUtils.newProxiedArray(context.runtime, fromArray.getClass().getComponentType(), size);
            Object newArray = proxy.getObject();
            System.arraycopy(fromArray, index, newArray, 0, size);

            return proxy;
        }
    }

    public static IRubyObject concatArraysDirect(ThreadContext context, Object original, IRubyObject additional) {
        Ruby runtime = context.runtime;
        int oldLength = Array.getLength(original);
        int addLength = (int)((RubyFixnum) Helpers.invoke(context, additional, "length")).getLongValue();

        ArrayJavaProxy proxy = ArrayUtils.newProxiedArray(runtime, original.getClass().getComponentType(), oldLength + addLength);
        Object newArray = proxy.getObject();

        System.arraycopy(original, 0, newArray, 0, oldLength);

        for (int i = 0; i < addLength; i++) {
            Helpers.invoke(context, proxy, "[]=", runtime.newFixnum(oldLength + i),
                    Helpers.invoke(context, additional, "[]", runtime.newFixnum(i)));
        }

        return proxy;
    }

    public static IRubyObject asetDirect(Ruby runtime, Object array, JavaUtil.JavaConverter javaConverter, int intIndex, IRubyObject value) {
        try {
            javaConverter.set(runtime, array, intIndex, value);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError(
                    "index out of bounds for java array (" + intIndex +
                            " for length " + Array.getLength(array) + ")");
        } catch (ArrayStoreException e) {
            throw runtime.newTypeError(
                    "wrong element type " + value.getClass() + "(array contains " +
                            array.getClass().getComponentType().getName() + ")");
        } catch (IllegalArgumentException iae) {
            throw runtime.newArgumentError(
                    "wrong element type " + value.getClass() + "(array contains " +
                            array.getClass().getComponentType().getName() + ")");
        }
        return value;
    }

    public static void setWithExceptionHandlingDirect(Ruby runtime, Object ary, int intIndex, Object javaObject) {
        try {
            Array.set(ary, intIndex, javaObject);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError(
                                    "index out of bounds for java array (" + intIndex +
                                    " for length " + Array.getLength(ary) + ")");
        } catch (ArrayStoreException e) {
            throw runtime.newTypeError(
                                    "wrong element type " + javaObject.getClass() + "(array contains " +
                                    ary.getClass().getComponentType().getName() + ")");
        } catch (IllegalArgumentException iae) {
            throw runtime.newArgumentError(
                                    "wrong element type " + javaObject.getClass() + "(array contains " +
                                    ary.getClass().getComponentType().getName() + ")");
        }
    }

    public static void copyDataToJavaArrayDirect(
            ThreadContext context, RubyArray rubyArray, Object javaArray) {
        int javaLength = Array.getLength(javaArray);
        Class targetType = javaArray.getClass().getComponentType();

        int rubyLength = rubyArray.getLength();

        int i = 0;
        for (; i < rubyLength && i < javaLength; i++) {
            Array.set(javaArray, i, rubyArray.entry(i).toJava(targetType));
        }
    }

    public static void copyDataToJavaArray(
            ThreadContext context, RubyArray rubyArray, int src, JavaArray javaArray, int dest, int length) {
        Class targetType = javaArray.getComponentType();

        int destLength = (int)javaArray.length().getLongValue();
        int srcLength = rubyArray.getLength();

        for (int i = 0; src + i < srcLength && dest + i < destLength && i < length; i++) {
            javaArray.setWithExceptionHandling(dest + i, rubyArray.entry(src + i).toJava(targetType));
        }
    }
}
