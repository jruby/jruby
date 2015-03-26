package org.jruby.java.addons;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArrayJavaAddons {

    @JRubyMethod(name = "copy_data")
    public static IRubyObject copy_data(final ThreadContext context,
        final IRubyObject fromRuby, final IRubyObject toJava, final IRubyObject fillValue) {
        JavaArray javaArray = (JavaArray) toJava.dataGetStruct();
        final int javaLength = javaArray.getLength();
        final Class<?> targetType = javaArray.getComponentType();

        Object fillJavaObject = null;
        if ( ! fillValue.isNil() ) fillJavaObject = fillValue.toJava(targetType);

        RubyArray rubyArray = null;
        if (fromRuby instanceof RubyArray) {
            rubyArray = (RubyArray) fromRuby;
        } else {
            fillJavaObject = fromRuby.toJava(targetType);
        }

        int i = 0;
        if ( rubyArray != null ) {
            final int rubyLength = rubyArray.getLength();
            for (; i < rubyLength && i < javaLength; i++) {
                javaArray.setWithExceptionHandling(i, rubyArray.eltInternal(i).toJava(targetType));
            }
        }

        if ( i < javaLength && fillJavaObject != null ) {
            javaArray.fillWithExceptionHandling(i, javaLength, fillJavaObject);
        }

        return toJava;
    }

    @JRubyMethod(name = { "copy_data", "copy_data_simple" })
    public static IRubyObject copy_data(final ThreadContext context,
        IRubyObject fromRuby, IRubyObject toJava) {
        JavaArray javaArray = (JavaArray) toJava.dataGetStruct();
        RubyArray rubyArray = (RubyArray) fromRuby;

        copyDataToJavaArray(rubyArray, javaArray, 0);

        return toJava;
    }

    @Deprecated // not used
    public static void copyDataToJavaArray(final ThreadContext context,
        final RubyArray rubyArray, final JavaArray javaArray) {
        copyDataToJavaArray(rubyArray, javaArray, 0);
    }

    private static void copyDataToJavaArray(
        final RubyArray rubyArray, final JavaArray javaArray, int offset) {
        int length = javaArray.getLength();
        if ( length > rubyArray.getLength() ) length = rubyArray.getLength();

        final Class<?> targetType = javaArray.getComponentType();
        for ( int i = offset; i < length; i++ ) {
            javaArray.setWithExceptionHandling(i, rubyArray.eltInternal(i).toJava(targetType));
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
}
