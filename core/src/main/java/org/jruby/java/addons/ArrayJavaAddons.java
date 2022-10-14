package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArrayJavaAddons {

    @JRubyMethod(name = "copy_data")
    public static IRubyObject copy_data(final ThreadContext context,
        final IRubyObject fromRuby, final IRubyObject toJava, final IRubyObject fillValue) {

        ArrayJavaProxy javaArray = assertJavaArrayProxy(context, toJava);
        final int javaLength = javaArray.length();
        final Class<?> targetType = javaArray.getComponentType();

        Object fillJavaObject = null;
        if ( ! fillValue.isNil() ) fillJavaObject = fillValue.toJava(targetType);

        RubyArray rubyArray = null;
        if (fromRuby instanceof RubyArray) {
            rubyArray = (RubyArray) fromRuby;
        } else {
            fillJavaObject = fromRuby.toJava(targetType);
        }

        final Ruby runtime = context.runtime;
        final Object array = javaArray.getObject();
        int i = 0;
        if ( rubyArray != null ) {
            final int rubyLength = rubyArray.getLength();
            for (; i < rubyLength && i < javaLength; i++) {
                Object javaObject = rubyArray.eltInternal(i).toJava(targetType);
                ArrayUtils.setWithExceptionHandlingDirect(runtime, array, i, javaObject);
            }
        }

        if ( i < javaLength && fillJavaObject != null ) {
            for (; i < javaLength; i++) {
                ArrayUtils.setWithExceptionHandlingDirect(runtime, array, i, fillJavaObject);
            }
        }

        return toJava;
    }

    @JRubyMethod(name = { "copy_data", "copy_data_simple" })
    public static IRubyObject copy_data(final ThreadContext context,
        IRubyObject fromRuby, IRubyObject toJava) {
        ArrayJavaProxy javaArray = assertJavaArrayProxy(context, toJava);
        RubyArray rubyArray = (RubyArray) fromRuby;

        copyDataToJavaArray(context, rubyArray, javaArray, 0);

        return toJava;
    }

    private static void copyDataToJavaArray(final ThreadContext context,
        final RubyArray rubyArray, final ArrayJavaProxy javaArray, int offset) {
        int length = javaArray.length();
        if ( length > rubyArray.getLength() ) length = rubyArray.getLength();

        final Ruby runtime = context.runtime;
        final Object array = javaArray.getObject();
        final Class<?> targetType = javaArray.getComponentType();
        for ( int i = offset; i < length; i++ ) {
            Object javaObject = rubyArray.eltInternal(i).toJava(targetType);
            ArrayUtils.setWithExceptionHandlingDirect(runtime, array, i, javaObject);
        }
    }

    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject rubyArray) {
        return dimensions(context, rubyArray, context.runtime.newEmptyArray());
    }

    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject rubyArray, IRubyObject dims) {
        final Ruby runtime = context.runtime;
        if ( ! ( rubyArray instanceof RubyArray ) ) {
            return runtime.newEmptyArray();
        }
        assert dims instanceof RubyArray;

        return calcDimensions(runtime, (RubyArray) rubyArray, (RubyArray) dims, 0);
    }

    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject rubyArray, IRubyObject dims, IRubyObject index) {
        final Ruby runtime = context.runtime;
        if ( ! ( rubyArray instanceof RubyArray ) ) {
            return runtime.newEmptyArray();
        }
        assert dims instanceof RubyArray;
        assert index instanceof RubyFixnum;

        final int i = (int) ((RubyFixnum) index).getLongValue();
        return calcDimensions(runtime, (RubyArray) rubyArray, (RubyArray) dims, i);
    }

    private static RubyArray calcDimensions(final Ruby runtime,
        final RubyArray array, final RubyArray dims, final int index) {

        while ( dims.size() <= index ) {
            dims.append( RubyFixnum.zero(runtime) );
        }

        final long dim = ((RubyFixnum) dims.eltInternal(index)).getLongValue();
        if ( array.size() > dim ) {
            dims.eltInternalSet(index, RubyFixnum.newFixnum(runtime, array.size()));
        }

        for ( int i = 0; i < array.size(); i++ ) {
            final IRubyObject element = array.eltInternal(i);
            if ( element instanceof RubyArray ) {
                calcDimensions(runtime, (RubyArray) element, dims, 1);
            }
        }

        return dims;
    }

    private static ArrayJavaProxy assertJavaArrayProxy(final ThreadContext context, final IRubyObject java_array) {
        if (java_array instanceof ArrayJavaProxy) {
            return (ArrayJavaProxy) java_array;
        }
        throw context.runtime.newTypeError("expected a Java array, got " + java_array.inspect());
    }

}
