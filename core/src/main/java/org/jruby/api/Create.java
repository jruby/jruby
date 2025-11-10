package org.jruby.api;

import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import static org.jruby.RubyArray.checkLength;
import static org.jruby.api.Access.stringClass;

public class Create {
    /**
     * Create a new array with the default allocation size
     *
     * @param context the current thread context
     * @return the new array
     */
    // mri: rb_ary_new
    public static RubyArray<?> newArray(ThreadContext context) {
        return RubyArray.newArray(context);
    }

    /**
     * Create an empty array with a specific allocated size.  This should be used to
     * make an array where you think you know how big the array will be and you plan on
     * adding data after its construction.
     *
     * It is ok if you add more than this size (it will resize) or less than this size
     * (it will waste a little more space).  The goal is to size in a way where we will not have
     * to arraycopy data when the Array grows.
     *
     * @param context the current thread context
     * @param length to allocate
     * @return the new array
     */
    // mri: rb_ary_new2
    public static RubyArray<?> allocArray(ThreadContext context, int length) {
        // note: this cannot be newBlankArray because packed arrays only exist fully populated.
        return RubyArray.newArray(context, length);
    }

    public static RubyArrayNative<?> allocNativeArray(ThreadContext context, int length) {
        // note: this cannot be newBlankArray because packed arrays only exist fully populated.
        return RubyArrayNative.newArray(context, length);
    }

    /**
     * Create an empty array with a specific allocated size.  This should be used to
     * make an array where you think you know how big the array will be and you plan on
     * adding data after its construction.
     *
     * It is ok if you add more than this size (it will resize) or less than this size
     * (it will waste a little more space).  The goal is to size in a way where we will not have
     * to arraycopy data when the Array grows.
     *
     * This version differs from the int override in that it verifies your long value can
     * fit into an int.  It will raise if it cannot.
     *
     * @param context the current thread context
     * @param length to allocate
     * @return the new array
     */
    // mri: rb_ary_new2
    public static RubyArray<?> allocArray(ThreadContext context, long length) {
        // note: this cannot be newBlankArray because packed arrays only exist fully populated.
        return allocArray(context, checkLength(context, length));
    }

    public static RubyArrayNative<?> allocNativeArray(ThreadContext context, long length) {
        // note: this cannot be newBlankArray because packed arrays only exist fully populated.
        return allocNativeArray(context, checkLength(context, length));
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
     * Create a new Array by applying the given function to the given elements.
     *
     * The resulting Array will be constructed with a minimum amount of allocation.
     *
     * @param context  the current thread context
     * @param elements the elements to transform
     * @param func the transformation function
     * @return the new array
     */
    public static <T> RubyArray<?> newArrayFrom(ThreadContext context, T[] elements, BiFunction<ThreadContext, T, IRubyObject> func) {
        boolean direct = true;
        IRubyObject elt0, elt1 = null;
        int length = elements.length;
        switch (length) {
            case 0:
                return newEmptyArray(context);
            default:
                direct = false;
            case 2:
                elt1 = func.apply(context, elements[1]);
            case 1:
                elt0 = func.apply(context, elements[0]);
        }

        if (direct) {
            if (elt1 == null) return newArray(context, elt0);
            return newArray(context, elt0, elt1);
        }

        IRubyObject[] ary = new IRubyObject[length];
        ary[0] = elt0;
        ary[1] = elt1;
        for (int i = 2; i < length; i++) {
            ary[i] = func.apply(context, elements[i]);
        }

        return RubyArray.newArrayNoCopy(context.runtime, ary);
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
     * Create a new array with elements from the given Collection.
     *
     * @param context the current thread context
     * @param elements the elements of the array
     * @return the new array
     */
    public static RubyArray<?> newArray(ThreadContext context, Collection<? extends IRubyObject> elements) {
        return RubyArray.newArray(context.runtime, elements);
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
     * Create a new empty Hash instance.
     *
     * The expectation is that it will remain empty, so we minimize allocation.
     *
     * TODO: make this actually avoid allocating buckets by fixing RubyHash support for zero buckets.
     *
     * @param context the current thread context
     * @return a new hash
     */
    // MRI: rb_hash_new
    public static RubyHash newEmptyHash(ThreadContext context) {
        return newSmallHash(context);
    }

    /**
     * Create a new Hash instance.  Use this when you expect very few pairs.
     *
     * TODO: provide a way to allocate a sized small hash
     *
     * @param context the current thread context
     * @return a new hash
     */
    // MRI: rb_hash_new
    public static RubyHash newSmallHash(ThreadContext context) {
        return new RubyHash(context.runtime, 1);
    }

    /**
     * Create a new Hash instance with the given pair, optimized for space.
     *
     * @param context the current thread context
     * @param key the key
     * @param value the value
     * @return a new hash
     */
    // MRI: rb_hash_new
    public static RubyHash newSmallHash(ThreadContext context, IRubyObject key, IRubyObject value) {
        RubyHash hash = new RubyHash(context.runtime, 1);
        hash.fastASetSmall(context.runtime, key, value, true);
        return hash;
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
     * Creates a new RubyString from the provided bytes.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, byte[] bytes) {
        return RubyString.newString(context.runtime, bytes);
    }

    /**
     * Creates a new RubyString from the provided bytes.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @param start start index in source bytes
     * @param length the length from start index in source bytes
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, byte[] bytes, int start, int length) {
        return RubyString.newString(context.runtime, bytes, start, length);
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
     * Creates a new RubyString from the provided bytelist but use the supplied
     * encoding if possible.  This bytelist may be from a shared source.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newSharedString(ThreadContext context, ByteList bytes) {
        return RubyString.newStringShared(context.runtime, bytes);
    }


    /**
     * Creates a new RubyString from the provided bytelist but use the supplied
     * encoding if possible.  This bytelist may be from a shared source.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @param encoding to be used (ignoring encoding of bytelist)
     * @return the new RubyString
     */
    public static RubyString newSharedString(ThreadContext context, ByteList bytes, Encoding encoding) {
        return RubyString.newStringShared(context.runtime, bytes, encoding);
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
    public static RubyStruct newStruct(ThreadContext context, RubyClass structClass, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass);
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
    public static RubyStruct newStruct(ThreadContext context, RubyClass structClass, IRubyObject arg0, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass);
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
    public static RubyStruct newStruct(ThreadContext context, RubyClass structClass, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyStruct struct = new RubyStruct(context, structClass);
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
    public static RubyStruct newStruct(ThreadContext context, RubyClass structClass, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2,
                                       Block block) {
        RubyStruct struct = new RubyStruct(context, structClass);
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
