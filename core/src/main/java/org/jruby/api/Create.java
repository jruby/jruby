package org.jruby.api;

import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.List;

import static org.jruby.RubyArray.checkLength;

public class Create {
    /**
     * Create a new array with the default allocation size
     *
     * @param context the current thread context
     * @return the new array
     */
    // mri: rb_ary_new2
    public static RubyArray<?> newArray(ThreadContext context) {
        return RubyArray.newArray(context.runtime);
    }

    /**
     * Create a new array with the allocation size specified by the provided length filled with nil.
     * This method will additionally make sure the long value will fit into an int size.
     *
     * @param context the current thread context
     * @param length the size to allocate for the array
     * @return the new array
     */
    // mri: rb_ary_new2
    public static RubyArray<?> newArray(ThreadContext context, long length) {
        checkLength(context, length);
        // FIXME: This should be newBlankArray but things go very wrong in a tough to figure out where sort of way.
        return RubyArray.newArray(context.runtime, (int)length);
    }

    /**
     * Create a new array with a single element.
     *
     * @param context the current thread context
     * @param one the lone element in the array
     * @return the new array
     */
    public static RubyArray<?> newArray(ThreadContext context, IRubyObject one) {
        return RubyArray.newArray(context.runtime, one);
    }

    /**
     * Create a new array with two elements.
     *
     * @param context the current thread context
     * @param one the lone element in the array
     * @param two the lone element in the array
     * @return the new array
     */
    // mri: rb_assoc_new
    public static RubyArray<?> newArray(ThreadContext context, IRubyObject one, IRubyObject two) {
        return RubyArray.newArray(context.runtime, one, two);
    }

    /**
     * Create a new array with many elements.
     *
     * @param context the current thread context
     * @param elements the elements of the array
     * @return the new array
     */
    public static RubyArray<?> newArray(ThreadContext context, IRubyObject... elements) {
        return RubyArray.newArray(context.runtime, elements);
    }

    /**
     * Create a new array with many elements from a java.util.List.
     *
     * @param context the current thread context
     * @param list the elements of the array
     * @return the new array
     */
    public static RubyArray<?> newArray(ThreadContext context, List<IRubyObject> list) {
        return RubyArray.newArray(context.runtime, list);
    }

    /**
     * Create a new array with many elements but do not copy the incoming array of elements..
     *
     * @param context the current thread context
     * @param elements the elements of the array
     * @return the new array
     */
    public static RubyArray<?> newArrayNoCopy(ThreadContext context, IRubyObject... elements) {
        return RubyArray.newArrayNoCopy(context.runtime, elements);
    }


    /**
     * Create a new array which is intended to be empty for its entire lifetime.
     * It can still grow but the intention is you think it won't grow (or cannot).
     *
     * @param context the current thread context
     * @return the new array
     */
    // mri: rb_ary_new2
    public static RubyArray<?> newEmptyArray(ThreadContext context) {
        return RubyArray.newEmptyArray(context.runtime);
    }

    /**
     * Creates a new RubyString from the provided bytelist.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, ByteList bytes) {
        return RubyString.newString(context.runtime, bytes);
    }

    /**
     * Creates a new RubyString from the provided bytelist but use the supplied
     * encoding if possible.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, ByteList bytes, Encoding encoding) {
        return RubyString.newString(context.runtime, bytes, encoding);
    }

    /**
     * Creates a new RubyString from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, String string, Encoding encoding) {
        return RubyString.newString(context.runtime, string, encoding);
    }

    /**
     * Creates a new RubyString from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, String string) {
        return RubyString.newString(context.runtime, string);
    }

}
