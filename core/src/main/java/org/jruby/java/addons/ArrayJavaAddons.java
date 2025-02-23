package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Error.typeError;

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
        return dimensions(context, rubyArray, newEmptyArray(context));
    }

    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject rubyArray, IRubyObject dims) {
        if (!(rubyArray instanceof RubyArray)) return newEmptyArray(context);

        assert dims instanceof RubyArray;

        return calcDimensions(context, (RubyArray<?>) rubyArray, (RubyArray<?>) dims, 0);
    }

    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject rubyArray, IRubyObject dims, IRubyObject index) {
        if (!(rubyArray instanceof RubyArray)) return newEmptyArray(context);

        assert dims instanceof RubyArray;
        assert index instanceof RubyFixnum;

        return calcDimensions(context, (RubyArray<?>) rubyArray, (RubyArray<?>) dims, ((RubyFixnum) index).asInt(context));
    }

    private static RubyArray<?> calcDimensions(ThreadContext context,
        final RubyArray<?> array, final RubyArray dims, final int index) {

        var zero = asFixnum(context, 0);
        while ( dims.size() <= index ) {
            dims.append(context, zero);
        }

        final long dim = ((RubyFixnum) dims.eltInternal(index)).getValue();
        if ( array.size() > dim ) {
            dims.eltInternalSet(index, asFixnum(context, array.size()));
        }

        for ( int i = 0; i < array.size(); i++ ) {
            final IRubyObject element = array.eltInternal(i);
            if ( element instanceof RubyArray ary) calcDimensions(context, ary, dims, 1);
        }

        return dims;
    }

    private static ArrayJavaProxy assertJavaArrayProxy(final ThreadContext context, final IRubyObject java_array) {
        if (!(java_array instanceof ArrayJavaProxy)) throw typeError(context, java_array, "Java array");
        return (ArrayJavaProxy) java_array;
    }

}
