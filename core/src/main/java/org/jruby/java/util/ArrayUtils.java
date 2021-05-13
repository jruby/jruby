package org.jruby.java.util;

import java.lang.reflect.Array;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.javasupport.Java;
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

    private ArrayUtils() { /* no instances */ }

    public static IRubyObject arefDirect(Ruby runtime, Object array, JavaUtil.JavaConverter javaConverter, int index) {
        try {
            return JavaUtil.convertJavaArrayElementToRuby(runtime, javaConverter, array, index);
        }
        catch (IndexOutOfBoundsException e) { throw mapIndexOutOfBoundsException(runtime, array, index); }
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
        RubyClass proxyClass = (RubyClass) Java.getProxyClass(runtime, array.getClass());
        return new ArrayJavaProxy(runtime, proxyClass, array, converter);
    }

    public static IRubyObject emptyJavaArrayDirect(ThreadContext context, Class componentType) {
        return newProxiedArray(context.runtime, componentType, 0);
    }

    public static IRubyObject javaArraySubarrayDirect(ThreadContext context, Object fromArray, int index, int size) {
        int actualLength = Array.getLength(fromArray);
        if (index >= actualLength) return context.nil;

        if (index + size > actualLength) {
            size = actualLength - index;
        }

        ArrayJavaProxy proxy = ArrayUtils.newProxiedArray(context.runtime, fromArray.getClass().getComponentType(), size);
        Object newArray = proxy.getObject();
        System.arraycopy(fromArray, index, newArray, 0, size);

        return proxy;
    }

    public static IRubyObject concatArraysDirect(ThreadContext context, Object original, IRubyObject additional) {
        final Ruby runtime = context.runtime;
        final int oldLength = Array.getLength(original);
        final int addLength = RubyFixnum.fix2int(Helpers.invoke(context, additional, "length"));

        ArrayJavaProxy proxy = ArrayUtils.newProxiedArray(runtime, original.getClass().getComponentType(), oldLength + addLength);

        System.arraycopy(original, 0, proxy.getObject(), 0, oldLength);

        for (int i = 0; i < addLength; i++) {
            IRubyObject val = Helpers.invoke(context, additional, "[]", runtime.newFixnum(i));
            proxy.setValue(runtime, oldLength + i, val); // [ oldLen + i ] = val
        }

        return proxy;
    }

    public static IRubyObject asetDirect(Ruby runtime, Object array, JavaUtil.JavaConverter javaConverter, int index, IRubyObject value) {
        try {
            javaConverter.set(runtime, array, index, value);
        }
        catch (IndexOutOfBoundsException e) { throw mapIndexOutOfBoundsException(runtime, array, index); }
        catch (ArrayStoreException e) { throw mapArrayStoreException(runtime, array, value.getClass()); }
        catch (IllegalArgumentException e) { throw mapIllegalArgumentException(runtime, array, value.getClass()); }
        return value;
    }

    public static void setWithExceptionHandlingDirect(Ruby runtime, Object array, int index, Object javaValue) {
        try {
            Array.set(array, index, javaValue);
        }
        catch (IndexOutOfBoundsException e) { throw mapIndexOutOfBoundsException(runtime, array, index); }
        catch (ArrayStoreException e) { throw mapArrayStoreException(runtime, array, javaValue.getClass()); }
        catch (IllegalArgumentException e) { throw mapIllegalArgumentException(runtime, array, javaValue.getClass()); }
    }

    private static RaiseException mapIndexOutOfBoundsException(final Ruby runtime, final Object array, int index) {
        return runtime.newIndexError("index out of bounds for java array (" +
                index + " for length " + Array.getLength(array) + ')');
    }

    private static RaiseException mapArrayStoreException(final Ruby runtime, final Object array, final Class<?> type) {
        return runtime.newTypeError("wrong element type " + type.getName() + " (array contains " +
                array.getClass().getComponentType().getName() + ')');
    }

    private static RaiseException mapIllegalArgumentException(final Ruby runtime, final Object array, final Class<?> type) {
        return runtime.newArgumentError("wrong element type " + type.getName() + " (array contains " +
                array.getClass().getComponentType().getName() + ')');
    }

    @Deprecated
    public static void copyDataToJavaArrayDirect(ThreadContext context,
        final RubyArray rubyArray, final Object javaArray) {
        copyDataToJavaArrayDirect(rubyArray, javaArray);
    }

    public static void copyDataToJavaArrayDirect(final RubyArray rubyArray, final Object javaArray) {
        Class targetType = javaArray.getClass().getComponentType();

        // 'premature' optimizations as reflected Array.set is (still) noticeably slower
        if ( ! targetType.isPrimitive() ) {
            copyDataToJavaArrayDirect(targetType, rubyArray, (Object[]) javaArray); return;
        }
        else if ( Integer.TYPE == targetType ) {
            copyDataToJavaArrayDirect(targetType, rubyArray, (int[]) javaArray); return;
        }
        else if ( Long.TYPE == targetType ) {
            copyDataToJavaArrayDirect(targetType, rubyArray, (long[]) javaArray); return;
        }
        else if ( Byte.TYPE == targetType ) {
            copyDataToJavaArrayDirect(targetType, rubyArray, (byte[]) javaArray); return;
        }

        int length = rubyArray.getLength();

        final int javaLength = Array.getLength(javaArray);
        if ( javaLength < length ) length = javaLength;

        for ( int i = 0; i < length; i++ ) {
            Array.set(javaArray, i, rubyArray.eltInternal(i).toJava(targetType));
        }
    }

    //public static void copyDataToJavaArrayDirect(ThreadContext context,
    //    final RubyArray rubyArray, final Object[] javaArray) {
    //    copyDataToJavaArrayDirect(javaArray.getClass().getComponentType(), rubyArray, javaArray)
    //}

    private static void copyDataToJavaArrayDirect(final Class<?> targetType,
        final RubyArray rubyArray, final Object[] javaArray) {
        int length = rubyArray.getLength();

        final int javaLength = javaArray.length;
        if ( javaLength < length ) length = javaLength;

        for ( int i = 0; i < length; i++ ) {
            javaArray[i] = rubyArray.eltInternal(i).toJava(targetType);
        }
    }

    private static void copyDataToJavaArrayDirect(final Class<?> targetType,
        final RubyArray rubyArray, final int[] javaArray) {
        int length = rubyArray.getLength();

        final int javaLength = javaArray.length;
        if ( javaLength < length ) length = javaLength;

        for ( int i = 0; i < length; i++ ) {
            javaArray[i] = (Integer) rubyArray.eltInternal(i).toJava(targetType);
        }
    }

    private static void copyDataToJavaArrayDirect(final Class<?> targetType,
        final RubyArray rubyArray, final long[] javaArray) {
        int length = rubyArray.getLength();

        final int javaLength = javaArray.length;
        if ( javaLength < length ) length = javaLength;

        for ( int i = 0; i < length; i++ ) {
            javaArray[i] = (Long) rubyArray.eltInternal(i).toJava(targetType);
        }
    }

    private static void copyDataToJavaArrayDirect(final Class<?> targetType,
        final RubyArray rubyArray, final byte[] javaArray) {
        int length = rubyArray.getLength();

        final int javaLength = javaArray.length;
        if ( javaLength < length ) length = javaLength;

        for ( int i = 0; i < length; i++ ) {
            javaArray[i] = (Byte) rubyArray.eltInternal(i).toJava(targetType);
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
