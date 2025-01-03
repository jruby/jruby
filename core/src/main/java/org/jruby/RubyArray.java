/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
 * Copyright (C) 2006 Daniel Steer <damian.steer@hp.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Stack;
import java.util.stream.Stream;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.util.ArrayUtils;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.JavaSites.ArraySites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.specialized.RubyArrayOneObject;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.specialized.RubyArrayTwoObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.StringArraySet;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyEnumerator.enumWithSize;
import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.fixnumClass;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.randomClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newHash;
import static org.jruby.api.Create.newSmallHash;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.api.Warn.warnDeprecated;
import static org.jruby.api.Warn.warning;
import static org.jruby.runtime.Helpers.*;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.Inspector.*;

/**
 * The implementation of the built-in class Array in Ruby.
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="Array", include = { "Enumerable" },
        overrides = {RubyArrayOneObject.class, RubyArrayTwoObject.class, StringArraySet.class})
public class RubyArray<T extends IRubyObject> extends RubyObject implements List, RandomAccess {
    public static final int DEFAULT_INSPECT_STR_SIZE = 10;

    private static final boolean USE_PACKED_ARRAYS = Options.PACKED_ARRAYS.load();

    public static RubyClass createArrayClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        return defineClass(context, "Array", Object, RubyArray::newEmptyArray).
                reifiedClass(RubyArray.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyArray.class)).
                classIndex(ClassIndex.ARRAY).
                include(context, Enumerable).
                defineMethods(context, RubyArray.class);
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.ARRAY;
    }

    protected final void concurrentModification() {
        throw concurrentModification(getRuntime().getCurrentContext(), null);
    }

    private static RuntimeException concurrentModification(ThreadContext context, Exception cause) {
        RuntimeException ex = context.runtime.newConcurrencyError("Detected invalid array contents due to unsynchronized modifications with concurrent users");
        // NOTE: probably not useful to be on except for debugging :
        // if ( cause != null ) ex.initCause(cause);
        return ex;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject create(IRubyObject klass, IRubyObject[] args, Block block) {
        return create(klass.getRuntime().getCurrentContext(), klass, args, block);
    }

    /** rb_ary_s_create
     *
     */
     @JRubyMethod(name = "[]", rest = true, meta = true)
     public static IRubyObject create(ThreadContext context, IRubyObject klass, IRubyObject[] args, Block block) {
         switch (args.length) {
             case 0: return ((RubyClass) klass).allocate(context);
             case 1: return new RubyArrayOneObject((RubyClass) klass, args[0]);
             case 2: return new RubyArrayTwoObject((RubyClass) klass, args[0], args[1]);
         }

         RubyArray arr = (RubyArray) ((RubyClass) klass).allocate(context);
         arr.values = args.clone();
         arr.realLength = args.length;
         return arr;
     }

    /**
     * Create array with specific allocated size
     * @deprecated Use {@link Create#newArray(ThreadContext, int)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static final RubyArray newArray(final Ruby runtime, final long len) {
        ThreadContext context = runtime.getCurrentContext();
        // FIXME: This should be newBlankArray but things go very wrong in a tough to figure out where sort of way.
        return Create.newArray(context, checkLength(context, len));
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static final RubyArray<?> newArrayLight(final Ruby runtime, final long len) {
        return newArrayLight(runtime, checkLength(runtime.getCurrentContext(), len));
    }

    public static final RubyArray<?> newArray(final Ruby runtime, final int len) {
        return newArray(runtime.getCurrentContext(), len);
    }

    public static final RubyArray<?> newArray(ThreadContext context, final int len) {
        if (len == 0) return newEmptyArray(context.runtime);
        IRubyObject[] values = Helpers.nilledArray(validateBufferLength(context.runtime, len), context.runtime);
        return new RubyArray<>(context.runtime, values, 0, 0);
    }

    public static final RubyArray<?> newArrayLight(final Ruby runtime, final int len) {
        if (len == 0) return newEmptyArray(runtime);
        IRubyObject[] values = Helpers.nilledArray(validateBufferLength(runtime, len), runtime);
        return new RubyArray<>(runtime, runtime.getArray(), values, 0, 0, false);
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
        return new RubyArray(context.runtime, arrayClass(context), IRubyObject.array(len), 0, 0, false);
    }

    public static RubyArray<?> newRawArray(final ThreadContext context, final long len) {
        return newRawArray(context, checkLength(context, len));
    }

    /**
     * Fill the remaining array slots with the given value. Pair with newArrayRaw to reduce the cost of setting up a new array.
     */
    public RubyArray<T> finishRawArray(final ThreadContext context) {
        int realLength = this.realLength;
        IRubyObject[] values = this.values;
        if (realLength != values.length) fillNil(context, values, realLength, values.length);
        return this;
    }

    @Deprecated(since = "10.0")
    public static final RubyArray<?> newArray(final Ruby runtime) {
        return newArray(runtime.getCurrentContext());
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray<?> newArray(ThreadContext context) {
        return newArray(context, ARRAY_DEFAULT_SIZE);
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray<?> newArrayLight(final Ruby runtime) {
        /* Ruby arrays default to holding 16 elements, so we create an
         * ArrayList of the same size if we're not told otherwise
         */
        return newArrayLight(runtime, ARRAY_DEFAULT_SIZE);
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject obj) {
        return USE_PACKED_ARRAYS ? new RubyArrayOneObject(runtime, obj) : new RubyArray<>(runtime, arrayOf(obj));
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject obj) {
        return USE_PACKED_ARRAYS ? new RubyArrayOneObject(runtime, obj) : new RubyArray<>(runtime, arrayOf(obj));
    }

    public static RubyArray<?> newArrayLight(RubyClass arrayClass, IRubyObject obj) {
        return USE_PACKED_ARRAYS ? new RubyArrayOneObject(arrayClass, obj) : new RubyArray<>(arrayClass, arrayOf(obj), false);
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return USE_PACKED_ARRAYS ? new RubyArrayTwoObject(runtime, car, cdr) : new RubyArray<>(runtime, arrayOf(car, cdr));
    }

    public static RubyArray<?> newArrayLight(RubyClass arrayClass, IRubyObject car, IRubyObject cdr) {
        return USE_PACKED_ARRAYS ? new RubyArrayTwoObject(arrayClass, car, cdr) : new RubyArray<>(arrayClass, arrayOf(car, cdr), false);
    }

    public static RubyArray<?> newArrayLight(Ruby runtime, IRubyObject... objs) {
        return new RubyArray<>(runtime, objs, false);
    }

    /** rb_assoc_new
     *
     */
    public static RubyArray<?> newArray(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return USE_PACKED_ARRAYS ? new RubyArrayTwoObject(runtime, car, cdr) : new RubyArray<>(runtime, arrayOf(car, cdr));
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third) {
        return new RubyArray<>(runtime, arrayOf(first, second, third));
    }

    public static RubyArray<?> newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third, IRubyObject fourth) {
        return new RubyArray<>(runtime, arrayOf(first, second, third, fourth));
    }

    public static RubyArray<?> newEmptyArray(Ruby runtime) {
        return new RubyArray<>(runtime, NULL_ARRAY);
    }

    public static RubyArray<?> newEmptyArray(Ruby runtime, RubyClass klass) {
        return new RubyArray<>(runtime, klass, NULL_ARRAY);
    }

    /** rb_ary_new4, rb_ary_new3
     *
     */
    public static RubyArray<?> newArray(Ruby runtime, IRubyObject[] args) {
        final int size = args.length;
        if (size == 0) {
            return newEmptyArray(runtime);
        }
        return isPackedArray(size) ? packedArray(runtime, args) : new RubyArray<>(runtime, args.clone());
    }

    public static RubyArray<?> newArray(Ruby runtime, Collection<? extends IRubyObject> collection) {
        if (collection.isEmpty()) {
            return newEmptyArray(runtime);
        }
        final IRubyObject[] arr = collection.toArray(IRubyObject.NULL_ARRAY);
        return isPackedArray(collection) ? packedArray(runtime, arr) : new RubyArray<>(runtime, arr);
    }

    public static RubyArray<?> newArray(Ruby runtime, List<? extends IRubyObject> list) {
        if (list.isEmpty()) {
            return newEmptyArray(runtime);
        }
        return isPackedArray(list) ? packedArray(runtime, list) : new RubyArray<>(runtime, list.toArray(IRubyObject.NULL_ARRAY));
    }

    public static RubyArray<?> newSharedArray(RubyClass arrayClass, IRubyObject[] shared) {
        var sharedArray = new RubyArray<>(arrayClass, shared, true);

        sharedArray.isShared = true;

        return sharedArray;
    }

    private static RubyArray<?> packedArray(final Ruby runtime, final IRubyObject[] args) {
        return args.length == 1 ?
                new RubyArrayOneObject(runtime, args[0]) :
                new RubyArrayTwoObject(runtime, args[0], args[1]);
    }

    private static RubyArray<?> packedArray(final Ruby runtime, final List<? extends IRubyObject> args) {
        return args.size() == 1 ?
                new RubyArrayOneObject(runtime, args.get(0)) :        
                new RubyArrayTwoObject(runtime, args.get(0), args.get(1));

    }

    private static boolean isPackedArray(final int size) {
        return USE_PACKED_ARRAYS && size <= 2;
    }

    private static boolean isPackedArray(final Collection<? extends IRubyObject> collection) {
        return USE_PACKED_ARRAYS && collection.size() <= 2;
    }

    /**
     * @see RubyArray#newArrayMayCopy(Ruby, IRubyObject[], int, int)
     */
    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject... args) {
        return newArrayMayCopy(runtime, args, 0, args.length);
    }

    /**
     * @see RubyArray#newArrayMayCopy(Ruby, IRubyObject[], int, int)
     */
    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject[] args, int start) {
        return newArrayMayCopy(runtime, args, start, args.length - start);
    }

    /**
     * Construct a new RubyArray given the specified range of elements in the source array. The elements
     * <i>may</i> be copied into a new backing store, and therefore you should not expect future changes to the
     * source array to be reflected. Conversely, you should not modify the array after passing it, since
     * the contents <i>may not</i> be copied.
     *
     * @param runtime the runtime
     * @param args the args
     * @param start start index
     * @param length number of elements
     * @return an array referencing the given elements
     */
    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject[] args, int start, int length) {
        if (length == 0) return newEmptyArray(runtime);

        if (USE_PACKED_ARRAYS) {
            if (length == 1) return new RubyArrayOneObject(runtime, args[start]);
            if (length == 2) return new RubyArrayTwoObject(runtime, args[start], args[start + 1]);
        }

        return newArrayNoCopy(runtime, args, start, length);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject... args) {
        return new RubyArray(runtime, args);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin) {
        assert begin >= 0 : "begin must be >= 0";
        assert begin <= args.length : "begin must be <= length";

        return new RubyArray(runtime, args, begin, args.length - begin);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin, int length) {
        assert begin >= 0 : "begin must be >= 0";
        assert length >= 0 : "length must be >= 0";

        return new RubyArray(runtime, args, begin, length);
    }

    public static RubyArray newArrayNoCopyLight(Ruby runtime, IRubyObject[] args) {
        return new RubyArray(runtime, args, false);
    }

    public static final int ARRAY_DEFAULT_SIZE = 16;
    private static final int SMALL_ARRAY_LEN = 16;

    private static final int TMPLOCK_ARR_F = 1 << 9;
    private static final int TMPLOCK_OR_FROZEN_ARR_F = TMPLOCK_ARR_F | FROZEN_F;

    private volatile boolean isShared = false;

    protected IRubyObject[] values;

    protected int begin = 0;
    protected int realLength = 0;

    /*
     * plain internal array assignment
     */
    private RubyArray(Ruby runtime, IRubyObject[] vals) {
        super(runtime, runtime.getArray());
        this.values = vals;
        this.realLength = vals.length;
    }

    /*
     * plain internal array assignment
     */
    private RubyArray(Ruby runtime, IRubyObject[] vals, boolean objectSpace) {
        super(runtime, runtime.getArray(), objectSpace);
        this.values = vals;
        this.realLength = vals.length;
    }

    /*
     * plain internal array assignment
     */
    public RubyArray(Ruby runtime, IRubyObject[] vals, int begin, int length) {
        super(runtime, runtime.getArray());
        this.values = vals;
        this.begin = begin;
        this.realLength = length;
    }

    private RubyArray(Ruby runtime, RubyClass metaClass, IRubyObject[] vals, int begin, int length, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        this.values = vals;
        this.begin = begin;
        this.realLength = length;
    }

    public RubyArray(Ruby runtime, int length) {
        super(runtime, runtime.getArray());
        this.values = IRubyObject.array(validateBufferLength(runtime, length));
    }

    /* NEWOBJ and OBJSETUP equivalent
     * fastest one, for shared arrays, optional objectspace
     */
    private RubyArray(Ruby runtime, boolean objectSpace) {
        super(runtime, runtime.getArray(), objectSpace);
    }

    protected RubyArray(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    /* Array constructors taking the MetaClass to fulfil MRI Array subclass behaviour
     *
     */
    private RubyArray(Ruby runtime, RubyClass klass, int length) {
        super(runtime, klass);
        values = IRubyObject.array(validateBufferLength(runtime, length));
    }

    private RubyArray(Ruby runtime, RubyClass klass, IRubyObject[] vals, boolean objectspace) {
        super(runtime, klass, objectspace);
        values = vals;
        realLength = vals.length;
    }

    protected RubyArray(Ruby runtime, RubyClass klass, boolean objectSpace) {
        super(runtime, klass, objectSpace);
    }

    public RubyArray(Ruby runtime, RubyClass klass, IRubyObject[] vals) {
        super(runtime, klass);
        values = vals;
        realLength = vals.length;
    }

    public RubyArray(RubyClass klass, IRubyObject[] vals, boolean shared) {
        super(klass);
        values = vals;
        realLength = vals.length;
        isShared = shared;
    }

    /**
     * Overridden by specialized arrays to fall back to IRubyObject[].
     */
    protected void unpack(ThreadContext context) {
    }

    private void alloc(ThreadContext context, int length) {
        IRubyObject[] newValues = IRubyObject.array(validateBufferLength(context, length));
        Helpers.fillNil(context, newValues);
        values = newValues;
        begin = 0;
    }

    private void realloc(ThreadContext context, int newLength, int valuesLength) {
        unpack(context);
        IRubyObject[] reallocated = IRubyObject.array(validateBufferLength(context, newLength));
        if (newLength > valuesLength) {
            Helpers.fillNil(context, reallocated, valuesLength, newLength);
            safeArrayCopy(context, values, begin, reallocated, 0, valuesLength); // elements and trailing nils
        } else {
            safeArrayCopy(context, values, begin, reallocated, 0, newLength); // ???
        }
        begin = 0;
        values = reallocated;
    }

    /**
     * check length of array
     * @param runtime the runtime
     * @param length the length to check
     * @deprecated this has been replaced by {@link RubyArray#checkLength(ThreadContext, long)}.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    protected static final void checkLength(Ruby runtime, long length) {
        checkLength(runtime.getCurrentContext(), length);
    }

    public static final int checkLength(ThreadContext context, long length) {
        if (length < 0) throw argumentError(context, "negative array size (or size too big)");
        if (length >= Integer.MAX_VALUE) throw argumentError(context, "array size too big");
        return (int) length;
    }

    /**
     * @deprecated RubyArray implements List, use it directly
     * @return a read-only copy of this list
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public final List<IRubyObject> getList() {
        return Arrays.asList(toJavaArray());
    }

    public int getLength() {
        return realLength;
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#toJavaArray(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject[] toJavaArray() {
        return toJavaArray(getCurrentContext());
    }

    /**
     * Return a Java array copy of the elements contained in this Array.
     *
     * This version always creates a new Java array that is exactly the length of the Array's elements.
     *
     * @return a Java array with exactly the size and contents of this RubyArray's elements
     */
    public IRubyObject[] toJavaArray(ThreadContext context) {
        IRubyObject[] copy = IRubyObject.array(realLength);
        copyInto(context, copy, 0);
        return copy;
    }

    /**
     * Return a reference to this RubyArray's underlying Java array, if it is not shared with another RubyArray, or
     * an exact copy of the relevant range otherwise.
     *
     * This method is typically used to work with the underlying array directly, knowing that it is not shared and that
     * all accesses must consider the begin offset.
     *
     * @return The underlying Java array for this RubyArray, or a copy if that array is shared.
     */

    public IRubyObject[] toJavaArrayUnsafe() {
        unpack(getRuntime().getCurrentContext());
        return !isShared ? values : toJavaArray();
    }

    /**
     * Return a Java array of the elements contained in this array, possibly a new array object.
     *
     * Use this method to potentially avoid making a new array and copying elements when the Array does not view a
     * subset of the underlying Java array.
     *
     * @return a Java array with exactly the size and contents of this RubyArray's elements, possibly the actual
     *         underlying array.
     */
    public IRubyObject[] toJavaArrayMaybeUnsafe() {
        unpack(getRuntime().getCurrentContext());
        return (!isShared && begin == 0 && values.length == realLength) ? values : toJavaArray();
    }

    public boolean isSharedJavaArray(RubyArray other) {
        return values == other.values && begin == other.begin && realLength == other.realLength;
    }

    /** rb_ary_make_shared
    *
    */
    protected RubyArray<?> makeShared() {
        var context = getRuntime().getCurrentContext();
        // TODO: (CON) Some calls to makeShared could create packed array almost as efficiently
        unpack(context);

        return makeShared(context, begin, realLength, arrayClass(context));
    }

    private RubyArray makeShared(ThreadContext context, int beg, int len, RubyClass klass) {
        return makeShared(context, beg, len, new RubyArray(context.runtime, klass));
    }

    private final RubyArray makeShared(ThreadContext context, int beg, int len, RubyArray sharedArray) {
        unpack(context);
        isShared = true;
        sharedArray.values = values;
        sharedArray.isShared = true;
        sharedArray.begin = beg;
        sharedArray.realLength = len;
        return sharedArray;
    }

    /** ary_shared_first
     *
     */
    private RubyArray makeSharedFirst(ThreadContext context, IRubyObject num, boolean last) {
        int n = RubyNumeric.num2int(num);

        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw argumentError(context, "negative array size");
        }

        return makeShared(context, last ? begin + realLength - n : begin, n, arrayClass(context));
    }

    /** rb_ary_modify_check
     *
     */
    protected final void modifyCheck(ThreadContext context) {
        if ((flags & TMPLOCK_OR_FROZEN_ARR_F) != 0) {
            if ((flags & FROZEN_F) != 0) throw context.runtime.newFrozenError(this);
            if ((flags & TMPLOCK_ARR_F) != 0) throw typeError(context, "can't modify array during iteration");
        }
    }

    /** rb_ary_modify
     *
     */
    protected void modify(ThreadContext context) {
        modifyCheck(context);
        if (isShared) {
            IRubyObject[] vals = IRubyObject.array(realLength);
            safeArrayCopy(context, values, begin, vals, 0, realLength);
            begin = 0;
            values = vals;
            isShared = false;
        }
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-", forRemoval = false)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 0:
            return initialize(context, block);
        case 1:
            return initializeCommon(context, args[0], null, block);
        case 2:
            return initializeCommon(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context, args.length, 0, 2);
            return null; // not reached
        }
    }

    /** rb_ary_initialize
     *
     */
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        modifyCheck(context);
        unpack(context);
        realLength = 0;
        if (block.isGiven()) warning(context, "given block not used");

        return this;
    }

    /** rb_ary_initialize
     *
     */
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, Block block) {
        return initializeCommon(context, arg0, null, block);
    }

    /** rb_ary_initialize
     *
     */
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return initializeCommon(context, arg0, arg1, block);
    }

    protected IRubyObject initializeCommon(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        unpack(context);

        if (arg1 == null && !(arg0 instanceof RubyFixnum)) {
            IRubyObject val = arg0.checkArrayType();
            if (!val.isNil()) {
                replace(val);
                return this;
            }
        }

        long len = toLong(context, arg0);
        if (len < 0) throw argumentError(context, "negative array size");
        int ilen = validateBufferLength(context, len);

        modify(context);

        if (ilen > values.length - begin) {
            values = IRubyObject.array(ilen);
            begin = 0;
        }

        if (block.isGiven()) {
            if (arg1 != null) warn(context, "block supersedes default value argument");

            if (block.getSignature() == Signature.NO_ARGUMENTS) {
                IRubyObject nil = context.nil;
                for (int i = 0; i < ilen; i++) {
                    storeInternal(context, i, block.yield(context, nil));
                    realLength = i + 1;
                }
            } else {
                for (int i = 0; i < ilen; i++) {
                    storeInternal(context, i, block.yield(context, asFixnum(context, i)));
                    realLength = i + 1;
                }
            }

        } else {
            try {
                if (arg1 == null) {
                    Helpers.fillNil(context, values, begin, begin + ilen);
                } else {
                    Arrays.fill(values, begin, begin + ilen, arg1);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(context, ex);
            }
            realLength = ilen;
        }
        return this;
    }

    /** rb_ary_initialize_copy
     *
     */
    @JRubyMethod(name = {"initialize_copy"}, visibility=PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject orig) {
        return replace(orig);
    }

    /**
     * Overridden dup for fast-path logic.
     *
     * @return A new RubyArray sharing the original backing store.
     */
    @Override
    public IRubyObject dup() {
        if (metaClass.getClassIndex() != ClassIndex.ARRAY) return super.dup();

        Ruby runtime = metaClass.runtime;
        RubyArray dup = dupImpl(runtime, runtime.getArray());
        return dup;
    }

    protected RubyArray dupImpl(Ruby runtime, RubyClass metaClass) {
        RubyArray dup = new RubyArray(runtime, metaClass, values, begin, realLength, true);
        dup.isShared = this.isShared = true;
        return dup;
    }

    /** rb_ary_dup
     *
     */
    public RubyArray aryDup() {
        // In 1.9, rb_ary_dup logic changed so that on subclasses of Array,
        // dup returns an instance of Array, rather than an instance of the subclass
        Ruby runtime = metaClass.runtime;
        return dupImpl(runtime, runtime.getArray());
    }

    /**
     * @param orig
     * @return ""
     * @deprecated Use {@link RubyArray#replace(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject replace(IRubyObject orig) {
        return replace(getCurrentContext(), orig);
    }

        /** rb_ary_replace
         *
         */
    @JRubyMethod(name = {"replace"})
    public IRubyObject replace(ThreadContext context, IRubyObject orig) {
        unpack(context);
        modifyCheck(context);

        if (this == orig) return this;

        var origArr = orig.convertToArray();
        origArr.unpack(context);
        origArr.isShared = true;

        isShared = true;
        values = origArr.values;
        realLength = origArr.realLength;
        begin = origArr.begin;


        return this;
    }

    /** rb_ary_to_s
     *
     */
    @Override
    public RubyString to_s(ThreadContext context) {
        return inspect(context);
    }

    public boolean includes(ThreadContext context, IRubyObject item) {
        int end = realLength;
        for (int i = 0; i < end; i++) {
            final IRubyObject value = eltOk(i);
            if (equalInternal(context, value, item)) return true;
        }

        return false;
    }

    public boolean includesByEql(ThreadContext context, IRubyObject item) {
        int end = realLength;
        for (int i = 0; i < end; i++) {
            final IRubyObject value = eltOk(i);
            if (eqlInternal(context, item, value)) return true;
        }

        return false;
    }

    /** rb_ary_hash
     *
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashImpl(context));
    }

    private long hashImpl(final ThreadContext context) {
        long h = Helpers.hashStart(context.runtime, realLength);

        h = Helpers.murmurCombine(h, System.identityHashCode(RubyArray.class));

        for (int i = 0; i < realLength; i++) {
            IRubyObject value = eltOk(i);
            RubyFixnum n = Helpers.safeHash(context, value);
            h = murmurCombine(h, n.value);
        }

        return hashEnd(h);
    }

    // NOTE: there's some (passing) RubySpec where [ ary ] is mocked with a custom hash
    // maybe JRuby doesn't need to obey 100% since it already has hashCode on other core types
    //@Override
    //public int hashCode() {
    //    return (int) hashImpl(getRuntime().getCurrentContext());
    //}
    public IRubyObject store(long index, IRubyObject value) {
        return store(metaClass.runtime.getCurrentContext(), index, value);
    }

    /**
     * Store an element at the specified index or throw if the index is invalid.
     * @param context the current thread context
     * @param index the offset to store the value
     * @param value the value to be stored
     * @return the value set
     */
    // MRI: rb_ary_store
    @JRubyAPI
    public IRubyObject store(ThreadContext context, long index, IRubyObject value) {
        if (index < 0 && (index += realLength) < 0) throw indexError(context, "index " + (index - realLength) + " out of array");
        if (index >= Integer.MAX_VALUE) throw indexError(context, "index " + index  + " too big");

        modify(context);

        storeInternal(context, (int) index, value);

        return value;
    }

    protected void storeInternal(ThreadContext context, final int index, final IRubyObject value) {
        assert index >= 0;

        if (index >= realLength) {
            int valuesLength = values.length - begin;
            if (index >= valuesLength) storeRealloc(context, index, valuesLength);
            realLength = index + 1;
        }

        safeArraySet(context, values, begin + index, value);
    }

    private void storeRealloc(ThreadContext context, final int index, final int valuesLength) {
        long newLength = valuesLength >> 1;

        if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

        newLength += index;
        if (newLength >= Integer.MAX_VALUE) throw indexError(context, "index " + index  + " too big");

        realloc(context, (int) newLength, valuesLength);
    }

    /** rb_ary_elt
     *
     */
    private final IRubyObject elt(long offset) {
        if (offset < 0 || offset >= realLength) {
            return metaClass.runtime.getNil();
        }
        return eltOk(offset);
    }

    public T eltOk(long offset) {
        try {
            return (T) eltInternal((int)offset);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(getRuntime().getCurrentContext(), ex);
        }
    }

    public T eltSetOk(long offset, T value) {
        return eltSetOk((int) offset, value);
    }

    public T eltSetOk(int offset, T value) {
        try {
            return eltInternalSet(offset, value);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(getRuntime().getCurrentContext(), ex);
        }
    }

    /** rb_ary_entry
     *
     */
    public final IRubyObject entry(long offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt(offset);
    }

    public final IRubyObject entry(int offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt(offset);
    }

    public T eltInternal(int offset) {
        return (T) values[begin + offset];
    }

    public T eltInternalSet(int offset, T item) {
        values[begin + offset] = item;
        return item;
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-", forRemoval = false)
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return fetch(context, args[0], block);
        case 2:
            return fetch(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context, args.length, 1, 2);
            return null; // not reached
        }
    }

    @JRubyMethod(rest = true)
    public IRubyObject fetch_values(ThreadContext context, IRubyObject[] args, Block block) {
        int length = args.length;
        if (length == 0) return Create.newEmptyArray(context);

        int arraySize = size();
        var result = Create.newRawArray(context, length);
        for (int i = 0; i < length; i++) {
            int index = toInt(context, args[i]);
            // FIXME: lookup the bounds part of this in error message??
            if (index >= arraySize) {
                if (!block.isGiven()) throw indexError(context, "index " + index + " outside of array bounds: 0...0");
                result.append(context, block.yield(context, asFixnum(context, index)));
            } else {
                result.append(context, eltOk(index));
            }
        }

        return result.finishRawArray(context);
    }

    /** rb_ary_fetch
     *
     */
    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject arg0, Block block) {
        long index = toLong(context, arg0);

        if (index < 0) index += realLength;
        if (index < 0 || index >= realLength) {
            if (block.isGiven()) return block.yield(context, arg0);
            throw indexError(context, "index " + index + " out of array");
        }

        return eltOk((int) index);
    }

    /** rb_ary_fetch
    *
    */
   @JRubyMethod
   public IRubyObject fetch(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
       if (block.isGiven()) warn(context, "block supersedes default value argument");

       long index = toLong(context, arg0);

       if (index < 0) index += realLength;
       if (index < 0 || index >= realLength) {
           if (block.isGiven()) return block.yield(context, arg0);
           return arg1;
       }

       return eltOk((int) index);
   }

    /** rb_ary_to_ary
     *
     */
    public static RubyArray aryToAry(ThreadContext context, IRubyObject obj) {
        IRubyObject tmp = TypeConverter.checkArrayType(context, obj);
        return tmp != context.nil ? (RubyArray) tmp : newArray(context.runtime, obj);
    }

    @Deprecated
    public static RubyArray aryToAry(IRubyObject obj) {
        return aryToAry(obj.getRuntime().getCurrentContext(), obj);
    }

    private void splice(ThreadContext context, int beg, int len, IRubyObject rpl) {
        if (len < 0) throw indexError(context, "negative length (" + len + ")");
        if (beg < 0 && (beg += realLength) < 0) throw indexError(context, "index " + (beg - realLength) + " out of array");

        final RubyArray rplArr;
        final int rlen;

        if (rpl == null) {
            rplArr = null;
            rlen = 0;
        } else if (rpl.isNil()) {
            // 1.9 replaces with nil
            rplArr = newArray(context.runtime, rpl);
            rlen = 1;
        } else {
            rplArr = aryToAry(context, rpl);
            rlen = rplArr.realLength;
        }

        splice(context, beg, len, rplArr, rlen);
    }

    /** rb_ary_splice
     *
     */
    private void splice(ThreadContext context, int beg, int len, final RubyArray rplArr, final int rlen) {
        if (len < 0) throw indexError(context, "negative length (" + len + ")");
        if (beg < 0 && (beg += realLength) < 0) indexError(context, "index " + (beg - realLength) + " out of array");

        unpack(context);
        modify(context);

        int valuesLength = values.length - begin;
        if (beg >= realLength) {
            len = beg + rlen;
            if (len >= valuesLength) spliceRealloc(context, len, valuesLength);
            try {
                Helpers.fillNil(context, values, begin + realLength, begin + beg);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw concurrentModification(context, e);
            }
            realLength = len;
        } else {
            if (beg + len > realLength) len = realLength - beg;
            int alen = realLength + rlen - len;
            if (alen >= valuesLength) spliceRealloc(context, alen, valuesLength);

            if (len != rlen) {
                safeArrayCopy(context, values, begin + (beg + len), values, begin + beg + rlen, realLength - (beg + len));
                realLength = alen;
            }
        }

        if (rlen > 0) {
            rplArr.copyInto(context, values, begin + beg, rlen);
        }
    }

    /** rb_ary_splice
     *
     */
    private final void spliceOne(ThreadContext context, long beg, IRubyObject rpl) {
        if (beg < 0 && (beg += realLength) < 0) throw indexError(context, "index " + (beg - realLength) + " out of array");

        unpack(context);
        modify(context);

        int valuesLength = values.length - begin;
        if (beg >= realLength) {
            int len = (int) beg + 1;
            if (len >= valuesLength) spliceRealloc(context, len, valuesLength);
            Helpers.fillNil(context, values, begin + realLength, begin + ((int) beg));
            realLength = len;
        } else {
            int len = beg > realLength ? realLength - (int) beg : 0;
            int alen = realLength + 1 - len;
            if (alen >= valuesLength) spliceRealloc(context, alen, valuesLength);

            if (len == 0) {
                safeArrayCopy(context, values, begin + (int) beg, values, begin + (int) beg + 1, realLength - (int) beg);
                realLength = alen;
            }
        }

        safeArraySet(context, values, begin + (int) beg, rpl);
    }

    private void spliceRealloc(ThreadContext context, int length, int valuesLength) {
        int tryLength = calculateBufferLength(context.runtime, valuesLength);
        int len = length > tryLength ? length : tryLength;
        IRubyObject[] vals = IRubyObject.array(len);
        System.arraycopy(values, begin, vals, 0, realLength);

        // only fill if there actually will remain trailing storage
        if (len > length) Helpers.fillNil(context, vals, length, len);

        begin = 0;
        values = vals;
    }

    private void unshiftRealloc(ThreadContext context, int valuesLength) {
        int newLength = valuesLength >> 1;
        if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

        newLength = addBufferLength(context, valuesLength, newLength);

        IRubyObject[] vals = IRubyObject.array(newLength);
        safeArrayCopy(context, values, begin, vals, 1, valuesLength);
        Helpers.fillNil(context, vals, valuesLength + 1, newLength);
        values = vals;
        begin = 0;
    }

    public IRubyObject insert() {
        throw argumentError(getRuntime().getCurrentContext(), 0, 1);
    }

    /**
     * @param arg
     * @return ""
     * @deprecated Use #{@link RubyArray#insert(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject insert(IRubyObject arg) {
        return insert(getCurrentContext(), arg);
    }

    /** rb_ary_insert
     *
     */
    @JRubyMethod(name = "insert")
    public IRubyObject insert(ThreadContext context, IRubyObject arg) {
        modifyCheck(context);
        toLong(context, arg);
        return this;
    }

    /**
     * @param arg1
     * @param arg2
     * @return itself
     * @deprecated See {@link RubyArray#insert(ThreadContext, IRubyObject, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        return insert(getCurrentContext(), arg1, arg2);
    }

    @JRubyMethod(name = "insert")
    public IRubyObject insert(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        modifyCheck(context);
        insert(context, toLong(context, arg1), arg2);
        return this;
    }

    private void insert(ThreadContext context, long pos, IRubyObject val) {
        if (pos == -1) pos = realLength;
        else if (pos < 0) {
            long minpos = -realLength - 1;
            if (pos < minpos) throw indexError(context, "index " + pos + " too small for array; minimum: " + minpos);
            pos++;
        }

        spliceOne(context, pos, val); // rb_ary_new4
    }

    @Deprecated
    public IRubyObject insert(IRubyObject[] args) {
        return insert(getCurrentContext(), args);
    }

    @JRubyMethod(name = "insert", required = 1, rest = true, checkArity = false)
    public IRubyObject insert(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        modifyCheck(context);

        if (argc == 1) return this;

        unpack(context);

        long pos = toLong(context, args[0]);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;

        RubyArray inserted = new RubyArray(context.runtime, false);
        inserted.values = args;
        inserted.begin = 1;
        inserted.realLength = argc - 1;

        splice(context, checkInt(context, pos), 0, inserted, inserted.realLength); // rb_ary_new4

        return this;
    }

    /**
     * @return new ary
     * @deprecated Use {@link RubyArray#transpose(ThreadContext)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray transpose() {
        return transpose(getCurrentContext());
    }

    /** rb_ary_transpose
     *
     */
    @JRubyMethod(name = "transpose")
    public RubyArray transpose(ThreadContext context) {
        int alen = realLength;
        if (alen == 0) return aryDup();
        int elen = -1;
        IRubyObject[] result = null;

        for (int i = 0; i < alen; i++) {
            var tmp = elt(i).convertToArray();
            if (elen < 0) {
                elen = tmp.realLength;
                result = IRubyObject.array(elen);
                for (int j = 0; j < elen; j++) {
                    result[j] = newBlankArray(context, alen);
                }
            } else if (elen != tmp.realLength) {
                throw indexError(context, "element size differs (" + tmp.realLength + " should be " + elen + ")");
            }
            for (int j = 0; j < elen; j++) {
                ((RubyArray<?>) result[j]).storeInternal(context, i, tmp.elt(j));
            }
        }
        return new RubyArray<>(context.runtime, result);
    }

    /**
     * @param args yes
     * @return values_at
     * @deprecated Use {@link RubyArray#values_at(ThreadContext, IRubyObject[])} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject values_at(IRubyObject[] args) {
        return values_at(getCurrentContext(), args);
    }

    /** rb_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public IRubyObject values_at(ThreadContext context, IRubyObject[] args) {
        final int length = realLength;
        RubyArray<?> result = Create.newRawArray(context, args.length);

        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if (arg instanceof RubyFixnum fix) {
                result.append(context, entry(fix.getValue()));
                continue;
            }

            final int[] begLen;
            if (!(arg instanceof RubyRange range)) {
                // do result.append
            } else if ((begLen = range.begLenInt(context, length, 1)) == null) {
                continue;
            } else {
                final int beg = begLen[0];
                final int len = begLen[1];
                for (int j = 0; j < len; j++) {
                    result.append(context, entry(j + beg));
                }
                continue;
            }
            result.append(context, entry(toLong(context, arg)));
        }

        return result.finishRawArray(context);
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseq(long beg, long len) {
        return subseq(getRuntime().getArray(), beg, len, true);
    }

    /** rb_ary_subseq_step
     *
     */
    public IRubyObject subseq_step(ThreadContext context, RubyArithmeticSequence arg0) {
        long beg, len, end;
        long step = getStep(context, arg0);
        IRubyObject aseqBeg = getBegin(context, arg0);
        IRubyObject aseqEnd = getEnd(context, arg0);
        boolean aseqExcl = arg0.exclude_end(context).isTrue();
        len = realLength;

        if (step < 0) {
            if (aseqExcl && !aseqEnd.isNil()) {
                /* Handle exclusion before range reversal */
                aseqEnd = asFixnum(context, toLong(context, aseqEnd) + 1);

                /* Don't exclude the previous beginning */
                aseqExcl = false;
            }
            IRubyObject tmp = aseqBeg;
            aseqBeg = aseqEnd;
            aseqEnd = tmp;
        }

        if (step < -1 || step > 1) {
            int [] ret;
            try {
                ret = RubyRange.newRange(context, aseqBeg, aseqEnd, aseqExcl).begLenInt(context, (int)len, 1);
            } catch(RaiseException ex){
                if (ex.getException() instanceof RubyRangeError) {
                    // convert exception message using an ArithsemeticSequece arg.
                    // e.g.
                    // [1].slice((-101..-1)%2) shoud throw "((-101..-1).%(2)) out of range".
                    // The original exception message is "-1..-1 out of range".
                    throw rangeError(context, arg0.inspect(context) + " out of range");
                } else {
                    throw ex;
                }
            }
            if (ret != null && (ret[0] > len || ret[1] > len)) throw rangeError(context, arg0.inspect(context) + " out of range");
        }

        beg = aseqBeg.isNil() ? 0 : aseqBeg.convertToFloat().asLong(context);
        end = aseqEnd.isNil() ? -1 : aseqEnd.convertToFloat().asLong(context);

        if (aseqEnd.isNil()) aseqExcl = false;

        if (beg < 0) {
            beg += len;
            if (beg < 0) throw rangeError(context, "integer " + beg + " out of range of fixnum");
        }

        if (end < 0) end += len;
        if (!aseqExcl) end++;

        len = end - beg;

        if (len < 0) len = 0;

        long alen = realLength;
        if (beg > alen)  return context.nil;
        if (beg < 0 || len < 0) return context.nil;

        if (alen < len || alen < beg + len) len = alen - beg;

        if (len == 0) return Create.newEmptyArray(context);
        if (step == 0) throw rangeError(context, "slice step cannot be zero");
        if (step == 1) return subseq(beg, len);

        long orig_len = len;
        if (step > 0 && step >= len) return Create.newArray(context, eltOk(beg));

        if (step < 0 && (step < -len)) step = -len;

        long ustep = (step < 0) ? -step : step;
        len = (len + ustep - 1) / ustep;

        long j = beg + ((step > 0) ? 0 : (orig_len - 1));
        RubyArray<?> result1;
        result1 = Create.newRawArray(context, len);
        RubyArray result = result1;

        for(long i = 0; i < len; ++i) {
            result.append(context, eltOk(j));
            j = j + step;
        }

        return result.finishRawArray(context);
    }

    private IRubyObject getBegin(ThreadContext context, RubyArithmeticSequence arg0) {
      return arg0.begin(context);
    }

    private IRubyObject getEnd(ThreadContext context, RubyArithmeticSequence arg0) {
        return arg0.end(context);
    }

    private Long getStep(ThreadContext context, RubyArithmeticSequence arg0) {
        return arg0.step(context).isNil() ? null: toLong(context, arg0.step(context));
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseqLight(long beg, long len) {
        return subseq(metaClass, beg, len, true);
    }

    public IRubyObject subseq(RubyClass metaClass, long beg, long len, boolean light) {
        Ruby runtime = metaClass.runtime;
        if (beg > realLength || beg < 0 || len < 0) return runtime.getNil();

        if (beg + len > realLength) {
            len = realLength - beg;
            if (len < 0) len = 0;
        }

        if (len == 0) return new RubyArray(runtime, metaClass, IRubyObject.NULL_ARRAY, !light);
        return makeShared(runtime.getCurrentContext(), begin + (int) beg, (int) len, new RubyArray(runtime, metaClass, !light));
    }

    /** rb_ary_length
     *
     */
    @JRubyMethod(name = "length", alias = "size")
    public RubyFixnum length(ThreadContext context) {
        return asFixnum(context, realLength);
    }

    @Deprecated
    public RubyFixnum length() {
        return length(getCurrentContext());
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    protected static IRubyObject size(ThreadContext context, RubyArray self, IRubyObject[] args) {
        return self.length(context);
    }

    /**
     * @param item
     * @return itself
     * @deprecated Use {@link RubyArray#append(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray<?> append(IRubyObject item) {
        return append(getCurrentContext(), item);
    }

    /** rb_ary_push - specialized rb_ary_store
     *
     */
    @JRubyMethod(name = "<<")
    public RubyArray append(ThreadContext context, IRubyObject item) {
        unpack(context);
        modify(context);
        int valuesLength = values.length - begin;
        if (realLength == valuesLength) {
            if (realLength == Integer.MAX_VALUE) throw indexError(context, "index " + Integer.MAX_VALUE + " too big");

            long newLength = valuesLength + (valuesLength >> 1);
            if (newLength > Integer.MAX_VALUE) {
                newLength = Integer.MAX_VALUE;
            } else if (newLength < ARRAY_DEFAULT_SIZE) {
                newLength = ARRAY_DEFAULT_SIZE;
            }

            realloc(context, (int) newLength, valuesLength);
        }

        safeArraySet(context, values, begin + realLength++, item);

        return this;
    }

    /** rb_ary_push_m - instance method push
     *
     */
    @Deprecated // not-used
    public RubyArray<?> push_m(IRubyObject[] items) {
        return push(items);
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray push(IRubyObject item) {
        append(item);

        return this;
    }

    @JRubyMethod(name = "push", alias = "append")
    public RubyArray push(ThreadContext context, IRubyObject item) {
        append(context, item);

        return this;
    }

    /**
     * @param items
     * @return ""
     * @deprecated Use {@link RubyArray@}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray<?> push(IRubyObject[] items) {
        return push(getCurrentContext(), items);
    }

    @JRubyMethod(name = "push", alias = "append", rest = true)
    public RubyArray push(ThreadContext context, IRubyObject[] items) {
        if (items.length == 0) modifyCheck(context);
        for (IRubyObject item : items) {
            append(context, item);
        }
        return this;
    }

    /** rb_ary_pop
     *
     */
    @JRubyMethod
    public IRubyObject pop(ThreadContext context) {
        unpack(context);
        modifyCheck(context);

        if (realLength == 0) return context.nil;

        if (isShared) {
            return safeArrayRef(context, values, begin + --realLength);
        } else {
            int index = begin + --realLength;
            return safeArrayRefSet(context, values, index, context.nil);
        }
    }

    @JRubyMethod
    public IRubyObject pop(ThreadContext context, IRubyObject num) {
        unpack(context);
        modifyCheck(context);
        RubyArray result = makeSharedFirst(context, num, true);
        realLength -= result.realLength;
        return result;
    }

    /** rb_ary_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        unpack(context);
        modifyCheck(context);

        if (realLength == 0) return context.nil;

        final IRubyObject obj = safeArrayRefCondSet(context, values, begin, !isShared, context.nil);
        begin++;
        realLength--;
        return obj;

    }

    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context, IRubyObject num) {
        unpack(context);
        modify(context);

        RubyArray result = makeSharedFirst(context, num, false);

        int n = result.realLength;
        begin += n;
        realLength -= n;
        return result;
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#unshift(ThreadContext)} instead
     */
    @Deprecated
    public IRubyObject unshift() {
        return unshift(getCurrentContext());
    }

    @JRubyMethod(name = "unshift", alias = "prepend")
    public IRubyObject unshift(ThreadContext context) {
        modifyCheck(context);
        return this;
    }

    /**
     * @param item
     * @return ""
     * @deprecated Use {@link RubyArray#unshift(ThreadContext, IRubyObject)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject unshift(IRubyObject item) {
        return unshift(getCurrentContext(), item);
    }
    
    /** rb_ary_unshift
     *
     */
    @JRubyMethod(name = "unshift", alias = "prepend")
    public IRubyObject unshift(ThreadContext context, IRubyObject item) {
        unpack(context);

        if (begin == 0 || isShared) {
            modify(context);
            final int valuesLength = values.length - begin;
            if (valuesLength == 0) {
                alloc(context, ARRAY_DEFAULT_SIZE);
                begin = ARRAY_DEFAULT_SIZE - 1;
            } else if (realLength == valuesLength) {
                unshiftRealloc(context, valuesLength);
            } else {
                safeArrayCopy(context, values, begin, values, begin + 1, realLength);
            }
        } else {
            modifyCheck(context);
            begin--;
        }
        realLength++;
        values[begin] = item;
        return this;
    }

    /**
     * @param items
     * @return ""
     * @deprecated Use {@link RubyArray#unshift(ThreadContext, IRubyObject[])}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject unshift(IRubyObject[] items) {
        return unshift(getCurrentContext(), items);
    }

    @JRubyMethod(name = "unshift", alias = "prepend",  rest = true)
    public IRubyObject unshift(ThreadContext context, IRubyObject[] items) {
        unpack(context);

        if (items.length == 0) {
            modifyCheck(context);
            return this;
        }

        final int len = realLength;
        store(((long) len) + items.length - 1, context.nil);

        try {
            System.arraycopy(values, begin, values, begin + items.length, len);
            ArraySupport.copy(items, 0, values, begin, items.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }

        return this;
    }

    /** rb_ary_includes
     *
     */
    @JRubyMethod(name = "include?")
    public RubyBoolean include_p(ThreadContext context, IRubyObject item) {
        return asBoolean(context, includes(context, item));
    }

    /** rb_ary_frozen_p
     *
     */
    @JRubyMethod(name = "frozen?")
    @Override
    public RubyBoolean frozen_p(ThreadContext context) {
        return asBoolean(context, isFrozen() || (flags & TMPLOCK_ARR_F) != 0);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4")
    public IRubyObject aref(IRubyObject[] args) {
        ThreadContext context = getCurrentContext();
        return switch (args.length) {
            case 1 -> aref(context, args[0]);
            case 2 -> aref(context, args[0], args[1]);
            default -> {
                Arity.raiseArgumentError(context, args.length, 1, 2);
                yield null;
            }
        };
    }

    @Deprecated
    public IRubyObject aref(IRubyObject arg0) {
        return aref(getCurrentContext(), arg0);
    }

    /** rb_ary_aref
     */
    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject aref(ThreadContext context, IRubyObject arg0) {
        if (arg0 instanceof RubyArithmeticSequence) {
            return subseq_step(context, (RubyArithmeticSequence) arg0);
        } else {
            return arg0 instanceof RubyFixnum ? entry(((RubyFixnum) arg0).value) : arefCommon(context, arg0);
        }
    }

    private IRubyObject arefCommon(ThreadContext context, IRubyObject arg0) {
        if (arg0 instanceof RubyRange range) {
            long[] beglen = range.begLen(context, realLength, 0);
            return beglen == null ? context.nil : subseq(beglen[0], beglen[1]);
        } else {
            ArraySites sites = sites(context);

            if (RubyRange.isRangeLike(context, arg0, sites.begin_checked, sites.end_checked, sites.exclude_end_checked)) {
                RubyRange range = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

                long[] beglen = range.begLen(context, realLength, 0);
                return beglen == null ? context.nil : subseq(beglen[0], beglen[1]);
            }
        }
        return entry(toLong(context, arg0));
    }

    @Deprecated(since = "10.0")
    public IRubyObject aref(IRubyObject arg0, IRubyObject arg1) {
        return aref(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject aref(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        long beg = toLong(context, arg0);
        if (beg < 0) beg += realLength;
        return subseq(beg, toLong(context, arg1));
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-", forRemoval = false)
    public IRubyObject aset(IRubyObject[] args) {
        return switch (args.length) {
            case 2 -> aset(args[0], args[1]);
            case 3 -> aset(args[0], args[1], args[2]);
            default -> throw argumentError(getCurrentContext(), "wrong number of arguments (" + args.length + " for 2)");
        };
    }

    /**
     * @param arg0
     * @param arg1
     * @return ""
     * @deprecated Use {@link RubyArray#aset(ThreadContext, IRubyObject, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1) {
        return aset(getCurrentContext(), arg0, arg1);
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        modifyCheck(context);
        if (arg0 instanceof RubyFixnum) {
            store(((RubyFixnum) arg0).value, arg1);
        } else if (arg0 instanceof RubyRange range) {
            int beg0 = checkLongForInt(context, range.begLen0(context, realLength));
            int beg1 = checkLongForInt(context, range.begLen1(context, realLength, beg0));
            splice(context, beg0, beg1, arg1);
        } else {
            asetFallback(context, arg0, arg1);
        }
        return arg1;
    }

    // stand in method for checkInt as it raises the wrong type of 
    // exception to mri
    private int checkLongForInt(ThreadContext context, long value) {
        if (((int)value) != value) throw indexError(context, String.format("index %d is too big", value));

        return (int)value;
    }

    private void asetFallback(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        ArraySites sites = sites(context);

        if (RubyRange.isRangeLike(context, arg0, sites.begin_checked, sites.end_checked, sites.exclude_end_checked)) {
            RubyRange range = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

            int beg = checkInt(context, range.begLen0(context, realLength));
            splice(context, beg, checkInt(context, range.begLen1(context, realLength, beg)), arg1);
        } else {
            store(toLong(context, arg0), arg1);
        }
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @return ""
     * @deprecated Use {@link RubyArray#aset(ThreadContext, IRubyObject, IRubyObject, IRubyObject)} instead.
     */
    @Deprecated
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return aset(getCurrentContext(), arg0, arg1, arg2);
    }

    /** rb_ary_aset
    *
    */
    @JRubyMethod(name = "[]=")
    public IRubyObject aset(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        modifyCheck(context);
        splice(context, RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1), arg2);
        return arg2;
    }

    /**
     * @param pos
     * @return ""
     * @deprecated Use {@link RubyArray#at(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated
    public IRubyObject at(IRubyObject pos) {
        return at(getCurrentContext(), pos);
    }

    /** rb_ary_at
     *
     */
    @JRubyMethod(name = "at")
    public IRubyObject at(ThreadContext context, IRubyObject pos) {
        return entry(toLong(context, pos));
    }

	/** rb_ary_concat
     *
     */
    @JRubyMethod(name = "concat")
    public RubyArray concat(ThreadContext context, IRubyObject obj) {
        modifyCheck(context);

        concat(context, obj.convertToArray());
        return this;
    }

    private void concat(ThreadContext context, RubyArray<?> obj) {
        splice(context, realLength, 0, obj, obj.realLength);
    }

    /**
     * @param y array
     * @return array
     * @deprecated Use {@link RubyArray#aryAppend(ThreadContext, RubyArray)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray aryAppend(RubyArray<?> y) {
        return aryAppend(getCurrentContext(), y);
    }

    // MRI: ary_append
    public RubyArray aryAppend(ThreadContext context, RubyArray<?> y) {
        if (y.realLength > 0) splice(context, realLength, 0, y, y.realLength);
        return this;
    }

    /** rb_ary_concat_multi
     *
     */
    @JRubyMethod(name = "concat", rest = true)
    public RubyArray concat(ThreadContext context, IRubyObject[] objs) {
        modifyCheck(context);

        if (objs.length > 0) {
            RubyArray<?> result;
            final int len = objs.length;
            result = Create.newRawArray(context, len);
            var tmp = result;

            for (IRubyObject obj : objs) {
                tmp.concat(context, obj.convertToArray());
            }

            return aryAppend(context, tmp.finishRawArray(context));
        }

        return this;
    }

    public RubyArray concat(IRubyObject obj) {
        return concat(metaClass.runtime.getCurrentContext(), obj);
    }

    /** inspect_ary
     *
     */
    protected IRubyObject inspectAry(ThreadContext context) {
        RubyString str = RubyString.newStringLight(context.runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        str.cat((byte) '[');

        for (int i = 0; i < realLength; i++) {

            RubyString s = inspect(context, safeArrayRef(context, values, begin + i));
            if (i > 0) {
                ByteList bytes = str.getByteList();
                bytes.append((byte) ',').append((byte) ' ');
            } else {
                EncodingUtils.encAssociateIndex(str, s.getEncoding());
            }
            str.catWithCodeRange(s);
        }
        str.cat((byte) ']');

        return str;
    }

    /** rb_ary_inspect
    *
    */
    @JRubyMethod(name = "inspect", alias = "to_s")
    public RubyString inspect(ThreadContext context) {
        final Ruby runtime = context.runtime;
        if (realLength == 0) return RubyString.newStringShared(runtime, EMPTY_ARRAY_BL);
        if (runtime.isInspecting(this)) return RubyString.newStringShared(runtime, RECURSIVE_ARRAY_BL);

        try {
            runtime.registerInspecting(this);
            return (RubyString) inspectAry(context);
        } finally {
            runtime.unregisterInspecting(this);
        }
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject first(IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> first(getCurrentContext());
            case 1 -> first(getCurrentContext(), args[0]);
            default -> {
                Arity.raiseArgumentError(getCurrentContext(), args.length, 0, 1);
                yield null;
            }
        };
    }

    @Deprecated(since = "10.0")
    public IRubyObject first() {
        return first(getCurrentContext());
    }

    // MRI: rb_ary_first
    @JRubyAPI
    @JRubyMethod(name = "first")
    public IRubyObject first(ThreadContext context) {
        return realLength == 0 ? context.nil : eltOk(0);
    }

    @Deprecated(since = "10.0")
    public IRubyObject first(IRubyObject arg0) {
        return first(getCurrentContext(), arg0);
    }

    // MRI: rb_ary_first
    @JRubyAPI
    @JRubyMethod(name = "first")
    public IRubyObject first(ThreadContext context, IRubyObject arg0) {
        long n = toLong(context, arg0);
        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw argumentError(context, "negative array size (or size too big)");
        } else if (n == 1) {
            return Create.newArray(context, eltOk(0));
        } else if (n == 2) {
            return Create.newArray(context, eltOk(0), eltOk(1));
        }

        unpack(context);
        return makeShared(context, begin, (int) n, arrayClass(context));
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject last(IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> last(getCurrentContext());
            case 1 -> last(getCurrentContext(), args[0]);
            default -> {
                Arity.raiseArgumentError(getCurrentContext(), args.length, 0, 1);
                yield null;
            }
        };
    }

    @Deprecated(since = "10.0")
    public IRubyObject last() {
        return last(getCurrentContext());
    }


    // MRI: rb_ary_last
    @JRubyAPI
    @JRubyMethod(name = "last")
    public IRubyObject last(ThreadContext context) {
        return realLength == 0 ? context.nil : eltOk(realLength - 1);
    }

    /**
     * @param arg0
     * @return ""
     * @deprecated Use {@link RubyArray#last(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject last(IRubyObject arg0) {
        return last(getCurrentContext(), arg0);
    }

    /** rb_ary_last
     *
     */
    @JRubyMethod(name = "last")
    public IRubyObject last(ThreadContext context, IRubyObject arg0) {
        long n = toLong(context, arg0);
        if (n > realLength) n = realLength;

        if (n < 0) throw argumentError(context, "negative array size (or size too big)");
        if (n == 1) return Create.newArray(context, eltOk(realLength - 1));
        if (n == 2) return Create.newArray(context, eltOk(realLength - 2), eltOk(realLength - 1));

        unpack(context);
        return makeShared(context, begin + realLength - (int) n, (int) n, arrayClass(context));
    }

    /**
     * mri: rb_ary_each
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "each", RubyArray::size);

        for (int i = 0; i < size(); i++) {
            // do not coarsen the "safe" catch, since it will misinterpret AIOOBE from the yielded code.
            // See JRUBY-5434
            block.yield(context, eltOk(i));
        }
        return this;
    }

    public IRubyObject eachSlice(ThreadContext context, int size, Block block) {
        unpack(context);
        var array = arrayClass(context);

        // local copies of everything
        int realLength = this.realLength;
        int begin = this.begin;

        // sliding window
        RubyArray window = makeShared(context, begin, size, array);

        // don't expose shared array to ruby
        Signature signature = block.getSignature();
        final boolean specificArity = signature.isFixed() && signature.required() != 1;

        for (; realLength >= size; realLength -= size) {
            block.yield(context, window);
            if (specificArity) { // array is never exposed to ruby, just use for yielding
                window.begin = begin += size;
            } else { // array may be exposed to ruby, create new
                window = makeShared(context, begin += size, size, array);
            }
        }

        // remainder
        if (realLength > 0) {
            window.realLength = realLength;
            block.yield(context, window);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject each_slice(ThreadContext context, IRubyObject arg, Block block) {
        final int size = RubyNumeric.num2int(arg);
        if (size <= 0) throw argumentError(context, "invalid slice size");
        return block.isGiven() ? eachSlice(context, size, block) : enumeratorizeWithSize(context, this, "each_slice", arg, arg);
    }

    /** rb_ary_each_index
     *
     */
    public IRubyObject eachIndex(ThreadContext context, Block block) {
        if (!block.isGiven()) throw context.runtime.newLocalJumpErrorNoBlock();

        for (int i = 0; i < realLength; i++) {
            block.yield(context, asFixnum(context, i));
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject each_index(ThreadContext context, Block block) {
        return block.isGiven() ? eachIndex(context, block) : enumeratorizeWithSize(context, this, "each_index", RubyArray::size);
    }

    /** rb_ary_reverse_each
     *
     */
    public IRubyObject reverseEach(ThreadContext context, Block block) {
        int len = realLength;

        while(len-- > 0) {
            // do not coarsen the "safe" catch, since it will misinterpret AIOOBE from the yielded code.
            // See JRUBY-5434
            block.yield(context, eltOk(len));
            if (realLength < len) len = realLength;
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject reverse_each(ThreadContext context, Block block) {
        return block.isGiven() ? reverseEach(context, block) : enumeratorizeWithSize(context, this, "reverse_each", RubyArray::size);
    }

    // MRI: ary_join_0
    protected int joinStrings(RubyString sep, int max, RubyString result) {
        if (max > 0 && eltOk(0) instanceof EncodingCapable ec) result.setEncoding(ec.getEncoding());

        int i;
        try {
            for (i = 0; i < max; i++) {
                IRubyObject val = eltInternal(i);
                if (!(val instanceof RubyString)) break;
                if (i > 0 && sep != null) result.catWithCodeRange(sep);
                result.append(val);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime().getCurrentContext(), e);
        }

        return i;
    }

    // MRI: ary_join_1
    private RubyString joinAny(ThreadContext context, RubyString sep, int i, RubyString result, boolean[] first) {
        assert i >= 0 : "joining elements before beginning of array";
        JavaSites.CheckedSites to_ary_checked = null;

        for (; i < realLength; i++) {
            if (i > 0 && sep != null) result.catWithCodeRange(sep);

            IRubyObject val = eltOk(i);

            if (val instanceof RubyString) {
                strJoin(result, (RubyString) val, first);
            } else if (val instanceof RubyArray) {
                recursiveJoin(context, val, sep, result, (RubyArray) val, first);
            } else {
                IRubyObject tmp = val.checkStringType();
                if (tmp != context.nil) {
                    strJoin(result, (RubyString) tmp, first);
                    continue;
                }

                if (to_ary_checked == null) to_ary_checked = sites(context).to_ary_checked;

                tmp = TypeConverter.convertToTypeWithCheck(context, val, arrayClass(context), to_ary_checked);
                if (tmp != context.nil) {
                    recursiveJoin(context, val, sep, result, (RubyArray) tmp, first);
                } else {
                    strJoin(result, RubyString.objAsString(context, val), first);
                }
            }
        }

        return result;
    }

    // MRI: ary_join_1, str_join label
    private static void strJoin(RubyString result, RubyString val, boolean[] first) {
        result.catWithCodeRange(val);
        if (first[0]) {
            result.setEncoding(val.getEncoding());
            first[0] = false;
        }
    }

    private void recursiveJoin(final ThreadContext context, final IRubyObject outValue,
                               final RubyString sep, final RubyString result, final RubyArray ary, final boolean[] first) {

        if (ary == this) throw argumentError(context, "recursive array join");

        first[0] = false;

        context.safeRecurse(JOIN_RECURSIVE, new JoinRecursive.State(ary, outValue, sep, result, first), outValue, "join", true);
    }

    @Deprecated
    public IRubyObject join19(final ThreadContext context, IRubyObject sep) {
        return join(context, sep);
    }

    /** rb_ary_join
     *
     */
    @JRubyMethod(name = "join")
    public IRubyObject join(final ThreadContext context, IRubyObject sep) {
        if (sep == context.nil) sep = getDefaultSeparator(context);

        if (realLength == 0) return RubyString.newEmptyString(context.runtime, USASCIIEncoding.INSTANCE);

        int len = 1;
        RubyString sepString = null;
        if (sep != context.nil) {
            sepString = sep.convertToString();
            len += sepString.size() * (realLength - 1);
        }

        for (int i = 0; i < realLength; i++) {
            IRubyObject val = eltOk(i);
            IRubyObject tmp = val.checkStringType();
            if (tmp == context.nil || tmp != val) {
                if (i > realLength) i = realLength;
                len += (realLength - i) * 10;
                RubyString result = RubyString.newStringLight(context.runtime, len, USASCIIEncoding.INSTANCE);
                i = joinStrings(sepString, i, result);
                boolean[] first = new boolean[] { i == 0 };
                return joinAny(context, sepString, i, result, first);
            }

            len += ((RubyString) tmp).getByteList().length();
        }

        RubyString result = RubyString.newStringLight(context.runtime, len);
        joinStrings(sepString, realLength, result);

        return result;
    }

    @Deprecated
    public IRubyObject join19(ThreadContext context) {
        return join(context);
    }

    @JRubyMethod(name = "join")
    public IRubyObject join(ThreadContext context) {
        return join(context, context.nil);
    }

    private IRubyObject getDefaultSeparator(ThreadContext context) {
        IRubyObject sep = globalVariables(context).get("$,");

        if (!sep.isNil()) warnDeprecated(context, "$, is set to non-nil value");

        return sep;
    }

    /** rb_ary_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a(ThreadContext context) {
        final RubyClass arrayClass = arrayClass(context);
        return this.metaClass != arrayClass ?
            dupImpl(context.runtime, arrayClass) : this;
    }

    @Deprecated(since = "10.0")
    public IRubyObject to_ary() {
        return this;
    }

    @JRubyMethod(name = "to_ary")
    public IRubyObject to_ary(ThreadContext context) {
    	return this;
    }

    @Deprecated
    public IRubyObject to_h(ThreadContext context) {
        return to_h(context, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "to_h")
    public IRubyObject to_h(ThreadContext context, Block block) {
        boolean useSmallHash = realLength <= 10;
        RubyHash hash = useSmallHash ? newSmallHash(context) : newHash(context);

        for (int i = 0; i < realLength; i++) {
            IRubyObject e = eltOk(i);
            IRubyObject elt = block.isGiven() ? block.yield(context, e) : e;
            IRubyObject key_value_pair = elt.checkArrayType();

            if (key_value_pair == context.nil) throw typeError(context, "wrong element type ", elt, " at " + i + " (expected array)");

            RubyArray ary = (RubyArray)key_value_pair;
            if (ary.getLength() != 2) {
                throw argumentError(context, "wrong array length at " + i + " (expected 2, was " + ary.getLength() + ")");
            }

            if (useSmallHash) {
                hash.fastASetSmall(context.runtime, ary.eltOk(0), ary.eltOk(1), true);
            } else {
                hash.fastASet(context.runtime, ary.eltOk(0), ary.eltOk(1), true);
            }
        }
        return hash;
    }

    @Override
    public RubyArray convertToArray() {
        return this;
    }

    @Override
    public IRubyObject checkArrayType(){
        return this;
    }

    /** rb_ary_equal
     *
     */
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (this == obj) return context.tru;

        if (!(obj instanceof RubyArray)) {
            if (obj == context.nil) return context.fals;

            if (!sites(context).respond_to_to_ary.respondsTo(context, obj, obj)) {
                return context.fals;
            }
            return Helpers.rbEqual(context, obj, this);
        }
        return RecursiveComparator.compare(context, sites(context).op_equal, this, obj, false);
    }

    public RubyBoolean compare(ThreadContext context, CallSite site, IRubyObject other) {
        if (!(other instanceof RubyArray)) {
            if (!sites(context).respond_to_to_ary.respondsTo(context, other, other)) return context.fals;

            return Helpers.rbEqual(context, other, this);
        }

        RubyArray ary = (RubyArray) other;

        if (realLength != ary.realLength) return context.fals;

        for (int i = 0; i < realLength; i++) {
            IRubyObject a = elt(i);
            IRubyObject b = ary.elt(i);

            if (a == b) continue; // matching MRI opt. mock frameworks can throw errors if we don't

            if (!site.call(context, a, a, b).isTrue()) return context.fals;
        }

        return context.tru;
    }

    /** rb_ary_eql
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject eql(ThreadContext context, IRubyObject obj) {
        if(!(obj instanceof RubyArray)) {
            return context.fals;
        }
        return RecursiveComparator.compare(context, sites(context).eql, this, obj, true);
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#compact_bang(ThreadContext)}
     */
    @Deprecated
    public IRubyObject compact_bang() {
        return compact_bang(getCurrentContext());
    }

    /** rb_ary_compact_bang
     *
     */
    @JRubyMethod(name = "compact!")
    public IRubyObject compact_bang(ThreadContext context) {
        unpack(context);
        modify(context);

        int p = begin;
        int t = p;
        int end = p + realLength;

        try {
            while (t < end) {
                if (values[t].isNil()) {
                    t++;
                } else {
                    values[p++] = values[t++];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }

        p -= begin;
        if (realLength == p) return context.nil;

        realloc(context, p, values.length - begin);
        realLength = p;
        return this;
    }

    @Deprecated(since = "10.0")
    public IRubyObject compact() {
        return compact(getCurrentContext());
    }

    // MRI: rb_ary_compact
    @JRubyMethod(name = "compact")
    public IRubyObject compact(ThreadContext context) {
        RubyArray ary = aryDup();
        ary.compact_bang(context);
        return ary;
    }

    @Deprecated(since = "10.0")
    public IRubyObject empty_p() {
        return empty_p(getCurrentContext());
    }

    /** rb_ary_empty_p
     *
     */
    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return realLength == 0 ? context.tru : context.fals;
    }

    /**
     * @return ""
     * @deprecated Use {@link }
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject rb_clear() {
        return rb_clear(getCurrentContext());
    }

    /** rb_ary_clear
     *
     */
    @JRubyMethod(name = "clear")
    public IRubyObject rb_clear(ThreadContext context) {
        modifyCheck(context);

        if (isShared) {
            alloc(context, ARRAY_DEFAULT_SIZE);
            isShared = false;
        } else if (values.length > ARRAY_DEFAULT_SIZE << 1) {
            alloc(context, ARRAY_DEFAULT_SIZE << 1);
        } else {
            try {
                begin = 0;
                Helpers.fillNil(context, values, 0, realLength);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw concurrentModification(context, e);
            }
        }

        realLength = 0;
        return this;
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, Block block) {
        if (!block.isGiven()) throw argumentError(context, 0, 1);
        return fillCommon(context, 0, realLength, block);
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg, Block block) {
        if (!block.isGiven()) return fillCommon(context, 0, realLength, arg);
        if (arg instanceof RubyRange range) {
            int[] beglen = range.begLenInt(context, realLength, 1);
            return fillCommon(context, beglen[0], beglen[1], block);
        }

        int beg = fillBegin(arg);
        return fillCommon(context, beg, fillLen(context, beg, null),  block);
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            int beg;
            return fillCommon(context, beg = fillBegin(arg1), fillLen(context, beg, arg2), block);
        } else {
            if (arg2 instanceof RubyRange) {
                int[] beglen = ((RubyRange) arg2).begLenInt(context, realLength, 1);
                return fillCommon(context, beglen[0], beglen[1], arg1);
            }
            int beg = fillBegin(arg2);
            return fillCommon(context, beg, fillLen(context, beg, null), arg1);
        }
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        if (block.isGiven()) throw argumentError(context, 3, 2);

        int beg = fillBegin(arg2);
        return fillCommon(context, beg, fillLen(context, beg, arg3), arg1);
    }

    private int fillBegin(IRubyObject arg) {
        int beg = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if (beg < 0) {
            beg = realLength + beg;
            if (beg < 0) beg = 0;
        }
        return beg;
    }

    private long fillLen(ThreadContext context, long beg, IRubyObject arg) {
        return arg == null || arg.isNil() ? realLength - beg : toLong(context, arg);
    }

    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, IRubyObject item) {
        unpack(context);
        modify(context);

        if (len < 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw argumentError(context, "argument too big");

        int end = (int)(beg + len);
        if (end > realLength) {
            int valuesLength = values.length - begin;
            if (end >= valuesLength) realloc(context, end, valuesLength);
            realLength = end;
        }

        if (len > 0) {
            try {
                Arrays.fill(values, begin + beg, begin + end, item);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(context, ex);
            }
        }

        return this;
    }

    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, Block block) {
        unpack(context);
        modify(context);

        if (len < 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw argumentError(context, "argument too big");

        int end = (int)(beg + len);
        if (end > realLength) {
            int valuesLength = values.length - begin;
            if (end >= valuesLength) realloc(context, end, valuesLength);
            realLength = end;
        }

        for (int i = beg; i < end; i++) {
            IRubyObject v = block.yield(context, asFixnum(context, i));
            if (i >= realLength) break;
            safeArraySet(context, values, begin + i, v);
        }
        return this;
    }


    /** rb_ary_index
     *
     */
    public IRubyObject index(ThreadContext context, IRubyObject obj) {
        for (int i = 0; i < realLength; i++) {
            if (equalInternal(context, eltOk(i), obj)) return asFixnum(context, i);
        }

        return context.nil;
    }

    @JRubyMethod(name = {"index", "find_index"})
    public IRubyObject index(ThreadContext context, IRubyObject obj, Block unused) {
        if (unused.isGiven()) warn(context, "given block not used");
        return index(context, obj);
    }

    @JRubyMethod(name = {"index", "find_index"})
    public IRubyObject index(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "index");

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return asFixnum(context, i);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject bsearch(ThreadContext context, Block block) {
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, this, "bsearch");
        }

        int rVal = bsearch_index_internal(context, block);
        if (rVal == -1) {
            return context.nil;
        } else {
            return eltOk(rVal);
        }
    }

    @JRubyMethod
    public IRubyObject bsearch_index(ThreadContext context, Block block) {
        if (!block.isGiven()) {
            return enumeratorize(context.runtime, this, "bsearch_index");
        }
        int rVal = bsearch_index_internal(context, block);
        if (rVal == -1) {
            return context.nil;
        } else {
            return asFixnum(context, rVal);
        }
    }

    private int bsearch_index_internal(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;

        int low = 0, high = realLength, mid;
        boolean smaller = false, satisfied = false;
        IRubyObject v;
        CallSite op_cmp = null;

        while (low < high) {
            mid = low + ((high - low) / 2);
            v = block.yieldSpecific(context, eltOk(mid));

            if (v instanceof RubyFixnum fixnum) {
                long fixValue = fixnum.asLong(context);
                if (fixValue == 0) return mid;
                smaller = fixValue < 0;
            } else if (v == context.tru) {
                satisfied = true;
                smaller = true;
            } else if (v == context.fals || v == context.nil) {
                smaller = false;
            } else if (runtime.getNumeric().isInstance(v)) {
                if (op_cmp == null) op_cmp = sites(context).op_cmp_bsearch;
                var zero = asFixnum(context, 0);
                switch (RubyComparable.cmpint(context, op_cmp.call(context, v, v, zero), v, zero)) {
                    case 0: return mid;
                    case 1: smaller = true; break;
                    case -1: smaller = false;
                }
            } else {
                throw typeError(context, "wrong argument type ", v, " (must be numeric, true, false or nil");
            }
            if (smaller) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        if (low == realLength) return -1;
        if (!satisfied) return -1;
        return low;
    }

    /** rb_ary_rindex
     *
     */
    public IRubyObject rindex(ThreadContext context, IRubyObject obj) {
        unpack(context);
        
        int i = realLength;

        while (i-- > 0) {
            if (i > realLength) {
                i = realLength;
                continue;
            }
            if (equalInternal(context, eltOk(i), obj)) return asFixnum(context, i);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject rindex(ThreadContext context, IRubyObject obj, Block unused) {
        if (unused.isGiven()) warn(context, "given block not used");
        return rindex(context, obj);
    }

    @JRubyMethod
    public IRubyObject rindex(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "rindex");

        int i = realLength;

        while (i-- > 0) {
            if (i >= realLength) {
                i = realLength;
                continue;
            }
            if (block.yield(context, eltOk(i)).isTrue()) return asFixnum(context, i);
        }

        return context.nil;
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyArray#reverse_bang(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject reverse_bang() {
        return reverse_bang(getCurrentContext());
    }

    /** rb_ary_reverse_bang
     *
     */
    @JRubyMethod(name = "reverse!")
    public IRubyObject reverse_bang(ThreadContext context) {
        modify(context);

        try {
            if (realLength > 1) {
                int len = realLength;
                for (int i = 0; i < len >> 1; i++) {
                    T tmp = eltInternal(i);
                    eltInternalSet(i, eltInternal(len - i - 1));
                    eltInternalSet(len - i - 1, tmp);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }
        return this;
    }

    @Deprecated(since = "10.0")
    public IRubyObject reverse() {
        return reverse(getCurrentContext());
    }

    // MRI: rb_ary_reverse_m
    @JRubyAPI
    @JRubyMethod(name = "reverse")
    public IRubyObject reverse(ThreadContext context) {
        return realLength > 1 ? safeReverse() : aryDup();
    }

    protected RubyArray safeReverse() {
        int length = realLength;
        int myBegin = this.begin;
        IRubyObject[] myValues = this.values;
        IRubyObject[] vals = IRubyObject.array(length);

        final Ruby runtime = metaClass.runtime;
        try {
            for (int i = 0; i <= length >> 1; i++) {
                vals[i] = myValues[myBegin + length - i - 1];
                vals[length - i - 1] = myValues[myBegin + i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime.getCurrentContext(), e);
        }
        return new RubyArray(runtime, runtime.getArray(), vals);
    }

    /**
     * Collect the contents of this array after filtering through block, or return a new equivalent array if the block
     * is not given (!isGiven()).
     *
     * @param context the current context
     * @param block a block for filtering or NULL_BLOCK
     * @return an array of the filtered or unfiltered results
     */
    public RubyArray collectArray(ThreadContext context, Block block) {
        if (!block.isGiven()) return makeShared();

        RubyArray<?> ary = Create.newRawArray(context, realLength);

        for (int i = 0; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield (see JRUBY-5434)
            // arr[i] = ...
            ary.append(context, block.yieldNonArray(context, eltOk(i), null));
        }

        return ary.finishRawArray(context);
    }

    /**
     * Produce a new enumerator that will filter the contents of this array using {@link #collectArray(ThreadContext, Block)}.
     *
     * @param context the current context
     * @return an enumerator that will filter results
     */
    public RubyEnumerator collectEnum(ThreadContext context) {
        return enumWithSize(context, this, "collect", RubyArray::size);
    }

    /**
     * @see #collectArray(ThreadContext, Block)
     */
    public RubyArray collect(ThreadContext context, Block block) {
        return collectArray(context, block);
    }

    /** rb_ary_collect
     *
     */
    @JRubyMethod(name = {"collect"})
    public IRubyObject rbCollect(ThreadContext context, Block block) {
        return block.isGiven() ? collectArray(context, block) : collectEnum(context);
    }

    @JRubyMethod(name = {"map"})
    public IRubyObject map(ThreadContext context, Block block) {
        return block.isGiven() ? collectArray(context, block) : enumeratorizeWithSize(context, this, "map", RubyArray::size);
    }

    /** rb_ary_collect_bang
     *
     */
    public RubyArray collectBang(ThreadContext context, Block block) {
        if (!block.isGiven()) throw context.runtime.newLocalJumpErrorNoBlock();
        modify(context);

        for (int i = 0; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            storeInternal(context, i, block.yield(context, eltOk(i)));
        }

        return this;
    }

    /** rb_ary_collect_bang
    *
    */
    @JRubyMethod(name = "collect!")
    public IRubyObject collect_bang(ThreadContext context, Block block) {
        return block.isGiven() ? collectBang(context, block) : enumeratorizeWithSize(context, this, "collect!", RubyArray::size);
    }

    /** rb_ary_collect_bang
    *
    */
    @JRubyMethod(name = "map!")
    public IRubyObject map_bang(ThreadContext context, Block block) {
        return block.isGiven() ? collectBang(context, block) : enumeratorizeWithSize(context, this, "map!", RubyArray::size);
    }

    /** rb_ary_select
     *
     */
    public IRubyObject selectCommon(ThreadContext context, Block block) {
        final Ruby runtime = context.runtime;

        // Packed array logic
        switch (realLength) {
            case 1: {
                IRubyObject value = eltOk(0);
                if (block.yield(context, value).isTrue()) return new RubyArrayOneObject(runtime, value);
                return newEmptyArray(runtime);
            }
            case 2: {
                IRubyObject value = eltOk(0);
                boolean first = block.yield(context, value).isTrue();
                IRubyObject value2 = eltOk(1);
                boolean second = block.yield(context, value2).isTrue();
                if (first) {
                    if (second) return newArray(runtime, value, value2);
                    return newArray(runtime, value);
                } else if (second) {
                    return newArray(runtime, value2);
                }
                return newEmptyArray(runtime);
            }
        }

        RubyArray<?> result = Create.newRawArray(context, realLength);

        for (int i = 0; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield (see JRUBY-5434)
            IRubyObject value = eltOk(i);
            if (block.yield(context, value).isTrue()) result.append(context, value);
        }

        return result.finishRawArray(context);
    }

    @JRubyMethod(name = "select", alias = "filter")
    public IRubyObject select(ThreadContext context, Block block) {
        return block.isGiven() ? selectCommon(context, block) : enumeratorizeWithSize(context, this, "select", RubyArray::size);
    }

    @JRubyMethod(name = "select!", alias = "filter!")
    public IRubyObject select_bang(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select!", RubyArray::size);

        unpack(context);
        modify(context);

        boolean modified = false;
        int len0 = 0, len1 = 0;

        try {
            int i1, i2;
            for (i1 = i2 = 0; i1 < realLength; len0 = ++i1) {
                // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield (see JRUBY-5434)
                IRubyObject value = eltOk(i1);

                if (!block.yield(context, value).isTrue()) {
                    modified = true;
                    continue;
                }

                if (i1 != i2) eltSetOk(i2, (T) value);
                len1 = ++i2;
            }
            return (i1 == i2) ? context.nil : this;
        }
        finally {
            if (modified) checkFrozen();
            selectBangEnsure(context, len0, len1);
        }
    }

    @JRubyMethod
    public IRubyObject keep_if(ThreadContext context, Block block) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, this, "keep_if", RubyArray::size);
        }
        select_bang(context, block);
        return this;
    }

    @JRubyMethod
    public IRubyObject deconstruct(ThreadContext context) {
        return this;
    }

    /** rb_ary_delete
     *
     */
    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject item, Block block) {
        unpack(context);
        int i2 = 0;
        IRubyObject value = item;

        for (int i1 = 0; i1 < realLength; i1++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from equalInternal (see JRUBY-5434)
            IRubyObject e = safeArrayRef(context, values, begin + i1);
            if (equalInternal(context, e, item)) {
                value = e;
                continue;
            }
            if (i1 != i2) store(i2, e);
            i2++;
        }

        if (realLength == i2) return block.isGiven() ? block.yield(context, item) : context.nil;

        modify(context);

        final int myRealLength = this.realLength;
        final int myBegin = this.begin;
        final IRubyObject[] myValues = this.values;
        try {
            if (myRealLength > i2) {
                Helpers.fillNil(context, myValues, myBegin + i2, myBegin + myRealLength);
                this.realLength = i2;
                int valuesLength = myValues.length - myBegin;
                if (i2 << 1 < valuesLength && valuesLength > ARRAY_DEFAULT_SIZE) realloc(context, i2 << 1, valuesLength);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }

        return value;
    }

    /**
     * @param pos
     * @return ""
     * @deprecated Use {@link RubyArray#delete_at(ThreadContext, int)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject delete_at(int pos) {
        return delete_at(getCurrentContext(), pos);
    }

    /** rb_ary_delete_at
     *
     */
    public IRubyObject delete_at(ThreadContext context, int pos) {
        int len = realLength;
        if (pos >= len || (pos < 0 && (pos += len) < 0)) return context.nil;

        unpack(context);
        modify(context);

        try {
            IRubyObject obj = values[begin + pos];
            // fast paths for head and tail
            if (pos == 0) {
                values[begin] = context.nil;
                begin++;
                realLength--;
                return obj;
            } else if (pos == realLength - 1) {
                values[begin + realLength - 1] = context.nil;
                realLength--;
                return obj;
            }

            System.arraycopy(values, begin + pos + 1, values, begin + pos, len - (pos + 1));
            values[begin + len - 1] = context.nil;
            realLength--;

            return obj;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }
    }

    @Deprecated(since = "10.0")
    public IRubyObject delete_at(IRubyObject obj) {
        return delete_at(getCurrentContext(), obj);
    }

    /** rb_ary_delete_at_m
     *
     */
    @JRubyMethod(name = "delete_at")
    public IRubyObject delete_at(ThreadContext context, IRubyObject obj) {
        return delete_at(toInt(context, obj));
    }

    /** rb_ary_reject_bang
     *
     */
    public final IRubyObject rejectCommon(ThreadContext context, Block block) {
        var ary = Create.newArray(context);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);
            if (!block.yieldSpecific(context, v).isTrue()) {
                ary.push(context, v);
            }
        }
        return ary;
    }

    @JRubyMethod
    public IRubyObject reject(ThreadContext context, Block block) {
        return block.isGiven() ? rejectCommon(context, block) : enumeratorizeWithSize(context, this, "reject", RubyArray::size);
    }

    // MRI: ary_reject_bang and reject_bang_i
    public IRubyObject rejectBang(ThreadContext context, Block block) {
        unpack(context);
        modify(context);

        final int beg = begin;
        boolean modified = false;

        int len0 = 0, len1 = 0;
        try {
            int i1, i2;
            for (i1 = i2 = 0; i1 < realLength; len0 = ++i1) {
                final IRubyObject[] values = this.values;
                // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield (see JRUBY-5434)
                IRubyObject value = safeArrayRef(context, values, beg + i1);

                if (block.yield(context, value).isTrue()) {
                    modified = true;
                    continue;
                }

                if (i1 != i2) safeArraySet(context, values, beg + i2, value);

                len1 = ++i2;
            }

            return (i1 == i2) ? context.nil : this;
        } finally {
            if (modified) checkFrozen();
            selectBangEnsure(context, len0, len1);
        }
    }

    // MRI: select_bang_ensure
    private void selectBangEnsure(ThreadContext context, int i1, int i2) {
        int len = realLength;

        if (i2 < len && i2 < i1) {
            int tail = 0;
            int beg = begin;
            if (i1 < len) {
                tail = len - i1;
                safeArrayCopy(context, values, beg + i1, values, beg + i2, tail);
            }
            else if (realLength > 0) {
                // nil out left-overs to avoid leaks (MRI doesn't)
                try {
                    Helpers.fillNil(context, values, beg + i2, beg + i1);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    throw concurrentModification(context, ex);
                }
            }
            realLength = i2 + tail;
        }
    }

    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(ThreadContext context, Block block) {
        return block.isGiven() ? rejectBang(context, block) : enumeratorizeWithSize(context, this, "reject!", RubyArray::size);
    }

    /** rb_ary_delete_if
     *
     */
    public IRubyObject deleteIf(ThreadContext context, Block block) {
        rejectBang(context, block);
        return this;
    }

    @JRubyMethod
    public IRubyObject delete_if(ThreadContext context, Block block) {
        return block.isGiven() ? deleteIf(context, block) : enumeratorizeWithSize(context, this, "delete_if", RubyArray::size);
    }

    /** rb_ary_zip
     *
     */
    @JRubyMethod(optional = 1, rest = true, checkArity = false)
    public IRubyObject zip(ThreadContext context, IRubyObject[] args, Block block) {
        RubyClass array = arrayClass(context);
        ArraySites sites = sites(context);

        final IRubyObject[] newArgs = IRubyObject.array(args.length);

        boolean hasUncoercible = false;
        JavaSites.CheckedSites to_ary_checked = sites.to_ary_checked;
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = TypeConverter.convertToType(context, args[i], array, to_ary_checked, false);
            if (newArgs[i].isNil()) {
                hasUncoercible = true;
            }
        }

        // Handle uncoercibles by trying to_enum conversion
        if (hasUncoercible) {
            CallSite to_enum = sites.to_enum;
            RubySymbol each = asSymbol(context, "each");
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                if (!arg.respondsTo("each")) throw typeError(context, arg, "must respond to :each");
                newArgs[i] = to_enum.call(context, arg, arg, each);
            }
        }

        if (hasUncoercible) {
            return zipCommon(context, newArgs, block, (ctx, arg, i) -> RubyEnumerable.zipEnumNext(ctx, arg));
        } else {
            return zipCommon(context, newArgs, block, (ctx, arg, i) -> ((RubyArray) arg).elt(i));
        }
    }

    // This can be shared with RubyEnumerable to clean #zipCommon{Enum,Arg} a little
    public static interface ArgumentVisitor {
        IRubyObject visit(ThreadContext ctx, IRubyObject arg, int i);
    }

    private IRubyObject zipCommon(ThreadContext context, IRubyObject[] args, Block block, ArgumentVisitor visitor) {
        if (block.isGiven()) {
            for (int i = 0; i < realLength; i++) {
                IRubyObject[] tmp = IRubyObject.array(addBufferLength(context, args.length, 1));
                // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
                // See JRUBY-5434
                tmp[0] = eltInternal(i);
                for (int j = 0; j < args.length; j++) {
                    tmp[j + 1] = visitor.visit(context, args[j], i);
                }
                block.yield(context, newArrayMayCopy(context.runtime, tmp));
            }
            return context.nil;
        }

        IRubyObject[] result = IRubyObject.array(realLength);
        try {
            for (int i = 0; i < realLength; i++) {
                IRubyObject[] tmp = IRubyObject.array(addBufferLength(context, args.length, 1));
                tmp[0] = eltInternal(i);
                for (int j = 0; j < args.length; j++) {
                    tmp[j + 1] = visitor.visit(context, args[j], i);
                }
                result[i] = newArrayMayCopy(context.runtime, tmp);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
        return newArrayMayCopy(context.runtime, result);
    }

    /** rb_ary_cmp
     *
     */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject obj) {
        boolean isAnArray = (obj instanceof RubyArray) || obj.getMetaClass().getSuperClass() == arrayClass(context);

        if (!isAnArray && !sites(context).respond_to_to_ary.respondsTo(context, obj, obj, true)) {
            return context.nil;
        }

        RubyArray ary2;
        if (!isAnArray) {
            ary2 = (RubyArray) sites(context).to_ary.call(context, obj, obj);
        } else {
            ary2 = obj.convertToArray();
        }

        return cmpCommon(context, ary2);
    }

    private IRubyObject cmpCommon(ThreadContext context, RubyArray ary2) {
        if (this == ary2 || context.runtime.isInspecting(this)) return asFixnum(context, 0);

        try {
            context.runtime.registerInspecting(this);

            int len = realLength;
            if (len > ary2.realLength) len = ary2.realLength;

            CallSite cmp = sites(context).cmp;
            for (int i = 0; i < len; i++) {
                IRubyObject elt = elt(i);
                IRubyObject v = cmp.call(context, elt, elt, ary2.elt(i));
                if (!(v instanceof RubyFixnum fixnum) || fixnum.asLong(context) != 0) return v;
            }
        } finally {
            context.runtime.unregisterInspecting(this);
        }

        int len = realLength - ary2.realLength;
        int cmpValue = len == 0 ? 0 : (len > 0 ? 1 : -1);   // -1 ... 0 ... 1
        return asFixnum(context, cmpValue);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    @Deprecated(since = "9.4-", forRemoval = false)
    public IRubyObject slice_bang(IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return slice_bang(args[0]);
        case 2:
            return slice_bang(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getCurrentContext(), args.length, 1, 2);
            return null; // not reached
        }
    }

    private IRubyObject slice_internal(ThreadContext context, int pos, int len) {
        if (len < 0) return context.nil;
        int orig_len = realLength;
        if (pos < 0) {
            pos += orig_len;
            if (pos < 0) return context.nil;
        }

        if (orig_len < pos) return context.nil;
        if (orig_len < pos + len) len = orig_len - pos;

        if (len == 0) return Create.newEmptyArray(context);

        unpack(context);

        var result = makeShared(context, begin + pos, len, arrayClass(context));
        splice(context, pos, len, null, 0);

        return result;
    }

    /**
     * @param arg0
     * @return ""
     * @deprecated Use {@link RubyArray#slice_bang(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject slice_bang(IRubyObject arg0) {
        return slice_bang(getCurrentContext(), arg0);
    }

        /** rb_ary_slice_bang
         *
         */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0) {
        modifyCheck(context);

        if (arg0 instanceof RubyRange range) {
            if (!range.checkBegin(context, realLength)) return context.nil;

            int pos = checkInt(context, range.begLen0(context, realLength));
            int len = checkInt(context, range.begLen1(context, realLength, pos));
            return slice_internal(context, pos, len);
        }
        return delete_at(RubyNumeric.num2int(arg0));
    }

    /**
     * @param arg0
     * @param arg1
     * @return ""
     * @deprecated Use {@link RubyArray#slice_bang(ThreadContext, IRubyObject, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        return slice_bang(getCurrentContext(), arg0, arg1);
    }

    /** rb_ary_slice_bang
    *
    */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        modifyCheck(context);

        return slice_internal(context, RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1));
    }

    /** rb_ary_assoc
     *
     */
    @JRubyMethod(name = "assoc")
    public IRubyObject assoc(ThreadContext context, IRubyObject key) {
        Ruby runtime = context.runtime;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);
            if (v instanceof RubyArray) {
                RubyArray arr = (RubyArray)v;
                if (arr.realLength > 0 && equalInternal(context, arr.elt(0), key)) return arr;
            }
        }

        return context.nil;
    }

    /** rb_ary_rassoc
     *
     */
    @JRubyMethod(name = "rassoc")
    public IRubyObject rassoc(ThreadContext context, IRubyObject value) {
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = TypeConverter.checkArrayType(context, sites(context).to_ary_checked, eltOk(i));
            if (v instanceof RubyArray) {
                RubyArray arr = (RubyArray)v;
                if (arr.realLength > 1 && equalInternal(context, arr.eltOk(1), value)) return arr;
            }
        }

        return context.nil;
    }

    // MRI array.c flatten
    protected boolean flatten(ThreadContext context, final int level, final RubyArray result) {
        // TODO: (CON) We can flatten packed versions efficiently if length does not change (e.g. [[1,2],[]])
        unpack(context);

        IRubyObject tmp = null;
        int i = 0;
        for (; i < realLength; i++) {
            IRubyObject elt = eltOk(i);
            tmp = TypeConverter.checkArrayType(context.runtime, elt);
            if (!tmp.isNil()) break;
        }

        safeArrayCopy(context, values, begin, result.values, result.begin, i);
        result.realLength = i;

        if (i == realLength) return false;

        Stack<Object> stack = new Stack<>();
        stack.push(this);
        stack.push(i + 1);
        IdentityHashMap<RubyArray, IRubyObject> memo; // used as an IdentityHashSet

        RubyArray ary = (RubyArray) tmp;

        if (level < 0) {
            memo = new IdentityHashMap<>(4 + 1);
            memo.put(this, NEVER);
            memo.put((RubyArray) tmp, NEVER);
        } else {
            memo = null;
        }

        i = 0;

        try {
            while (true) {
                while (i < ary.realLength) {
                    IRubyObject elt = ary.eltOk(i++);
                    if (level >= 0 && stack.size() / 2 >= level) {
                        result.append(context, elt);
                        continue;
                    }
                    tmp = TypeConverter.checkArrayType(context, elt);
                    if (tmp.isNil()) {
                        result.append(context, elt);
                    } else { // nested array element
                        if (memo != null) {
                            if (memo.get(tmp) != null) throw argumentError(context, "tried to flatten recursive array");
                            memo.put(ary, NEVER);
                        }
                        stack.push(ary);
                        stack.push(i); // add (ary, i) pair
                        ary = (RubyArray) tmp;
                        i = 0;
                    }
                }
                if (stack.isEmpty()) break;
                if (memo != null) memo.remove(ary);

                i = (Integer) stack.pop();
                ary = (RubyArray) stack.pop();
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }

        return stack != null;
    }

    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(ThreadContext context) {
        unpack(context);
        modifyCheck(context);

        RubyArray result = new RubyArray(context.runtime, getType(), realLength);
        if (flatten(context, -1, result)) {
            modifyCheck(context);
            isShared = false;
            begin = 0;
            realLength = result.realLength;
            values = result.values;
            return this;
        }
        return context.nil;
    }

    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(ThreadContext context, IRubyObject arg) {
        unpack(context);
        modifyCheck(context);

        int level = RubyNumeric.num2int(arg);
        if (level == 0) return context.nil;

        RubyArray result = new RubyArray(context.runtime, getType(), realLength);
        if (flatten(context, level, result)) {
            isShared = false;
            begin = 0;
            realLength = result.realLength;
            values = result.values;
            return this;
        }
        return context.nil;
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context) {
        var result = new RubyArray(context.runtime, arrayClass(context), realLength);
        flatten(context, -1, result);
        return result;
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context, IRubyObject arg) {
        int level = RubyNumeric.num2int(arg);
        if (level == 0) return makeShared();

        RubyArray result = new RubyArray(context.runtime, arrayClass(context), realLength);
        flatten(context, level, result);
        return result;
    }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, Block block) {
        if (block.isGiven()) {
            int n = 0;
            for (int i = 0; i < realLength; i++) {
                if (block.yield(context, elt(i)).isTrue()) n++;
            }
            return asFixnum(context, n);
        } else {
            return asFixnum(context, realLength);
        }
    }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, IRubyObject obj, Block block) {
        if (block.isGiven()) warn(context, "given block not used");

        int n = 0;
        for (int i = 0; i < realLength; i++) {
            if (equalInternal(context, elt(i), obj)) n++;
        }
        return asFixnum(context, n);
    }

    /** rb_ary_nitems
     *
     */
    @JRubyMethod(name = "nitems")
    public IRubyObject nitems(ThreadContext context) {
        int n = 0;

        for (int i = 0; i < realLength; i++) {
            if (!eltOk(i).isNil()) n++;
        }

        return asFixnum(context, n);
    }

    @Deprecated
    public IRubyObject nitems() {
        return nitems(getCurrentContext());
    }

    /**
     * @param obj to be plussed
     * @return object
     * @deprecated Use {@link RubyArray#op_plus(ThreadContext, IRubyObject)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject op_plus(IRubyObject obj) {
        return op_plus(getCurrentContext(), obj);
    }

    /** rb_ary_plus
     *
     */
    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject obj) {
        RubyArray y = obj.convertToArray();
        int len = realLength + y.realLength;

        switch (len) {
            case 1:
                return new RubyArrayOneObject(context.runtime, realLength == 1 ? eltInternal(0) : y.eltInternal(0));
            case 2:
                switch (realLength) {
                    case 0:
                        return newArray(context.runtime, y.eltInternal(0), y.eltInternal(1));
                    case 1:
                        return newArray(context.runtime, eltInternal(0), y.eltInternal(0));
                    case 2:
                        return newArray(context.runtime, eltInternal(0), eltInternal(1));
                }
        }

        RubyArray<?> z = Create.newRawArray(context, len);
        try {
            copyInto(context, z.values, 0);
            y.copyInto(context, z.values, realLength);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }
        z.realLength = len;
        return z.finishRawArray(context);
    }

    /** rb_ary_times
     *
     */
    @JRubyMethod(name = "*")
    public IRubyObject op_times(ThreadContext context, IRubyObject times) {
        IRubyObject tmp = times.checkStringType();

        if (!tmp.isNil()) return join(context, tmp);

        long len = toLong(context, times);
        if (len == 0) return RubyArray.newEmptyArray(context.runtime);
        if (len < 0) throw argumentError(context, "negative argument");
        if (Long.MAX_VALUE / len < realLength) throw argumentError(context, "argument too big");

        len *= realLength;

        RubyArray<?> ary2 = Create.newRawArray(context, len);
        ary2.realLength = ary2.values.length;

        try {
            for (int i = 0; i < len; i += realLength) {
                copyInto(context, ary2.values, i);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }

        return ary2.finishRawArray(context);
    }

    /** ary_make_hash
     *
     */
    private RubyHash makeHash(Ruby runtime) {
        return makeHash(new RubyHash(runtime, Math.min(realLength, 128), false));
    }

    private RubyHash makeHash(RubyHash hash) {
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            hash.internalPutIfNoKey(v, v);
        }
        return hash;
    }

    private RubyHash makeHash(ThreadContext context, Block block) {
        return makeHash(context, new RubyHash(context.runtime, Math.min(realLength, 128), false), block);
    }

    private RubyHash makeHash(ThreadContext context, RubyHash hash, Block block) {
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            IRubyObject k = block.yield(context, v);
            hash.internalPutIfNoKey(k, v);
        }
        return hash;
    }

    private void setValuesFrom(ThreadContext context, RubyHash hash) {
        try {
            hash.visitAll(context, RubyHash.SetValueVisitor, this);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    private void clearValues(ThreadContext context, final int from, final int to) {
        try {
            Helpers.fillNil(context, values, begin + from, begin + to);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    /** rb_ary_uniq_bang
     *
     */
    public IRubyObject uniq_bang(ThreadContext context) {
        final RubyHash hash = makeHash(context.runtime);
        final int newLength = hash.size;
        if (realLength == newLength) return context.nil;

        modify(context); // in case array isShared
        unpack(context);

        setValuesFrom(context, hash);
        clearValues(context, newLength, realLength);
        realLength = newLength;

        return this;
    }

    @JRubyMethod(name = "uniq!")
    public IRubyObject uniq_bang(ThreadContext context, Block block) {
        modifyCheck(context);

        if (!block.isGiven()) return uniq_bang(context);

        final RubyHash hash = makeHash(context, block);
        final int newLength = hash.size;
        if (realLength == newLength) return context.nil;

        // after evaluating the block, a new modify check is needed
        modify(context); // in case array isShared
        unpack(context);

        setValuesFrom(context, hash);
        clearValues(context, newLength, realLength);
        realLength = newLength;

        return this;
    }

    /** rb_ary_uniq
     *
     */
    public IRubyObject uniq(ThreadContext context) {
        RubyHash hash = makeHash(context.runtime);
        final int newLength = hash.size;
        if (realLength == newLength) return makeShared();

        RubyArray result = newBlankArrayInternal(context.runtime, arrayClass(context), newLength);
        result.setValuesFrom(context, hash);
        result.realLength = newLength;
        return result;
    }

    @JRubyMethod(name = "uniq")
    public IRubyObject uniq(ThreadContext context, Block block) {
        if (!block.isGiven()) return uniq(context);
        RubyHash hash = makeHash(context, block);
        final int newLength = hash.size;
        if (realLength == newLength) return makeShared();

        RubyArray result = newBlankArrayInternal(context.runtime, arrayClass(context), newLength);
        result.setValuesFrom(context, hash);
        result.realLength = newLength;
        return result;
    }

    /**
     * @param other
     * @return ""
     * @deprecated Use {@link RubyArray#op_diff(ThreadContext, IRubyObject)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject op_diff(IRubyObject other) {
        return op_diff(getCurrentContext(), other);
    }
    /** rb_ary_diff
     *
     */
    @JRubyMethod(name = "-")
    public IRubyObject op_diff(ThreadContext context, IRubyObject other) {
        final int len = realLength;
        RubyArray<?> res = newBlankArrayInternal(context.runtime, len);

        int index = 0;
        RubyHash hash = other.convertToArray().makeHash(context.runtime);
        for (int i = 0; i < len; i++) {
            IRubyObject val = eltOk(i);
            if (hash.fastARef(val) == null) res.storeInternal(context, index++, val);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 0) return newEmptyArray(context.runtime);
        if (index == 1 && len == 2) return newArray(context.runtime, res.eltInternal(0));

        assert index == res.realLength;
        if (!(res instanceof RubyArraySpecialized)) {
            Helpers.fillNil(context, res.values, index, res.values.length);
        }

        return res;
    }

    /** rb_ary_difference_multi
     *
     */
    @JRubyMethod(name = "difference", rest = true)
    public IRubyObject difference(ThreadContext context, IRubyObject[] args) {
        BitSet isHash = new BitSet(args.length);
        RubyArray[] arrays = new RubyArray[args.length];
        RubyHash[] hashes = new RubyHash[args.length];

        var diff = Create.newArray(context);

        for (int i = 0; i < args.length; i++) {
            arrays[i] = args[i].convertToArray();
            isHash.set(i, (realLength > ARRAY_DEFAULT_SIZE && arrays[i].realLength > ARRAY_DEFAULT_SIZE));
            if (isHash.get(i)) hashes[i] = arrays[i].makeHash(context.runtime);
        }

        for (int i = 0; i < realLength; i++) {
            IRubyObject elt = elt(i);
            int j;
            for (j = 0; j < args.length; j++) {
                if (isHash.get(j)) {
                    if (hashes[j].fastARef(elt) != null) break;
                } else {
                    if (arrays[j].includesByEql(context, elt)) break;
                }
            }
            if (j == args.length) diff.append(context, elt);
        }

        return diff;
    }

    @JRubyMethod(rest = true)
    public IRubyObject intersection(ThreadContext context, IRubyObject[] args) {
        RubyArray result = aryDup();

        for (IRubyObject arg: args) {
            result = (RubyArray) result.op_and(arg);
        }

        return result;
    }

    /** MRI: rb_ary_intersect_p
     *
     */
    @JRubyMethod(name = "intersect?")
    public IRubyObject intersect_p(ThreadContext context, IRubyObject other) {

        RubyArray ary2 = other.convertToArray();
        final int len = realLength;

        if (len == 0 || ary2.realLength == 0) return context.fals;

        if (len <= SMALL_ARRAY_LEN && ary2.realLength <= SMALL_ARRAY_LEN) {
            for (int i = 0; i < len; i++) {
                if (ary2.includesByEql(context, elt(i))) return context.tru;
            }
            return context.fals;
        }

        RubyArray shorter = this;
        RubyArray longer = ary2;

        if (len > ary2.realLength) {
            longer = this;
            shorter = ary2;
        }

        RubyHash hash = shorter.makeHash(context.runtime);
        for (int i = 0; i < longer.realLength; i++) {
            IRubyObject val = longer.eltOk(i);
            if (hash.fastARef(val) != null) return context.tru;
        }

        return context.fals;
    }

    /**
     * @param other
     * @return ""
     * @deprecated Use {@link RubyArray#op_and(ThreadContext, IRubyObject)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject op_and(IRubyObject other) {
        return op_and(getCurrentContext(), other);
    }

    /** MRI: rb_ary_and
     *
     */
    @JRubyMethod(name = "&")
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        RubyArray<?> ary2 = other.convertToArray();

        final int len = realLength;
        int maxSize = len < ary2.realLength ? len : ary2.realLength;
        RubyArray res;
        switch (maxSize) {
            case 0:
                return newEmptyArray(context.runtime);
            case 1:
                if (len == 0 || ary2.realLength == 0) return newEmptyArray(context.runtime);
            default:
                res = newBlankArrayInternal(context.runtime, maxSize);
                break;
        }

        int index = 0;
        RubyHash hash = ary2.makeHash(context.runtime);
        for (int i = 0; i < len; i++) {
            IRubyObject val = elt(i);
            if (hash.fastDelete(val)) res.storeInternal(context, index++, val);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 0) return Create.newEmptyArray(context);
        if (index == 1 && maxSize == 2) return Create.newArray(context, res.eltInternal(0));

        assert index == res.realLength;
        if (!(res instanceof RubyArraySpecialized)) {
            Helpers.fillNil(context, res.values, index, res.values.length);
        }

        return res;
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_or(IRubyObject other) {
        return op_or(getCurrentContext(), other);
    }

    /** rb_ary_or
     *
     */
    @JRubyMethod(name = "|")
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        RubyArray ary2 = other.convertToArray();

        int maxSize = realLength + ary2.realLength;
        if (maxSize == 0) return Create.newEmptyArray(context);

        RubyHash set = ary2.makeHash(makeHash(context.runtime));
        RubyArray res = newBlankArrayInternal(context.runtime, set.size);
        res.setValuesFrom(context, set);
        res.realLength = set.size;

        int index = res.realLength;
        // if index is 1 and we made a size 2 array, repack
        if (index == 1 && maxSize == 2) return Create.newArray(context, res.eltInternal(0));

        return res;
    }

    /** rb_ary_union_multi
     *
     */
    @JRubyMethod(name = "union", rest = true)
    public IRubyObject union(ThreadContext context, IRubyObject[] args) {
        RubyArray[] arrays = new RubyArray[args.length];
        RubyArray result;

        int maxSize = realLength;
        for (int i = 0; i < args.length; i++) {
            arrays[i] = args[i].convertToArray();
            maxSize += arrays[i].realLength;
        }
        if (maxSize == 0) return Create.newEmptyArray(context);

        if (maxSize <= ARRAY_DEFAULT_SIZE) {
            result = Create.newArray(context);

            result.unionInternal(context, this);
            result.unionInternal(context, arrays);

            return result;
        }

        RubyHash set = makeHash(context.runtime);

        for (int i = 0; i < arrays.length; i++) {
            for (int j = 0; j < arrays[i].realLength; j++) {
                set.fastASet(arrays[i].elt(j), NEVER);
            }
        }

        result = set.keys();
        return result;
    }

    /** rb_ary_union
     *
     */
    private void unionInternal(ThreadContext context, RubyArray... args) {
        for (int i = 0; i < args.length; i++) {
            for (int j = 0; j < args[i].realLength; j++) {
                IRubyObject elt = args[i].elt(j);
                if (includesByEql(context, elt)) continue;
                append(context, elt);
            }
        }
    }

    /** rb_ary_sort
     *
     */
    @JRubyMethod(name = "sort")
    public RubyArray sort(ThreadContext context, Block block) {
        RubyArray ary = aryDup();
        ary.sort_bang(context, block);
        return ary;
    }

    /** rb_ary_sort_bang
     *
     */
    @JRubyMethod(name = "sort!")
    public IRubyObject sort_bang(ThreadContext context, Block block) {
        modify(context);
        if (realLength > 1) {
            return block.isGiven() ? sortInternal(context, block) : sortInternal(context, true);
        }
        return this;
    }

    protected IRubyObject sortInternal(final ThreadContext context, final boolean honorOverride) {
        try {
            Arrays.sort(values, begin, begin + realLength, new DefaultComparator(context, honorOverride) {
                protected int compareGeneric(IRubyObject o1, IRubyObject o2) {
                    int result = super.compareGeneric(o1, o2);
                    modifyCheck(context);
                    return result;
                }
            });
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
        return this;
    }

    // @Deprecated
    protected static int compareFixnums(RubyFixnum o1, RubyFixnum o2) {
        return DefaultComparator.compareInteger(o1, o2);
    }

    // @Deprecated
    protected static int compareOthers(ThreadContext context, IRubyObject o1, IRubyObject o2) {
        return DefaultComparator.compareGeneric(context, o1, o2);
    }

    public static class DefaultComparator implements Comparator<IRubyObject> {

        final ThreadContext context;

        private final boolean fixnumBypass;
        private final boolean stringBypass;

        public DefaultComparator(ThreadContext context) {
            this(context, true);
        }

        DefaultComparator(ThreadContext context, final boolean honorOverride) {
            this.context = context;
            if ( honorOverride && context != null ) {
                this.fixnumBypass = !honorOverride || fixnumClass(context).isMethodBuiltin("<=>");
                this.stringBypass = !honorOverride || stringClass(context).isMethodBuiltin("<=>");
            }
            else { // no-opt
                this.fixnumBypass = false;
                this.stringBypass = false;
            }
        }

        /*
        DefaultComparator(ThreadContext context, final boolean fixnumBypass, final boolean stringBypass) {
            this.context = context;
            this.fixnumBypass = fixnumBypass;
            this.stringBypass = stringBypass;
        } */

        public int compare(IRubyObject obj1, IRubyObject obj2) {
            if (fixnumBypass && obj1 instanceof RubyFixnum && obj2 instanceof RubyFixnum) {
                return compareInteger((RubyFixnum) obj1, (RubyFixnum) obj2);
            }
            if (stringBypass && obj1 instanceof RubyString && obj2 instanceof RubyString) {
                return compareString((RubyString) obj1, (RubyString) obj2);
            }
            return compareGeneric(obj1, obj2);
        }

        protected int compareGeneric(IRubyObject o1, IRubyObject o2) {
            final ThreadContext context = context();
            return compareGeneric(context, sites(context).op_cmp_sort, o1, o2);
        }

        protected ThreadContext context() {
            return context;
        }

        public static int compareInteger(RubyFixnum o1, RubyFixnum o2) {
            long a = o1.getValue();
            long b = o2.getValue();
            return a > b ? 1 : a == b ? 0 : -1;
        }

        public static int compareString(RubyString o1, RubyString o2) {
            return o1.op_cmp(o2);
        }

        public static int compareGeneric(ThreadContext context, IRubyObject o1, IRubyObject o2) {
            return compareGeneric(context, sites(context).op_cmp_sort, o1, o2);
        }

        public static int compareGeneric(ThreadContext context, CallSite op_cmp_sort, IRubyObject o1, IRubyObject o2) {
            IRubyObject ret = op_cmp_sort.call(context, o1, o1, o2);
            return RubyComparable.cmpint(context, ret, o1, o2);
        }

    }

    static class BlockComparator implements Comparator<IRubyObject> {

        final ThreadContext context;

        protected final Block block;
        protected final IRubyObject self;

        private final CallSite gt;
        private final CallSite lt;

        BlockComparator(ThreadContext context, Block block, CallSite gt, CallSite lt) {
            this(context, block, null, gt, lt);
        }

        BlockComparator(ThreadContext context, Block block, IRubyObject self, CallSite gt, CallSite lt) {
            this.context = context == null ? self.getRuntime().getCurrentContext() : context;
            this.block = block; this.self = self;
            this.gt = gt; this.lt = lt;
        }

        public int compare(IRubyObject obj1, IRubyObject obj2) {
            return RubyComparable.cmpint(context, gt, lt, yieldBlock(obj1, obj2), obj1, obj2);
        }

        protected final IRubyObject yieldBlock(IRubyObject obj1, IRubyObject obj2) {
            return block.yieldArray(context, Create.newArray(context, obj1, obj2), self);
        }

        protected final ThreadContext context() {
            return context;
        }

    }

    protected IRubyObject sortInternal(final ThreadContext context, final Block block) {
        // block code can modify, so we need to iterate
        unpack(context);
        IRubyObject[] newValues = IRubyObject.array(realLength);
        int length = realLength;

        copyInto(context, newValues, 0);
        CallSite gt = sites(context).op_gt_sort;
        CallSite lt = sites(context).op_lt_sort;
        Arrays.sort(newValues, 0, length, new BlockComparator(context, block, gt, lt) {
            @Override
            public int compare(IRubyObject obj1, IRubyObject obj2) {
                int result = super.compare(obj1, obj2);
                modifyCheck(context);
                return result;
            }
        });

        values = newValues;
        begin = 0;
        realLength = length;
        return this;
    }

    /** rb_ary_sort_by_bang
     *
     */
    @JRubyMethod(name = "sort_by!")
    public IRubyObject sort_by_bang(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "sort_by!", RubyArray::size);

        modifyCheck(context);

        RubyArray sorted = sites(context).sort_by.call(context, this, this, block).convertToArray();

        replace(sorted);

        return this;
    }

    /** rb_ary_take
     *
     */
    @JRubyMethod(name = "take")
    public IRubyObject take(ThreadContext context, IRubyObject n) {
        long len = toLong(context, n);
        if (len < 0) throw argumentError(context, "attempt to take negative size");

        return subseq(0, len);
    }

    /** rb_ary_take_while
     *
     */
    @JRubyMethod(name = "take_while")
    public IRubyObject take_while(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "take_while");

        int i = 0;
        for (; i < realLength; i++) {
            if (!block.yield(context, eltOk(i)).isTrue()) break;
        }
        return subseq(0, i);
    }

    /** rb_ary_take
     *
     */
    @JRubyMethod(name = "drop")
    public IRubyObject drop(ThreadContext context, IRubyObject n) {
        long pos = toLong(context, n);
        if (pos < 0) throw argumentError(context, "attempt to drop negative size");

        IRubyObject result = subseq(pos, realLength);
        return result.isNil() ? Create.newEmptyArray(context) : result;
    }

    /** rb_ary_take_while
     *
     */
    @JRubyMethod(name = "drop_while")
    public IRubyObject drop_while(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "drop_while");

        int i = 0;
        for (; i < realLength; i++) {
            if (!block.yield(context, eltOk(i)).isTrue()) break;
        }
        IRubyObject result = subseq(i, realLength);
        return result.isNil() ? Create.newEmptyArray(context) : result;
    }

    /** rb_ary_cycle
     *
     */
    @JRubyMethod(name = "cycle")
    public IRubyObject cycle(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "cycle", RubyArray::cycleSize);
        return cycleCommon(context, -1, block);
    }

    /** rb_ary_cycle
     *
     */
    @JRubyMethod(name = "cycle")
    public IRubyObject cycle(ThreadContext context, IRubyObject arg, Block block) {
        if (arg.isNil()) return cycle(context, block);
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "cycle", new IRubyObject[] {arg}, RubyArray::cycleSize);

        long times = toLong(context, arg);
        return times <= 0 ?
                context.nil :
                cycleCommon(context, times, block);
    }

    private IRubyObject cycleCommon(ThreadContext context, long n, Block block) {
        while (realLength > 0 && (n < 0 || 0 < n--)) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, eltOk(i));
            }
        }
        return context.nil;
    }

    /**
     * A cycle size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject cycleSize(ThreadContext context, RubyArray self, IRubyObject[] args) {
        if (self.realLength == 0) return asFixnum(context, 0);

        IRubyObject n = args != null && args.length > 0 ? args[0] : context.nil;

        if (n == null || n.isNil()) return asFloat(context, RubyFloat.INFINITY);

        long multiple = toLong(context, n);
        if (multiple <= 0) return asFixnum(context, 0);

        RubyFixnum length = self.length(context);
        return sites(context).op_times.call(context, length, length, asFixnum(context, multiple));
    }


    /** rb_ary_product
     *
     */
    public IRubyObject product(ThreadContext context, IRubyObject[] args) {
        return product(context, args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "product", rest = true)
    public IRubyObject product(ThreadContext context, IRubyObject[] args, Block block) {
        boolean useBlock = block.isGiven();

        int n = args.length + 1;
        RubyArray arrays[] = new RubyArray[n];
        int counters[] = new int[n];

        arrays[0] = this;
        var array = arrayClass(context);
        JavaSites.CheckedSites to_ary_checked = sites(context).to_ary_checked;
        for (int i = 1; i < n; i++) arrays[i] = (RubyArray) TypeConverter.convertToType(context, args[i - 1], array, to_ary_checked);

        int resultLen = 1;
        for (int i = 0; i < n; i++) {
            int k = arrays[i].realLength;
            int l = resultLen;
            if (k == 0) return useBlock ? this : Create.newEmptyArray(context);
            resultLen *= k;
            if (resultLen < k || resultLen < l || resultLen / k != l) {
                if (!block.isGiven()) throw rangeError(context, "too big to product");
            }
        }

        RubyArray result = useBlock ? null : newBlankArrayInternal(context.runtime, resultLen);

        for (int i = 0; i < resultLen; i++) {
            RubyArray sub = newBlankArrayInternal(context.runtime, n);
            for (int j = 0; j < n; j++) sub.eltInternalSet(j, arrays[j].entry(counters[j]));
            sub.realLength = n;

            if (useBlock) {
                block.yieldSpecific(context, sub);
            } else {
                result.eltInternalSet(i, sub);
            }
            int m = n - 1;
            counters[m]++;

            while (m > 0 && counters[m] == arrays[m].realLength) {
                counters[m] = 0;
                m--;
                counters[m]++;
            }
        }

        if (useBlock) return this;
        result.realLength = resultLen;
        return result;
    }

    /** rb_ary_combination
     *
     */
    @JRubyMethod(name = "combination")
    public IRubyObject combination(ThreadContext context, IRubyObject num, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "combination", new IRubyObject[]{num}, RubyArray::combinationSize);

        int n = RubyNumeric.num2int(num);

        if (n == 0) {
            block.yield(context, Create.newEmptyArray(context));
        } else if (n == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, Create.newArray(context, eltOk(i)));
            }
        } else if (n >= 0 && realLength >= n) {
            int stack[] = new int[n + 1];
            RubyArray values = makeShared();

            combinate(context, size(), n, stack, values, block);
        }

        return this;
    }

    private static void combinate(ThreadContext context, int len, int n, int[] stack, RubyArray values, Block block) {
        int lev = 0;

        Arrays.fill(stack, 1, 1 + n, 0);
        stack[0] = -1;
        for (;;) {
            for (lev++; lev < n; lev++) {
                stack[lev+1] = stack[lev]+1;
            }
            // TODO: MRI has a weird reentrancy check that depends on having a null class in values
            yieldValues(context, n, stack, 1, values, block);
            do {
                if (lev == 0) return;
                stack[lev--]++;
            } while (stack[lev+1]+n == len+lev+1);
        }
    }

    private static void rcombinate(ThreadContext context, int n, int r, int[] p, RubyArray values, Block block) {
        int i = 0, index = 0;

        p[index] = i;
        for (;;) {
            if (++index < r-1) {
                p[index] = i;
                continue;
            }
            for (; i < n; ++i) {
                p[index] = i;
                // TODO: MRI has a weird reentrancy check that depends on having a null class in values
                yieldValues(context, r, p, 0, values, block);
            }
            do {
                if (index <= 0) return;
            } while ((i = ++p[--index]) >= n);
        }
    }

    /**
     * A combination size method suitable for method reference implementation of SizeFn{@link #size(ThreadContext, RubyArray, IRubyObject[])}.
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject combinationSize(ThreadContext context, RubyArray self, IRubyObject[] args) {
        long n = self.realLength;
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #combination ensures arg[0] is numeric
        long k = ((RubyNumeric) args[0]).asLong(context);

        return binomialCoefficient(context, k, n);
    }

    private static IRubyObject binomialCoefficient(ThreadContext context, long comb, long size) {
        if (comb > size - comb) comb = size - comb;
        if (comb < 0) return asFixnum(context, 0);

        IRubyObject r = descendingFactorial(context, size, comb);
        IRubyObject v = descendingFactorial(context, comb, comb);
        return sites(context).op_quo.call(context, r, r, v);
    }

    @JRubyMethod(name = "repeated_combination")
    public IRubyObject repeatedCombination(ThreadContext context, IRubyObject num, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "repeated_combination", new IRubyObject[] { num }, RubyArray::repeatedCombinationSize);

        int n = RubyNumeric.num2int(num);

        if (n < 0) {
            // yield nothing
        } else if (n == 0) {
            block.yield(context, Create.newEmptyArray(context));
        } else if (n == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, Create.newArray(context, eltOk(i)));
            }
        } else {
            int[] p = new int[n];
            RubyArray values = makeShared();
            rcombinate(context, realLength, n, p, values, block);
        }

        return this;
    }

    /**
     * A repeated combination size method suitable for method reference implementation of SizeFn{@link #size(ThreadContext, RubyArray, IRubyObject[])}.
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject repeatedCombinationSize(ThreadContext context, RubyArray self, IRubyObject[] args) {
        long n = self.realLength;
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #repeated_combination ensures arg[0] is numeric
        long k = ((RubyNumeric) args[0]).asLong(context);

        return k == 0 ? asFixnum(context, 1) : binomialCoefficient(context, k, n + k - 1);
    }

    private static void permute(ThreadContext context, int n, int r, int[] p, boolean[] used, RubyArray values, Block block) {
        int i = 0, index = 0;
        for (;;) {
            int unused = Helpers.memchr(used, i, n - i, false);
            if (unused == -1) {
                if (index == 0) break;
                i = p[--index];
                used[i++] = false;
            } else {
                i = unused;
                p[index] = i;
                used[i] = true;
                index++;
                if (index < r - 1) {
                    p[index] = i = 0;
                    continue;
                }
                for (i = 0; i < n; i++) {
                    if (used[i]) continue;
                    p[index] = i;
                    // TODO: MRI has a weird reentrancy check that depends on having a null class in values
                    yieldValues(context, r, p, 0, values, block);
                }
            }
        }
    }

    private static void yieldValues(ThreadContext context, int r, int[] p, int pStart, RubyArray values, Block block) {
        RubyArray result = newBlankArrayInternal(context.runtime, r);
        for (int j = 0; j < r; j++) {
            result.eltInternalSet(j, values.eltInternal(p[j + pStart]));
        }
        result.realLength = r;

        block.yield(context, result);
    }

    private static void rpermute(ThreadContext context, int n, int r, int[] p, RubyArray values, Block block) {
        int index = 0;

        p[index] = 0;
        for (;;) {
            if (++index < r-1) {
                p[index] = 0;
                continue;
            }
            for (int i = 0; i < n; ++i) {
                p[index] = i;
                // TODO: MRI has a weird reentrancy check that depends on having a null class in values
                yieldValues(context, r, p, 0, values, block);
            }
            do {
                if (index <= 0) return;
            } while ((++p[--index]) >= n);
        }
    }

    /** rb_ary_permutation
     *
     */
    @JRubyMethod(name = "permutation")
    public IRubyObject permutation(ThreadContext context, IRubyObject num, Block block) {
        return block.isGiven() ? permutationCommon(context, RubyNumeric.num2int(num), false, block) : enumeratorizeWithSize(context, this, "permutation", new IRubyObject[] { num }, RubyArray::permutationSize);
    }

    @JRubyMethod(name = "permutation")
    public IRubyObject permutation(ThreadContext context, Block block) {
        return block.isGiven() ? permutationCommon(context, realLength, false, block) : enumeratorizeWithSize(context, this, "permutation", RubyArray::permutationSize);
    }

    @JRubyMethod(name = "repeated_permutation")
    public IRubyObject repeated_permutation(ThreadContext context, IRubyObject num, Block block) {
        return block.isGiven() ? permutationCommon(context, RubyNumeric.num2int(num), true, block) : enumeratorizeWithSize(context, this, "repeated_permutation", new IRubyObject[]{num}, RubyArray::repeatedPermutationSize);
    }

    /**
     * A repeated permutation size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject repeatedPermutationSize(ThreadContext context, RubyArray self, IRubyObject[] args) {
        RubyFixnum n = self.length(context);
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #repeated_permutation ensures arg[0] is numeric
        long k = ((RubyNumeric) args[0]).asLong(context);

        if (k < 0) return asFixnum(context, 0);

        RubyFixnum v = asFixnum(context, k);
        return sites(context).op_exp.call(context, n, n, v);
    }

    private IRubyObject permutationCommon(ThreadContext context, int r, boolean repeat, Block block) {
        if (r == 0) {
            block.yield(context, Create.newEmptyArray(context));
        } else if (r == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, Create.newArray(context, eltOk(i)));
            }
        } else if (r >= 0) {
            unpack(context);
            int n = realLength;
            if (repeat) {
                rpermute(context, n, r,
                        new int[r],
                        makeShared(context, begin, n, metaClass), block);
            } else {
                permute(context, n, r,
                        new int[r],
                        new boolean[n],
                        makeShared(context, begin, n, metaClass),
                        block);
            }
        }
        return this;
    }

    /**
     * A permutation size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject permutationSize(ThreadContext context, RubyArray self, IRubyObject[] args) {
        long n = self.realLength;
        long k;

        if (args != null && args.length > 0) {
            assert args[0] instanceof RubyNumeric; // #permutation ensures arg[0] is numeric
            k = ((RubyNumeric) args[0]).asLong(context);
        } else {
            k = n;
        }

        return descendingFactorial(context, n, k);
    }

    private static IRubyObject descendingFactorial(ThreadContext context, long from, long howMany) {
        IRubyObject cnt = asFixnum(context, howMany >= 0 ?  1 : 0);
        CallSite op_times = sites(context).op_times;
        while (howMany-- > 0) {
            cnt = op_times.call(context, cnt, cnt, asFixnum(context, from--));
        }
        return cnt;
    }

    @JRubyMethod(name = "shuffle!")
    public IRubyObject shuffle_bang(ThreadContext context) {
        return shuffleBang(context, context.runtime.getRandomClass());
    }

    @JRubyMethod(name = "shuffle!")
    public IRubyObject shuffle_bang(ThreadContext context, IRubyObject opts) {
        IRubyObject hash = TypeConverter.checkHashType(context.runtime, opts);
        if (hash.isNil()) throw argumentError(context, 1, 0, 0);

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");
        return ret == null ? shuffle(context) : shuffleBang(context, ret);
    }

    private IRubyObject shuffleBang(ThreadContext context, IRubyObject randgen) {
        modify(context);

        int i = realLength;
        int len = i;
        try {
            while (i > 0) {
                int r = (int) RubyRandom.randomLongLimited(context, randgen, i - 1);
                if (len != realLength) { // || ptr != RARRAY_CONST_PTR(ary)
                    throw runtimeError(context, "modified during shuffle");
                }
                T tmp = eltOk(--i);
                eltSetOk(i, eltOk(r));
                eltSetOk(r, tmp);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }

        return this;
    }


    @JRubyMethod(name = "shuffle")
    public IRubyObject shuffle(ThreadContext context) {
        RubyArray ary = aryDup();
        ary.shuffle_bang(context);
        return ary;
    }

    @JRubyMethod(name = "shuffle")
    public IRubyObject shuffle(ThreadContext context, IRubyObject opts) {
        RubyArray ary = aryDup();
        ary.shuffle_bang(context, opts);
        return ary;
    }

    private static final int SORTED_THRESHOLD = 10;

    @JRubyMethod(name = "sample")
    public IRubyObject sample(ThreadContext context) {
        return sampleCommon(context, randomClass(context));
    }

    @JRubyMethod(name = "sample")
    public IRubyObject sample(ThreadContext context, IRubyObject sampleOrOpts) {
        IRubyObject hash = TypeConverter.checkHashType(context.runtime, sampleOrOpts);
        if (hash.isNil()) return sampleCommon(context, sampleOrOpts, randomClass(context));

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");

        return sampleCommon(context, ret != null ? ret : randomClass(context));
    }

    @JRubyMethod(name = "sample")
    public IRubyObject sample(ThreadContext context, IRubyObject sample, IRubyObject opts) {
        IRubyObject hash = TypeConverter.checkHashType(context.runtime, opts);
        if (hash.isNil()) throw argumentError(context, 2, 0, 1);

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");
        return sampleCommon(context, sample, ret != null ? ret : randomClass(context));
    }

    /**
     * Common sample logic when no sample size was specified.
     */
    private IRubyObject sampleCommon(ThreadContext context, IRubyObject randgen) {
        if (realLength == 0) return context.nil;

        return eltOk(realLength == 1 ? 0 : RubyRandom.randomLongLimited(context, randgen, realLength - 1));
    }

    /**
     * Common sample logic when a sample size was specified.
     */
    private IRubyObject sampleCommon(ThreadContext context, IRubyObject sample, IRubyObject randgen) {
        int n = RubyNumeric.num2int(sample);

        try {
            if (n < 0) throw argumentError(context, "negative sample number");
            if (n > realLength) n = realLength;

            long[] rnds = new long[SORTED_THRESHOLD];
            if (n <= SORTED_THRESHOLD) {
                for (int idx = 0; idx < n; ++idx) {
                    rnds[idx] = RubyRandom.randomLongLimited(context, randgen, realLength - idx - 1);
                }
            }

            int i, j, k;
            switch (n) {
            case 0:
                return Create.newEmptyArray(context);
            case 1:
                return realLength <= 0 ? Create.newEmptyArray(context) : Create.newArray(context, eltOk((int) rnds[0]));
            case 2:
                i = (int) rnds[0];
                j = (int) rnds[1];

                if (j >= i) j++;

                return Create.newArray(context, eltOk(i), eltOk(j));
            case 3:
                i = (int) rnds[0];
                j = (int) rnds[1];
                k = (int) rnds[2];

                int l = j, g = i;

                if (j >= i) {
                    l = i;
                    g = ++j;
                }

                if (k >= l && (++k >= g)) ++k;

                return Create.newArray(context, eltOk(i), eltOk(j), eltOk(k));
            }

            int len = realLength;

            if (n > len) n = len;
            if (n < SORTED_THRESHOLD) {
                int idx[] = new int[SORTED_THRESHOLD];
                int sorted[] = new int[SORTED_THRESHOLD];
                sorted[0] = idx[0] = (int) rnds[0];
                for (i = 1; i < n; i++) {
                    k = (int) rnds[i];
                    for (j = 0; j < i; j++, k++) {
                        if (k < sorted[j]) break;
                    }
                    System.arraycopy(sorted, j, sorted, j + 1, i - j);
                    sorted[j] = idx[i] = k;
                }
                IRubyObject[] result = IRubyObject.array(n);
                for (i = 0; i < n; i++) {
                    result[i] = eltOk(idx[i]);
                }
                return RubyArray.newArrayMayCopy(context.runtime, result);
            } else {
                IRubyObject[] result = IRubyObject.array(len);
                System.arraycopy(values, begin, result, 0, len);
                for (i = 0; i < n; i++) {
                    j = (int) RubyRandom.randomLongLimited(context, randgen, len - i - 1) + i;
                    IRubyObject tmp = result[j];
                    result[j] = result[i];
                    result[i] = tmp;
                }
                RubyArray<?> ary = newArrayNoCopy(context.runtime, result);
                ary.realLength = n;
                return ary;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    private static void aryReverse(IRubyObject[] _p1, int p1, IRubyObject[] _p2, int p2) {
        while(p1 < p2) {
            IRubyObject tmp = _p1[p1];
            _p1[p1++] = _p2[p2];
            _p2[p2--] = tmp;
        }
    }

    protected IRubyObject internalRotateBang(ThreadContext context, int cnt) {
        modify(context);

        try {
            if(cnt != 0) {
                IRubyObject[] ptr = values;
                int len = realLength;

                if(len > 0 && (cnt = rotateCount(cnt, len)) > 0) {
                    --len;
                    if(cnt < len) aryReverse(ptr, begin + cnt, ptr, begin + len);
                    if(--cnt > 0) aryReverse(ptr, begin, ptr, begin + cnt);
                    if(len > 0)   aryReverse(ptr, begin, ptr, begin + len);
                    return this;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }

        return context.nil;
    }

    private static int rotateCount(int cnt, int len) {
        return (cnt < 0) ? (len - (~cnt % len) - 1) : (cnt % len);
    }

    protected IRubyObject internalRotate(ThreadContext context, int cnt) {
        int len = realLength;
        RubyArray rotated = aryDup();
        rotated.modify(context);

        try {
            if(len > 0) {
                cnt = rotateCount(cnt, len);
                IRubyObject[] ptr = this.values;
                IRubyObject[] ptr2 = rotated.values;
                len -= cnt;
                System.arraycopy(ptr, begin + cnt, ptr2, 0, len);
                System.arraycopy(ptr, begin, ptr2, len, cnt);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }

        return rotated;
    }

    @JRubyMethod(name = "rotate!")
    public IRubyObject rotate_bang(ThreadContext context) {
        internalRotateBang(context, 1);
        return this;
    }

    @JRubyMethod(name = "rotate!")
    public IRubyObject rotate_bang(ThreadContext context, IRubyObject cnt) {
        internalRotateBang(context, RubyNumeric.fix2int(cnt));
        return this;
    }

    @JRubyMethod(name = "rotate")
    public IRubyObject rotate(ThreadContext context) {
        return internalRotate(context, 1);
    }

    @JRubyMethod(name = "rotate")
    public IRubyObject rotate(ThreadContext context, IRubyObject cnt) {
        return internalRotate(context, RubyNumeric.fix2int(cnt));
    }

    @JRubyMethod(name = "all?")
    public IRubyObject all_p(ThreadContext context, Block block) {
        return all_pCommon(context, null, block);
    }

    @JRubyMethod(name = "all?")
    public IRubyObject all_p(ThreadContext context, IRubyObject arg, Block block) {
        return all_pCommon(context, arg, block);
    }

    public IRubyObject all_pCommon(ThreadContext context, IRubyObject arg, Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.all_pCommon(context, self_each, this, arg, block);
        boolean patternGiven = arg != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");
        if (!block.isGiven() || patternGiven) return all_pBlockless(context, arg);

        for (int i = 0; i < realLength; i++) {
            if (!block.yield(context, eltOk(i)).isTrue()) return context.fals;
        }

        return context.tru;
    }

    private IRubyObject all_pBlockless(ThreadContext context, IRubyObject pattern) {
        if (pattern == null) {
            for (int i = 0; i < realLength; i++) {
                if (!eltOk(i).isTrue()) return context.fals;
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (!(pattern.callMethod(context, "===", eltOk(i)).isTrue())) return context.fals;
            }
        }

        return context.tru;
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        return any_pCommon(context, null, block);
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, IRubyObject arg, Block block) {
        return any_pCommon(context, arg, block);
    }

    public IRubyObject any_pCommon(ThreadContext context, IRubyObject arg, Block block) {
        if (isEmpty()) return context.fals;
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.any_pCommon(context, self_each, this, arg, block);
        boolean patternGiven = arg != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");

        if (!block.isGiven() || patternGiven) return any_pBlockless(context, arg);

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return context.tru;
        }

        return context.fals;
    }

    private IRubyObject any_pBlockless(ThreadContext context, IRubyObject pattern) {
        if (pattern == null) {
            for (int i = 0; i < realLength; i++) {
                if (eltOk(i).isTrue()) return context.tru;
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (pattern.callMethod(context, "===", eltOk(i)).isTrue()) return context.tru;
            }
        }

        return context.fals;
    }

    @JRubyMethod(name = "none?")
    public IRubyObject none_p(ThreadContext context, Block block) {
        return none_pCommon(context, null, block);
    }

    @JRubyMethod(name = "none?")
    public IRubyObject none_p(ThreadContext context, IRubyObject arg, Block block) {
        return none_pCommon(context, arg, block);
    }

    public IRubyObject none_pCommon(ThreadContext context, IRubyObject arg, Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.none_pCommon(context, self_each, this, arg, block);
        boolean patternGiven = arg != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");
        if (!block.isGiven() || patternGiven) return none_pBlockless(context, arg);

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return context.fals;
        }

        return context.tru;
    }

    private IRubyObject none_pBlockless(ThreadContext context, IRubyObject pattern) {
        if (pattern == null) {
            for (int i = 0; i < realLength; i++) {
                if (eltOk(i).isTrue()) return context.fals;
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (pattern.callMethod(context, "===", eltOk(i)).isTrue()) return context.fals;
            }
        }

        return context.tru;
    }

    @JRubyMethod(name = "one?")
    public IRubyObject one_p(ThreadContext context, Block block) {
        return one_pCommon(context, null, block);
    }

    @JRubyMethod(name = "one?")
    public IRubyObject one_p(ThreadContext context, IRubyObject arg, Block block) {
        return one_pCommon(context, arg, block);
    }

    public IRubyObject one_pCommon(ThreadContext context, IRubyObject arg, Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.one_pCommon(context, self_each, this, arg, block);
        boolean patternGiven = arg != null;

        if (block.isGiven() && patternGiven) warn(context, "given block not used");
        if (!block.isGiven() || patternGiven) return one_pBlockless(context, arg);

        boolean found = false;
        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) {
                if (found) return context.fals;
                found = true;
            }
        }

        return found ? context.tru : context.fals;
    }

    private IRubyObject one_pBlockless(ThreadContext context, IRubyObject pattern) {
        boolean found = false;

        if (pattern == null) {
            for (int i = 0; i < realLength; i++) {
                if (eltOk(i).isTrue()) {
                    if (found) return context.fals;
                    found = true;
                }
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (pattern.callMethod(context, "===", eltOk(i)).isTrue()) {
                    if (found) return context.fals;
                    found = true;
                }
            }
        }

        return found ? context.tru : context.fals;
    }

    @JRubyMethod
    public IRubyObject sum(final ThreadContext context, final Block block) {
        RubyFixnum zero = asFixnum(context, 0);
        CachingCallSite self_each = sites(context).self_each;

        return !self_each.isBuiltin(this) ?
                RubyEnumerable.sumCommon(context, self_each, this, zero, block) :
                sumCommon(context, zero, block);
    }

    @JRubyMethod
    public IRubyObject sum(final ThreadContext context, IRubyObject init, final Block block) {
        CachingCallSite self_each = sites(context).self_each;
        return !self_each.isBuiltin(this) ?
                RubyEnumerable.sumCommon(context, self_each, this, init, block) :
                sumCommon(context, init, block);
    }

    public IRubyObject sumCommon(final ThreadContext context, IRubyObject init, final Block block) {
        IRubyObject result = init;

        /*
         * This state machine is simple: it assumes all elements are the same type,
         * and transitions to a later state when that assumption fails.
         *
         * The order of states is:
         *
         * - is_fixnum =&gt; { is_bignum | is_rational | is_float | other }
         * - is_bignum =&gt; { is_rational | is_float | other }
         * - is_rational =&gt; { is_float | other }
         * - is_float =&gt; { other }
         * - other [terminal]
         */
        boolean is_fixnum=false, is_bignum=false, is_rational=false, is_float=false;

        if (result instanceof RubyFixnum) {
            is_fixnum = true;
        } else if (result instanceof RubyBignum) {
            is_bignum = true;
        } else if (result instanceof RubyRational) {
            is_rational = true;
        } else if (result instanceof RubyFloat) {
            is_float = true;
        }

        int i = 0;
        IRubyObject value = null;

        if (is_fixnum) {
            long sum = ((RubyFixnum) result).value;
fixnum_loop:
            for (; i < realLength; value=null, i++) {
                if (value == null) {
                    value = eltOk(i);
                    if (block.isGiven()) {
                        value = block.yield(context, value);
                    }
                }

                if (value instanceof RubyFixnum) {
                    /* should not overflow long type */
                    try {
                        sum = Math.addExact(sum, ((RubyFixnum) value).value);
                    } catch (ArithmeticException ae) {
                        is_bignum = true;
                        break fixnum_loop;
                    }
                } else if (value instanceof RubyBignum) {
                    is_bignum = true;
                    break fixnum_loop;
                } else if (value instanceof RubyRational) {
                    is_rational = true;
                    break fixnum_loop;
                } else {
                    is_float = value instanceof RubyFloat;
                    break fixnum_loop;
                }
            }

            if (is_bignum) {
                result = RubyBignum.newBignum(context.runtime, sum);
            } else if (is_rational) {
                result = RubyRational.newRational(context.runtime, sum, 1);
            } else if (is_float) {
                result = asFloat(context, (double) sum);
            } else {
                result = asFixnum(context, sum);
            }
        }
        if (is_bignum) {
            BigInteger sum = ((RubyBignum) result).value;
bignum_loop:
            for (; i < realLength; value=null, i++) {
                if (value == null) {
                    value = eltOk(i);
                    if (block.isGiven()) {
                        value = block.yield(context, value);
                    }
                }

                if (value instanceof RubyFixnum) {
                    final long val = ((RubyFixnum) value).value;
                    sum = sum.add(BigInteger.valueOf(val));
                } else if (value instanceof RubyBignum) {
                    sum = sum.add(((RubyBignum) value).value);
                } else if (value instanceof RubyRational) {
                    is_rational = true;
                    break bignum_loop;
                } else {
                    is_float = value instanceof RubyFloat;
                    break bignum_loop;
                }
            }

            if (is_rational) {
                result = RubyRational.newInstance(context, RubyBignum.newBignum(context.runtime, sum));
            } else if (is_float) {
                result = asFloat(context, sum.doubleValue());
            } else {
                result = RubyBignum.newBignum(context.runtime, sum);
            }
        }
        if (is_rational) {
rational_loop:
            for (; i < realLength; value=null, i++) {
                if (value == null) {
                    value = eltOk(i);
                    if (block.isGiven()) {
                        value = block.yield(context, value);
                    }
                }

                if (value instanceof RubyFixnum || value instanceof RubyBignum || value instanceof RubyRational) {
                    if (result instanceof RubyInteger) {
                        result = ((RubyInteger) result).op_plus(context, value);
                    } else if (result instanceof RubyRational) {
                        result = ((RubyRational) result).op_plus(context, value);
                    } else {
                        throw typeError(context, "BUG: unexpected type in rational part of Array#sum");
                    }
                } else if (value instanceof RubyFloat) {
                    result = asFloat(context, ((RubyRational) result).asDouble(context));
                    is_float = true;
                    break rational_loop;
                } else {
                    break rational_loop;
                }
            }
        }
        if (is_float) {
            /*
             * Kahan-Babuska balancing compensated summation algorithm
             * See http://link.springer.com/article/10.1007/s00607-005-0139-x
             */
            double f = ((RubyFloat) result).value;
            double c = 0.0;
            double x, t;
float_loop:
            for (; i < realLength; value=null, i++) {
                if (value == null) {
                    value = eltOk(i);
                    if (block.isGiven()) {
                        value = block.yield(context, value);
                    }
                }

                if (!(value instanceof RubyNumeric num)) break;
                x = num.asDouble(context);

                if (Double.isNaN(f)) continue;
                if (Double.isNaN(x)) {
                    f = x;
                    continue;
                }
                if (Double.isInfinite(x)) {
                    if (Double.isInfinite(f) && Math.signum(x) != Math.signum(f))
                        f = Double.NaN;
                    else
                        f = x;
                    continue;
                }
                if (Double.isInfinite(f)) continue;

                t = f + x;
                if (Math.abs(f) >= Math.abs(x)) {
                    c += ((f - t) + x);
                } else {
                    c += ((x - t) + f);
                }
                f = t;
            }
            f += c;

            result = asFloat(context, f);
        }
//object_loop:
        for (; i < realLength; value=null, i++) {
            if (value == null) {
                value = eltOk(i);
                if (block.isGiven()) {
                    value = block.yield(context, value);
                }
            }

            result = result.callMethod(context, "+", value);
        }

        return result;
    }

    public IRubyObject find(ThreadContext context, IRubyObject ifnone, Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.detectCommon(context, self_each, this, block);

        return detectCommon(context, ifnone, block);
    }

    public IRubyObject find_index(ThreadContext context, Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.find_indexCommon(context, self_each, this, block, Signature.OPTIONAL);

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return asFixnum(context, i);
        }

        return context.nil;
    }

    public IRubyObject find_index(ThreadContext context, IRubyObject cond) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.find_indexCommon(context, self_each, this, cond);

        for (int i = 0; i < realLength; i++) {
            if (eltOk(i).equals(cond)) return asFixnum(context, i);
        }

        return context.nil;
    }

    public IRubyObject detectCommon(ThreadContext context, IRubyObject ifnone, Block block) {
        for (int i = 0; i < realLength; i++) {
            IRubyObject value = eltOk(i);

            if (block.yield(context, value).isTrue()) return value;
        }

        return ifnone != null && !ifnone.isNil() ? sites(context).call.call(context, ifnone, ifnone) : context.nil;
    }

    public static void marshalTo(RubyArray array, MarshalStream output) throws IOException {
        output.registerLinkTarget(array);

        int length = array.realLength;

        output.writeInt(length);
        try {
            for (int i = 0; i < length; i++) {
                output.dumpObject(array.eltInternal(i));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(array.getRuntime().getCurrentContext(), ex);
        }
    }

    public static void marshalTo(RubyArray array, NewMarshal output, ThreadContext context, NewMarshal.RubyOutputStream out) {
        output.registerLinkTarget(array);

        int length = array.realLength;

        output.writeInt(out, length);
        try {
            for (int i = 0; i < length; i++) {
                output.dumpObject(context, out, array.eltInternal(i));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(array.getRuntime().getCurrentContext(), ex);
        }
    }

    public static RubyArray unmarshalFrom(UnmarshalStream input) throws IOException {
        int size = input.unmarshalInt();
        var context = input.getRuntime().getCurrentContext();

        // FIXME: We used to use newArrayBlankInternal but this will not hash into a HashSet without an NPE.
        // we create this now with an empty, nulled array so it's available for links in the marshal data
        var result = (RubyArray<?>) input.entry(Create.newRawArray(context, size));

        for (int i = 0; i < size; i++) {
            result.append(context, input.unmarshalObject());
        }

        return result.finishRawArray(context);
    }

    @Deprecated(since = "10.0")
    public static RubyArray newBlankArray(Ruby runtime, int size) {
        return newBlankArray(runtime.getCurrentContext(), size);
    }

    /**
     * Construct the most efficient array shape for the given size. This should only be used when you
     * intend to populate all elements, since the packed arrays will be born with a nonzero size and
     * would have to be unpacked to partially populate.
     *
     * We nil-fill all cases, to ensure nulls will never leak out if there's an unpopulated element
     * or an index accessed before assignment.
     *
     * @param context the current thread context
     * @param size the size
     * @return a RubyArray shaped for the given size
     */
    public static RubyArray newBlankArray(ThreadContext context, int size) {
        switch (size) {
            case 0:
                return Create.newEmptyArray(context);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(context.runtime, context.nil);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(context.runtime, context.nil, context.nil);
                break;
        }

        return newArray(context, size);
    }

    // when caller is sure to set all elements (avoids nil elements initialization)
    static RubyArray newBlankArrayInternal(Ruby runtime, int size) {
        return newBlankArrayInternal(runtime, runtime.getArray(), size);
    }

    // when caller is sure to set all elements (avoids nil elements initialization)
    static RubyArray newBlankArrayInternal(Ruby runtime, RubyClass metaClass, int size) {
        switch (size) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(metaClass, null);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(metaClass, null, null);
                break;
        }

        return new RubyArray(runtime, metaClass, size);
    }

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return arg.checkArrayType();
    }

    @JRubyMethod(name = "pack")
    public RubyString pack(ThreadContext context, IRubyObject obj) {
        RubyString format = obj.convertToString();
        try {
            RubyString buffer = newString(context, new ByteList());
            return Pack.pack(context, this, format, buffer);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }
    }

    @JRubyMethod(name = "pack")
    public RubyString pack(ThreadContext context, IRubyObject obj, IRubyObject maybeOpts) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context, maybeOpts);
        IRubyObject buffer = null;

        if (opts != context.nil) {
            buffer = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "buffer");
            if (buffer == context.nil) buffer = null;
            if (buffer != null && !(buffer instanceof RubyString)) {
                throw typeError(context, "buffer must be String, not ", buffer, "");
            }
        }

        if (buffer==null) buffer = newString(context, new ByteList());

        return Pack.pack(context, this, obj.convertToString(), (RubyString) buffer);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0) {
        return at( arg0 );
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        final IRubyObject val = at( arg0 );
        return RubyObject.dig1(context, val, arg1);
    }

    @JRubyMethod(name = "dig")
    public IRubyObject dig(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final IRubyObject val = at( arg0 );
        return RubyObject.dig2(context, val, arg1, arg2);
    }

    @JRubyMethod(name = "dig", required = 1, rest = true, checkArity = false)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);

        final IRubyObject val = at( args[0] );
        return argc == 1 ? val : RubyObject.dig(context, val, args, 1);
    }

    private IRubyObject maxWithBlock(ThreadContext context, Block block) {
        IRubyObject result = UNDEF;
        ArraySites sites = sites(context);
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF ||
                    RubyComparable.cmpint(context, op_gt, op_lt, block.yieldArray(context, Create.newArray(context, v, result), null), v, result) > 0) {
                result = v;
            }
        }

        return result == UNDEF ? context.nil : result;
    }

    @JRubyMethod(name = "max")
    public IRubyObject max(ThreadContext context, Block block) {
        if (block.isGiven()) return maxWithBlock(context, block);
        if (realLength < 1) return context.nil;

        IRubyObject result = UNDEF;
        ArraySites sites = sites(context);
        CachingCallSite op_cmp = sites.op_cmp_minmax;
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        int generation = getArg0Generation(op_cmp);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF || optimizedCmp(context, v, result, generation, op_cmp, op_gt, op_lt) > 0) {
                result = v;
            }
        }

        return result == UNDEF ? context.nil : result;
    }

    @JRubyMethod(name = "max")
    public IRubyObject max(ThreadContext context, IRubyObject num, Block block) {
        if (!num.isNil()) {
            return RubyEnumerable.max(context, this, num, block);
        }

        return max(context, block);
    }

    private IRubyObject minWithBlock(ThreadContext context, Block block) {
        IRubyObject result = UNDEF;
        ArraySites sites = sites(context);
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF ||
                    RubyComparable.cmpint(context, op_gt, op_lt, block.yieldArray(context, Create.newArray(context, v, result), null), v, result) < 0) {
                result = v;
            }
        }

        return result == UNDEF ? context.nil : result;
    }

    @JRubyMethod(name = "min")
    public IRubyObject min(ThreadContext context, Block block) {
        if (block.isGiven()) return minWithBlock(context, block);

        if (realLength == 0) {
            return context.nil;
        }

        if (realLength == 1) {
            return eltInternal(0);
        }

        IRubyObject result = UNDEF;
        ArraySites sites = sites(context);
        CachingCallSite op_cmp = sites.op_cmp_minmax;
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        int generation = getArg0Generation(op_cmp);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF || optimizedCmp(context, v, result, generation, op_cmp, op_gt, op_lt) < 0) {
                result = v;
            }
        }

        return result == UNDEF ? context.nil : result;
    }

    private int getArg0Generation(CachingCallSite op_cmp) {
        IRubyObject arg0 = eltInternal(0);
        RubyClass metaclass = arg0.getMetaClass();
        CacheEntry entry = op_cmp.retrieveCache(metaclass);
        int generation = -1;

        if (entry.method.isBuiltin()) {
            generation = entry.token;
        }
        return generation;
    }

    @JRubyMethod(name = "min")
    public IRubyObject min(ThreadContext context, IRubyObject num, Block block) {
        return num.isNil() ?
                min(context, block) :
                RubyEnumerable.min(context, this, num, block);
    }

    @JRubyMethod
    public IRubyObject minmax(ThreadContext context, Block block) {
        return block.isGiven() ?
                Helpers.invokeSuper(context, this, arrayClass(context), "minmax", NULL_ARRAY, block) :
                Create.newArray(context, callMethod("min"), callMethod("max"));
    }

    private static final int optimizedCmp(ThreadContext context, IRubyObject a, IRubyObject b, int token,
                                          CachingCallSite op_cmp, CallSite op_gt, CallSite op_lt) {
        if (token == ((RubyBasicObject) a).metaClass.generation) {
            if (a instanceof RubyFixnum aa && b instanceof RubyFixnum bb) return Long.compare(aa.asLong(context), bb.asLong(context));
            if (a instanceof RubyString aa && b instanceof RubyString bb) return aa.op_cmp(bb);
        }

        return RubyComparable.cmpint(context, op_gt, op_lt, op_cmp.call(context, a, a, b), a, b);
    }

    @Override
    public Class getJavaClass() {
        return List.class;
    }

    /**
     * @param target
     * @param start
     * @deprecated Use {@link RubyArray#copyInto(ThreadContext, IRubyObject[], int)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public void copyInto(IRubyObject[] target, int start) {
        copyInto(getCurrentContext(), target, start);
    }

    /**
     * Copy the values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public void copyInto(ThreadContext context, IRubyObject[] target, int start) {
        assert target.length - start >= realLength;
        safeArrayCopy(context, values, begin, target, start, realLength);
    }

    /**
     * @param target
     * @param start
     * @param len
     * @deprecated Use {@link RubyArray#copyInto(ThreadContext, IRubyObject[], int, int)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public void copyInto(IRubyObject[] target, int start, int len) {
        copyInto(getCurrentContext(), target, start, len);
    }

    /**
     * Copy the specified number of values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public void copyInto(ThreadContext context, IRubyObject[] target, int start, int len) {
        assert target.length - start >= len;
        safeArrayCopy(context, values, begin, target, start, len);
    }

    // Satisfy java.util.List interface (for Java integration)
    public int size() {
        return realLength;
    }

    public boolean isEmpty() {
        return realLength == 0;
    }

    public boolean contains(Object element) {
        return indexOf(element) != -1;
    }

    public Object[] toArray() {
        // no catch for ArrayIndexOutOfBounds here because this impls a List method
        Object[] array = new Object[realLength];
        for (int i = 0; i < realLength; i++) {
            array[i] = eltInternal(i).toJava(Object.class);
        }
        return array;
    }

    public Object[] toArray(final Object[] arg) {
        // no catch for ArrayIndexOutOfBounds here because this impls a List method
        Object[] array = arg;
        Class type = array.getClass().getComponentType();
        if (array.length < realLength) {
            array = (Object[]) Array.newInstance(type, realLength);
        }
        int length = realLength;

        for (int i = 0; i < length; i++) {
            array[i] = eltInternal(i).toJava(type);
        }
        return array;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target.isArray()) {
            Class type = target.getComponentType();
            Object rawJavaArray = Array.newInstance(type, realLength);
            try {
                ArrayUtils.copyDataToJavaArrayDirect(this, rawJavaArray);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(getRuntime().getCurrentContext(), ex);
            }
            return target.cast(rawJavaArray);
        } else {
            return super.toJava(target);
        }
    }

    public boolean add(Object element) {
        return add(getRuntime().getCurrentContext(), element);
    }

    public boolean add(ThreadContext context, Object element) {
        append(context, JavaUtil.convertJavaToUsableRubyObject(context.runtime, element));
        return true;
    }

    public boolean remove(Object element) {
        final Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        unpack(context);
        IRubyObject item = JavaUtil.convertJavaToUsableRubyObject(runtime, element);
        boolean listchanged = false;

        for (int i1 = 0; i1 < realLength; i1++) {
            IRubyObject e = values[begin + i1];
            if (equalInternal(context, e, item)) {
                delete_at(i1);
                listchanged = true;
                break;
            }
        }

        return listchanged;
    }

    public boolean containsAll(Collection c) {
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            if (indexOf(iter.next()) == -1) {
                return false;
            }
        }

        return true;
    }

    public boolean addAll(Collection c) {
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            add(iter.next());
        }
        return !c.isEmpty();
    }

    public boolean addAll(int index, Collection c) {
        Iterator iter = c.iterator();
        for (int i = index; iter.hasNext(); i++) {
            add(i, iter.next());
        }
        return !c.isEmpty();
    }

    public boolean removeAll(Collection c) {
        boolean listChanged = false;
        final Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            IRubyObject deleted = delete(context, JavaUtil.convertJavaToUsableRubyObject(runtime, iter.next()), Block.NULL_BLOCK);
            if (!deleted.isNil()) {
                listChanged = true;
            }
        }
        return listChanged;
    }

    public boolean retainAll(Collection c) {
        boolean listChanged = false;

        for (Iterator iter = iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (!c.contains(element)) {
                remove(element);
                listChanged = true;
            }
        }
        return listChanged;
    }

    public Object get(int index) {
        return elt(index).toJava(Object.class);
    }

    public Object set(int index, Object element) {
        Object previous = elt(index).toJava(Object.class);
        store(index, JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element));
        return previous;
    }

    public void add(int index, Object element) {
        insert(metaClass.runtime.getCurrentContext(), index, JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element));
    }

    public Object remove(int index) {
        return delete_at(index).toJava(Object.class);
    }

    public int indexOf(Object element) {
        int myBegin = this.begin;

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element);

            for (int i = myBegin; i < myBegin + realLength; i++) {
                if (convertedElement.equals(values[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object element) {
        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element);

            for (int i = realLength - 1; i >= 0; i--) {
                if (convertedElement.equals(eltInternal(i))) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static class JoinRecursive implements ThreadContext.RecursiveFunctionEx<JoinRecursive.State> {
        protected static class State {
            private final RubyArray ary;
            //private final IRubyObject outValue;
            private final RubyString sep;
            private final RubyString result;
            private final boolean[] first;

            State(RubyArray ary, IRubyObject outValue, RubyString sep, RubyString result, boolean[] first) {
                this.ary = ary;
                //this.outValue = outValue;
                this.sep = sep;
                this.result = result;
                this.first = first;
            }
        }

        public IRubyObject call(ThreadContext context, State state, IRubyObject obj, boolean recur) {
            if (recur) throw argumentError(context, "recursive array join");

            state.ary.joinAny(context, state.sep, 0, state.result, state.first);

            return context.nil;
        }
    }

    private static final JoinRecursive JOIN_RECURSIVE = new JoinRecursive();

    public class RubyArrayConversionIterator implements Iterator {
        protected int index = 0;
        protected int last = -1;

        public boolean hasNext() {
            return index < realLength;
        }

        public Object next() {
            IRubyObject element = elt(index);
            last = index++;
            return element.toJava(Object.class);
        }

        public void remove() {
            if (last == -1) throw new IllegalStateException();

            delete_at(last);
            if (last < index) index--;

            last = -1;

        }
    }

    public Iterator iterator() {
        return new RubyArrayConversionIterator();
    }

    final class RubyArrayConversionListIterator extends RubyArrayConversionIterator implements ListIterator {
        public RubyArrayConversionListIterator() {
        }

        public RubyArrayConversionListIterator(int index) {
            this.index = index;
        }

        public boolean hasPrevious() {
            return index >= 0;
        }

        public Object previous() {
            return elt(last = --index).toJava(Object.class);
        }

        public int nextIndex() {
            return index;
        }

        public int previousIndex() {
            return index - 1;
        }

        public void set(Object obj) {
            if (last == -1) {
                throw new IllegalStateException();
            }

            store(last, JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, obj));
        }

        public void add(Object obj) {
            Ruby runtime = metaClass.runtime;
            insert(new IRubyObject[] { asFixnum(runtime.getCurrentContext(), index++), JavaUtil.convertJavaToUsableRubyObject(runtime, obj) });
            last = -1;
        }
    }

    public ListIterator listIterator() {
        return new RubyArrayConversionListIterator();
    }

    public ListIterator listIterator(int index) {
        return new RubyArrayConversionListIterator(index);
	}

    // TODO: list.subList(from, to).clear() is supposed to clear the sublist from the list
    public List subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }

        IRubyObject subList = subseq(fromIndex, toIndex - fromIndex);

        return subList.isNil() ? null : (List) subList;
    }

    public void clear() {
        rb_clear(getRuntime().getCurrentContext());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof RubyArray) {
            return op_equal(metaClass.runtime.getCurrentContext(), (RubyArray) other).isTrue();
        }
        return false;
    }

    private static IRubyObject safeArrayRef(ThreadContext context, IRubyObject[] values, int i) {
        try {
            return values[i];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    protected static IRubyObject safeArraySet(ThreadContext context, IRubyObject[] values, int i, IRubyObject value) {
        try {
            return values[i] = value;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    private static IRubyObject safeArrayRefSet(ThreadContext context, IRubyObject[] values, int i, IRubyObject value) {
        try {
            IRubyObject tmp = values[i];
            values[i] = value;
            return tmp;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context, e);
        }
    }

    private static IRubyObject safeArrayRefCondSet(ThreadContext context, IRubyObject[] values, int i, boolean doSet, IRubyObject value) {
        try {
            IRubyObject tmp = values[i];
            if (doSet) values[i] = value;
            return tmp;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }
    
    private static void safeArrayCopy(ThreadContext context, IRubyObject[] source, int sourceStart, IRubyObject[] target, int targetStart, int length) {
        try {
            System.arraycopy(source, sourceStart, target, targetStart, length);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context, ex);
        }
    }

    private static ArraySites sites(ThreadContext context) {
        return context.sites.Array;
    }

    /**
     * Increases the capacity of this <code>Array</code>, if necessary.
     * @param minCapacity the desired minimum capacity of the internal array
     */
    @Deprecated
    public void ensureCapacity(int minCapacity) {
        unpack(getCurrentContext());
        if ( isShared || (values.length - begin) < minCapacity ) {
            final int len = this.realLength;
            int newCapacity = minCapacity > len ? minCapacity : len;
            IRubyObject[] values = IRubyObject.array(newCapacity);
            ArraySupport.copy(this.values, begin, values, 0, len);
            this.values = values;
            this.begin = 0;
        }
    }

    @Deprecated
    @Override
    public RubyArray to_a() {
        var context = metaClass.runtime.getCurrentContext();
        final RubyClass arrayClass = arrayClass(context);
        return metaClass != arrayClass ? dupImpl(context.runtime, arrayClass) : this;
    }

    @Deprecated
    public IRubyObject shuffle(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return shuffle(context);
            case 1:
                return shuffle(context, args[0]);
            default:
                throw argumentError(context, args.length, 0, 0);
        }
    }

    @Deprecated
    public IRubyObject shuffle_bang(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return shuffle_bang(context, context.nil);
            case 1:
                return shuffle_bang(context, args[0]);
            default:
                throw argumentError(context, args.length, 0, 0);
        }
    }

    @Deprecated
    public IRubyObject sample(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return sample(context);
            case 1:
                return sample(context, args[0]);
            case 2:
                return sample(context, args[0], args[1]);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    /**
     * Returns a stream of each IRubyObject
     * @return
     */
    public Stream<IRubyObject> rubyStream() {
        return Stream.iterate(0, i -> i + 1).limit(realLength).map(this::eltInternal);
    }
}
