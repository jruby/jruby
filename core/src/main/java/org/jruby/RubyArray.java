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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
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
import static org.jruby.RubyNumeric.checkInt;
import static org.jruby.runtime.Helpers.addBufferLength;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.calculateBufferLength;
import static org.jruby.runtime.Helpers.hashEnd;
import static org.jruby.runtime.Helpers.murmurCombine;
import static org.jruby.runtime.Helpers.validateBufferLength;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.Inspector.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

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

    public static RubyClass createArrayClass(Ruby runtime) {
        RubyClass arrayc = runtime.defineClass("Array", runtime.getObject(), RubyArray::newEmptyArray);

        arrayc.setClassIndex(ClassIndex.ARRAY);
        arrayc.setReifiedClass(RubyArray.class);

        arrayc.kindOf = new RubyModule.JavaClassKindOf(RubyArray.class);

        arrayc.includeModule(runtime.getEnumerable());
        arrayc.defineAnnotatedMethods(RubyArray.class);

        return arrayc;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.ARRAY;
    }

    protected final void concurrentModification() {
        throw concurrentModification(getRuntime(), null);
    }

    private static RuntimeException concurrentModification(Ruby runtime, Exception cause) {
        RuntimeException ex = runtime.newConcurrencyError("Detected invalid array contents due to unsynchronized modifications with concurrent users");
        // NOTE: probably not useful to be on except for debugging :
        // if ( cause != null ) ex.initCause(cause);
        return ex;
    }

    /** rb_ary_s_create
     *
     */
    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject create(IRubyObject klass, IRubyObject[] args, Block block) {
        RubyArray arr;

        switch (args.length) {
            case 1:
                return new RubyArrayOneObject((RubyClass) klass, args[0]);
            case 2:
                return new RubyArrayTwoObject((RubyClass) klass, args[0], args[1]);
            default:
                arr = (RubyArray) ((RubyClass) klass).allocate();
        }

        if (args.length > 0) {
            arr.values = args.clone();
            arr.realLength = args.length;
        }
        return arr;
    }

    /** rb_ary_new2
     *
     */
    public static final RubyArray newArray(final Ruby runtime, final long len) {
        checkLength(runtime, len);
        return newArray(runtime, (int)len);
    }

    public static final RubyArray newArrayLight(final Ruby runtime, final long len) {
        checkLength(runtime, len);
        return newArrayLight(runtime, (int)len);
    }

    public static final RubyArray newArray(final Ruby runtime, final int len) {
        IRubyObject[] values = IRubyObject.array(validateBufferLength(runtime, len));
        Helpers.fillNil(values, 0, len, runtime);
        return new RubyArray(runtime, values, 0, 0);
    }

    public static final RubyArray newArrayLight(final Ruby runtime, final int len) {
        IRubyObject[] values = IRubyObject.array(validateBufferLength(runtime, len));
        Helpers.fillNil(values, 0, len, runtime);
        return new RubyArray(runtime, runtime.getArray(), values, 0, 0, false);
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray newArray(final Ruby runtime) {
        return newArray(runtime, ARRAY_DEFAULT_SIZE);
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray newArrayLight(final Ruby runtime) {
        /* Ruby arrays default to holding 16 elements, so we create an
         * ArrayList of the same size if we're not told otherwise
         */
        return newArrayLight(runtime, ARRAY_DEFAULT_SIZE);
    }

    public static RubyArray newArray(Ruby runtime, IRubyObject obj) {
        return USE_PACKED_ARRAYS ? new RubyArrayOneObject(runtime, obj) : new RubyArray(runtime, arrayOf(obj));
    }

    public static RubyArray newArrayLight(Ruby runtime, IRubyObject obj) {
        return USE_PACKED_ARRAYS ? new RubyArrayOneObject(runtime, obj) : new RubyArray(runtime, arrayOf(obj));
    }

    public static RubyArray newArrayLight(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return USE_PACKED_ARRAYS ? new RubyArrayTwoObject(runtime, car, cdr) : new RubyArray(runtime, arrayOf(car, cdr));
    }

    public static RubyArray newArrayLight(Ruby runtime, IRubyObject... objs) {
        return new RubyArray(runtime, objs, false);
    }

    /** rb_assoc_new
     *
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return USE_PACKED_ARRAYS ? new RubyArrayTwoObject(runtime, car, cdr) : new RubyArray(runtime, arrayOf(car, cdr));
    }

    public static RubyArray newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third) {
        return new RubyArray(runtime, arrayOf(first, second, third));
    }

    public static RubyArray newArray(Ruby runtime, IRubyObject first, IRubyObject second, IRubyObject third, IRubyObject fourth) {
        return new RubyArray(runtime, arrayOf(first, second, third, fourth));
    }

    public static RubyArray newEmptyArray(Ruby runtime) {
        return new RubyArray(runtime, NULL_ARRAY);
    }

    public static RubyArray newEmptyArray(Ruby runtime, RubyClass klass) {
        return new RubyArray(runtime, klass, NULL_ARRAY);
    }

    /** rb_ary_new4, rb_ary_new3
     *
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject[] args) {
        final int size = args.length;
        if (size == 0) {
            return newEmptyArray(runtime);
        }
        return isPackedArray(size) ? packedArray(runtime, args) : new RubyArray(runtime, args.clone());
    }

    public static RubyArray newArray(Ruby runtime, Collection<? extends IRubyObject> collection) {
        if (collection.isEmpty()) {
            return newEmptyArray(runtime);
        }
        final IRubyObject[] arr = collection.toArray(IRubyObject.NULL_ARRAY);
        return isPackedArray(collection) ? packedArray(runtime, arr) : new RubyArray(runtime, arr);
    }

    public static RubyArray newArray(Ruby runtime, List<? extends IRubyObject> list) {
        if (list.isEmpty()) {
            return newEmptyArray(runtime);
        }
        return isPackedArray(list) ? packedArray(runtime, list) : new RubyArray(runtime, list.toArray(IRubyObject.NULL_ARRAY));
    }

    private static RubyArray packedArray(final Ruby runtime, final IRubyObject[] args) {
        if (args.length == 1) {
            return new RubyArrayOneObject(runtime, args[0]);
        } else {
            return new RubyArrayTwoObject(runtime, args[0], args[1]);
        }
    }

    private static RubyArray packedArray(final Ruby runtime, final List<? extends IRubyObject> args) {
        if (args.size() == 1) {
            return new RubyArrayOneObject(runtime, args.get(0));
        } else {
            return new RubyArrayTwoObject(runtime, args.get(0), args.get(1));
        }
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
        if (length == 0) {
            return newEmptyArray(runtime);
        }
        if (USE_PACKED_ARRAYS) {
            if (length == 1) {
                return new RubyArrayOneObject(runtime, args[start]);
            }
            if (length == 2) {
                return new RubyArrayTwoObject(runtime, args[start], args[start + 1]);
            }
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
    private RubyArray(Ruby runtime, IRubyObject[] vals, int begin, int length) {
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

    /**
     * Overridden by specialized arrays to fall back to IRubyObject[].
     */
    protected void unpack() {
    }

    private void alloc(int length) {
        Ruby runtime = metaClass.runtime;
        IRubyObject[] newValues = IRubyObject.array(validateBufferLength(runtime, length));
        Helpers.fillNil(newValues, runtime);
        values = newValues;
        begin = 0;
    }

    private void realloc(int newLength, int valuesLength) {
        unpack();
        Ruby runtime = metaClass.runtime;
        IRubyObject[] reallocated = IRubyObject.array(validateBufferLength(runtime, newLength));
        if (newLength > valuesLength) {
            Helpers.fillNil(reallocated, valuesLength, newLength, runtime);
            safeArrayCopy(values, begin, reallocated, 0, valuesLength); // elements and trailing nils
        } else {
            safeArrayCopy(values, begin, reallocated, 0, newLength); // ???
        }
        begin = 0;
        values = reallocated;
    }

    protected static final void checkLength(Ruby runtime, long length) {
        if (length < 0) {
            throw runtime.newArgumentError("negative array size (or size too big)");
        }

        if (length >= Integer.MAX_VALUE) {
            throw runtime.newArgumentError("array size too big");
        }
    }

    /**
     * @deprecated RubyArray implements List, use it directly
     * @return a read-only copy of this list
     */
    public final List<IRubyObject> getList() {
        return Arrays.asList(toJavaArray());
    }

    public int getLength() {
        return realLength;
    }

    /**
     * Return a Java array copy of the elements contained in this Array.
     *
     * This version always creates a new Java array that is exactly the length of the Array's elements.
     *
     * @return a Java array with exactly the size and contents of this RubyArray's elements
     */
    public IRubyObject[] toJavaArray() {
        IRubyObject[] copy = IRubyObject.array(realLength);
        copyInto(copy, 0);
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
        unpack();
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
        unpack();
        return (!isShared && begin == 0 && values.length == realLength) ? values : toJavaArray();
    }

    public boolean isSharedJavaArray(RubyArray other) {
        return values == other.values && begin == other.begin && realLength == other.realLength;
    }

    /** rb_ary_make_shared
    *
    */
    protected RubyArray makeShared() {
        // TODO: (CON) Some calls to makeShared could create packed array almost as efficiently
        unpack();

        return makeShared(begin, realLength, metaClass);
    }

    private RubyArray makeShared(int beg, int len, RubyClass klass) {
        return makeShared(beg, len, new RubyArray(klass.runtime, klass));
    }

    private final RubyArray makeShared(int beg, int len, RubyArray sharedArray) {
        unpack();
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
    private RubyArray makeSharedFirst(ThreadContext context, IRubyObject num, boolean last, RubyClass klass) {
        int n = RubyNumeric.num2int(num);

        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw context.runtime.newArgumentError("negative array size");
        }

        return makeShared(last ? begin + realLength - n : begin, n, klass);
    }

    /** rb_ary_modify_check
     *
     */
    protected final void modifyCheck() {
        if ((flags & TMPLOCK_OR_FROZEN_ARR_F) != 0) {
            if ((flags & FROZEN_F) != 0) throw getRuntime().newFrozenError(metaClass);
            if ((flags & TMPLOCK_ARR_F) != 0) throw getRuntime().newTypeError("can't modify array during iteration");
        }
    }

    /** rb_ary_modify
     *
     */
    protected void modify() {
        modifyCheck();
        if (isShared) {
            IRubyObject[] vals = IRubyObject.array(realLength);
            safeArrayCopy(values, begin, vals, 0, realLength);
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
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 0:
            return initialize(context, block);
        case 1:
            return initializeCommon(context, args[0], null, block);
        case 2:
            return initializeCommon(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 2);
            return null; // not reached
        }
    }

    /** rb_ary_initialize
     *
     */
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        modifyCheck();
        unpack();
        realLength = 0;
        if (block.isGiven() && context.runtime.isVerbose()) {
            context.runtime.getWarnings().warning(ID.BLOCK_UNUSED, "given block not used");
        }
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
        unpack();
        Ruby runtime = context.runtime;

        if (arg1 == null && !(arg0 instanceof RubyFixnum)) {
            IRubyObject val = arg0.checkArrayType();
            if (!val.isNil()) {
                replace(val);
                return this;
            }
        }

        long len = RubyNumeric.num2long(arg0);
        if (len < 0) throw runtime.newArgumentError("negative array size");
        int ilen = validateBufferLength(runtime, (int) len);

        modify();

        if (ilen > values.length - begin) {
            values = IRubyObject.array(ilen);
            begin = 0;
        }

        if (block.isGiven()) {
            if (arg1 != null) {
                runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
            }

            if (block.getSignature() == Signature.NO_ARGUMENTS) {
                IRubyObject nil = context.nil;
                for (int i = 0; i < ilen; i++) {
                    storeInternal(i, block.yield(context, nil));
                    realLength = i + 1;
                }
            } else {
                for (int i = 0; i < ilen; i++) {
                    storeInternal(i, block.yield(context, RubyFixnum.newFixnum(runtime, i)));
                    realLength = i + 1;
                }
            }

        } else {
            try {
                if (arg1 == null) {
                    Helpers.fillNil(values, begin, begin + ilen, runtime);
                } else {
                    Arrays.fill(values, begin, begin + ilen, arg1);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(runtime, ex);
            }
            realLength = ilen;
        }
        return this;
    }

    /** rb_ary_initialize_copy
     *
     */
    @JRubyMethod(name = {"initialize_copy"}, required = 1, visibility=PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        return this.replace(orig);
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
        dup.flags |= flags & TAINTED_F; // from DUP_SETUP
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
        // Also, taintedness and trustedness are not inherited to duplicates
        Ruby runtime = metaClass.runtime;
        return dupImpl(runtime, runtime.getArray());
    }

    /** rb_ary_replace
     *
     */
    @JRubyMethod(name = {"replace"}, required = 1)
    public IRubyObject replace(IRubyObject orig) {
        unpack();
        modifyCheck();

        if (this == orig) return this;

        RubyArray origArr = orig.convertToArray();

        origArr.unpack();

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
    @JRubyMethod(name = "to_s")
    public RubyString to_s(ThreadContext context) {
        return inspect(context);
    }

    @Override
    public IRubyObject to_s() {
        return to_s(metaClass.runtime.getCurrentContext());
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

    @Deprecated
    public RubyFixnum hash19(ThreadContext context) {
        return hash(context);
    }

    @Override
    public RubyFixnum hash() {
        return hash(metaClass.runtime.getCurrentContext());
    }

    /** rb_ary_hash
     *
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return RubyFixnum.newFixnum(context.runtime, hashImpl(context));
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

    /** rb_ary_store
     *
     */
    public IRubyObject store(long index, IRubyObject value) {
        if (index < 0 && (index += realLength) < 0) {
            throw metaClass.runtime.newIndexError("index " + (index - realLength) + " out of array");
        }
        if (index >= Integer.MAX_VALUE) {
            throw metaClass.runtime.newIndexError("index " + index  + " too big");
        }

        modify();

        storeInternal((int) index, value);

        return value;
    }

    protected void storeInternal(final int index, final IRubyObject value) {
        assert index >= 0;

        if (index >= realLength) {
            int valuesLength = values.length - begin;
            if (index >= valuesLength) storeRealloc(index, valuesLength);
            realLength = index + 1;
        }

        safeArraySet(values, begin + index, value);
    }

    private void storeRealloc(final int index, final int valuesLength) {
        long newLength = valuesLength >> 1;

        if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

        newLength += index;
        if (newLength >= Integer.MAX_VALUE) {
            throw getRuntime().newIndexError("index " + index  + " too big");
        }
        realloc((int) newLength, valuesLength);
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
            throw concurrentModification(getRuntime(), ex);
        }
    }

    public T eltSetOk(long offset, T value) {
        return eltSetOk((int) offset, value);
    }

    public T eltSetOk(int offset, T value) {
        try {
            return eltInternalSet(offset, value);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(getRuntime(), ex);
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
    public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return fetch(context, args[0], block);
        case 2:
            return fetch(context, args[0], args[1], block);
        default:
            Arity.raiseArgumentError(context.runtime, args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_ary_fetch
     *
     */
    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject arg0, Block block) {
        long index = RubyNumeric.num2long(arg0);

        if (index < 0) index += realLength;
        if (index < 0 || index >= realLength) {
            if (block.isGiven()) return block.yield(context, arg0);
            throw context.runtime.newIndexError("index " + index + " out of array");
        }

        return eltOk((int) index);
    }

    /** rb_ary_fetch
    *
    */
   @JRubyMethod
   public IRubyObject fetch(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
       if (block.isGiven()) {
           context.runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
       }

       long index = RubyNumeric.num2long(arg0);

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

    private void splice(final Ruby runtime, int beg, int len, IRubyObject rpl) {
        if (len < 0) throw runtime.newIndexError("negative length (" + len + ")");
        if (beg < 0 && (beg += realLength) < 0)
            throw runtime.newIndexError("index " + (beg - realLength) + " out of array");

        final RubyArray rplArr;
        final int rlen;

        if (rpl == null) {
            rplArr = null;
            rlen = 0;
        } else if (rpl.isNil()) {
            // 1.9 replaces with nil
            rplArr = newArray(runtime, rpl);
            rlen = 1;
        } else {
            rplArr = aryToAry(runtime.getCurrentContext(), rpl);
            rlen = rplArr.realLength;
        }

        splice(runtime, beg, len, rplArr, rlen);
    }

    /** rb_ary_splice
     *
     */
    private void splice(final Ruby runtime, int beg, int len, final RubyArray rplArr, final int rlen) {
        if (len < 0) throw runtime.newIndexError("negative length (" + len + ")");
        if (beg < 0 && (beg += realLength) < 0) throw runtime.newIndexError("index " + (beg - realLength) + " out of array");

        unpack();
        modify();

        int valuesLength = values.length - begin;
        if (beg >= realLength) {
            len = beg + rlen;
            if (len >= valuesLength) spliceRealloc(len, valuesLength);
            try {
                Helpers.fillNil(values, begin + realLength, begin + beg, runtime);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw concurrentModification(runtime, e);
            }
            realLength = len;
        } else {
            if (beg + len > realLength) len = realLength - beg;
            int alen = realLength + rlen - len;
            if (alen >= valuesLength) spliceRealloc(alen, valuesLength);

            if (len != rlen) {
                safeArrayCopy(values, begin + (beg + len), values, begin + beg + rlen, realLength - (beg + len));
                realLength = alen;
            }
        }

        if (rlen > 0) {
            rplArr.copyInto(values, begin + beg, rlen);
        }
    }

    /** rb_ary_splice
     *
     */
    private final void spliceOne(long beg, IRubyObject rpl) {
        if (beg < 0 && (beg += realLength) < 0) throw getRuntime().newIndexError("index " + (beg - realLength) + " out of array");

        unpack();
        modify();

        int valuesLength = values.length - begin;
        if (beg >= realLength) {
            int len = (int) beg + 1;
            if (len >= valuesLength) spliceRealloc(len, valuesLength);
            Helpers.fillNil(values, begin + realLength, begin + ((int) beg), metaClass.runtime);
            realLength = len;
        } else {
            int len = beg > realLength ? realLength - (int) beg : 0;
            int alen = realLength + 1 - len;
            if (alen >= valuesLength) spliceRealloc(alen, valuesLength);

            if (len == 0) {
                safeArrayCopy(values, begin + (int) beg, values, begin + (int) beg + 1, realLength - (int) beg);
                realLength = alen;
            }
        }

        safeArraySet(values, begin + (int) beg, rpl);
    }

    private void spliceRealloc(int length, int valuesLength) {
        Ruby runtime = metaClass.runtime;

        int tryLength = calculateBufferLength(runtime, valuesLength);
        int len = length > tryLength ? length : tryLength;
        IRubyObject[] vals = IRubyObject.array(len);
        System.arraycopy(values, begin, vals, 0, realLength);

        // only fill if there actually will remain trailing storage
        if (len > length) {
            Helpers.fillNil(vals, length, len, runtime);
        }
        begin = 0;
        values = vals;
    }

    private void unshiftRealloc(int valuesLength) {
        Ruby runtime = metaClass.runtime;

        int newLength = valuesLength >> 1;
        if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

        newLength = addBufferLength(runtime, valuesLength, newLength);

        IRubyObject[] vals = IRubyObject.array(newLength);
        safeArrayCopy(values, begin, vals, 1, valuesLength);
        Helpers.fillNil(vals, valuesLength + 1, newLength, runtime);
        values = vals;
        begin = 0;
    }

    public IRubyObject insert() {
        throw metaClass.runtime.newArgumentError(0, 1);
    }

    /** rb_ary_insert
     *
     */
    @JRubyMethod(name = "insert")
    public IRubyObject insert(IRubyObject arg) {
        modifyCheck();

        RubyNumeric.num2long(arg);

        return this;
    }

    @Deprecated
    public IRubyObject insert19(IRubyObject arg) {
        return insert(arg);
    }

    @JRubyMethod(name = "insert")
    public IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        modifyCheck();

        insert(RubyNumeric.num2long(arg1), arg2);
        return this;
    }

    private void insert(long pos, IRubyObject val) {
        if (pos == -1) pos = realLength;
        else if (pos < 0) {
            long minpos = -realLength - 1;
            if (pos < minpos) {
                throw getRuntime().newIndexError("index " + pos + " too small for array; minimum: " + minpos);
            }
            pos++;
        }

        spliceOne(pos, val); // rb_ary_new4
    }

    @Deprecated
    public IRubyObject insert19(IRubyObject arg1, IRubyObject arg2) {
        return insert(arg1, arg2);
    }

    @JRubyMethod(name = "insert", required = 1, rest = true)
    public IRubyObject insert(IRubyObject[] args) {
        modifyCheck();

        if (args.length == 1) return this;

        unpack();

        long pos = RubyNumeric.num2long(args[0]);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;

        final Ruby runtime = metaClass.runtime;

        RubyArray inserted = new RubyArray(runtime, false);
        inserted.values = args;
        inserted.begin = 1;
        inserted.realLength = args.length - 1;

        splice(runtime, checkInt(runtime, pos), 0, inserted, inserted.realLength); // rb_ary_new4

        return this;
    }

    @Deprecated
    public IRubyObject insert19(IRubyObject[] args) {
        return insert(args);
    }

    /** rb_ary_transpose
     *
     */
    @JRubyMethod(name = "transpose")
    public RubyArray transpose() {

        int alen = realLength;
        if (alen == 0) return aryDup();

        Ruby runtime = metaClass.runtime;

        int elen = -1;
        IRubyObject[] result = null;
        for (int i = 0; i < alen; i++) {
            RubyArray tmp = elt(i).convertToArray();
            if (elen < 0) {
                elen = tmp.realLength;
                result = IRubyObject.array(elen);
                for (int j = 0; j < elen; j++) {
                    result[j] = newBlankArray(runtime, alen);
                }
            } else if (elen != tmp.realLength) {
                throw runtime.newIndexError("element size differs (" + tmp.realLength
                        + " should be " + elen + ")");
            }
            for (int j = 0; j < elen; j++) {
                ((RubyArray) result[j]).storeInternal(i, tmp.elt(j));
            }
        }
        return new RubyArray(runtime, result);
    }

    /** rb_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        final Ruby runtime = metaClass.runtime;
        final int length = realLength;

        RubyArray result = newArray(runtime, args.length);

        for (int i = 0; i < args.length; i++) {
            final IRubyObject arg = args[i];
            if ( arg instanceof RubyFixnum ) {
                result.append( entry(((RubyFixnum) arg).value) );
                continue;
            }

            final int[] begLen;
            if ( ! ( arg instanceof RubyRange ) ) {
                // do result.append
            }
            else if ( ( begLen = ((RubyRange) arg).begLenInt(length, 1) ) == null ) {
                continue;
            }
            else {
                final int beg = begLen[0];
                final int len = begLen[1];
                for (int j = 0; j < len; j++) {
                    result.append( entry(j + beg) );
                }
                continue;
            }
            result.append( entry(RubyNumeric.num2long(arg)) );
        }

        Helpers.fillNil(result.values, result.realLength, result.values.length, runtime);
        return result;
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseq(long beg, long len) {
        return subseq(metaClass, beg, len, true);
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
        return makeShared(begin + (int) beg, (int) len, new RubyArray(runtime, metaClass, !light));
    }

    /** rb_ary_length
     *
     */
    @JRubyMethod(name = "length", alias = "size")
    public RubyFixnum length() {
        return metaClass.runtime.newFixnum(realLength);
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    protected static IRubyObject size(ThreadContext context, RubyArray self, IRubyObject[] args) {
        return self.length();
    }

    /** rb_ary_push - specialized rb_ary_store
     *
     */
    @JRubyMethod(name = "<<", required = 1)
    public RubyArray append(IRubyObject item) {
        unpack();
        modify();
        int valuesLength = values.length - begin;
        if (realLength == valuesLength) {
            if (realLength == Integer.MAX_VALUE) throw getRuntime().newIndexError("index " + Integer.MAX_VALUE + " too big");

            long newLength = valuesLength + (valuesLength >> 1);
            if (newLength > Integer.MAX_VALUE) {
                newLength = Integer.MAX_VALUE;
            } else if (newLength < ARRAY_DEFAULT_SIZE) {
                newLength = ARRAY_DEFAULT_SIZE;
            }

            realloc((int) newLength, valuesLength);
        }

        safeArraySet(values, begin + realLength++, item);

        return this;
    }

    /** rb_ary_push_m - instance method push
     *
     */
    @Deprecated // not-used
    public RubyArray push_m(IRubyObject[] items) {
        return push(items);
    }

    @JRubyMethod(name = "push", alias = "append", required = 1)
    public RubyArray push(IRubyObject item) {
        append(item);

        return this;
    }

    @JRubyMethod(name = "push", alias = "append", rest = true)
    public RubyArray push(IRubyObject[] items) {
        if (items.length == 0) modifyCheck();
        for (int i = 0; i < items.length; i++) {
            append(items[i]);
        }
        return this;
    }

    @Deprecated
    public RubyArray push_m19(IRubyObject[] items) {
        return push(items);
    }

    /** rb_ary_pop
     *
     */
    @JRubyMethod
    public IRubyObject pop(ThreadContext context) {
        unpack();
        modifyCheck();

        if (realLength == 0) return context.nil;

        if (isShared) {
            return safeArrayRef(context.runtime, values, begin + --realLength);
        } else {
            int index = begin + --realLength;
            return safeArrayRefSet(context.runtime, values, index, context.nil);
        }
    }

    @JRubyMethod
    public IRubyObject pop(ThreadContext context, IRubyObject num) {
        unpack();
        modifyCheck();
        RubyArray result = makeSharedFirst(context, num, true, context.runtime.getArray());
        realLength -= result.realLength;
        return result;
    }

    /** rb_ary_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context) {
        unpack();
        modifyCheck();

        if (realLength == 0) return context.nil;

        final IRubyObject obj = safeArrayRefCondSet(context.runtime, values, begin, !isShared, context.nil);
        begin++;
        realLength--;
        return obj;

    }

    @JRubyMethod(name = "shift")
    public IRubyObject shift(ThreadContext context, IRubyObject num) {
        unpack();
        modify();

        RubyArray result = makeSharedFirst(context, num, false, context.runtime.getArray());

        int n = result.realLength;
        begin += n;
        realLength -= n;
        return result;
    }

    @JRubyMethod(name = "unshift", alias = "prepend")
    public IRubyObject unshift() {
        modifyCheck();
        return this;
    }

    @Deprecated
    public IRubyObject unshift19() {
        return unshift();
    }

    /** rb_ary_unshift
     *
     */
    @JRubyMethod(name = "unshift", alias = "prepend")
    public IRubyObject unshift(IRubyObject item) {
        unpack();

        if (begin == 0 || isShared) {
            modify();
            final int valuesLength = values.length - begin;
            if (valuesLength == 0) {
                alloc(ARRAY_DEFAULT_SIZE);
                begin = ARRAY_DEFAULT_SIZE - 1;
            } else if (realLength == valuesLength) {
                unshiftRealloc(valuesLength);
            } else {
                safeArrayCopy(values, begin, values, begin + 1, realLength);
            }
        } else {
            modifyCheck();
            begin--;
        }
        realLength++;
        values[begin] = item;
        return this;
    }

    @Deprecated
    public IRubyObject unshift19(IRubyObject item) {
        return unshift(item);
    }

    @JRubyMethod(name = "unshift", alias = "prepend",  rest = true)
    public IRubyObject unshift(IRubyObject[] items) {
        unpack();

        if (items.length == 0) {
            modifyCheck();
            return this;
        }

        final int len = realLength;
        store(((long) len) + items.length - 1, metaClass.runtime.getNil());

        try {
            System.arraycopy(values, begin, values, begin + items.length, len);
            ArraySupport.copy(items, 0, values, begin, items.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime(), e);
        }

        return this;
    }

    @Deprecated
    public IRubyObject unshift19(IRubyObject[] items) {
        return unshift(items);
    }

    /** rb_ary_includes
     *
     */
    @JRubyMethod(name = "include?", required = 1)
    public RubyBoolean include_p(ThreadContext context, IRubyObject item) {
        return RubyBoolean.newBoolean(context, includes(context, item));
    }

    /** rb_ary_frozen_p
     *
     */
    @JRubyMethod(name = "frozen?")
    @Override
    public RubyBoolean frozen_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isFrozen() || (flags & TMPLOCK_ARR_F) != 0);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject aref(IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return aref(args[0]);
        case 2:
            return aref(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    /** rb_ary_aref
     */
    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject aref(IRubyObject arg0) {
        return arg0 instanceof RubyFixnum ? entry(((RubyFixnum) arg0).value) : arefCommon(arg0);
    }

    @Deprecated
    public IRubyObject aref19(IRubyObject arg0) {
        return aref(arg0);
    }

    private IRubyObject arefCommon(IRubyObject arg0) {
        Ruby runtime = metaClass.runtime;

        if (arg0 instanceof RubyRange) {
            long[] beglen = ((RubyRange) arg0).begLen(realLength, 0);
            return beglen == null ? runtime.getNil() : subseq(beglen[0], beglen[1]);
        } else {
            ThreadContext context = runtime.getCurrentContext();
            ArraySites sites = sites(context);

            if (RubyRange.isRangeLike(context, arg0, sites.begin_checked, sites.end_checked, sites.exclude_end_checked)) {
                RubyRange range = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

                long[] beglen = range.begLen(realLength, 0);
                return beglen == null ? runtime.getNil() : subseq(beglen[0], beglen[1]);
            }
        }
        return entry(RubyNumeric.num2long(arg0));
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject aref(IRubyObject arg0, IRubyObject arg1) {
        return arefCommon(arg0, arg1);
    }

    @Deprecated
    public IRubyObject aref19(IRubyObject arg0, IRubyObject arg1) {
        return aref(arg0, arg1);
    }

    private IRubyObject arefCommon(IRubyObject arg0, IRubyObject arg1) {
        long beg = RubyNumeric.num2long(arg0);
        if (beg < 0) beg += realLength;
        return subseq(beg, RubyNumeric.num2long(arg1));
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject aset(IRubyObject[] args) {
        switch (args.length) {
        case 2:
            return aset(args[0], args[1]);
        case 3:
            return aset(args[0], args[1], args[2]);
        default:
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for 2)");
        }
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1) {
        modifyCheck();
        if (arg0 instanceof RubyFixnum) {
            store(((RubyFixnum) arg0).value, arg1);
        } else if (arg0 instanceof RubyRange) {
            RubyRange range = (RubyRange) arg0;
            final Ruby runtime = metaClass.runtime;
            int beg = checkInt(runtime, range.begLen0(realLength));
            splice(runtime, beg, checkInt(runtime, range.begLen1(realLength, beg)), arg1);
        } else {
            asetFallback(arg0, arg1);
        }
        return arg1;
    }

    private void asetFallback(IRubyObject arg0, IRubyObject arg1) {
        final Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
        ArraySites sites = sites(context);

        if (RubyRange.isRangeLike(context, arg0, sites.begin_checked, sites.end_checked, sites.exclude_end_checked)) {
            RubyRange range = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

            int beg = checkInt(runtime, range.begLen0(realLength));
            splice(runtime, beg, checkInt(runtime, range.begLen1(realLength, beg)), arg1);
        } else {
            store(RubyNumeric.num2long(arg0), arg1);
        }
    }

    @Deprecated
    public IRubyObject aset19(IRubyObject arg0, IRubyObject arg1) {
        return aset(arg0, arg1);
    }

    /** rb_ary_aset
    *
    */
    @JRubyMethod(name = "[]=")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        modifyCheck();
        splice(metaClass.runtime, RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1), arg2);
        return arg2;
    }

    @Deprecated
    public IRubyObject aset19(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return aset(arg0, arg1, arg2);
    }

    /** rb_ary_at
     *
     */
    @JRubyMethod(name = "at", required = 1)
    public IRubyObject at(IRubyObject pos) {
        return entry(RubyNumeric.num2long(pos));
    }

	/** rb_ary_concat
     *
     */
    @JRubyMethod(name = "concat")
    public RubyArray concat(ThreadContext context, IRubyObject obj) {
        modifyCheck();

        concat(context.runtime, obj.convertToArray());
        return this;
    }

    private void concat(final Ruby runtime, RubyArray obj) {
        splice(runtime, realLength, 0, obj, obj.realLength);
    }

    // MRI: ary_append
    public RubyArray aryAppend(RubyArray y) {
        if (y.realLength > 0) splice(metaClass.runtime, realLength, 0, y, y.realLength);

        return this;
    }

    /** rb_ary_concat_multi
     *
     */
    @JRubyMethod(name = "concat", rest = true)
    public RubyArray concat(ThreadContext context, IRubyObject[] objs) {
        modifyCheck();

        if (objs.length > 0) {
            Ruby runtime = context.runtime;
            RubyArray tmp = newArray(runtime, objs.length);

            for (IRubyObject obj : objs) {
                tmp.concat(runtime, obj.convertToArray());
            }

            return aryAppend(tmp);
        }

        return this;
    }

    public RubyArray concat(IRubyObject obj) {
        return concat(metaClass.runtime.getCurrentContext(), obj);
    }

    @Deprecated
    public RubyArray concat19(IRubyObject obj) {
        return concat(obj);
    }

    /** inspect_ary
     *
     */
    protected IRubyObject inspectAry(ThreadContext context) {
        final Ruby runtime = context.runtime;
        RubyString str = RubyString.newStringLight(runtime, DEFAULT_INSPECT_STR_SIZE, USASCIIEncoding.INSTANCE);
        str.cat((byte) '[');
        boolean tainted = isTaint();

        for (int i = 0; i < realLength; i++) {

            RubyString s = inspect(context, safeArrayRef(runtime, values, begin + i));
            if (s.isTaint()) tainted = true;
            if (i > 0) {
                ByteList bytes = str.getByteList();
                bytes.append((byte) ',').append((byte) ' ');
            } else {
                EncodingUtils.encAssociateIndex(str, s.getEncoding());
            }
            str.cat19(s);
        }
        str.cat((byte) ']');

        if (tainted) str.setTaint(true);

        return str;
    }

    /** rb_ary_inspect
    *
    */
    @JRubyMethod(name = "inspect")
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

    @Override
    public IRubyObject inspect() {
        return inspect(metaClass.runtime.getCurrentContext());
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject first(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return first();
        case 1:
            return first(args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /** rb_ary_first
     *
     */
    @JRubyMethod(name = "first")
    public IRubyObject first() {
        if (realLength == 0) return metaClass.runtime.getNil();
        return eltOk(0);
    }

    /** rb_ary_first
    *
    */
    @JRubyMethod(name = "first")
    public IRubyObject first(IRubyObject arg0) {
        long n = RubyNumeric.num2long(arg0);
        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw getRuntime().newArgumentError("negative array size (or size too big)");
        } else if (n == 1) {
            return newArray(metaClass.runtime, eltOk(0));
        } else if (n == 2) {
            return newArray(metaClass.runtime, eltOk(0), eltOk(1));
        }

        unpack();
        return makeShared(begin, (int) n, metaClass.runtime.getArray());
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject last(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return last();
        case 1:
            return last(args[0]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 0, 1);
            return null; // not reached
        }
    }

    /** rb_ary_last
     *
     */
    @JRubyMethod(name = "last")
    public IRubyObject last() {
        if (realLength == 0) return metaClass.runtime.getNil();
        return eltOk(realLength - 1);
    }

    /** rb_ary_last
    *
    */
    @JRubyMethod(name = "last")
    public IRubyObject last(IRubyObject arg0) {
        long n = RubyNumeric.num2long(arg0);
        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw metaClass.runtime.newArgumentError("negative array size (or size too big)");
        } else if (n == 1) {
            return newArray(metaClass.runtime, eltOk(realLength - 1));
        } else if (n == 2) {
            return newArray(metaClass.runtime, eltOk(realLength - 2), eltOk(realLength - 1));
        }

        unpack();
        return makeShared(begin + realLength - (int) n, (int) n, metaClass.runtime.getArray());
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
        unpack();
        final RubyClass array = context.runtime.getArray();

        // local copies of everything
        int realLength = this.realLength;
        int begin = this.begin;

        // sliding window
        RubyArray window = makeShared(begin, size, array);

        // don't expose shared array to ruby
        Signature signature = block.getSignature();
        final boolean specificArity = signature.isFixed() && signature.required() != 1;

        for (; realLength >= size; realLength -= size) {
            block.yield(context, window);
            if (specificArity) { // array is never exposed to ruby, just use for yielding
                window.begin = begin += size;
            } else { // array may be exposed to ruby, create new
                window = makeShared(begin += size, size, array);
            }
        }

        // remainder
        if (realLength > 0) {
            window.realLength = realLength;
            block.yield(context, window);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject each_slice(ThreadContext context, IRubyObject arg, Block block) {
        final int size = RubyNumeric.num2int(arg);
        final Ruby runtime = context.runtime;
        if (size <= 0) throw runtime.newArgumentError("invalid slice size");
        return block.isGiven() ? eachSlice(context, size, block) : enumeratorizeWithSize(context, this, "each_slice", arg, arg);
    }

    /** rb_ary_each_index
     *
     */
    public IRubyObject eachIndex(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) {
            throw runtime.newLocalJumpErrorNoBlock();
        }
        for (int i = 0; i < realLength; i++) {
            block.yield(context, runtime.newFixnum(i));
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

    /** rb_ary_join
     *
     */
    public IRubyObject join(ThreadContext context, IRubyObject sep) {
        return join19(context, sep);
    }

    public IRubyObject join(ThreadContext context) {
        return join19(context);
    }

    // MRI: ary_join_0
    protected RubyString joinStrings(RubyString sep, int max, RubyString result) {
        IRubyObject first;
        if (max > 0 && (first = eltOk(0)) instanceof EncodingCapable) {
            result.setEncoding(((EncodingCapable) first).getEncoding());
        }

        try {
            for (int i = 0; i < max; i++) {
                if (i > 0 && sep != null) result.cat19(sep);
                result.append19(eltInternal(i));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime(), e);
        }

        return result;
    }

    // MRI: ary_join_1
    private RubyString joinAny(ThreadContext context, RubyString sep, int i, RubyString result, boolean[] first) {
        assert i >= 0 : "joining elements before beginning of array";

        RubyClass arrayClass = context.runtime.getArray();
        JavaSites.CheckedSites to_ary_checked = null;

        for (; i < realLength; i++) {
            if (i > 0 && sep != null) result.cat19(sep);

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

                tmp = TypeConverter.convertToTypeWithCheck(context, val, arrayClass, to_ary_checked);
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
        result.cat19(val);
        if (first[0]) {
            result.setEncoding(val.getEncoding());
            first[0] = false;
        }
    }

    private void recursiveJoin(final ThreadContext context, final IRubyObject outValue,
                               final RubyString sep, final RubyString result, final RubyArray ary, final boolean[] first) {

        if (ary == this) throw context.runtime.newArgumentError("recursive array join");

        first[0] = false;

        context.safeRecurse(JOIN_RECURSIVE, new JoinRecursive.State(ary, outValue, sep, result, first), outValue, "join", true);
    }

    /** rb_ary_join
     *
     */
    @JRubyMethod(name = "join")
    public IRubyObject join19(final ThreadContext context, IRubyObject sep) {
        final Ruby runtime = context.runtime;

        if (realLength == 0) return RubyString.newEmptyString(runtime, USASCIIEncoding.INSTANCE);

        if (sep == context.nil) sep = runtime.getGlobalVariables().get("$,");

        int len = 1;
        RubyString sepString = null;
        if (sep != context.nil) {
            sepString = sep.convertToString();
            len += sepString.size() * (realLength - 1);
        }

        boolean[] first = null;
        for (int i = 0; i < realLength; i++) {
            IRubyObject val = eltOk(i);
            IRubyObject tmp = val.checkStringType();
            if (tmp == context.nil || tmp != val) {
                if (first == null) first = new boolean[] {false};
                else first[0] = false;
                len += (realLength - i) * 10;
                RubyString result = (RubyString) RubyString.newStringLight(runtime, len, USASCIIEncoding.INSTANCE).infectBy(this);
                joinStrings(sepString, i, result);
                first[0] = i == 0;
                return joinAny(context, sepString, i, result, first);
            }

            len += ((RubyString) tmp).getByteList().length();
        }

        return joinStrings(sepString, realLength, (RubyString) RubyString.newStringLight(runtime, len).infectBy(this));
    }

    @JRubyMethod(name = "join")
    public IRubyObject join19(ThreadContext context) {
        return join19(context, context.runtime.getGlobalVariables().get("$,"));
    }


    /** rb_ary_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a(ThreadContext context) {
        final RubyClass metaClass = this.metaClass;
        Ruby runtime = context.runtime;
        final RubyClass arrayClass = runtime.getArray();
        if (metaClass != arrayClass) {
            return dupImpl(runtime, arrayClass);
        }
        return this;
    }

    @JRubyMethod(name = "to_ary")
    public IRubyObject to_ary() {
    	return this;
    }

    @Deprecated
    public IRubyObject to_h(ThreadContext context) {
        return to_h(context, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "to_h")
    public IRubyObject to_h(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        int realLength = this.realLength;

        boolean useSmallHash = realLength <= 10;

        RubyHash hash = useSmallHash ? RubyHash.newSmallHash(runtime) : RubyHash.newHash(runtime);

        for (int i = 0; i < realLength; i++) {
            IRubyObject e = eltInternal(i);
            IRubyObject elt = block.isGiven() ? block.yield(context, e) : e;
            IRubyObject key_value_pair = elt.checkArrayType();

            if (key_value_pair == context.nil) {
                throw context.runtime.newTypeError("wrong element type " + elt.getMetaClass().getRealClass() + " at " + i + " (expected array)");
            }

            RubyArray ary = (RubyArray)key_value_pair;
            if (ary.getLength() != 2) {
                throw context.runtime.newArgumentError("wrong array length at " + i + " (expected 2, was " + ary.getLength() + ")");
            }

            if (useSmallHash) {
                hash.fastASetSmall(runtime, ary.eltInternal(0), ary.eltInternal(1), true);
            } else {
                hash.fastASet(runtime, ary.eltInternal(0), ary.eltInternal(1), true);
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
    @JRubyMethod(name = "==", required = 1)
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
        return RecursiveComparator.compare(context, sites(context).op_equal, this, obj);
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
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql(ThreadContext context, IRubyObject obj) {
        if(!(obj instanceof RubyArray)) {
            return context.fals;
        }
        return RecursiveComparator.compare(context, sites(context).eql, this, obj);
    }

    /** rb_ary_compact_bang
     *
     */
    @JRubyMethod(name = "compact!")
    public IRubyObject compact_bang() {
        unpack();
        modify();

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
            throw concurrentModification(getRuntime(), e);
        }

        p -= begin;
        if (realLength == p) return metaClass.runtime.getNil();

        realloc(p, values.length - begin);
        realLength = p;
        return this;
    }

    /** rb_ary_compact
     *
     */
    @JRubyMethod(name = "compact")
    public IRubyObject compact() {
        RubyArray ary = aryDup();
        ary.compact_bang();
        return ary;
    }

    @Deprecated
    public IRubyObject compact19() {
        return compact();
    }

    /** rb_ary_empty_p
     *
     */
    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p() {
        Ruby runtime = metaClass.runtime;
        return realLength == 0 ? runtime.getTrue() : runtime.getFalse();
    }

    /** rb_ary_clear
     *
     */
    @JRubyMethod(name = "clear")
    public IRubyObject rb_clear() {
        modifyCheck();

        if (isShared) {
            alloc(ARRAY_DEFAULT_SIZE);
            isShared = false;
        } else if (values.length > ARRAY_DEFAULT_SIZE << 1) {
            alloc(ARRAY_DEFAULT_SIZE << 1);
        } else {
            try {
                begin = 0;
                Helpers.fillNil(values, 0, realLength, metaClass.runtime);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw concurrentModification(getRuntime(), e);
            }
        }

        realLength = 0;
        return this;
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, Block block) {
        if (block.isGiven()) return fillCommon(context, 0, realLength, block);
        throw context.runtime.newArgumentError(0, 1);
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg, Block block) {
        if (block.isGiven()) {
            if (arg instanceof RubyRange) {
                int[] beglen = ((RubyRange) arg).begLenInt(realLength, 1);
                return fillCommon(context, beglen[0], beglen[1], block);
            }
            int beg;
            return fillCommon(context, beg = fillBegin(arg), fillLen(beg, null),  block);
        } else {
            return fillCommon(context, 0, realLength, arg);
        }
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (block.isGiven()) {
            int beg;
            return fillCommon(context, beg = fillBegin(arg1), fillLen(beg, arg2), block);
        } else {
            if (arg2 instanceof RubyRange) {
                int[] beglen = ((RubyRange) arg2).begLenInt(realLength, 1);
                return fillCommon(context, beglen[0], beglen[1], arg1);
            }
            int beg;
            return fillCommon(context, beg = fillBegin(arg2), fillLen(beg, null), arg1);
        }
    }

    @JRubyMethod
    public IRubyObject fill(ThreadContext context, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        if (block.isGiven()) {
            throw context.runtime.newArgumentError(3, 2);
        } else {
            int beg;
            return fillCommon(context, beg = fillBegin(arg2), fillLen(beg, arg3), arg1);
        }
    }

    private int fillBegin(IRubyObject arg) {
        int beg = arg.isNil() ? 0 : RubyNumeric.num2int(arg);
        if (beg < 0) {
            beg = realLength + beg;
            if (beg < 0) beg = 0;
        }
        return beg;
    }

    private long fillLen(long beg, IRubyObject arg) {
        if (arg == null || arg.isNil()) {
            return realLength - beg;
        } else {
            return RubyNumeric.num2long(arg);
        }
        // TODO: In MRI 1.9, an explicit check for negative length is
        // added here. IndexError is raised when length is negative.
        // See [ruby-core:12953] for more details.
        //
        // New note: This is actually under re-evaluation,
        // see [ruby-core:17483].
    }

    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, IRubyObject item) {
        unpack();
        modify();

        // See [ruby-core:17483]
        if (len <= 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw context.runtime.newArgumentError("argument too big");

        int end = (int)(beg + len);
        if (end > realLength) {
            int valuesLength = values.length - begin;
            if (end >= valuesLength) realloc(end, valuesLength);
            realLength = end;
        }

        if (len > 0) {
            try {
                Arrays.fill(values, begin + beg, begin + end, item);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(context.runtime, ex);
            }
        }

        return this;
    }

    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, Block block) {
        unpack();
        modify();

        // See [ruby-core:17483]
        if (len <= 0) return this;

        if (len > Integer.MAX_VALUE - beg) throw context.runtime.newArgumentError("argument too big");

        int end = (int)(beg + len);
        if (end > realLength) {
            int valuesLength = values.length - begin;
            if (end >= valuesLength) realloc(end, valuesLength);
            realLength = end;
        }

        final Ruby runtime = context.runtime;
        for (int i = beg; i < end; i++) {
            IRubyObject v = block.yield(context, runtime.newFixnum(i));
            if (i >= realLength) break;
            safeArraySet(runtime, values, begin + i, v);
        }
        return this;
    }


    /** rb_ary_index
     *
     */
    public IRubyObject index(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.runtime;

        for (int i = 0; i < realLength; i++) {
            if (equalInternal(context, eltOk(i), obj)) return runtime.newFixnum(i);
        }

        return context.nil;
    }

    @JRubyMethod(name = {"index", "find_index"})
    public IRubyObject index(ThreadContext context, IRubyObject obj, Block unused) {
        if (unused.isGiven()) context.runtime.getWarnings().warn(ID.BLOCK_UNUSED, "given block not used");
        return index(context, obj);
    }

    @JRubyMethod(name = {"index", "find_index"})
    public IRubyObject index(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorize(runtime, this, "index");

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return runtime.newFixnum(i);
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
            return RubyFixnum.newFixnum(context.runtime, rVal);
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

            if (v instanceof RubyFixnum) {
                long fixValue = ((RubyFixnum) v).getLongValue();
                if (fixValue == 0) return mid;
                smaller = fixValue < 0;
            } else if (v == context.tru) {
                satisfied = true;
                smaller = true;
            } else if (v == context.fals || v == context.nil) {
                smaller = false;
            } else if (runtime.getNumeric().isInstance(v)) {
                if (op_cmp == null) op_cmp = sites(context).op_cmp_bsearch;
                switch (RubyComparable.cmpint(context, op_cmp.call(context, v, v, RubyFixnum.zero(runtime)), v, RubyFixnum.zero(runtime))) {
                    case 0: return mid;
                    case 1: smaller = true; break;
                    case -1: smaller = false;
                }
            } else {
                throw runtime.newTypeError(str(runtime, "wrong argument type ", types(runtime, v.getType()), " (must be numeric, true, false or nil"));
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
        Ruby runtime = context.runtime;
        int i = realLength;

        while (i-- > 0) {
            if (i > realLength) {
                i = realLength;
                continue;
            }
            if (equalInternal(context, eltOk(i), obj)) return runtime.newFixnum(i);
        }

        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject rindex(ThreadContext context, IRubyObject obj, Block unused) {
        if (unused.isGiven()) context.runtime.getWarnings().warn(ID.BLOCK_UNUSED, "given block not used");
        return rindex(context, obj);
    }

    @JRubyMethod
    public IRubyObject rindex(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorize(runtime, this, "rindex");

        int i = realLength;

        while (i-- > 0) {
            if (i >= realLength) {
                i = realLength;
                continue;
            }
            if (block.yield(context, eltOk(i)).isTrue()) return runtime.newFixnum(i);
        }

        return runtime.getNil();
    }

    /** rb_ary_indexes
     *
     */
    @JRubyMethod(name = {"indexes", "indices"}, required = 1, rest = true)
    public IRubyObject indexes(IRubyObject[] args) {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Array#indexes is deprecated; use Array#values_at");

        if (args.length == 1) return newArray(getRuntime(), args[0]);

        RubyArray ary = newBlankArrayInternal(getRuntime(), args.length);

        for (int i = 0; i < args.length; i++) {
            ary.storeInternal(i, aref(args[i]));
        }
        ary.realLength = args.length;

        return ary;
    }

    /** rb_ary_reverse_bang
     *
     */
    @JRubyMethod(name = "reverse!")
    public IRubyObject reverse_bang() {
        modify();

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
            throw concurrentModification(getRuntime(), e);
        }
        return this;
    }

    /** rb_ary_reverse_m
     *
     */
    @JRubyMethod(name = "reverse")
    public IRubyObject reverse() {
        if (realLength > 1) {
            return safeReverse();
        }
        return aryDup();
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
            throw concurrentModification(runtime, e);
        }
        return new RubyArray(runtime, runtime.getArray(), vals);
    }

    /** rb_ary_collect
     *
     */
    public RubyArray collectCommon(ThreadContext context, Block block) {
        if (!block.isGiven()) return makeShared();

        final Ruby runtime = context.runtime;

        IRubyObject[] arr = IRubyObject.array(realLength);

        int i = 0;
        for (; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            safeArraySet(runtime, arr, i, block.yieldNonArray(context, eltOk(i), null)); // arr[i] = ...
        }

        // use iteration count as new size in case something was deleted along the way
        return newArrayMayCopy(context.runtime, arr, 0, i);
    }

    @JRubyMethod(name = {"collect"})
    public IRubyObject collect(ThreadContext context, Block block) {
        return block.isGiven() ? collectCommon(context, block) : enumeratorizeWithSize(context, this, "collect", RubyArray::size);
    }

    @JRubyMethod(name = {"map"})
    public IRubyObject map(ThreadContext context, Block block) {
        return block.isGiven() ? collectCommon(context, block) : enumeratorizeWithSize(context, this, "map", RubyArray::size);
    }

    /** rb_ary_collect_bang
     *
     */
    public RubyArray collectBang(ThreadContext context, Block block) {
        if (!block.isGiven()) throw context.runtime.newLocalJumpErrorNoBlock();
        modify();

        for (int i = 0, len = realLength; i < len; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            storeInternal(i, block.yield(context, eltOk(i)));
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

        RubyArray result = newArray(runtime, realLength);

        for (int i = 0; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            IRubyObject value = eltOk(i);

            if (block.yield(context, value).isTrue()) result.append(value);
        }

        Helpers.fillNil(result.values, result.realLength, result.values.length, runtime);
        return result;
    }

    @JRubyMethod(name = "select", alias = "filter")
    public IRubyObject select(ThreadContext context, Block block) {
        return block.isGiven() ? selectCommon(context, block) : enumeratorizeWithSize(context, this, "select", RubyArray::size);
    }

    @JRubyMethod(name = "select!", alias = "filter!")
    public IRubyObject select_bang(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select!", RubyArray::size);

        unpack();
        modify();

        final Ruby runtime = context.runtime;
        final int len = realLength; final int beg = begin;

        int len0 = 0, len1 = 0;
        try {
            int i1, i2;
            for (i1 = i2 = 0; i1 < len; len0 = ++i1) {
                final IRubyObject[] values = this.values;
                // Do not coarsen the "safe" check, since it will misinterpret
                // AIOOBE from the yield (see JRUBY-5434)
                IRubyObject value = safeArrayRef(runtime, values, begin + i1);

                if (!block.yield(context, value).isTrue()) continue;

                if (i1 != i2) safeArraySet(runtime, values, beg + i2, value);
                len1 = ++i2;
            }
            return (i1 == i2) ? context.nil : this;
        }
        finally {
            selectBangEnsure(runtime, len, beg, len0, len1);
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

    /** rb_ary_delete
     *
     */
    @JRubyMethod(required = 1)
    public IRubyObject delete(ThreadContext context, IRubyObject item, Block block) {
        unpack();
        int i2 = 0;
        IRubyObject value = item;

        final Ruby runtime = context.runtime;
        for (int i1 = 0; i1 < realLength; i1++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from equalInternal
            // See JRUBY-5434
            IRubyObject e = safeArrayRef(runtime, values, begin + i1);
            if (equalInternal(context, e, item)) {
                value = e;
                continue;
            }
            if (i1 != i2) store(i2, e);
            i2++;
        }

        if (realLength == i2) {
            if (block.isGiven()) return block.yield(context, item);

            return context.nil;
        }

        modify();

        final int myRealLength = this.realLength;
        final int myBegin = this.begin;
        final IRubyObject[] myValues = this.values;
        try {
            if (myRealLength > i2) {
                Helpers.fillNil(myValues, myBegin + i2, myBegin + myRealLength, context.runtime);
                this.realLength = i2;
                int valuesLength = myValues.length - myBegin;
                if (i2 << 1 < valuesLength && valuesLength > ARRAY_DEFAULT_SIZE) realloc(i2 << 1, valuesLength);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }

        return value;
    }

    /** rb_ary_delete_at
     *
     */
    public IRubyObject delete_at(int pos) {
        int len = realLength;
        if (pos >= len || (pos < 0 && (pos += len) < 0)) return metaClass.runtime.getNil();

        unpack();
        modify();

        IRubyObject nil = metaClass.runtime.getNil();
        IRubyObject obj;

        try {
            obj = values[begin + pos];
            // fast paths for head and tail
            if (pos == 0) {
                values[begin] = nil;
                begin++;
                realLength--;
                return obj;
            } else if (pos == realLength - 1) {
                values[begin + realLength - 1] = nil;
                realLength--;
                return obj;
            }

            System.arraycopy(values, begin + pos + 1, values, begin + pos, len - (pos + 1));
            values[begin + len - 1] = nil;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime(), e);
        }
        realLength--;

        return obj;
    }

    /** rb_ary_delete_at_m
     *
     */
    @JRubyMethod(name = "delete_at", required = 1)
    public IRubyObject delete_at(IRubyObject obj) {
        return delete_at((int) RubyNumeric.num2long(obj));
    }

    /** rb_ary_reject_bang
     *
     */
    public final IRubyObject rejectCommon(ThreadContext context, Block block) {
        RubyArray ary = aryDup();
        ary.rejectBang(context, block);
        return ary;
    }

    @JRubyMethod
    public IRubyObject reject(ThreadContext context, Block block) {
        return block.isGiven() ? rejectCommon(context, block) : enumeratorizeWithSize(context, this, "reject", RubyArray::size);
    }

    // MRI: ary_reject_bang and reject_bang_i
    public IRubyObject rejectBang(ThreadContext context, Block block) {
        unpack();
        modify();

        final Ruby runtime = context.runtime;
        final int beg = begin;

        int len0 = 0, len1 = 0;
        try {
            int i1, i2;
            for (i1 = i2 = 0; i1 < realLength; len0 = ++i1) {
                final IRubyObject[] values = this.values;
                // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
                // See JRUBY-5434
                IRubyObject value = safeArrayRef(runtime, values, beg + i1);

                if (block.yield(context, value).isTrue()) continue;

                if (i1 != i2) safeArraySet(runtime, values, beg + i2, value);
                len1 = ++i2;
            }

            return (i1 == i2) ? context.nil : this;
        }
        finally {
            selectBangEnsure(runtime, realLength, beg, len0, len1);
        }
    }

    // MRI: select_bang_ensure
    private void selectBangEnsure(final Ruby runtime, final int len, final int beg,
        int i1, int i2) {
        if (i2 < i1) {
            realLength = len - i1 + i2;
            if (i1 < len) {
                safeArrayCopy(runtime, values, beg + i1, values, beg + i2, len - i1);
            }
            else if (realLength > 0) {
                // nil out left-overs to avoid leaks (MRI doesn't)
                try {
                    Helpers.fillNil(values, beg + i2, beg + i1, runtime);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    throw concurrentModification(runtime, ex);
                }
            }
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
    @JRubyMethod(optional = 1, rest = true)
    public IRubyObject zip(ThreadContext context, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;
        RubyClass array = runtime.getArray();
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
            RubySymbol each = runtime.newSymbol("each");
            for (int i = 0; i < args.length; i++) {
                IRubyObject arg = args[i];
                if (!arg.respondsTo("each")) throw runtime.newTypeError(arg, "must respond to :each");
                newArgs[i] = to_enum.call(context, arg, arg, each);
            }
        }

        if (hasUncoercible) {
            return zipCommon(context, newArgs, block, new ArgumentVisitor() {
                public IRubyObject visit(ThreadContext ctx, IRubyObject arg, int i) {
                    return RubyEnumerable.zipEnumNext(ctx, arg);
                }
            });
        } else {
            return zipCommon(context, newArgs, block, new ArgumentVisitor() {
                public IRubyObject visit(ThreadContext ctx, IRubyObject arg, int i) {
                    return ((RubyArray) arg).elt(i);
                }
            });
        }
    }

    // This can be shared with RubyEnumerable to clean #zipCommon{Enum,Arg} a little
    public static interface ArgumentVisitor {
        IRubyObject visit(ThreadContext ctx, IRubyObject arg, int i);
    }

    private IRubyObject zipCommon(ThreadContext context, IRubyObject[] args, Block block, ArgumentVisitor visitor) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) {
            for (int i = 0; i < realLength; i++) {
                IRubyObject[] tmp = IRubyObject.array(addBufferLength(runtime, args.length, 1));
                // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
                // See JRUBY-5434
                tmp[0] = eltInternal(i);
                for (int j = 0; j < args.length; j++) {
                    tmp[j + 1] = visitor.visit(context, args[j], i);
                }
                block.yield(context, newArrayMayCopy(runtime, tmp));
            }
            return runtime.getNil();
        }

        IRubyObject[] result = IRubyObject.array(realLength);
        try {
            for (int i = 0; i < realLength; i++) {
                IRubyObject[] tmp = IRubyObject.array(addBufferLength(runtime, args.length, 1));
                tmp[0] = eltInternal(i);
                for (int j = 0; j < args.length; j++) {
                    tmp[j + 1] = visitor.visit(context, args[j], i);
                }
                result[i] = newArrayMayCopy(runtime, tmp);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
        return newArrayMayCopy(runtime, result);
    }

    /** rb_ary_cmp
     *
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject obj) {
        final Ruby runtime = context.runtime;

        boolean isAnArray = (obj instanceof RubyArray) || obj.getMetaClass().getSuperClass() == runtime.getArray();

        if (!isAnArray && !sites(context).respond_to_to_ary.respondsTo(context, obj, obj, true)) {
            return context.nil;
        }

        RubyArray ary2;
        if (!isAnArray) {
            ary2 = (RubyArray) sites(context).to_ary.call(context, obj, obj);
        } else {
            ary2 = obj.convertToArray();
        }

        return cmpCommon(context, runtime, ary2);
    }

    private IRubyObject cmpCommon(ThreadContext context, Ruby runtime, RubyArray ary2) {
        if (this == ary2 || runtime.isInspecting(this)) return RubyFixnum.zero(runtime);

        try {
            runtime.registerInspecting(this);

            int len = realLength;
            if (len > ary2.realLength) len = ary2.realLength;

            CallSite cmp = sites(context).cmp;
            for (int i = 0; i < len; i++) {
                IRubyObject elt = elt(i);
                IRubyObject v = cmp.call(context, elt, elt, ary2.elt(i));
                if (!(v instanceof RubyFixnum) || ((RubyFixnum) v).getLongValue() != 0) return v;
            }
        } finally {
            runtime.unregisterInspecting(this);
        }

        int len = realLength - ary2.realLength;

        if (len == 0) return RubyFixnum.zero(runtime);
        if (len > 0) return RubyFixnum.one(runtime);

        return RubyFixnum.minus_one(runtime);
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * @deprecated Use the versions with zero, one, or two args.
     */
    public IRubyObject slice_bang(IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return slice_bang(args[0]);
        case 2:
            return slice_bang(args[0], args[1]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    private IRubyObject slice_internal(final Ruby runtime, int pos, int len) {
        if (len < 0) return runtime.getNil();
        int orig_len = realLength;
        if (pos < 0) {
            pos += orig_len;
            if (pos < 0) return runtime.getNil();
        } else if (orig_len < pos) {
            return runtime.getNil();
        }

        if (orig_len < pos + len) {
            len = orig_len - pos;
        }
        if (len == 0) {
            return runtime.newEmptyArray();
        }

        unpack();

        RubyArray result = makeShared(begin + pos, len, metaClass);
        splice(runtime, pos, len, null, 0);

        return result;
    }

    /** rb_ary_slice_bang
     *
     */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0) {
        modifyCheck();
        Ruby runtime = metaClass.runtime;
        if (arg0 instanceof RubyRange) {
            RubyRange range = (RubyRange) arg0;
            if (!range.checkBegin(realLength)) {
                return runtime.getNil();
            }

            int pos = checkInt(runtime, range.begLen0(realLength));
            int len = checkInt(runtime, range.begLen1(realLength, pos));
            return slice_internal(runtime, pos, len);
        }
        return delete_at(RubyNumeric.num2int(arg0));
    }

    /** rb_ary_slice_bang
    *
    */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        modifyCheck();

        return slice_internal(metaClass.runtime, RubyNumeric.num2int(arg0), RubyNumeric.num2int(arg1));
    }

    /** rb_ary_assoc
     *
     */
    @JRubyMethod(name = "assoc", required = 1)
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
    @JRubyMethod(name = "rassoc", required = 1)
    public IRubyObject rassoc(ThreadContext context, IRubyObject value) {
        Ruby runtime = context.runtime;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);
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
        unpack();
        final Ruby runtime = context.runtime;

        ArrayList<Object> stack = null;
        IdentityHashMap<RubyArray, IRubyObject> memo = null; // used as an IdentityHashSet

        RubyArray ary = this;
        int i = 0;

        try {
            while (true) {
                while (i < ary.realLength) {
                    IRubyObject elt = ary.eltOk(i++);
                    if (level >= 0 && (stack == null ? 0 : stack.size()) / 2 >= level) {
                        result.append(elt);
                        continue;
                    }
                    IRubyObject tmp = TypeConverter.checkArrayType(context, elt);
                    if (tmp == context.nil) result.append(elt);
                    else { // nested array element
                        if (memo == null) {
                            memo = new IdentityHashMap<>(4 + 1);
                            memo.put(this, NEVER);
                        }
                        if (memo.get(tmp) != null) throw runtime.newArgumentError("tried to flatten recursive array");
                        if (stack == null) stack = new ArrayList<>(8); // fine hold 4-level deep nesting
                        stack.add(ary); stack.add(i); // add (ary, i) pair
                        ary = (RubyArray) tmp;
                        memo.put(ary, NEVER);
                        i = 0;
                    }
                }
                if (stack == null || stack.size() == 0) break;
                memo.remove(ary); // memo != null since stack != null
                final int s = stack.size(); // pop (ary, i)
                i = (Integer) stack.remove(s - 1);
                ary = (RubyArray) stack.remove(s - 2);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }
        return stack != null;
    }

    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(ThreadContext context) {
        unpack();
        modifyCheck();

        RubyArray result = new RubyArray(context.runtime, getType(), realLength);
        if (flatten(context, -1, result)) {
            modifyCheck();
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
        unpack();
        modifyCheck();

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

    @Deprecated
    public IRubyObject flatten_bang19(ThreadContext context, IRubyObject arg) {
        return flatten_bang(context, arg);
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context) {
        Ruby runtime = context.runtime;

        RubyArray result = new RubyArray(runtime, getType(), realLength);
        flatten(context, -1, result);
        result.infectBy(this);
        return result;
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        int level = RubyNumeric.num2int(arg);
        if (level == 0) return makeShared();

        RubyArray result = new RubyArray(runtime, getType(), realLength);
        flatten(context, level, result);
        result.infectBy(this);
        return result;
    }

    @Deprecated
    public IRubyObject flatten19(ThreadContext context) {
        return flatten(context);
    }

    @Deprecated
    public IRubyObject flatten19(ThreadContext context, IRubyObject arg) {
        return flatten(context, arg);
    }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, Block block) {
        if (block.isGiven()) {
            int n = 0;
            for (int i = 0; i < realLength; i++) {
                if (block.yield(context, elt(i)).isTrue()) n++;
            }
            return RubyFixnum.newFixnum(context.runtime, n);
        } else {
            return RubyFixnum.newFixnum(context.runtime, realLength);
        }
    }

    @JRubyMethod(name = "count")
    public IRubyObject count(ThreadContext context, IRubyObject obj, Block block) {
        if (block.isGiven()) context.runtime.getWarnings().warn(ID.BLOCK_UNUSED, "given block not used");

        int n = 0;
        for (int i = 0; i < realLength; i++) {
            if (equalInternal(context, elt(i), obj)) n++;
        }
        return RubyFixnum.newFixnum(context.runtime, n);
    }

    /** rb_ary_nitems
     *
     */
    @JRubyMethod(name = "nitems")
    public IRubyObject nitems() {
        int n = 0;

        for (int i = 0; i < realLength; i++) {
            if (!eltOk(i).isNil()) n++;
        }

        return metaClass.runtime.newFixnum(n);
    }

    /** rb_ary_plus
     *
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject obj) {
        Ruby runtime = metaClass.runtime;
        RubyArray y = obj.convertToArray();
        int len = realLength + y.realLength;

        switch (len) {
            case 1:
                return new RubyArrayOneObject(runtime, realLength == 1 ? eltInternal(0) : y.eltInternal(0));
            case 2:
                switch (realLength) {
                    case 0:
                        return newArray(runtime, y.eltInternal(0), y.eltInternal(1));
                    case 1:
                        return newArray(runtime, eltInternal(0), y.eltInternal(0));
                    case 2:
                        return newArray(runtime, eltInternal(0), eltInternal(1));
                }
        }

        RubyArray z = newArray(runtime, len);
        try {
            copyInto(z.values, 0);
            y.copyInto(z.values, realLength);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime, e);
        }
        z.realLength = len;
        return z;
    }

    /** rb_ary_times
     *
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_times(ThreadContext context, IRubyObject times) {
        IRubyObject tmp = times.checkStringType();

        if (!tmp.isNil()) return join19(context, tmp);

        long len = RubyNumeric.num2long(times);
        Ruby runtime = context.runtime;
        if (len == 0) return new RubyArray(runtime, metaClass, IRubyObject.NULL_ARRAY).infectBy(this);
        if (len < 0) throw runtime.newArgumentError("negative argument");

        if (Long.MAX_VALUE / len < realLength) {
            throw runtime.newArgumentError("argument too big");
        }

        len *= realLength;

        checkLength(runtime, len);
        RubyArray ary2 = new RubyArray(runtime, metaClass, (int)len);
        ary2.realLength = ary2.values.length;

        try {
            for (int i = 0; i < len; i += realLength) {
                copyInto(ary2.values, i);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime, e);
        }

        ary2.infectBy(this);

        return ary2;
    }

    @Deprecated
    public IRubyObject op_times19(ThreadContext context, IRubyObject times) {
        return op_times(context, times);
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
            throw concurrentModification(context.runtime, ex);
        }
    }

    private void clearValues(final int from, final int to) {
        try {
            Helpers.fillNil(values, begin + from, begin + to, metaClass.runtime);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(getRuntime(), ex);
        }
    }

    /** rb_ary_uniq_bang
     *
     */
    public IRubyObject uniq_bang(ThreadContext context) {
        final RubyHash hash = makeHash(context.runtime);
        final int newLength = hash.size;
        if (realLength == newLength) return context.nil;

        modify(); // in case array isShared
        unpack();

        setValuesFrom(context, hash);
        clearValues(newLength, realLength);
        realLength = newLength;

        return this;
    }

    @JRubyMethod(name = "uniq!")
    public IRubyObject uniq_bang(ThreadContext context, Block block) {
        modifyCheck();

        if (!block.isGiven()) return uniq_bang(context);

        final RubyHash hash = makeHash(context, block);
        final int newLength = hash.size;
        if (realLength == newLength) return context.nil;

        // after evaluating the block, a new modify check is needed
        modify(); // in case array isShared
        unpack();

        setValuesFrom(context, hash);
        clearValues(newLength, realLength);
        realLength = newLength;

        return this;
    }

    @Deprecated
    public IRubyObject uniq_bang19(ThreadContext context, Block block) {
        return uniq_bang(context, block);
    }

    /** rb_ary_uniq
     *
     */
    public IRubyObject uniq(ThreadContext context) {
        RubyHash hash = makeHash(context.runtime);
        final int newLength = hash.size;
        if (realLength == newLength) return makeShared();

        RubyArray result = newBlankArrayInternal(context.runtime, metaClass, newLength);
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

        RubyArray result = newBlankArrayInternal(context.runtime, metaClass, newLength);
        result.setValuesFrom(context, hash);
        result.realLength = newLength;
        return result;
    }

    @Deprecated
    public IRubyObject uniq19(ThreadContext context, Block block) {
        return uniq(context, block);
    }

    /** rb_ary_diff
     *
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_diff(IRubyObject other) {
        final Ruby runtime = metaClass.runtime;

        final int len = realLength;
        RubyArray res = newBlankArrayInternal(runtime, len);

        int index = 0;
        RubyHash hash = other.convertToArray().makeHash(runtime);
        for (int i = 0; i < len; i++) {
            IRubyObject val = eltOk(i);
            if (hash.fastARef(val) == null) res.storeInternal(index++, val);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 0) return newEmptyArray(runtime);
        if (index == 1 && len == 2) return newArray(runtime, res.eltInternal(0));

        assert index == res.realLength;
        if (!(res instanceof RubyArraySpecialized)) {
            Helpers.fillNil(res.values, index, res.values.length, runtime);
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

        RubyArray diff = newArray(context.runtime);

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
            if (j == args.length) diff.append(elt);
        }

        return diff;
    }

    /** rb_ary_and
     *
     */
    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and(IRubyObject other) {
        final Ruby runtime = metaClass.runtime;

        RubyArray ary2 = other.convertToArray();

        final int len = realLength;
        int maxSize = len < ary2.realLength ? len : ary2.realLength;
        RubyArray res;
        switch (maxSize) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (len == 0 || ary2.realLength == 0) return newEmptyArray(runtime);
            default:
                res = newBlankArrayInternal(runtime, maxSize);
                break;
        }

        int index = 0;
        RubyHash hash = ary2.makeHash(runtime);
        for (int i = 0; i < len; i++) {
            IRubyObject val = elt(i);
            if (hash.fastDelete(val)) res.storeInternal(index++, val);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 0) return newEmptyArray(runtime);
        if (index == 1 && maxSize == 2) return newArray(runtime, res.eltInternal(0));

        assert index == res.realLength;
        if (!(res instanceof RubyArraySpecialized)) {
            Helpers.fillNil(res.values, index, res.values.length, runtime);
        }

        return res;
    }

    /** rb_ary_or
     *
     */
    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or(IRubyObject other) {
        final Ruby runtime = metaClass.runtime;
        RubyArray ary2 = other.convertToArray();

        int maxSize = realLength + ary2.realLength;
        if (maxSize == 0) return newEmptyArray(runtime);

        RubyHash set = ary2.makeHash(makeHash(runtime));
        RubyArray res = newBlankArrayInternal(runtime, set.size);
        res.setValuesFrom(runtime.getCurrentContext(), set);
        res.realLength = set.size;

        int index = res.realLength;
        // if index is 1 and we made a size 2 array, repack
        if (index == 1 && maxSize == 2) return newArray(runtime, res.eltInternal(0));

        return res;
    }

    /** rb_ary_union_multi
     *
     */
    @JRubyMethod(name = "union", rest = true)
    public IRubyObject union(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyArray[] arrays = new RubyArray[args.length];
        RubyArray result;

        int maxSize = realLength;
        for (int i = 0; i < args.length; i++) {
            arrays[i] = args[i].convertToArray();
            maxSize += arrays[i].realLength;
        }
        if (maxSize == 0) return newEmptyArray(runtime);

        if (maxSize <= ARRAY_DEFAULT_SIZE) {
            result = newArray(runtime);

            result.unionInternal(context, this);
            result.unionInternal(context, arrays);

            return result;
        }

        RubyHash set = makeHash(runtime);

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
                append(elt);
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

    @Deprecated
    public RubyArray sort19(ThreadContext context, Block block) {
        return sort(context, block);
    }

    /** rb_ary_sort_bang
     *
     */
    @JRubyMethod(name = "sort!")
    public IRubyObject sort_bang(ThreadContext context, Block block) {
        modify();
        if (realLength > 1) {
            return block.isGiven() ? sortInternal(context, block) : sortInternal(context, true);
        }
        return this;
    }

    @Deprecated
    public IRubyObject sort_bang19(ThreadContext context, Block block) {
        return sort_bang(context, block);
    }

    protected IRubyObject sortInternal(final ThreadContext context, final boolean honorOverride) {
        try {
            Arrays.sort(values, begin, begin + realLength, new DefaultComparator(context, honorOverride) {
                protected int compareGeneric(IRubyObject o1, IRubyObject o2) {
                    int result = super.compareGeneric(o1, o2);
                    modifyCheck();
                    return result;
                }
            });
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
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
                this.fixnumBypass = !honorOverride || context.runtime.getFixnum().isMethodBuiltin("<=>");
                this.stringBypass = !honorOverride || context.runtime.getString().isMethodBuiltin("<=>");
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
            long a = o1.getLongValue();
            long b = o2.getLongValue();
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
            final ThreadContext context = context();
            return block.yieldArray(context, context.runtime.newArray(obj1, obj2), self);
        }

        protected final ThreadContext context() {
            return context;
        }

    }

    protected IRubyObject sortInternal(final ThreadContext context, final Block block) {
        // block code can modify, so we need to iterate
        unpack();
        IRubyObject[] newValues = IRubyObject.array(realLength);
        int length = realLength;

        copyInto(newValues, 0);
        CallSite gt = sites(context).op_gt_sort;
        CallSite lt = sites(context).op_lt_sort;
        Arrays.sort(newValues, 0, length, new BlockComparator(context, block, gt, lt) {
            @Override
            public int compare(IRubyObject obj1, IRubyObject obj2) {
                int result = super.compare(obj1, obj2);
                modifyCheck();
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

        modifyCheck();

        RubyArray sorted = sites(context).sort_by.call(context, this, this, block).convertToArray();

        replace(sorted);

        return this;
    }

    /** rb_ary_take
     *
     */
    @JRubyMethod(name = "take")
    public IRubyObject take(ThreadContext context, IRubyObject n) {
        Ruby runtime = context.runtime;
        long len = RubyNumeric.num2long(n);
        if (len < 0) throw runtime.newArgumentError("attempt to take negative size");

        return subseq(0, len);
    }

    /** rb_ary_take_while
     *
     */
    @JRubyMethod(name = "take_while")
    public IRubyObject take_while(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorize(runtime, this, "take_while");

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
        Ruby runtime = context.runtime;
        long pos = RubyNumeric.num2long(n);
        if (pos < 0) throw runtime.newArgumentError("attempt to drop negative size");

        IRubyObject result = subseq(pos, realLength);
        return result.isNil() ? runtime.newEmptyArray() : result;
    }

    /** rb_ary_take_while
     *
     */
    @JRubyMethod(name = "drop_while")
    public IRubyObject drop_while(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorize(runtime, this, "drop_while");

        int i = 0;
        for (; i < realLength; i++) {
            if (!block.yield(context, eltOk(i)).isTrue()) break;
        }
        IRubyObject result = subseq(i, realLength);
        return result.isNil() ? runtime.newEmptyArray() : result;
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

        long times = RubyNumeric.num2long(arg);
        if (times <= 0) return context.nil;

        return cycleCommon(context, times, block);
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
        Ruby runtime = context.runtime;
        IRubyObject n = context.nil;

        if (self.realLength == 0) {
            return RubyFixnum.zero(runtime);
        }

        if (args != null && args.length > 0) {
            n = args[0];
        }

        if (n == null || n.isNil()) {
            return RubyFloat.newFloat(runtime, RubyFloat.INFINITY);
        }

        long multiple = RubyNumeric.num2long(n);
        if (multiple <= 0) {
            return RubyFixnum.zero(runtime);
        }

        RubyFixnum length = self.length();
        return sites(context).op_times.call(context, length, length, RubyFixnum.newFixnum(runtime, multiple));
    }


    /** rb_ary_product
     *
     */
    public IRubyObject product(ThreadContext context, IRubyObject[] args) {
        return product(context, args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "product", rest = true)
    public IRubyObject product(ThreadContext context, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        boolean useBlock = block.isGiven();

        int n = args.length + 1;
        RubyArray arrays[] = new RubyArray[n];
        int counters[] = new int[n];

        arrays[0] = this;
        RubyClass array = runtime.getArray();
        JavaSites.CheckedSites to_ary_checked = sites(context).to_ary_checked;
        for (int i = 1; i < n; i++) arrays[i] = (RubyArray) TypeConverter.convertToType(context, args[i - 1], array, to_ary_checked);

        int resultLen = 1;
        for (int i = 0; i < n; i++) {
            int k = arrays[i].realLength;
            int l = resultLen;
            if (k == 0) return useBlock ? this : newEmptyArray(runtime);
            resultLen *= k;
            if (resultLen < k || resultLen < l || resultLen / k != l) {
                if (!block.isGiven()) throw runtime.newRangeError("too big to product");
            }
        }

        RubyArray result = useBlock ? null : newBlankArrayInternal(runtime, resultLen);

        for (int i = 0; i < resultLen; i++) {
            RubyArray sub = newBlankArrayInternal(runtime, n);
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

    @Deprecated
    public IRubyObject product19(ThreadContext context, IRubyObject[] args, Block block) {
        return product(context, args, block);
    }

    /** rb_ary_combination
     *
     */
    @JRubyMethod(name = "combination")
    public IRubyObject combination(ThreadContext context, IRubyObject num, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "combination", new IRubyObject[]{num}, RubyArray::combinationSize);

        int n = RubyNumeric.num2int(num);

        if (n == 0) {
            block.yield(context, newEmptyArray(runtime));
        } else if (n == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, newArray(runtime, eltOk(i)));
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
        long k = ((RubyNumeric) args[0]).getLongValue();

        return binomialCoefficient(context, k, n);
    }

    private static IRubyObject binomialCoefficient(ThreadContext context, long comb, long size) {
        Ruby runtime = context.runtime;
        if (comb > size - comb) {
            comb = size - comb;
        }

        if (comb < 0) {
            return RubyFixnum.zero(runtime);
        }

        IRubyObject r = descendingFactorial(context, size, comb);
        IRubyObject v = descendingFactorial(context, comb, comb);
        return sites(context).op_quo.call(context, r, r, v);
    }

    @JRubyMethod(name = "repeated_combination")
    public IRubyObject repeatedCombination(ThreadContext context, IRubyObject num, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "repeated_combination", new IRubyObject[] { num }, RubyArray::repeatedCombinationSize);

        int n = RubyNumeric.num2int(num);

        if (n < 0) {
            // yield nothing
        } else if (n == 0) {
            block.yield(context, newEmptyArray(runtime));
        } else if (n == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, newArray(runtime, eltOk(i)));
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
        long k = ((RubyNumeric) args[0]).getLongValue();

        if (k == 0) {
            return RubyFixnum.one(context.runtime);
        }

        return binomialCoefficient(context, k, n + k - 1);
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
        RubyFixnum n = self.length();
        assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #repeated_permutation ensures arg[0] is numeric
        long k = ((RubyNumeric) args[0]).getLongValue();

        Ruby runtime = context.runtime;
        if (k < 0) {
            return RubyFixnum.zero(runtime);
        }

        RubyFixnum v = RubyFixnum.newFixnum(runtime, k);
        return sites(context).op_exp.call(context, n, n, v);
    }

    private IRubyObject permutationCommon(ThreadContext context, int r, boolean repeat, Block block) {
        if (r == 0) {
            block.yield(context, newEmptyArray(context.runtime));
        } else if (r == 1) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, newArray(context.runtime, eltOk(i)));
            }
        } else if (r >= 0) {
            unpack();
            int n = realLength;
            if (repeat) {
                rpermute(context, n, r,
                        new int[r],
                        makeShared(begin, n, metaClass), block);
            } else {
                permute(context, n, r,
                        new int[r],
                        new boolean[n],
                        makeShared(begin, n, metaClass),
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
            k = ((RubyNumeric) args[0]).getLongValue();
        } else {
            k = n;
        }

        return descendingFactorial(context, n, k);
    }

    private static IRubyObject descendingFactorial(ThreadContext context, long from, long howMany) {
        Ruby runtime = context.runtime;
        IRubyObject cnt = howMany >= 0 ? RubyFixnum.one(runtime) : RubyFixnum.zero(runtime);
        CallSite op_times = sites(context).op_times;
        while (howMany-- > 0) {
            RubyFixnum v = RubyFixnum.newFixnum(runtime, from--);
            cnt = op_times.call(context, cnt, cnt, v);
        }
        return cnt;
    }

    @Deprecated
    public IRubyObject choice(ThreadContext context) {
        if (realLength == 0) {
            return context.nil;
        }
        return eltOk((int) (context.runtime.getDefaultRand().genrandReal() * realLength));
    }

    @JRubyMethod(name = "shuffle!")
    public IRubyObject shuffle_bang(ThreadContext context) {
        return shuffleBang(context, context.runtime.getRandomClass());
    }

    @JRubyMethod(name = "shuffle!")
    public IRubyObject shuffle_bang(ThreadContext context, IRubyObject opts) {
        Ruby runtime = context.runtime;

        IRubyObject hash = TypeConverter.checkHashType(runtime, opts);

        if (hash.isNil()) {
            throw runtime.newArgumentError(1, 0, 0);
        }

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");

        if (ret == null) {
            return shuffle(context);
        }

        return shuffleBang(context, ret);
    }

    private IRubyObject shuffleBang(ThreadContext context, IRubyObject randgen) {
        Ruby runtime = context.runtime;

        modify();

        int i = realLength;
        int len = i;
        try {
            while (i > 0) {
                int r = (int) RubyRandom.randomLongLimited(context, randgen, i - 1);
                if (len != realLength) { // || ptr != RARRAY_CONST_PTR(ary)
                    throw runtime.newRuntimeError("modified during shuffle");
                }
                T tmp = eltOk(--i);
                eltSetOk(i, eltOk(r));
                eltSetOk(r, tmp);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
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
        return sampleCommon(context, context.runtime.getRandomClass());
    }

    @JRubyMethod(name = "sample")
    public IRubyObject sample(ThreadContext context, IRubyObject sampleOrOpts) {
        final Ruby runtime = context.runtime;

        IRubyObject hash = TypeConverter.checkHashType(runtime, sampleOrOpts);

        if (hash.isNil()) {
            return sampleCommon(context, sampleOrOpts, runtime.getRandomClass());
        }

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");

        return sampleCommon(context, ret != null ? ret : runtime.getRandomClass());
    }

    @JRubyMethod(name = "sample")
    public IRubyObject sample(ThreadContext context, IRubyObject sample, IRubyObject opts) {
        final Ruby runtime = context.runtime;

        IRubyObject hash = TypeConverter.checkHashType(runtime, opts);

        if (hash.isNil()) {
            throw runtime.newArgumentError(2, 0, 1);
        }

        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) hash, "random");

        return sampleCommon(context, sample, ret != null ? ret : runtime.getRandomClass());
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
        Ruby runtime = context.runtime;

        int n = RubyNumeric.num2int(sample);

        try {
            if (n < 0) throw runtime.newArgumentError("negative sample number");
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
                return newEmptyArray(runtime);
            case 1:
                return realLength <= 0 ? newEmptyArray(runtime) : newArray(runtime, eltOk((int) rnds[0]));
            case 2:
                i = (int) rnds[0];
                j = (int) rnds[1];

                if (j >= i) j++;

                return newArray(runtime, eltOk(i), eltOk(j));
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

                return newArray(runtime, eltOk(i), eltOk(j), eltOk(k));
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
                return RubyArray.newArrayMayCopy(runtime, result);
            } else {
                IRubyObject[] result = IRubyObject.array(len);
                System.arraycopy(values, begin, result, 0, len);
                for (i = 0; i < n; i++) {
                    j = (int) RubyRandom.randomLongLimited(context, randgen, len - i - 1) + i;
                    IRubyObject tmp = result[j];
                    result[j] = result[i];
                    result[i] = tmp;
                }
                RubyArray ary = newArrayNoCopy(runtime, result);
                ary.realLength = n;
                return ary;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
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
        modify();

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
            throw concurrentModification(context.runtime, ex);
        }

        return context.nil;
    }

    private static int rotateCount(int cnt, int len) {
        return (cnt < 0) ? (len - (~cnt % len) - 1) : (cnt % len);
    }

    protected IRubyObject internalRotate(ThreadContext context, int cnt) {
        int len = realLength;
        RubyArray rotated = aryDup();
        rotated.modify();

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
            throw concurrentModification(context.runtime, ex);
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

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

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

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

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

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

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

        if (block.isGiven() && patternGiven) {
            context.runtime.getWarnings().warn("given block not used");
        }

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
        final Ruby runtime = context.runtime;
        RubyFixnum zero = RubyFixnum.zero(runtime);

        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.sumCommon(context, self_each, this, zero, block);

        return sumCommon(context, zero, block);
    }

    @JRubyMethod
    public IRubyObject sum(final ThreadContext context, IRubyObject init, final Block block) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.sumCommon(context, self_each, this, init, block);

        return sumCommon(context, init, block);
    }

    public IRubyObject sumCommon(final ThreadContext context, IRubyObject init, final Block block) {
        final Ruby runtime = context.runtime;
        IRubyObject result = init;

        /*
         * This state machine is simple: it assumes all elements are the same type,
         * and transitions to a later state when that assumption fails.
         *
         * The order of states is:
         *
         * - is_fixnum => { is_bignum | is_rational | is_float | other }
         * - is_bignum => { is_rational | is_float | other }
         * - is_rational => { is_float | other }
         * - is_float => { other }
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
                result = RubyBignum.newBignum(runtime, sum);
            } else if (is_rational) {
                result = RubyRational.newRational(runtime, sum, 1);
            } else if (is_float) {
                result = RubyFloat.newFloat(runtime, (double) sum);
            } else {
                result = RubyFixnum.newFixnum(runtime, sum);
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
                result = RubyRational.newInstance(context, RubyBignum.newBignum(runtime, sum));
            } else if (is_float) {
                result = RubyFloat.newFloat(runtime, sum.doubleValue());
            } else {
                result = RubyBignum.newBignum(runtime, sum);
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
                        throw runtime.newTypeError("BUG: unexpected type in rational part of Array#sum");
                    }
                } else if (value instanceof RubyFloat) {
                    result = RubyFloat.newFloat(runtime, ((RubyRational) result).getDoubleValue(context));
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

                if (value instanceof RubyFixnum) {
                    x = ((RubyFixnum) value).value;
                } else if (value instanceof RubyBignum) {
                    x = ((RubyBignum) value).getDoubleValue();
                } else if (value instanceof RubyRational) {
                    x = ((RubyRational) value).getDoubleValue(context);
                } else if (value instanceof RubyFloat) {
                    x = ((RubyFloat) value).value;
                } else {
                    break float_loop;
                }

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

            result = new RubyFloat(runtime, f);
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
            if (block.yield(context, eltOk(i)).isTrue()) return context.runtime.newFixnum(i);
        }

        return context.nil;
    }

    public IRubyObject find_index(ThreadContext context, IRubyObject cond) {
        CachingCallSite self_each = sites(context).self_each;
        if (!self_each.isBuiltin(this)) return RubyEnumerable.find_indexCommon(context, self_each, this, cond);

        for (int i = 0; i < realLength; i++) {
            if (eltOk(i).equals(cond)) return context.runtime.newFixnum(i);
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
            throw concurrentModification(array.getRuntime(), ex);
        }
    }

    public static RubyArray unmarshalFrom(UnmarshalStream input) throws IOException {
        Ruby runtime = input.getRuntime();
        int size = input.unmarshalInt();

        // we create this now with an empty, nulled array so it's available for links in the marshal data
        RubyArray result = newBlankArrayInternal(runtime, size);

        input.registerLinkTarget(result);

        for (int i = 0; i < size; i++) {
            result.storeInternal(i, input.unmarshalObject());
        }

        return result;
    }

    /**
     * Construct the most efficient array shape for the given size. This should only be used when you
     * intend to populate all elements, since the packed arrays will be born with a nonzero size and
     * would have to be unpacked to partially populate.
     *
     * We nil-fill all cases, to ensure nulls will never leak out if there's an unpopulated element
     * or an index accessed before assignment.
     *
     * @param runtime the runtime
     * @param size the size
     * @return a RubyArray shaped for the given size
     */
    public static RubyArray newBlankArray(Ruby runtime, int size) {
        switch (size) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, runtime.getNil());
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, runtime.getNil(), runtime.getNil());
                break;
        }

        return newArray(runtime, size);
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

    @JRubyMethod(name = "pack", required = 1)
    public RubyString pack(ThreadContext context, IRubyObject obj) {
        RubyString format = obj.convertToString();
        try {
            RubyString buffer = context.runtime.newString();
            return Pack.pack(context, this, format, buffer);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context.runtime, e);
        }
    }

    @JRubyMethod(name = "pack")
    public RubyString pack(ThreadContext context, IRubyObject obj, IRubyObject maybeOpts) {
        final Ruby runtime = context.runtime;
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, maybeOpts);
        IRubyObject buffer = null;

        if (opts != context.nil) {
            buffer = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "buffer");
            if (buffer == context.nil) buffer = null;
            if (buffer != null && !(buffer instanceof RubyString)) {
                throw runtime.newTypeError(str(runtime, "buffer must be String, not ", types(runtime, buffer.getType())));
            }
        }

        if(buffer==null) {
            buffer = context.runtime.newString();
        }

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

    @JRubyMethod(name = "dig", required = 1, rest = true)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        final IRubyObject val = at( args[0] );
        return args.length == 1 ? val : RubyObject.dig(context, val, args, 1);
    }

    private IRubyObject maxWithBlock(ThreadContext context, Block block) {
        IRubyObject result = UNDEF;

        Ruby runtime = context.runtime;
        ArraySites sites = sites(context);
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF || RubyComparable.cmpint(context, op_gt, op_lt, block.yieldArray(context, runtime.newArray(v, result), null), v, result) > 0) {
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

        Ruby runtime = context.runtime;
        ArraySites sites = sites(context);
        CallSite op_gt = sites.op_gt_minmax;
        CallSite op_lt = sites.op_lt_minmax;

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = eltOk(i);

            if (result == UNDEF || RubyComparable.cmpint(context, op_gt, op_lt, block.yieldArray(context, runtime.newArray(v, result), null), v, result) < 0) {
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
        if (!num.isNil()) {
            return RubyEnumerable.min(context, this, num, block);
        }

        return min(context, block);
    }

    private static final int optimizedCmp(ThreadContext context, IRubyObject a, IRubyObject b, int token, CachingCallSite op_cmp, CallSite op_gt, CallSite op_lt) {
        if (token == ((RubyBasicObject) a).metaClass.generation) {
            if (a instanceof RubyFixnum && b instanceof RubyFixnum) {
                long aLong = ((RubyFixnum) a).getLongValue();
                long bLong = ((RubyFixnum) b).getLongValue();
                return Long.compare(aLong, bLong);
            }
            if (a instanceof RubyString && b instanceof RubyString) {
                return ((RubyString) a).op_cmp((RubyString) b);
            }
        }

        return RubyComparable.cmpint(context, op_gt, op_lt, op_cmp.call(context, a, a, b), a, b);
    }

    @Override
    public Class getJavaClass() {
        return List.class;
    }

    /**
     * Copy the values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public void copyInto(IRubyObject[] target, int start) {
        assert target.length - start >= realLength;
        safeArrayCopy(values, begin, target, start, realLength);
    }

    /**
     * Copy the specified number of values contained in this array into the target array at the specified offset.
     * It is expected that the target array is large enough to hold all necessary values.
     */
    public void copyInto(IRubyObject[] target, int start, int len) {
        assert target.length - start >= len;
        safeArrayCopy(values, begin, target, start, len);
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
                throw concurrentModification(getRuntime(), ex);
            }
            return target.cast(rawJavaArray);
        } else {
            return super.toJava(target);
        }
    }

    public boolean add(Object element) {
        append(JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element));
        return true;
    }

    public boolean remove(Object element) {
        unpack();
        final Ruby runtime = metaClass.runtime;
        ThreadContext context = runtime.getCurrentContext();
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
        insert(index, JavaUtil.convertJavaToUsableRubyObject(metaClass.runtime, element));
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
            if (recur) throw context.runtime.newArgumentError("recursive array join");

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
            insert(new IRubyObject[] { RubyFixnum.newFixnum(runtime, index++), JavaUtil.convertJavaToUsableRubyObject(runtime, obj) });
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
        rb_clear();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof RubyArray) {
            return op_equal(metaClass.runtime.getCurrentContext(), (RubyArray) other).isTrue();
        }
        return false;
    }

    private static IRubyObject safeArrayRef(Ruby runtime, IRubyObject[] values, int i) {
        try {
            return values[i];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    private IRubyObject safeArraySet(IRubyObject[] values, int i, IRubyObject value) {
        return safeArraySet(metaClass.runtime, values, i, value);
    }

    protected static IRubyObject safeArraySet(Ruby runtime, IRubyObject[] values, int i, IRubyObject value) {
        try {
            return values[i] = value;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    private static IRubyObject safeArrayRefSet(Ruby runtime, IRubyObject[] values, int i, IRubyObject value) {
        try {
            IRubyObject tmp = values[i];
            values[i] = value;
            return tmp;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime, e);
        }
    }

    private static IRubyObject safeArrayRefCondSet(Ruby runtime, IRubyObject[] values, int i, boolean doSet, IRubyObject value) {
        try {
            IRubyObject tmp = values[i];
            if (doSet) values[i] = value;
            return tmp;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    private void safeArrayCopy(IRubyObject[] source, int sourceStart, IRubyObject[] target, int targetStart, int length) {
        safeArrayCopy(metaClass.runtime, source, sourceStart, target, targetStart, length);
    }

    private static void safeArrayCopy(Ruby runtime, IRubyObject[] source, int sourceStart, IRubyObject[] target, int targetStart, int length) {
        try {
            System.arraycopy(source, sourceStart, target, targetStart, length);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    private static ArraySites sites(ThreadContext context) {
        return context.sites.Array;
    }

    @Deprecated
    public IRubyObject compatc19() {
        return compact19();
    }

    @Deprecated
    public final RubyArray aryDup19() {
        return aryDup();
    }

    /**
     * Increases the capacity of this <tt>Array</tt>, if necessary.
     * @param minCapacity the desired minimum capacity of the internal array
     */
    @Deprecated
    public void ensureCapacity(int minCapacity) {
        unpack();
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
    public IRubyObject flatten_bang19(ThreadContext context) {
        return flatten_bang(context);
    }

    @Deprecated
    public IRubyObject map19(ThreadContext context, Block block) {
        return map(context, block);
    }

    @Deprecated
    public IRubyObject collect19(ThreadContext context, Block block) {
        return collect(context, block);
    }

    @Deprecated
    @Override
    public RubyArray to_a() {
        final RubyClass metaClass = this.metaClass;
        Ruby runtime = metaClass.runtime;
        final RubyClass arrayClass = runtime.getArray();
        if (metaClass != arrayClass) {
            return dupImpl(runtime, arrayClass);
        }
        return this;
    }

    @Deprecated
    public IRubyObject shuffle(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return shuffle(context);
            case 1:
                return shuffle(context, args[0]);
            default:
                throw context.runtime.newArgumentError(args.length, 0, 0);
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
                throw context.runtime.newArgumentError(args.length, 0, 0);
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
                throw context.runtime.newArgumentError(args.length, 0, 1);
        }
    }
}
