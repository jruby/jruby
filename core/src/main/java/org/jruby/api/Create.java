package org.jruby.api;

import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.func.TriConsumer;

import java.util.List;
import java.util.function.Consumer;

import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Access.structClass;

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

    // mri: rb_ary_new2

    public static RubyArray<?> newArray(ThreadContext context, int length) {
        // FIXME: This should be newBlankArray but things go very wrong in a tough to figure out where sort of way.
        return RubyArray.newArray(context.runtime, length);
    }

    public static <T extends IRubyObject> RubyArray<?> newArray(ThreadContext context, int length,
                                                                Consumer<RubyArray<T>> populator) {
        RubyArray rawArray = newRawArray(context, length);
        populator.accept(rawArray);
        return rawArray.finishRawArray(context);
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
     * Create a new array with three elements.
     *
     * @param context the current thread context
     * @param elt1 the first element
     * @param elt2 the second element
     * @param elt3 the third element
     * @return the new array
     */
    public static RubyArray<?> newArray(ThreadContext context, IRubyObject elt1, IRubyObject elt2, IRubyObject elt3) {
        return RubyArray.newArray(context.runtime, elt1, elt2, elt3);
    }

    /**
     * Create a new array with many elements from a java.util.List.
     *
     * @param context the current thread context
     * @param list the elements of the array
     * @return a new array
     */
    public static RubyArray<?> newArray(ThreadContext context, List<IRubyObject> list) {
        return RubyArray.newArray(context.runtime, list);
    }

    /**
     * Create a new array with many elements but do not copy the incoming array of elements.
     *
     * @param context the current thread context
     * @param elements the elements of the array
     * @return a new array
     */
    public static RubyArray<?> newArrayNoCopy(ThreadContext context, IRubyObject... elements) {
        return RubyArray.newArrayNoCopy(context.runtime, elements);
    }

    /**
     * Construct an array of the requested size by calling the given consumer, which should add elements to the
     * array. After invoking the consumer, the remaining elements in the array will be filled with nil.
     *
     * @param context the current context
     * @param state a state object for the consumer
     * @param length the requested available size for the array
     * @param populator the consumer that will populate the array
     * @return the finished array
     * @param <State> a state object for the consumer
     * @param <T> the type of object the Array will hold
     */
    public static <State, T extends IRubyObject> RubyArray<?> constructArray(ThreadContext context, State state, int length, TriConsumer<ThreadContext, State, RubyArray<T>> populator) {
        RubyArray rawArray = newRawArray(context, length);
        populator.accept(context, state, rawArray);
        return rawArray.finishRawArray(context);
    }

    /**
     * Construct an array with the specified backing storage length. The array must be filled with non-null values
     * before entering Rubyspace.
     *
     * @param context the current context
     * @param len the length of the array buffer requested
     * @return an array with the given buffer size, entries initialized to null
     */
    public static RubyArray<?> newRawArray(final ThreadContext context, final int len) {
        return RubyArray.newRawArray(context, len);
    }

    public static RubyArray<?> newRawArray(final ThreadContext context, final long len) {
        return RubyArray.newRawArray(context, len);
    }

    /**
     * Create a new Hash instance.
     * @param context the current thread context
     * @return a new hash
     */
    // MRI: rb_hash_new
    public static RubyHash newHash(ThreadContext context) {
        return new RubyHash(context.runtime);
    }

    /**
     * Create a new Hash instance.  Use this when you expect very few pairs.
     * @param context the current thread context
     * @return a new hash
     */
    // MRI: rb_hash_new
    public static RubyHash newSmallHash(ThreadContext context) {
        return new RubyHash(context.runtime, 1);
    }


    /**
     * Create a new array which is intended to be empty for its entire lifetime.
     * It can still grow but the intention is you think it won't grow (or cannot).
     * If you want a default-size array then use {@link Create#newArray(ThreadContext)}.
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

    /**
     * Creates a new frozen RubyString from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubyString newFrozenString(ThreadContext context, String string) {
        // replace with custom subclass that is born frozen
        RubyString rubyString = RubyString.newString(context.runtime, string);
        rubyString.setFrozen(true);
        return rubyString;
    }

    /**
     * Create a new String which is intended to be empty for its entire lifetime.
     * It can still grow but the intention is you think it won't grow (or cannot).
     * If you want a default-size array then use {@link Create#newString(ThreadContext, String)}.
     *
     * @param context the current thread context
     * @return the new string
     */
    public static RubyString newEmptyString(ThreadContext context) {
        return RubyString.newEmptyString(context.runtime);
    }

    /**
     * Create a new String which is intended to be empty for its entire lifetime with
     * a specific encoding.  It can still grow but the intention is you think it won't grow
     * (or cannot).  If you want a default-size array then use {@link Create#newString(ThreadContext, ByteList)}.
     *
     * @param context the current thread context
     * @return the new string
     */
    public static RubyString newEmptyString(ThreadContext context, Encoding encoding) {
        return RubyString.newEmptyString(context.runtime, encoding);
    }

    /**
     * Duplicate the given string and return a String (original subclass of String is not preserved).
     *
     * @param context the current thread context
     * @param string the string to be duplicated
     * @return the new string
     */
    public static RubyString dupString(ThreadContext context, RubyString string) {
        return string.strDup(context.runtime, stringClass(context));
    }

    /**
     * Create a new Rational with the given long values for numerator and denominator.
     *
     * @param context the current thread context
     * @param num the numerator
     * @param den the denominator
     * @return a new Rational
     */
    public static RubyRational newRational(ThreadContext context, long num, long den) {
        return RubyRational.newRational(context.runtime, num, den);
    }

    /**
     * Create a new Struct.
     *
     * @param context the current thread context
     * @param block
     * @return
     */
    public static RubyStruct newStruct(ThreadContext context, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass(context));
        struct.callInit(block);
        return struct;
    }

    /**
     * Create a new Struct.
     *
     * @param context the current thread context
     * @param arg0 name of class or first member of struct
     * @param block
     * @return
     */
    public static RubyStruct newStruct(ThreadContext context, IRubyObject arg0, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass(context));
        struct.callInit(arg0, block);
        return struct;
    }

    /**
     * Create a new Struct.
     *
     * @param context the current thread context
     * @param arg0 name of class or first member of struct
     * @param arg1 a member of struct
     * @param block
     * @return
     */
    public static RubyStruct newStruct(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass(context));
        struct.callInit(arg0, arg1, block);
        return struct;
    }

    /**
     * Create a new Struct.
     *
     * @param context the current thread context
     * @param arg0 name of class or first member of struct
     * @param arg1 a member of struct
     * @param arg2 a member of struct
     * @param block
     * @return
     */
    public static RubyStruct newStruct(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2,
                                       Block block) {
        RubyStruct struct = new RubyStruct(context, structClass(context));
        struct.callInit(arg0, arg1, arg2, block);
        return struct;
    }

    /**
     * Create a new Struct (prefer 0-3 arity versions of this function if you know you arity and it is Struct and
     * not a subclass of Struct).
     *
     * @param context the current thread context
     * @param structClass expects either a reference to Struct or a subclass of Struct (Passwd for example)
     * @param args for thr struct
     * @param block
     * @return
     */
    public static RubyStruct newStruct(ThreadContext context, RubyClass structClass, IRubyObject[] args, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass);
        struct.callInit(args, block);
        return struct;
    }

}
