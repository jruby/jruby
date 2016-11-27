/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.specialized.RubyArrayOneObject;
import org.jruby.specialized.RubyArrayTwoObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.RecursiveComparator;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.io.EncodingUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.runtime.Helpers.hashEnd;
import static org.jruby.runtime.Helpers.murmurCombine;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.RubyEnumerator.SizeFn;

/**
 * The implementation of the built-in class Array in Ruby.
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="Array")
public class RubyArray extends RubyObject implements List, RandomAccess {
    public static final int DEFAULT_INSPECT_STR_SIZE = 10;

    private static final boolean USE_PACKED_ARRAYS = Options.PACKED_ARRAYS.load();

    public static RubyClass createArrayClass(Ruby runtime) {
        RubyClass arrayc = runtime.defineClass("Array", runtime.getObject(), ARRAY_ALLOCATOR);
        runtime.setArray(arrayc);

        arrayc.setClassIndex(ClassIndex.ARRAY);
        arrayc.setReifiedClass(RubyArray.class);

        arrayc.kindOf = new RubyModule.JavaClassKindOf(RubyArray.class);

        arrayc.includeModule(runtime.getEnumerable());
        arrayc.defineAnnotatedMethods(RubyArray.class);

        return arrayc;
    }

    private static ObjectAllocator ARRAY_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyArray(runtime, klass, IRubyObject.NULL_ARRAY);
        }
    };


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
            arr.values = new IRubyObject[args.length];
            System.arraycopy(args, 0, arr.values, 0, args.length);
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
        RubyArray array = new RubyArray(runtime, len);
        Helpers.fillNil(array.values, 0, len, runtime);
        return array;
    }

    public static final RubyArray newArrayLight(final Ruby runtime, final int len) {
        RubyArray array = new RubyArray(runtime, len, false);
        Helpers.fillNil(array.values, 0, len, runtime);
        return array;
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

    /** rb_ary_new4, rb_ary_new3
     *
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, args[0]);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, args[0], args[1]);
                break;
        }
        RubyArray arr = new RubyArray(runtime, new IRubyObject[args.length]);
        System.arraycopy(args, 0, arr.values, 0, args.length);
        arr.realLength = args.length;
        return arr;
    }

    /**
     * @see RubyArray#newArrayMayCopy(Ruby, IRubyObject[], int, int)
     */
    public static RubyArray newArrayMayCopy(Ruby runtime, IRubyObject... args) {
        switch (args.length) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, args[0]);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, args[0], args[1]);
                break;
        }
        return newArrayNoCopy(runtime, args, 0, args.length);
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
        switch (length) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, args[start]);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, args[start], args[start + 1]);
                break;
        }
        return newArrayNoCopy(runtime, args, start, length);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args) {
        return new RubyArray(runtime, args);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin) {
        return new RubyArray(runtime, args, begin);
    }

    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin, int length) {
        assert begin >= 0 : "begin must be >= 0";
        assert length >= 0 : "length must be >= 0";

        return new RubyArray(runtime, args, begin, length);
    }

    public static RubyArray newArrayNoCopyLight(Ruby runtime, IRubyObject[] args) {
        RubyArray arr = new RubyArray(runtime, false);
        arr.values = args;
        arr.realLength = args.length;
        return arr;
    }

    public static RubyArray newArray(Ruby runtime, Collection<? extends IRubyObject> collection) {
        // This may seem inefficient for packed arrays, but the cost of this versus is not really worse
        // than the cost of constructing and walking an Iterator.
        IRubyObject[] values = collection.toArray(new IRubyObject[collection.size()]);
        switch (values.length) {
            case 0: return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, values[0]);
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, values[0], values[1]);
                break;
        }
        return new RubyArray(runtime, values);
    }

    public static RubyArray newArray(Ruby runtime, List<? extends IRubyObject> list) {
        switch (list.size()) {
            case 0: return newEmptyArray(runtime);
            case 1:
                if (USE_PACKED_ARRAYS) return new RubyArrayOneObject(runtime, list.get(0));
                break;
            case 2:
                if (USE_PACKED_ARRAYS) return new RubyArrayTwoObject(runtime, list.get(0), list.get(1));
                break;
        }
        return new RubyArray(runtime, list.toArray(new IRubyObject[list.size()]));
    }

    public static final int ARRAY_DEFAULT_SIZE = 16;

    // volatile to ensure that initial nil-fill is visible to other threads
    protected volatile IRubyObject[] values;

    private static final int TMPLOCK_ARR_F = 1 << 9;
    private static final int TMPLOCK_OR_FROZEN_ARR_F = TMPLOCK_ARR_F | FROZEN_F;

    protected volatile boolean isShared = false;
    protected int begin = 0;
    protected int realLength = 0;

    private static final ByteList EMPTY_ARRAY_BYTELIST = new ByteList(ByteList.plain("[]"), USASCIIEncoding.INSTANCE);
    private static final ByteList RECURSIVE_ARRAY_BYTELIST = new ByteList(ByteList.plain("[...]"), USASCIIEncoding.INSTANCE);

    /*
     * plain internal array assignment
     */
    private RubyArray(Ruby runtime, IRubyObject[] vals) {
        super(runtime, runtime.getArray());
        values = vals;
        realLength = vals.length;
    }

    /*
     * plain internal array assignment
     */
    private RubyArray(Ruby runtime, IRubyObject[] vals, boolean objectSpace) {
        super(runtime, runtime.getArray(), objectSpace);
        values = vals;
        realLength = vals.length;
    }

    /*
     * plain internal array assignment
     */
    private RubyArray(Ruby runtime, IRubyObject[] vals, int begin) {
        super(runtime, runtime.getArray());
        this.values = vals;
        this.begin = begin;
        this.realLength = vals.length - begin;
        this.isShared = true;
    }

    private RubyArray(Ruby runtime, IRubyObject[] vals, int begin, int length) {
        super(runtime, runtime.getArray());
        this.values = vals;
        this.begin = begin;
        this.realLength = length;
        this.isShared = true;
    }

    public RubyArray(Ruby runtime, int length) {
        super(runtime, runtime.getArray());
        values = length == 0 ? IRubyObject.NULL_ARRAY : new IRubyObject[length];
    }

    private RubyArray(Ruby runtime, int length, boolean objectspace) {
        super(runtime, runtime.getArray(), objectspace);
        values = length == 0 ? IRubyObject.NULL_ARRAY : new IRubyObject[length];
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
        values = length == 0 ? IRubyObject.NULL_ARRAY : new IRubyObject[length];
    }

    private RubyArray(Ruby runtime, RubyClass klass, IRubyObject[]vals, boolean objectspace) {
        super(runtime, klass, objectspace);
        values = vals;
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
        IRubyObject[] newValues = length == 0 ? IRubyObject.NULL_ARRAY : new IRubyObject[length];
        Helpers.fillNil(newValues, getRuntime());
        values = newValues;
        begin = 0;
    }

    private void realloc(int newLength, int valuesLength) {
        unpack();
        IRubyObject[] reallocated = new IRubyObject[newLength];
        if (newLength > valuesLength) {
            Helpers.fillNil(reallocated, valuesLength, newLength, getRuntime());
            safeArrayCopy(values, begin, reallocated, 0, valuesLength); // elements and trailing nils
        } else {
            safeArrayCopy(values, begin, reallocated, 0, newLength); // ???
        }
        begin = 0;
        values = reallocated;
    }

    private static void fill(IRubyObject[]arr, int from, int to, IRubyObject with) {
        for (int i=from; i<to; i++) {
            arr[i] = with;
        }
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

    public IRubyObject[] toJavaArray() {
        IRubyObject[] copy = new IRubyObject[realLength];
        copyInto(copy, 0);
        return copy;
    }

    public IRubyObject[] toJavaArrayUnsafe() {
        unpack();
        return !isShared ? values : toJavaArray();
    }

    public IRubyObject[] toJavaArrayMaybeUnsafe() {
        unpack();
        return (!isShared && begin == 0 && values.length == realLength) ? values : toJavaArray();
    }

    /** rb_ary_make_shared
    *
    */
    private RubyArray makeShared() {
        // TODO: (CON) Some calls to makeShared could create packed array almost as efficiently
        unpack();

        return makeShared(begin, realLength, getMetaClass());
    }

    private RubyArray makeShared(int beg, int len, RubyClass klass) {
        return makeShared(beg, len, new RubyArray(klass.getRuntime(), klass));
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
            if ((flags & FROZEN_F) != 0) throw getRuntime().newFrozenError("array");
            if ((flags & TMPLOCK_ARR_F) != 0) throw getRuntime().newTypeError("can't modify array during iteration");
        }
    }

    /** rb_ary_modify
     *
     */
    protected void modify() {
        modifyCheck();
        if (isShared) {
            IRubyObject[] vals = new IRubyObject[realLength];
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
        Ruby runtime = context.runtime;
        realLength = 0;
        if (block.isGiven() && runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.BLOCK_UNUSED, "given block not used");
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
        if (len >= Integer.MAX_VALUE) throw runtime.newArgumentError("array size too big");
        int ilen = (int) len;

        modify();

        if (ilen > values.length - begin) {
            values = new IRubyObject[ilen];
            begin = 0;
        }

        if (block.isGiven()) {
            if (arg1 != null) {
                runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
            }

            if (block.getSignature() == Signature.NO_ARGUMENTS) {
                IRubyObject nil = runtime.getNil();
                for (int i = 0; i < ilen; i++) {
                    store(i, block.yield(context, nil));
                    realLength = i + 1;
                }
            } else {
                for (int i = 0; i < ilen; i++) {
                    store(i, block.yield(context, RubyFixnum.newFixnum(runtime, i)));
                    realLength = i + 1;
                }
            }

        } else {
            try {
                if (arg1 == null) {
                    Helpers.fillNil(values, begin, begin + ilen, runtime);
                } else {
                    fill(values, begin, begin + ilen, arg1);
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
    public IRubyObject dup() {
        if (metaClass.getClassIndex() != ClassIndex.ARRAY) return super.dup();

        RubyArray dup = new RubyArray(metaClass.getClassRuntime(), values, begin, realLength);
        dup.isShared = isShared = true;
        dup.flags |= flags & TAINTED_F; // from DUP_SETUP

        return dup;
    }

    /** rb_ary_replace
     *
     */
    @JRubyMethod(name = {"replace"}, required = 1)
    public IRubyObject replace(IRubyObject orig) {
        unpack();
        modifyCheck();

        RubyArray origArr = orig.convertToArray();

        if (this == orig) return this;

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
    @Override
    public IRubyObject to_s() {
        return inspect();
    }


    public boolean includes(ThreadContext context, IRubyObject item) {
        int myBegin = this.begin;
        int end = myBegin + realLength;
        IRubyObject[] values = this.values;
        final Ruby runtime = context.runtime;
        for (int i = myBegin; i < end; i++) {
            final IRubyObject value = safeArrayRef(runtime, values, i);
            if (equalInternal(context, value, item)) return true;
        }

        return false;
    }

    public RubyFixnum hash19(ThreadContext context) {
        return hash(context);
    }

    /** rb_ary_hash
     *
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        Ruby runtime = context.runtime;

        long h = Helpers.hashStart(runtime, realLength);

        h = Helpers.murmurCombine(h, System.identityHashCode(RubyArray.class));

        for (int i = 0; i < realLength; i++) {
            IRubyObject value = eltOk(i);
            RubyFixnum n = Helpers.safeHash(context, value);
            h = murmurCombine(h, n.getLongValue());
        }

        h = hashEnd(h);

        return runtime.newFixnum(h);
    }

    /** rb_ary_store
     *
     */
    public IRubyObject store(long index, IRubyObject value) {
        if (index < 0 && (index += realLength) < 0) {
            throw getRuntime().newIndexError("index " + (index - realLength) + " out of array");
        }

        modify();

        if (index >= realLength) {
            int valuesLength = values.length - begin;
            if (index >= valuesLength) storeRealloc(index, valuesLength);
            realLength = (int) index + 1;
        }

        safeArraySet(values, begin + (int) index, value);

        return value;
    }

    private void storeRealloc(long index, int valuesLength) {
        long newLength = valuesLength >> 1;

        if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

        newLength += index;
        if (index >= Integer.MAX_VALUE || newLength >= Integer.MAX_VALUE) {
            throw getRuntime().newArgumentError("index too big");
        }
        realloc((int) newLength, valuesLength);
    }

    /** rb_ary_elt
     *
     */
    private final IRubyObject elt(long offset) {
        if (offset < 0 || offset >= realLength) {
            return getRuntime().getNil();
        }
        return eltOk(offset);
    }

    public IRubyObject eltOk(long offset) {
        try {
            return eltInternal((int)offset);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(getRuntime(), ex);
        }
    }

    public IRubyObject eltSetOk(long offset, IRubyObject value) {
        return eltSetOk((int) offset, value);
    }

    public IRubyObject eltSetOk(int offset, IRubyObject value) {
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

    public IRubyObject eltInternal(int offset) {
        return values[begin + offset];
    }

    public IRubyObject eltInternalSet(int offset, IRubyObject item) {
        return values[begin + offset] = item;
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
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
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
    public static RubyArray aryToAry(IRubyObject obj) {
        IRubyObject tmp = TypeConverter.checkArrayType(obj);

        if (!tmp.isNil()) return (RubyArray)tmp;
        return obj.getRuntime().newArray(obj);
    }

    /** rb_ary_splice
     *
     */
    private final void splice(long beg, long len, IRubyObject rpl, boolean oneNine) {
        if (len < 0) throw getRuntime().newIndexError("negative length (" + len + ")");
        if (beg < 0 && (beg += realLength) < 0) throw getRuntime().newIndexError("index " + (beg - realLength) + " out of array");

        final RubyArray rplArr;
        final int rlen;

        if (rpl == null || (rpl.isNil() && !oneNine)) {
            rplArr = null;
            rlen = 0;
        } else if (rpl.isNil()) {
            // 1.9 replaces with nil
            rplArr = newArray(getRuntime(), rpl);
            rlen = 1;
        } else {
            rplArr = aryToAry(rpl);
            rlen = rplArr.realLength;
        }

        unpack();
        modify();

        int valuesLength = values.length - begin;
        if (beg >= realLength) {
            len = beg + rlen;
            if (len >= valuesLength) spliceRealloc((int)len, valuesLength);
            try {
                Helpers.fillNil(values, begin + realLength, begin + ((int) beg), getRuntime());
            } catch (ArrayIndexOutOfBoundsException e) {
                throw concurrentModification(getRuntime(), e);
            }
            realLength = (int) len;
        } else {
            if (beg + len > realLength) len = realLength - beg;
            int alen = realLength + rlen - (int)len;
            if (alen >= valuesLength) spliceRealloc(alen, valuesLength);

            if (len != rlen) {
                safeArrayCopy(values, begin + (int) (beg + len), values, begin + (int) beg + rlen, realLength - (int) (beg + len));
                realLength = alen;
            }
        }

        if (rlen > 0) {
            rplArr.copyInto(values, begin + (int) beg, rlen);
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
            Helpers.fillNil(values, begin + realLength, begin + ((int) beg), getRuntime());
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

        safeArraySet(values, begin + (int)beg, rpl);
    }

    private void spliceRealloc(int length, int valuesLength) {
        int tryLength = valuesLength + (valuesLength >> 1);
        int len = length > tryLength ? length : tryLength;
        IRubyObject[] vals = new IRubyObject[len];
        System.arraycopy(values, begin, vals, 0, realLength);

        // only fill if there actually will remain trailing storage
        if (len > length) Helpers.fillNil(vals, length, len, getRuntime());
        begin = 0;
        values = vals;
    }

    public IRubyObject insert() {
        throw getRuntime().newArgumentError(0, 1);
    }

    /** rb_ary_insert
     *
     */
    @JRubyMethod(name = "insert")
    public IRubyObject insert(IRubyObject arg) {
        modifyCheck();

        return this;
    }

    @Deprecated
    public IRubyObject insert19(IRubyObject arg) {
        return insert(arg);
    }

    @JRubyMethod(name = "insert")
    public IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        modifyCheck();

        long pos = RubyNumeric.num2long(arg1);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;

        spliceOne(pos, arg2); // rb_ary_new4

        return this;
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

        RubyArray inserted = new RubyArray(getRuntime(), false);
        inserted.values = args;
        inserted.begin = 1;
        inserted.realLength = args.length - 1;

        splice(pos, 0, inserted, false); // rb_ary_new4

        return this;
    }

    @Deprecated
    public IRubyObject insert19(IRubyObject[] args) {
        return insert(args);
    }

    /** rb_ary_dup
     *
     */
    public RubyArray aryDup() {
        // In 1.9, rb_ary_dup logic changed so that on subclasses of Array,
        // dup returns an instance of Array, rather than an instance of the subclass
        // Also, taintedness and trustedness are not inherited to duplicates
        RubyArray dup = new RubyArray(metaClass.getClassRuntime(), values, begin, realLength);
        dup.isShared = true;
        isShared = true;
        // rb_copy_generic_ivar from DUP_SETUP here ...unlikely..
        return dup;
    }

    /** rb_ary_transpose
     *
     */
    @JRubyMethod(name = "transpose")
    public RubyArray transpose() {
        RubyArray tmp, result = null;

        int alen = realLength;
        if (alen == 0) return aryDup();

        Ruby runtime = getRuntime();
        int elen = -1;
        for (int i = 0; i < alen; i++) {
            tmp = elt(i).convertToArray();
            if (elen < 0) {
                elen = tmp.realLength;
                result = newBlankArray(runtime, elen);
                for (int j = 0; j < elen; j++) {
                    result.store(j, newBlankArray(runtime, alen));
                }
            } else if (elen != tmp.realLength) {
                throw runtime.newIndexError("element size differs (" + tmp.realLength
                        + " should be " + elen + ")");
            }
            for (int j = 0; j < elen; j++) {
                ((RubyArray) result.elt(j)).store(i, tmp.elt(j));
            }
        }
        return result;
    }

    /** rb_values_at (internal)
     *
     */
    private final IRubyObject values_at(long olen, IRubyObject[] args) {
        RubyArray result = newArray(getRuntime(), args.length);

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RubyFixnum) {
                result.append(entry(((RubyFixnum)args[i]).getLongValue()));
                continue;
            }

            long beglen[];
            if (!(args[i] instanceof RubyRange)) {
            } else if ((beglen = ((RubyRange) args[i]).begLen(olen, 0)) == null) {
                continue;
            } else {
                int beg = (int) beglen[0];
                int len = (int) beglen[1];
                int end = len;
                for (int j = 0; j < end; j++) {
                    result.append(entry(j + beg));
                }
                continue;
            }
            result.append(entry(RubyNumeric.num2long(args[i])));
        }

        Helpers.fillNil(result.values, result.realLength, result.values.length, getRuntime());
        return result;
    }

    /** rb_values_at
     *
     */
    @JRubyMethod(name = "values_at", rest = true)
    public IRubyObject values_at(IRubyObject[] args) {
        return values_at(realLength, args);
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseq(long beg, long len) {
        return subseq(getMetaClass(), beg, len, true);
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseqLight(long beg, long len) {
        return subseq(getMetaClass(), beg, len, true);
    }

    public IRubyObject subseq(RubyClass metaClass, long beg, long len, boolean light) {
        Ruby runtime = getRuntime();
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
        return getRuntime().newFixnum(realLength);
    }

    protected SizeFn enumLengthFn() {
        final RubyArray self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return self.length();
            }
        };
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
            if (realLength == Integer.MAX_VALUE) throw getRuntime().newArgumentError("index too big");

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

    public RubyArray push_m(IRubyObject[] items) {
        return push(items);
    }

    @JRubyMethod(name = "push", required = 1)
    public RubyArray push(IRubyObject item) {
        append(item);

        return this;
    }

    @JRubyMethod(name = "push", rest = true)
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

    @JRubyMethod(name = "unshift")
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
    @JRubyMethod(name = "unshift")
    public IRubyObject unshift(IRubyObject item) {
        unpack();
        modifyCheck();

        if (begin == 0 || isShared) {
            modify();
            final int valuesLength = values.length - begin;
            if (realLength == valuesLength) {
                int newLength = valuesLength >> 1;
                if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

                newLength += valuesLength;
                IRubyObject[] vals = new IRubyObject[newLength];
                safeArrayCopy(values, begin, vals, 1, valuesLength);
                Helpers.fillNil(vals, valuesLength + 1, newLength, getRuntime());
                values = vals;
                begin = 0;
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

    @JRubyMethod(name = "unshift", rest = true)
    public IRubyObject unshift(IRubyObject[] items) {
        unpack();
        modifyCheck();

        long len = realLength;
        if (items.length == 0) return this;

        store(len + items.length - 1, getRuntime().getNil());

        try {
            System.arraycopy(values, begin, values, begin + items.length, (int) len);
            System.arraycopy(items, 0, values, begin, items.length);
        } catch (ArrayIndexOutOfBoundsException e) {
            concurrentModification();
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
        return context.runtime.newBoolean(includes(context, item));
    }

    /** rb_ary_frozen_p
     *
     */
    @JRubyMethod(name = "frozen?")
    @Override
    public RubyBoolean frozen_p(ThreadContext context) {
        return context.runtime.newBoolean(isFrozen() || (flags & TMPLOCK_ARR_F) != 0);
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
        return arg0 instanceof RubyFixnum ? entry(((RubyFixnum)arg0).getLongValue()) : arefCommon(arg0);
    }

    @Deprecated
    public IRubyObject aref19(IRubyObject arg0) {
        return aref(arg0);
    }

    private IRubyObject arefCommon(IRubyObject arg0) {
        Ruby runtime = getRuntime();

        if (arg0 instanceof RubyRange) {
            long[] beglen = ((RubyRange) arg0).begLen(realLength, 0);
            return beglen == null ? runtime.getNil() : subseq(beglen[0], beglen[1]);
        } else {
            ThreadContext context = runtime.getCurrentContext();
            ArraySites sites = sites(context);

            if (RubyRange.isRangeLike(context, arg0, sites.respond_to_begin, sites.respond_to_end)) {
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
            store(((RubyFixnum)arg0).getLongValue(), arg1);
        } else if (arg0 instanceof RubyRange) {
            RubyRange range = (RubyRange)arg0;
            long beg = range.begLen0(realLength);
            splice(beg, range.begLen1(realLength, beg), arg1, true);
        } else {
            ThreadContext context = getRuntime().getCurrentContext();
            ArraySites sites = sites(context);

            if (RubyRange.isRangeLike(context, arg0, sites.respond_to_begin, sites.respond_to_end)) {
                RubyRange range = RubyRange.rangeFromRangeLike(context, arg0, sites.begin, sites.end, sites.exclude_end);

                long beg = range.begLen0(realLength);
                splice(beg, range.begLen1(realLength, beg), arg1, true);
            } else {
                store(RubyNumeric.num2long(arg0), arg1);
            }
        }
        return arg1;
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
        splice(RubyNumeric.num2long(arg0), RubyNumeric.num2long(arg1), arg2, true);
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
    @JRubyMethod(name = "concat", required = 1)
    public RubyArray concat(IRubyObject obj) {
        modifyCheck();

        RubyArray ary = obj.convertToArray();

        if (ary.realLength > 0) splice(realLength, 0, ary, false);

        return this;
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
        EncodingUtils.strBufCat(runtime, str, OPEN_BRACKET);
        boolean tainted = isTaint();

        for (int i = 0; i < realLength; i++) {

            RubyString s = inspect(context, safeArrayRef(runtime, values, begin + i));
            if (s.isTaint()) tainted = true;
            if (i > 0) EncodingUtils.strBufCat(runtime, str, COMMA_SPACE);
            else str.setEncoding(s.getEncoding());
            str.cat19(s);
        }
        EncodingUtils.strBufCat(runtime, str, CLOSE_BRACKET);

        if (tainted) str.setTaint(true);

        return str;
    }

    protected static final ByteList OPEN_BRACKET = new ByteList(new byte[]{(byte)'['}, USASCIIEncoding.INSTANCE);
    protected static final ByteList CLOSE_BRACKET = new ByteList(new byte[]{(byte)']'}, USASCIIEncoding.INSTANCE);
    protected static final ByteList COMMA_SPACE = new ByteList(new byte[]{(byte)',', (byte)' '}, USASCIIEncoding.INSTANCE);

    /** rb_ary_inspect
    *
    */
    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        if (realLength == 0) return RubyString.newStringShared(getRuntime(), EMPTY_ARRAY_BYTELIST);
        if (getRuntime().isInspecting(this)) return  RubyString.newStringShared(getRuntime(), RECURSIVE_ARRAY_BYTELIST);

        try {
            getRuntime().registerInspecting(this);
            return inspectAry(getRuntime().getCurrentContext());
        } finally {
            getRuntime().unregisterInspecting(this);
        }
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
        if (realLength == 0) return getRuntime().getNil();
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
            return newArray(getRuntime(), eltOk(0));
        } else if (n == 2) {
            return newArray(getRuntime(), eltOk(0), eltOk(1));
        }

        unpack();
        return makeShared(begin, (int) n, getRuntime().getArray());
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
        if (realLength == 0) return getRuntime().getNil();
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
            throw getRuntime().newArgumentError("negative array size (or size too big)");
        } else if (n == 1) {
            return newArray(getRuntime(), eltOk(realLength - 1));
        } else if (n == 2) {
            return newArray(getRuntime(), eltOk(realLength - 2), eltOk(realLength - 1));
        }

        unpack();
        return makeShared(begin + realLength - (int) n, (int) n, getRuntime().getArray());
    }

    /**
     * mri: rb_ary_each
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "each", enumLengthFn());

        for (int i = 0; i < realLength; i++) {
            // do not coarsen the "safe" catch, since it will misinterpret AIOOBE from the yielded code.
            // See JRUBY-5434
            block.yield(context, eltOk(i));
        }
        return this;
    }

    public IRubyObject eachSlice(ThreadContext context, int size, Block block) {
        unpack();
        Ruby runtime = context.runtime;
        RubyClass array = runtime.getArray();

        // local copies of everything
        int localRealLength = realLength;
        IRubyObject[] localValues = values;
        int localBegin = begin;

        // sliding window
        RubyArray window = makeShared(localBegin, size, array);

        // don't expose shared array to ruby
        Signature signature = block.getSignature();
        final boolean specificArity = signature.isFixed() && signature.required() != 1;

        for (; localRealLength >= size; localRealLength -= size) {
            block.yield(context, window);
            if (specificArity) { // array is never exposed to ruby, just use for yielding
                window.begin = localBegin += size;
            } else { // array may be exposed to ruby, create new
                window = makeShared(localBegin += size, size, array);
            }
        }

        // remainder
        if (localRealLength > 0) {
            window.realLength = localRealLength;
            block.yield(context, window);
        }
        return runtime.getNil();
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
        return block.isGiven() ? eachIndex(context, block) : enumeratorize(context.runtime, this, "each_index");
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
        return block.isGiven() ? reverseEach(context, block) : enumeratorizeWithSize(context, this, "reverse_each", enumLengthFn());
    }

    private IRubyObject inspectJoin(ThreadContext context, RubyArray tmp, IRubyObject sep) {
        Ruby runtime = context.runtime;

        // If already inspecting, there is no need to register/unregister again.
        if (runtime.isInspecting(this)) {
            return tmp.join(context, sep);
        }

        try {
            runtime.registerInspecting(this);
            return tmp.join(context, sep);
        } finally {
            runtime.unregisterInspecting(this);
        }
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

    // 1.9 MRI: ary_join_0
    protected RubyString joinStrings(RubyString sep, int max, RubyString result) {
        IRubyObject first = eltOk(0);
        if (max > 0 && first instanceof EncodingCapable) {
            result.setEncoding(((EncodingCapable)first).getEncoding());
        }

        try {
            for (int i = 0; i < max; i++) {
                if (i > 0 && sep != null) result.append19(sep);
                result.append19(eltInternal(i));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime(), e);
        }

        return result;
    }

    // 1.9 MRI: ary_join_1
    private RubyString joinAny(ThreadContext context, IRubyObject obj, RubyString sep,
            int i, RubyString result) {
        assert i >= 0 : "joining elements before beginning of array";

        RubyClass arrayClass = context.runtime.getArray();
        JavaSites.CheckedSites to_ary_checked = null;

        for (; i < realLength; i++) {
            if (i > 0 && sep != null) result.append19(sep);

            IRubyObject val = eltOk(i);

            if (val instanceof RubyString) {
                result.append19(val);
            } else if (val instanceof RubyArray) {
                obj = val;
                recursiveJoin(context, obj, sep, result, val);
            } else {
                IRubyObject tmp = val.checkStringType19();
                if (!tmp.isNil()) {
                    result.append19(tmp);
                    continue;
                }

                if (to_ary_checked == null) to_ary_checked = sites(context).to_ary_checked;

                tmp = TypeConverter.convertToTypeWithCheck(context, val, arrayClass, to_ary_checked);
                if (!tmp.isNil()) {
                    obj = val;
                    recursiveJoin(context, obj, sep, result, tmp);
                } else {
                    result.append19(RubyString.objAsString(context, val));
                }
            }
        }

        return result;
    }

    private void recursiveJoin(final ThreadContext context, final IRubyObject outValue,
            final RubyString sep, final RubyString result, final IRubyObject ary) {

        Ruby runtime = context.runtime;

        if (ary == this) throw runtime.newArgumentError("recursive array join");

        runtime.safeRecurse(new Ruby.RecursiveFunctionEx<Ruby>() {
            public IRubyObject call(ThreadContext context, Ruby runtime, IRubyObject obj, boolean recur) {
                if (recur) throw runtime.newArgumentError("recursive array join");

                RubyArray recAry = ((RubyArray) ary);
                recAry.joinAny(context, outValue, sep, 0, result);

                return runtime.getNil();
            }}, context, runtime, outValue, "join", true);
    }

    /** rb_ary_join
     *
     */
    @JRubyMethod(name = "join")
    public IRubyObject join19(final ThreadContext context, IRubyObject sep) {
        final Ruby runtime = context.runtime;

        if (realLength == 0) return RubyString.newEmptyString(runtime, USASCIIEncoding.INSTANCE);

        if (sep.isNil()) sep = runtime.getGlobalVariables().get("$,");

        int len = 1;
        RubyString sepString = null;
        if (!sep.isNil()) {
            sepString = sep.convertToString();
            len += sepString.size() * (realLength - 1);
        }

        for (int i = 0; i < realLength; i++) {
            IRubyObject val = eltOk(i);
            IRubyObject tmp = val.checkStringType19();
            if (tmp.isNil() || tmp != val) {
                len += (realLength - i) * 10;
                RubyString result = (RubyString) RubyString.newStringLight(runtime, len, USASCIIEncoding.INSTANCE).infectBy(this);
                RubyString sepStringFinal = sepString;
                int iFinal = i;

                return joinAny(context, RubyArray.this, sepStringFinal, iFinal, joinStrings(sepStringFinal, iFinal, result));
            }

            len += ((RubyString) tmp).getByteList().length();
        }

        return joinStrings(sepString, realLength,
                (RubyString) RubyString.newStringLight(runtime, len).infectBy(this));
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
    public RubyArray to_a() {
        if(getMetaClass() != getRuntime().getArray()) {
            return aryDup();
        }
        return this;
    }

    @JRubyMethod(name = "to_ary")
    public IRubyObject to_ary() {
    	return this;
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
        Ruby runtime = context.runtime;

        if (this == obj) {
            return runtime.getTrue();
        }

        if (!(obj instanceof RubyArray)) {
            if (obj == context.nil) return runtime.getFalse();

            if (!sites(context).respond_to_to_ary.respondsTo(context, obj, obj)) {
                return runtime.getFalse();
            }
            return Helpers.rbEqual(context, obj, this);
        }
        return RecursiveComparator.compare(context, sites(context).op_equal, this, obj);
    }

    public RubyBoolean compare(ThreadContext context, CallSite site, IRubyObject other) {
        if (!(other instanceof RubyArray)) {
            if (!sites(context).respond_to_to_ary.respondsTo(context, other, other)) return context.runtime.getFalse();

            return Helpers.rbEqual(context, other, this);
        }

        RubyArray ary = (RubyArray) other;

        if (realLength != ary.realLength) return context.runtime.getFalse();

        for (int i = 0; i < realLength; i++) {
            IRubyObject a = elt(i);
            IRubyObject b = ary.elt(i);

            if (a == b) continue; // matching MRI opt. mock frameworks can throw errors if we don't

            if (!site.call(context, a, a, b).isTrue()) return context.runtime.getFalse();
        }

        return context.runtime.getTrue();
    }

    /** rb_ary_eql
     *
     */
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql(ThreadContext context, IRubyObject obj) {
        if(!(obj instanceof RubyArray)) {
            return context.runtime.getFalse();
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
        if (realLength == p) return getRuntime().getNil();

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
        return realLength == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
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
                Helpers.fillNil(values, 0, realLength, getRuntime());
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
                fill(values, begin + beg, begin + end, item);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw concurrentModification(context.runtime, ex);
            }
        }

        return this;
    }

    protected IRubyObject fillCommon(ThreadContext context, int beg, long len, Block block) {
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

        return runtime.getNil();
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

        return runtime.getNil();
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
                long fixValue = ((RubyFixnum)v).getLongValue();
                if (fixValue == 0) return mid;
                smaller = fixValue < 0;
            } else if (v == runtime.getTrue()) {
                satisfied = true;
                smaller = true;
            } else if (v == runtime.getFalse() || v == runtime.getNil()) {
                smaller = false;
            } else if (runtime.getNumeric().isInstance(v)) {
                if (op_cmp == null) op_cmp = sites(context).op_cmp_bsearch;
                switch (RubyComparable.cmpint(context, op_cmp.call(context, v, v, RubyFixnum.zero(runtime)), v, RubyFixnum.zero(runtime))) {
                    case 0: return mid;
                    case 1: smaller = true; break;
                    case -1: smaller = false;
                }
            } else {
                throw runtime.newTypeError("wrong argument type " + v.getType().getName() + " (must be numeric, true, false or nil");
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

        RubyArray ary = newArray(getRuntime(), args.length);

        for (int i = 0; i < args.length; i++) {
            ary.append(aref(args[i]));
        }

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
                    IRubyObject tmp = eltInternal(i);
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
            RubyArray dup = safeReverse();
            dup.flags |= flags & TAINTED_F; // from DUP_SETUP
            // rb_copy_generic_ivar from DUP_SETUP here ...unlikely..
            return dup;
        } else {
            return dup();
        }
    }

    protected RubyArray safeReverse() {
        int length = realLength;
        int myBegin = this.begin;
        IRubyObject[] myValues = this.values;
        IRubyObject[] vals = new IRubyObject[length];

        try {
            for (int i = 0; i <= length >> 1; i++) {
                vals[i] = myValues[myBegin + length - i - 1];
                vals[length - i - 1] = myValues[myBegin + i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(getRuntime(), e);
        }
        return new RubyArray(getRuntime(), getMetaClass(), vals);
    }

    /** rb_ary_collect
     *
     */
    public IRubyObject collect(ThreadContext context, Block block) {
        final Ruby runtime = context.runtime;
        if (!block.isGiven()) return makeShared();

        IRubyObject[] arr = new IRubyObject[realLength];

        int i;
        for (i = 0; i < realLength; i++) {
            // Do not coarsen the "safe" check, since it will misinterpret AIOOBE from the yield
            // See JRUBY-5434
            arr[i] = block.yield(context, eltOk(i));
        }

        // use iteration count as new size in case something was deleted along the way
        return newArrayMayCopy(runtime, arr, 0, i);
    }

    @JRubyMethod(name = {"collect"})
    public IRubyObject collect19(ThreadContext context, Block block) {
        return block.isGiven() ? collect(context, block) : enumeratorizeWithSize(context, this, "collect", enumLengthFn());
    }

    @JRubyMethod(name = {"map"})
    public IRubyObject map19(ThreadContext context, Block block) {
        return block.isGiven() ? collect(context, block) : enumeratorizeWithSize(context, this, "map", enumLengthFn());
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
            store(i, block.yield(context, eltOk(i)));
        }

        return this;
    }

    /** rb_ary_collect_bang
    *
    */
    @JRubyMethod(name = "collect!")
    public IRubyObject collect_bang(ThreadContext context, Block block) {
        return block.isGiven() ? collectBang(context, block) : enumeratorizeWithSize(context, this, "collect!", enumLengthFn());
    }

    /** rb_ary_collect_bang
    *
    */
    @JRubyMethod(name = "map!")
    public IRubyObject map_bang(ThreadContext context, Block block) {
        return block.isGiven() ? collectBang(context, block) : enumeratorizeWithSize(context, this, "map!", enumLengthFn());
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

    @JRubyMethod
    public IRubyObject select(ThreadContext context, Block block) {
        return block.isGiven() ? selectCommon(context, block) : enumeratorizeWithSize(context, this, "select", enumLengthFn());
    }

    @JRubyMethod(name = "select!")
    public IRubyObject select_bang(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "select!", enumLengthFn());

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
            return enumeratorizeWithSize(context, this, "keep_if", enumLengthFn());
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
        if (pos >= len || (pos < 0 && (pos += len) < 0)) return getRuntime().getNil();

        unpack();
        modify();

        IRubyObject nil = getRuntime().getNil();
        IRubyObject obj = null; // should never return null below

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
            values[begin + len - 1] = getRuntime().getNil();
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
        return block.isGiven() ? rejectCommon(context, block) : enumeratorizeWithSize(context, this, "reject", enumLengthFn());
    }

    // MRI: ary_reject_bang and reject_bang_i
    public IRubyObject rejectBang(ThreadContext context, Block block) {
        unpack();
        modify();

        final Ruby runtime = context.runtime;
        final int len = realLength; final int beg = begin;

        int len0 = 0, len1 = 0;
        try {
            int i1, i2;
            for (i1 = i2 = 0; i1 < len; len0 = ++i1) {
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
            selectBangEnsure(runtime, len, beg, len0, len1);
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
        return block.isGiven() ? rejectBang(context, block) : enumeratorizeWithSize(context, this, "reject!", enumLengthFn());
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
        return block.isGiven() ? deleteIf(context, block) : enumeratorizeWithSize(context, this, "delete_if", enumLengthFn());
    }

    /** rb_ary_zip
     *
     */
    @JRubyMethod(optional = 1, rest = true)
    public IRubyObject zip(ThreadContext context, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;
        RubyClass array = runtime.getArray();
        ArraySites sites = sites(context);

        final IRubyObject[] newArgs = new IRubyObject[args.length];

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
                IRubyObject[] tmp = new IRubyObject[args.length + 1];
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

        IRubyObject[] result = new IRubyObject[realLength];
        try {
            for (int i = 0; i < realLength; i++) {
                IRubyObject[] tmp = new IRubyObject[args.length + 1];
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
        Ruby runtime = context.runtime;
        IRubyObject ary2 = runtime.getNil();
        boolean isAnArray = (obj instanceof RubyArray) || obj.getMetaClass().getSuperClass() == runtime.getArray();

        if (!isAnArray && !sites(context).respond_to_to_ary.respondsTo(context, obj, obj, true)) {
            return ary2;
        } else if (!isAnArray) {
            ary2 = sites(context).to_ary.call(context, obj, obj);
        } else {
            ary2 = obj.convertToArray();
        }

        return cmpCommon(context, runtime, (RubyArray) ary2);
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

    private IRubyObject slice_internal(long pos, long len, Ruby runtime) {
        if(len < 0) return runtime.getNil();
        int orig_len = realLength;
        if(pos < 0) {
            pos += orig_len;
            if(pos < 0) {
                return runtime.getNil();
            }
        } else if(orig_len < pos) {
            return runtime.getNil();
        }

        if(orig_len < pos + len) {
            len = orig_len - pos;
        }
        if(len == 0) {
            return runtime.newEmptyArray();
        }

        unpack();

        IRubyObject result = makeShared(begin + (int)pos, (int)len, getMetaClass());
        splice(pos, len, null, false);

        return result;
    }

    /** rb_ary_slice_bang
     *
     */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0) {
        modifyCheck();
        Ruby runtime = getRuntime();
        if (arg0 instanceof RubyRange) {
            RubyRange range = (RubyRange) arg0;
            if (!range.checkBegin(realLength)) {
                return runtime.getNil();
            }

            long pos = range.begLen0(realLength);
            long len = range.begLen1(realLength, pos);
            return slice_internal(pos, len, runtime);
        }
        return delete_at((int) RubyNumeric.num2long(arg0));
    }

    /** rb_ary_slice_bang
    *
    */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        modifyCheck();
        long pos = RubyNumeric.num2long(arg0);
        long len = RubyNumeric.num2long(arg1);
        return slice_internal(pos, len, getRuntime());
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

        return runtime.getNil();
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

        return runtime.getNil();
    }

    // MRI array.c flatten
    protected boolean flatten(ThreadContext context, final int level, final RubyArray result) {
        // TODO: (CON) We can flatten packed versions efficiently if length does not change (e.g. [[1,2],[]])
        unpack();
        final Ruby runtime = context.runtime;
        RubyArray stack = newArrayLight(runtime, ARRAY_DEFAULT_SIZE);
        IdentityHashMap<Object, Object> memo = new IdentityHashMap<Object, Object>();
        RubyArray ary = this;
        memo.put(ary, NEVER);
        boolean modified = false;

        int i = 0;

        try {
            while (true) {
                IRubyObject tmp;
                while (i < ary.realLength) {
                    IRubyObject elt = ary.eltOk(i++);
                    if (level >= 0 && stack.size() / 2 >= level) {
                        result.append(elt);
                        continue;
                    }
                    tmp = TypeConverter.checkArrayType(elt);
                    if (tmp.isNil()) {
                        result.append(elt);
                    } else {
                        modified = true;
                        if (memo.get(tmp) != null) throw runtime.newArgumentError("tried to flatten recursive array");
                        memo.put(tmp, NEVER);
                        stack.append(ary);
                        stack.append(RubyFixnum.newFixnum(runtime, i));
                        ary = (RubyArray)tmp;
                        i = 0;
                    }
                }
                if (stack.realLength == 0) break;
                memo.remove(ary);
                tmp = stack.pop(context);
                i = (int) ((RubyFixnum) tmp).getLongValue();
                ary = (RubyArray) stack.pop(context);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }
        return modified;
    }

    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(ThreadContext context) {
        unpack();
        modifyCheck();

        RubyArray result = new RubyArray(context.runtime, getMetaClass(), realLength);
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

        RubyArray result = new RubyArray(context.runtime, getMetaClass(), realLength);
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

        RubyArray result = new RubyArray(runtime, getMetaClass(), realLength);
        flatten(context, -1, result);
        result.infectBy(this);
        return result;
    }

    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        int level = RubyNumeric.num2int(arg);
        if (level == 0) return makeShared();

        RubyArray result = new RubyArray(runtime, getMetaClass(), realLength);
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

        return getRuntime().newFixnum(n);
    }

    /** rb_ary_plus
     *
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject obj) {
        Ruby runtime = getRuntime();
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
        if (len == 0) return new RubyArray(runtime, getMetaClass(), IRubyObject.NULL_ARRAY).infectBy(this);
        if (len < 0) throw runtime.newArgumentError("negative argument");

        if (Long.MAX_VALUE / len < realLength) {
            throw runtime.newArgumentError("argument too big");
        }

        len *= realLength;

        checkLength(runtime, len);
        RubyArray ary2 = new RubyArray(runtime, getMetaClass(), (int)len);
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
    private RubyHash makeHash() {
        return makeHash(new RubyHash(getRuntime(), false));
    }

    private RubyHash makeHash(RubyHash hash) {
        for (int i = 0; i < realLength; i++) {
            hash.fastASet(elt(i), NEVER);
        }
        return hash;
    }

    private RubyHash makeHash(RubyArray ary2) {
        return ary2.makeHash(makeHash());
    }

    private RubyHash makeHash(ThreadContext context, Block block) {
        return makeHash(context, new RubyHash(getRuntime(), false), block);
    }

    private RubyHash makeHash(ThreadContext context, RubyHash hash, Block block) {
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            IRubyObject k = block.yield(context, v);
            if (hash.fastARef(k) == null) hash.fastASet(k, v);
        }
        return hash;
    }

    /** rb_ary_uniq_bang
     *
     */
    public IRubyObject uniq_bang(ThreadContext context) {
        RubyHash hash = makeHash();
        if (realLength == hash.size()) return context.runtime.getNil();

        // TODO: (CON) This could be a no-op for packed arrays if size does not change
        unpack();

        int j = 0;
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (hash.fastDelete(v)) store(j++, v);
        }
        realLength = j;
        return this;
    }

    @JRubyMethod(name = "uniq!")
    public IRubyObject uniq_bang19(ThreadContext context, Block block) {
        modifyCheck();

        if (!block.isGiven()) return uniq_bang(context);
        RubyHash hash = makeHash(context, block);
        if (realLength == hash.size()) return context.runtime.getNil();

        // TODO: (CON) This could be a no-op for packed arrays if size does not change
        unpack();

        realLength = 0;

        hash.visitAll(context, RubyHash.StoreValueVisitor, this);
        return this;
    }

    /** rb_ary_uniq
     *
     */
    public IRubyObject uniq(ThreadContext context) {
        RubyHash hash = makeHash();
        if (realLength == hash.size()) return makeShared();

        RubyArray result = new RubyArray(context.runtime, getMetaClass(), hash.size());

        int j = 0;
        try {
            for (int i = 0; i < realLength; i++) {
                IRubyObject v = elt(i);
                if (hash.fastDelete(v)) result.values[j++] = v;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }
        result.realLength = j;
        return result;
    }

    @JRubyMethod(name = "uniq")
    public IRubyObject uniq19(ThreadContext context, Block block) {
        if (!block.isGiven()) return uniq(context);
        RubyHash hash = makeHash(context, block);

        RubyArray result = new RubyArray(context.runtime, getMetaClass(), hash.size());
        hash.visitAll(context, RubyHash.StoreValueVisitor, result);
        return result;
    }

    /** rb_ary_diff
     *
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_diff(IRubyObject other) {
        Ruby runtime = getRuntime();
        RubyHash hash = other.convertToArray().makeHash();
        RubyArray ary3 = newArray(runtime);

        try {
            for (int i = 0; i < realLength; i++) {
                IRubyObject value = eltOk(i);
                if (hash.fastARef(value) != null) continue;
                ary3.append(value);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime, e);
        }
        Helpers.fillNil(ary3.values, ary3.realLength, ary3.values.length, runtime);

        return ary3;
    }

    /** rb_ary_and
     *
     */
    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and(IRubyObject other) {
        Ruby runtime = getRuntime();
        RubyArray ary2 = other.convertToArray();
        RubyHash hash = ary2.makeHash();

        int maxSize = realLength < ary2.realLength ? realLength : ary2.realLength;

        if (maxSize == 0) return newEmptyArray(runtime);

        RubyArray ary3 = newBlankArray(runtime, maxSize);

        int index = 0;
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (hash.fastDelete(v)) ary3.store(index++, v);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 0) return newEmptyArray(runtime);
        if (index == 1 && maxSize == 2) return newArray(runtime, ary3.eltInternal(0));

        return ary3;
    }

    /** rb_ary_or
     *
     */
    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or(IRubyObject other) {
        Ruby runtime = getRuntime();
        RubyArray ary2 = other.convertToArray();
        RubyHash set = makeHash(ary2);

        int maxSize = realLength + ary2.realLength;

        if (maxSize == 0) return newEmptyArray(runtime);

        RubyArray ary3 = newBlankArray(runtime, maxSize);

        int index = 0;
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (set.fastDelete(v)) ary3.store(index++, v);
        }
        for (int i = 0; i < ary2.realLength; i++) {
            IRubyObject v = ary2.elt(i);
            if (set.fastDelete(v)) ary3.store(index++, v);
        }

        // if index is 1 and we made a size 2 array, repack
        if (index == 1 && maxSize == 2) return newArray(runtime, ary3.eltInternal(0));

        return ary3;
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

    protected IRubyObject sortInternal(final ThreadContext context, boolean honorOverride) {
        Ruby runtime = context.runtime;

        // One check per specialized fast-path to make the check invariant.
        final boolean fixnumBypass = !honorOverride || runtime.getFixnum().isMethodBuiltin("<=>");
        final boolean stringBypass = !honorOverride || runtime.getString().isMethodBuiltin("<=>");

        try {
            Arrays.sort(values, begin, begin + realLength, new Comparator() {
                public int compare(Object o1, Object o2) {
                    if (fixnumBypass && o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                        return compareFixnums((RubyFixnum) o1, (RubyFixnum) o2);
                    }
                    if (stringBypass && o1 instanceof RubyString && o2 instanceof RubyString) {
                        return ((RubyString) o1).op_cmp((RubyString) o2);
                    }
                    return compareOthers(context, (IRubyObject)o1, (IRubyObject)o2);
                }
            });
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }
        return this;
    }

    protected static int compareFixnums(RubyFixnum o1, RubyFixnum o2) {
        long a = o1.getLongValue();
        long b = o2.getLongValue();
        return a > b ? 1 : a == b ? 0 : -1;
    }

    protected static int compareOthers(ThreadContext context, IRubyObject o1, IRubyObject o2) {
        IRubyObject ret = sites(context).op_cmp_sort.call(context, o1, o1, o2);
        int n = RubyComparable.cmpint(context, ret, o1, o2);
        //TODO: ary_sort_check should be done here
        return n;
    }

    protected IRubyObject sortInternal(final ThreadContext context, final Block block) {
        IRubyObject[] newValues = new IRubyObject[realLength];
        int length = realLength;

        copyInto(newValues, 0);
        Arrays.sort(newValues, 0, length, new Comparator() {
            CallSite gt = sites(context).op_gt_sort;
            CallSite lt = sites(context).op_lt_sort;
            public int compare(Object o1, Object o2) {
                IRubyObject obj1 = (IRubyObject) o1;
                IRubyObject obj2 = (IRubyObject) o2;
                IRubyObject ret = block.yieldArray(context, getRuntime().newArray(obj1, obj2), null);
                //TODO: ary_sort_check should be done here
                return RubyComparable.cmpint(context, gt, lt, ret, obj1, obj2);
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
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "sort_by!", enumLengthFn());

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
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "cycle", cycleSizeFn(context));
        return cycleCommon(context, -1, block);
    }

    /** rb_ary_cycle
     *
     */
    @JRubyMethod(name = "cycle")
    public IRubyObject cycle(ThreadContext context, IRubyObject arg, Block block) {
        if (arg.isNil()) return cycle(context, block);
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "cycle", new IRubyObject[] {arg}, cycleSizeFn(context));

        long times = RubyNumeric.num2long(arg);
        if (times <= 0) return context.runtime.getNil();

        return cycleCommon(context, times, block);
    }

    private IRubyObject cycleCommon(ThreadContext context, long n, Block block) {
        while (realLength > 0 && (n < 0 || 0 < n--)) {
            for (int i = 0; i < realLength; i++) {
                block.yield(context, eltOk(i));
            }
        }
        return context.runtime.getNil();
    }

    private SizeFn cycleSizeFn(final ThreadContext context) {
        return new SizeFn() {
            CallSite op_times = sites(context).op_times;
            @Override
            public IRubyObject size(IRubyObject[] args) {
                Ruby runtime = context.runtime;
                IRubyObject n = runtime.getNil();

                if (realLength == 0) {
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

                RubyFixnum length = length();
                return op_times.call(context, length, length, RubyFixnum.newFixnum(runtime, multiple));
            }
        };
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
        for (int i = 1; i < n; i++) arrays[i] = (RubyArray) TypeConverter.convertToType19(context, args[i - 1], array, to_ary_checked);

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

        RubyArray result = useBlock ? null : newBlankArray(runtime, resultLen);

        for (int i = 0; i < resultLen; i++) {
            RubyArray sub = newBlankArray(runtime, n);
            for (int j = 0; j < n; j++) sub.store(j, arrays[j].entry(counters[j]));

            if (useBlock) {
                block.yieldSpecific(context, sub);
            } else {
                result.store(i, sub);
            }
            int m = n - 1;
            counters[m]++;

            while (m > 0 && counters[m] == arrays[m].realLength) {
                counters[m] = 0;
                m--;
                counters[m]++;
            }
        }
        return useBlock ? this : result;
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
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "combination", new IRubyObject[]{num}, combinationSize(context));

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

    private SizeFn combinationSize(final ThreadContext context) {
        final RubyArray self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                long n = self.realLength;
                assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #combination ensures arg[0] is numeric
                long k = ((RubyNumeric) args[0]).getLongValue();

                return binomialCoefficient(context, k, n);
            }
        };
    }

    private IRubyObject binomialCoefficient(ThreadContext context, long comb, long size) {
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
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "repeated_combination", new IRubyObject[] { num }, repeatedCombinationSize(context));

        int n = RubyNumeric.num2int(num);
        int len = realLength;

        if (n < 0) {
            // yield nothing
        } else if (n == 0) {
            block.yield(context, newEmptyArray(runtime));
        } else if (n == 1) {
            for (int i = 0; i < len; i++) {
                block.yield(context, newArray(runtime, eltOk(i)));
            }
        } else {
            int[] p = new int[n];
            RubyArray values = makeShared();
            rcombinate(context, len, n, p, values, block);
        }

        return this;
    }

    private SizeFn repeatedCombinationSize(final ThreadContext context) {
        final RubyArray self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                long n = self.realLength;
                assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #repeated_combination ensures arg[0] is numeric
                long k = ((RubyNumeric) args[0]).getLongValue();

                if (k == 0) {
                    return RubyFixnum.one(context.runtime);
                }

                return binomialCoefficient(context, k, n + k - 1);
            }
        };
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
        RubyArray result = newBlankArray(context.runtime, r);

        for (int j = 0; j < r; j++) {
            result.store(j, values.eltInternal(p[j + pStart]));
        }

        result.realLength = r;
        block.yield(context, result);
    }

    private static void rpermute(ThreadContext context, int n, int r, int[] p, RubyArray values, Block block) {
        int i = 0, index = 0;

        p[index] = i;
        for (;;) {
            if (++index < r-1) {
                p[index] = i = 0;
                continue;
            }
            for (i = 0; i < n; ++i) {
                p[index] = i;
                // TODO: MRI has a weird reentrancy check that depends on having a null class in values
                yieldValues(context, r, p, 0, values, block);
            }
            do {
                if (index <= 0) return;
            } while ((i = ++p[--index]) >= n);
        }
    }

    /** rb_ary_permutation
     *
     */
    @JRubyMethod(name = "permutation")
    public IRubyObject permutation(ThreadContext context, IRubyObject num, Block block) {
        return block.isGiven() ? permutationCommon(context, RubyNumeric.num2int(num), false, block) : enumeratorizeWithSize(context, this, "permutation", new IRubyObject[] { num }, permutationSize(context));
    }

    @JRubyMethod(name = "permutation")
    public IRubyObject permutation(ThreadContext context, Block block) {
        return block.isGiven() ? permutationCommon(context, realLength, false, block) : enumeratorizeWithSize(context, this, "permutation", permutationSize(context));
    }

    @JRubyMethod(name = "repeated_permutation")
    public IRubyObject repeated_permutation(ThreadContext context, IRubyObject num, Block block) {
        return block.isGiven() ? permutationCommon(context, RubyNumeric.num2int(num), true, block) : enumeratorizeWithSize(context, this, "repeated_permutation", new IRubyObject[]{num}, repeatedPermutationSize(context));
    }

    private SizeFn repeatedPermutationSize(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final RubyArray self = this;

        return new SizeFn() {
            CallSite op_exp = sites(context).op_exp;
            @Override
            public IRubyObject size(IRubyObject[] args) {
                RubyFixnum n = self.length();
                assert args != null && args.length > 0 && args[0] instanceof RubyNumeric; // #repeated_permutation ensures arg[0] is numeric
                long k = ((RubyNumeric) args[0]).getLongValue();

                if (k < 0) {
                    return RubyFixnum.zero(runtime);
                }

                RubyFixnum v = RubyFixnum.newFixnum(runtime, k);
                return op_exp.call(context, n, n, v);
            }
        };
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
                        makeShared(begin, n, getMetaClass()), block);
            } else {
                permute(context, n, r,
                        new int[r],
                        new boolean[n],
                        makeShared(begin, n, getMetaClass()),
                        block);
            }
        }
        return this;
    }

    private SizeFn permutationSize(final ThreadContext context) {
        final RubyArray self = this;

        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
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
        };
    }

    private IRubyObject descendingFactorial(ThreadContext context, long from, long howMany) {
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

    public IRubyObject shuffle_bang(ThreadContext context) {
        return shuffle_bang(context, IRubyObject.NULL_ARRAY);
    }

    @JRubyMethod(name = "shuffle!", optional = 1)
    public IRubyObject shuffle_bang(ThreadContext context, IRubyObject[] args) {
        modify();
        IRubyObject randgen = context.runtime.getRandomClass();
        if (args.length > 0) {
            IRubyObject hash = TypeConverter.checkHashType(context.runtime, args[args.length - 1]);
            if (!hash.isNil()) {
                IRubyObject argRandgen = ((RubyHash) hash).fastARef(context.runtime.newSymbol("random"));
                if (argRandgen != null) {
                    randgen = argRandgen;
                }
            }
        }
        int i = realLength;
        try {
            while (i > 0) {
                int r = (int) (RubyRandom.randomReal(context, randgen) * i);
                IRubyObject tmp = eltOk(--i);
                eltSetOk(i, eltOk(r));
                eltSetOk(r, tmp);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(context.runtime, ex);
        }

        return this;
    }

    public IRubyObject shuffle(ThreadContext context) {
        return shuffle(context, IRubyObject.NULL_ARRAY);
    }

    @JRubyMethod(name = "shuffle", optional = 1)
    public IRubyObject shuffle(ThreadContext context, IRubyObject[] args) {
        RubyArray ary = aryDup();
        ary.shuffle_bang(context, args);
        return ary;
    }

    private static final int SORTED_THRESHOLD = 10;

    @JRubyMethod(name = "sample", optional = 2)
    public IRubyObject sample(ThreadContext context, IRubyObject[] args) {
        unpack();
        try {
            IRubyObject randgen = context.runtime.getRandomClass();
            if (args.length == 0) {
                if (realLength == 0)
                    return context.nil;
                int i = realLength == 1 ? 0 : randomReal(context, randgen, realLength);
                return eltOk(i);
            }
            if (args.length > 0) {
                IRubyObject hash = TypeConverter.checkHashType(context.runtime,
                        args[args.length - 1]);
                if (!hash.isNil()) {
                    IRubyObject argRandgen = ((RubyHash) hash).fastARef(context.runtime.newSymbol("random"));
                    if (argRandgen != null) {
                        randgen = argRandgen;
                    }
                    args = ArraySupport.newCopy(args, args.length - 1);
                }
            }
            if (args.length == 0) {
                if (realLength == 0) {
                    return context.nil;
                } else if (realLength == 1) {
                    return eltOk(0);
                }
                return eltOk(randomReal(context, randgen, realLength));
            }
            final Ruby runtime = context.runtime;
            int n = RubyNumeric.num2int(args[0]);

            if (n < 0)
                throw runtime.newArgumentError("negative sample number");
            if (n > realLength)
                n = realLength;
            double[] rnds = new double[SORTED_THRESHOLD];
            if (n <= SORTED_THRESHOLD) {
                for (int idx = 0; idx < n; ++idx) {
                    rnds[idx] = RubyRandom.randomReal(context, randgen);
                }
            }

            int i, j, k;
            switch (n) {
            case 0:
                return newEmptyArray(runtime);
            case 1:
                if (realLength <= 0)
                    return newEmptyArray(runtime);

                return newArray(runtime, eltOk((int) (rnds[0] * realLength)));
            case 2:
                i = (int) (rnds[0] * realLength);
                j = (int) (rnds[1] * (realLength - 1));
                if (j >= i)
                    j++;
                return newArray(runtime, eltOk(i), eltOk(j));
            case 3:
                i = (int) (rnds[0] * realLength);
                j = (int) (rnds[1] * (realLength - 1));
                k = (int) (rnds[2] * (realLength - 2));
                int l = j,
                g = i;
                if (j >= i) {
                    l = i;
                    g = ++j;
                }
                if (k >= l && (++k >= g))
                    ++k;
                return newArray(runtime, eltOk(i), eltOk(j), eltOk(k));
            }

            int len = realLength;
            if (n > len)
                n = len;
            if (n < SORTED_THRESHOLD) {
                int idx[] = new int[SORTED_THRESHOLD];
                int sorted[] = new int[SORTED_THRESHOLD];
                sorted[0] = idx[0] = (int) (rnds[0] * len);
                for (i = 1; i < n; i++) {
                    k = (int) (rnds[i] * --len);
                    for (j = 0; j < i; j++) {
                        if (k < sorted[j])
                            break;
                        k++;
                    }
                    System.arraycopy(sorted, j, sorted, j + 1, i - j);
                    sorted[j] = idx[i] = k;
                }
                IRubyObject[] result = new IRubyObject[n];
                for (i = 0; i < n; i++)
                    result[i] = eltOk(idx[i]);
                return RubyArray.newArrayMayCopy(runtime, result);
            } else {
                IRubyObject[] result = new IRubyObject[len];
                System.arraycopy(values, begin, result, 0, len);
                for (i = 0; i < n; i++) {
                    j = randomReal(context, randgen, len - i) + i;
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

    private int randomReal(ThreadContext context, IRubyObject randgen, int len) {
        return (int) (RubyRandom.randomReal(context, randgen) * len);
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

    // Enumerable direct implementations (non-"each" versions)
    // NOTE: not a @JRubyMethod(name = "all?") as there's no Array#all? on MRI
    public IRubyObject all_p(ThreadContext context, Block block) {
        if (!isBuiltin("each")) return RubyEnumerable.all_pCommon(context, this, block);
        if (!block.isGiven()) return all_pBlockless(context);

        for (int i = 0; i < realLength; i++) {
            if (!block.yield(context, eltOk(i)).isTrue()) return context.runtime.getFalse();
        }

        return context.runtime.getTrue();
    }

    private IRubyObject all_pBlockless(ThreadContext context) {
        for (int i = 0; i < realLength; i++) {
            if (!eltOk(i).isTrue()) return context.runtime.getFalse();
        }

        return context.runtime.getTrue();
    }

    @JRubyMethod(name = "any?")
    public IRubyObject any_p(ThreadContext context, Block block) {
        if (isEmpty()) return context.runtime.getFalse();
        if (!isBuiltin("each")) return RubyEnumerable.any_pCommon(context, this, block);
        if (!block.isGiven()) return any_pBlockless(context);

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return context.runtime.getTrue();
        }

        return context.runtime.getFalse();
    }

    private IRubyObject any_pBlockless(ThreadContext context) {
        for (int i = 0; i < realLength; i++) {
            if (eltOk(i).isTrue()) return context.runtime.getTrue();
        }

        return context.runtime.getFalse();
    }

    public IRubyObject find(ThreadContext context, IRubyObject ifnone, Block block) {
        if (!isBuiltin("each")) return RubyEnumerable.detectCommon(context, this, block);

        return detectCommon(context, ifnone, block);
    }

    public IRubyObject find_index(ThreadContext context, Block block) {
        if (!isBuiltin("each")) return RubyEnumerable.find_indexCommon(context, this, block, Signature.OPTIONAL);

        for (int i = 0; i < realLength; i++) {
            if (block.yield(context, eltOk(i)).isTrue()) return context.runtime.newFixnum(i);
        }

        return context.nil;
    }

    public IRubyObject find_index(ThreadContext context, IRubyObject cond) {
        if (!isBuiltin("each")) return RubyEnumerable.find_indexCommon(context, this, cond);

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

        return ifnone != null ? sites(context).call.call(context, ifnone, ifnone) : context.nil;
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
        RubyArray result = newBlankArray(runtime, size);

        input.registerLinkTarget(result);

        for (int i = 0; i < size; i++) {
            result.store(i, input.unmarshalObject());
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

    @JRubyMethod(name = "try_convert", meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return arg.checkArrayType();
    }

    @JRubyMethod(name = "pack", required = 1)
    public RubyString pack(ThreadContext context, IRubyObject obj) {
        RubyString iFmt = obj.convertToString();
        try {
            return Pack.pack(context, context.runtime, this, iFmt);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(context.runtime, e);
        }
    }

    @JRubyMethod(name = "dig", required = 1, rest = true)
    public IRubyObject dig(ThreadContext context, IRubyObject[] args) {
        return dig(context, args, 0);
    }

    final IRubyObject dig(ThreadContext context, IRubyObject[] args, int idx) {
        final IRubyObject val = at( args[idx++] );
        return idx == args.length ? val : RubyObject.dig(context, val, args, idx);
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
        if (token == ((RubyBasicObject) a).getMetaClass().generation) {
            if (a instanceof RubyFixnum && b instanceof RubyFixnum) {
                long aLong = ((RubyFixnum) a).getLongValue();
                long bLong = ((RubyFixnum) b).getLongValue();
                return Long.compare(aLong, bLong);
            } else if (a instanceof RubyString && b instanceof RubyString) {
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
    public Object toJava(Class target) {
        if (target.isArray()) {
            Class type = target.getComponentType();
            Object rawJavaArray = Array.newInstance(type, realLength);
            try {
                ArrayUtils.copyDataToJavaArrayDirect(getRuntime().getCurrentContext(), this, rawJavaArray);
            } catch (ArrayIndexOutOfBoundsException aioob) {
                concurrentModification();
            }
            return rawJavaArray;
        } else {
            return super.toJava(target);
        }
    }

    public boolean add(Object element) {
        append(JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element));
        return true;
    }

    public boolean remove(Object element) {
        unpack();
        Ruby runtime = getRuntime();
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
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            IRubyObject deleted = delete(getRuntime().getCurrentContext(), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), iter.next()), Block.NULL_BLOCK);
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
        store(index, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element));
        return previous;
    }

    // TODO: make more efficient by not creating IRubyArray[]
    public void add(int index, Object element) {
        insert(new IRubyObject[]{RubyFixnum.newFixnum(getRuntime(), index), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element)});
    }

    public Object remove(int index) {
        return delete_at(index).toJava(Object.class);
    }

    public int indexOf(Object element) {
        int myBegin = this.begin;

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element);

            for (int i = myBegin; i < myBegin + realLength; i++) {
                if (convertedElement.equals(values[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object element) {
        int myBegin = 0;

        if (element != null) {
            IRubyObject convertedElement = JavaUtil.convertJavaToUsableRubyObject(getRuntime(), element);

            for (int i = realLength - 1; i >= 0; i--) {
                if (convertedElement.equals(eltInternal(i))) {
                    return i;
                }
            }
        }

        return -1;
    }

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

            store(last, JavaUtil.convertJavaToUsableRubyObject(getRuntime(), obj));
        }

        public void add(Object obj) {
            insert(new IRubyObject[] { RubyFixnum.newFixnum(getRuntime(), index++), JavaUtil.convertJavaToUsableRubyObject(getRuntime(), obj) });
            last = -1;
        }
    }

    public ListIterator listIterator() {
        return new RubyArrayConversionListIterator();
    }

    public ListIterator listIterator(int index) {
        return new RubyArrayConversionListIterator(index);
	}

    // TODO: list.subList(from, to).clear() is supposed to clear the sublist from the list.
    // How can we support this operation?
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

    private IRubyObject safeArrayRef(IRubyObject[] values, int i) {
        return safeArrayRef(getRuntime(), values, i);
    }

    private static IRubyObject safeArrayRef(Ruby runtime, IRubyObject[] values, int i) {
        try {
            return values[i];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    private IRubyObject safeArraySet(IRubyObject[] values, int i, IRubyObject value) {
        return safeArraySet(getRuntime(), values, i, value);
    }

    private static IRubyObject safeArraySet(Ruby runtime, IRubyObject[] values, int i, IRubyObject value) {
        try {
            return values[i] = value;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw concurrentModification(runtime, ex);
        }
    }

    //private IRubyObject safeArrayRefSet(IRubyObject[] values, int i, IRubyObject value) {
    //    return safeArrayRefSet(getRuntime(), values, i, value);
    //}

    private static IRubyObject safeArrayRefSet(Ruby runtime, IRubyObject[] values, int i, IRubyObject value) {
        try {
            IRubyObject tmp = values[i];
            values[i] = value;
            return tmp;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw concurrentModification(runtime, e);
        }
    }

    //private IRubyObject safeArrayRefCondSet(IRubyObject[] values, int i, boolean doSet, IRubyObject value) {
    //    return safeArrayRefCondSet(getRuntime(), values, i, doSet, value);
    //}

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
        safeArrayCopy(getRuntime(), source, sourceStart, target, targetStart, length);
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
            IRubyObject[] values = new IRubyObject[newCapacity];
            System.arraycopy(this.values, begin, values, 0, len);
            this.values = values;
            this.begin = 0;
        }
    }

    @Deprecated
    public IRubyObject flatten_bang19(ThreadContext context) {
        return flatten_bang(context);
    }
}
