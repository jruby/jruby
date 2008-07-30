/*
 **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.lang.reflect.Array;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;

/**
 * The implementation of the built-in class Array in Ruby.
 *
 * Concurrency: no synchronization is required among readers, but
 * all users must synchronize externally with writers.
 *
 */
@JRubyClass(name="Array")
public class RubyArray extends RubyObject implements List {

    public static RubyClass createArrayClass(Ruby runtime) {
        RubyClass arrayc = runtime.defineClass("Array", runtime.getObject(), ARRAY_ALLOCATOR);
        runtime.setArray(arrayc);
        arrayc.index = ClassIndex.ARRAY;
        arrayc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyArray;
            }
        };

        arrayc.includeModule(runtime.getEnumerable());
        arrayc.defineAnnotatedMethods(RubyArray.class);

        return arrayc;
    }

    private static ObjectAllocator ARRAY_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyArray(runtime, klass);
        }
    };

    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.ARRAY;
    }

    /** rb_ary_s_create
     * 
     */
    @JRubyMethod(name = "[]", rest = true, frame = true, meta = true)
    public static IRubyObject create(IRubyObject klass, IRubyObject[] args, Block block) {
        RubyArray arr = (RubyArray) ((RubyClass) klass).allocate();
        arr.callInit(IRubyObject.NULL_ARRAY, block);
    
        if (args.length > 0) {
            arr.alloc(args.length);
            System.arraycopy(args, 0, arr.values, 0, args.length);
            arr.realLength = args.length;
        }
        return arr;
    }

    /** rb_ary_new2
     *
     */
    public static final RubyArray newArray(final Ruby runtime, final long len) {
        return new RubyArray(runtime, len);
    }
    public static final RubyArray newArrayLight(final Ruby runtime, final long len) {
        return new RubyArray(runtime, len, false);
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray newArray(final Ruby runtime) {
        return new RubyArray(runtime, ARRAY_DEFAULT_SIZE);
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray newArrayLight(final Ruby runtime) {
        /* Ruby arrays default to holding 16 elements, so we create an
         * ArrayList of the same size if we're not told otherwise
         */
        RubyArray arr = new RubyArray(runtime, false);
        arr.alloc(ARRAY_DEFAULT_SIZE);
        return arr;
    }

    public static RubyArray newArray(Ruby runtime, IRubyObject obj) {
        return new RubyArray(runtime, new IRubyObject[] { obj });
    }

    public static RubyArray newArrayLight(Ruby runtime, IRubyObject obj) {
        return new RubyArray(runtime, new IRubyObject[] { obj }, false);
    }

    /** rb_assoc_new
     *
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return new RubyArray(runtime, new IRubyObject[] { car, cdr });
    }
    
    public static RubyArray newEmptyArray(Ruby runtime) {
        return new RubyArray(runtime, NULL_ARRAY);
    }

    /** rb_ary_new4, rb_ary_new3
     *   
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject[] args) {
        RubyArray arr = new RubyArray(runtime, args.length);
        System.arraycopy(args, 0, arr.values, 0, args.length);
        arr.realLength = args.length;
        return arr;
    }
    
    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args) {
        return new RubyArray(runtime, args);
    }
    
    public static RubyArray newArrayNoCopy(Ruby runtime, IRubyObject[] args, int begin) {
        return new RubyArray(runtime, args, begin);
    }
    
    public static RubyArray newArrayNoCopyLight(Ruby runtime, IRubyObject[] args) {
        RubyArray arr = new RubyArray(runtime, false);
        arr.values = args;
        arr.realLength = args.length;
        return arr;
    }

    public static RubyArray newArray(Ruby runtime, Collection collection) {
        RubyArray arr = new RubyArray(runtime, collection.size());
        collection.toArray(arr.values);
        arr.realLength = arr.values.length;
        return arr;
    }

    public static final int ARRAY_DEFAULT_SIZE = 16;    

    private IRubyObject[] values;

    private static final int TMPLOCK_ARR_F = 1 << 9;
    private static final int TMPLOCK_OR_FROZEN_ARR_F = TMPLOCK_ARR_F | FROZEN_F;

    private volatile boolean isShared = false;
    private int begin = 0;
    private int realLength = 0;

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
    
    /* rb_ary_new2
     * just allocates the internal array
     */
    private RubyArray(Ruby runtime, long length) {
        super(runtime, runtime.getArray());
        checkLength(length);
        alloc((int) length);
    }

    private RubyArray(Ruby runtime, long length, boolean objectspace) {
        super(runtime, runtime.getArray(), objectspace);
        checkLength(length);
        alloc((int)length);
    }     

    /* rb_ary_new3, rb_ary_new4
     * allocates the internal array of size length and copies the 'length' elements
     */
    public RubyArray(Ruby runtime, long length, IRubyObject[] vals) {
        super(runtime, runtime.getArray());
        checkLength(length);
        int ilength = (int) length;
        alloc(ilength);
        if (ilength > 0 && vals.length > 0) System.arraycopy(vals, 0, values, 0, ilength);

        realLength = ilength;
    }

    /* NEWOBJ and OBJSETUP equivalent
     * fastest one, for shared arrays, optional objectspace
     */
    private RubyArray(Ruby runtime, boolean objectSpace) {
        super(runtime, runtime.getArray(), objectSpace);
    }

    private RubyArray(Ruby runtime) {
        super(runtime, runtime.getArray());
        alloc(ARRAY_DEFAULT_SIZE);
    }

    public RubyArray(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        alloc(ARRAY_DEFAULT_SIZE);
    }
    
    /* Array constructors taking the MetaClass to fulfil MRI Array subclass behaviour
     * 
     */
    private RubyArray(Ruby runtime, RubyClass klass, int length) {
        super(runtime, klass);
        alloc(length);
    }
    
    private RubyArray(Ruby runtime, RubyClass klass, long length) {
        super(runtime, klass);
        checkLength(length);
        alloc((int)length);
    }

    private RubyArray(Ruby runtime, RubyClass klass, long length, boolean objectspace) {
        super(runtime, klass, objectspace);
        checkLength(length);
        alloc((int)length);
    }    

    private RubyArray(Ruby runtime, RubyClass klass, boolean objectSpace) {
        super(runtime, klass, objectSpace);
    }
    
    private RubyArray(Ruby runtime, RubyClass klass, RubyArray original) {
        super(runtime, klass);
        realLength = original.realLength;
        alloc(realLength);
        System.arraycopy(original.values, original.begin, values, 0, realLength);
    }
    
    private final IRubyObject[] reserve(int length) {
        return new IRubyObject[length];
    }

    private final void alloc(int length) {
        values = new IRubyObject[length];
    }

    private final void realloc(int newLength) {
        IRubyObject[] reallocated = new IRubyObject[newLength];
        System.arraycopy(values, 0, reallocated, 0, newLength > realLength ? realLength : newLength);
        values = reallocated;
    }

    private final void checkLength(long length) {
        if (length < 0) {
            throw getRuntime().newArgumentError("negative array size (or size too big)");
        }

        if (length >= Integer.MAX_VALUE) {
            throw getRuntime().newArgumentError("array size too big");
        }
    }

    /** Getter for property list.
     * @return Value of property list.
     */
    public List getList() {
        return Arrays.asList(toJavaArray()); 
    }

    public int getLength() {
        return realLength;
    }

    public IRubyObject[] toJavaArray() {
        IRubyObject[] copy = reserve(realLength);
        System.arraycopy(values, begin, copy, 0, realLength);
        return copy;
    }
    
    public IRubyObject[] toJavaArrayUnsafe() {
        return !isShared ? values : toJavaArray();
    }    

    public IRubyObject[] toJavaArrayMaybeUnsafe() {
        return (!isShared && begin == 0 && values.length == realLength) ? values : toJavaArray();
    }    

    /** rb_ary_make_shared
    *
    */
    private final RubyArray makeShared(int beg, int len, RubyClass klass) {
        return makeShared(beg, len, klass, klass.getRuntime().isObjectSpaceEnabled());
    }    
    
    /** rb_ary_make_shared
     *
     */
    private final RubyArray makeShared(int beg, int len, RubyClass klass, boolean objectSpace) {
        RubyArray sharedArray = new RubyArray(getRuntime(), klass, objectSpace);
        isShared = true;
        sharedArray.values = values;
        sharedArray.isShared = true;
        sharedArray.begin = beg;
        sharedArray.realLength = len;
        return sharedArray;
    }

    /** rb_ary_modify_check
     *
     */
    private final void modifyCheck() {
        if ((flags & TMPLOCK_OR_FROZEN_ARR_F) != 0) {
            if ((flags & FROZEN_F) != 0) throw getRuntime().newFrozenError("array");           
            if ((flags & TMPLOCK_ARR_F) != 0) throw getRuntime().newTypeError("can't modify array during iteration");
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify array");
        }
    }

    /** rb_ary_modify
     *
     */
    private final void modify() {
        modifyCheck();
        if (isShared) {
            IRubyObject[] vals = reserve(realLength);
            isShared = false;
            System.arraycopy(values, begin, vals, 0, realLength);
            begin = 0;            
            values = vals;
        }
    }

    /*  ================
     *  Instance Methods
     *  ================ 
     */

    /** rb_ary_initialize
     * 
     */
    @JRubyMethod(name = "initialize", required = 0, optional = 2, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = args.length;
        Ruby runtime = getRuntime();

        if (argc == 0) {
            modifyCheck();
            realLength = 0;
            if (block.isGiven()) runtime.getWarnings().warn(ID.BLOCK_UNUSED, "given block not used");

    	    return this;
    	}

        if (argc == 1 && !(args[0] instanceof RubyFixnum)) {
            IRubyObject val = args[0].checkArrayType();
            if (!val.isNil()) {
                replace(val);
                return this;
            }
        }

        long len = RubyNumeric.num2long(args[0]);

        if (len < 0) throw runtime.newArgumentError("negative array size");

        if (len >= Integer.MAX_VALUE) throw runtime.newArgumentError("array size too big");

        int ilen = (int) len;

        modify();

        if (ilen > values.length) values = reserve(ilen);

        if (block.isGiven()) {
            if (argc == 2) {
                runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
            }

            for (int i = 0; i < ilen; i++) {
                store(i, block.yield(context, new RubyFixnum(runtime, i)));
                realLength = i + 1;
            }
        } else {
            Arrays.fill(values, 0, ilen, (argc == 2) ? args[1] : runtime.getNil());
            realLength = ilen;
        }
    	return this;
    }

    /** rb_ary_initialize_copy
     * 
     */
    @JRubyMethod(name = {"initialize_copy"}, required = 1, visibility=Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        return this.replace(orig);
    }
    
    /** rb_ary_replace
     *
     */
    @JRubyMethod(name = {"replace"}, required = 1)
    public IRubyObject replace(IRubyObject orig) {
        modifyCheck();

        RubyArray origArr = orig.convertToArray();

        if (this == orig) return this;

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
        if (realLength == 0) return RubyString.newEmptyString(getRuntime());

        return join(getRuntime().getCurrentContext(), getRuntime().getGlobalVariables().get("$,"));
    }

    
    public boolean includes(ThreadContext context, IRubyObject item) {
        int begin = this.begin;
        
        for (int i = begin; i < begin + realLength; i++) {
            if (equalInternal(context, values[i], item)) return true;
    	}
        
        return false;
    }

    /** rb_ary_hash
     * 
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        int h = realLength;

        Ruby runtime = getRuntime();
        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            h ^= RubyNumeric.num2long(values[i].callMethod(context, MethodIndex.HASH, "hash"));
        }

        return runtime.newFixnum(h);
    }

    /** rb_ary_store
     *
     */
    public final IRubyObject store(long index, IRubyObject value) {
        if (index < 0) {
            index += realLength;
            if (index < 0) {
                throw getRuntime().newIndexError("index " + (index - realLength) + " out of array");
            }
        }

        modify();

        if (index >= realLength) {
            if (index >= values.length) {
                long newLength = values.length >> 1;

                if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

                newLength += index;
                if (newLength >= Integer.MAX_VALUE) {
                    throw getRuntime().newArgumentError("index too big");
                }
                realloc((int) newLength);
            }
            if(index != realLength) Arrays.fill(values, realLength, (int) index + 1, getRuntime().getNil());
            
            realLength = (int) index + 1;
        }

        values[(int) index] = value;
        return value;
    }

    /** rb_ary_elt
     *
     */
    private final IRubyObject elt(long offset) {
        final IRubyObject value;
        try {
            value = values[begin + (int)offset];
        } catch (ArrayIndexOutOfBoundsException e) {
            return getRuntime().getNil();
        }
        if (value == null || offset < 0 || offset >= realLength) {
            return getRuntime().getNil();
        } else {
            return value;
        }
    }

    /** rb_ary_entry
     *
     */
    public final IRubyObject entry(long offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt(offset);
    }


    /** rb_ary_entry
     *
     */
    public final IRubyObject entry(int offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt(offset);
    }

    public final IRubyObject eltInternal(int offset) {
        return values[begin + offset];
    }
    
    public final IRubyObject eltInternalSet(int offset, IRubyObject item) {
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
    @JRubyMethod(name = "fetch", frame = true)
    public IRubyObject fetch(ThreadContext context, IRubyObject arg0, Block block) {
        long index = RubyNumeric.num2long(arg0);

        if (index < 0) index += realLength;
        if (index < 0 || index >= realLength) {
            if (block.isGiven()) return block.yield(context, arg0);
            throw getRuntime().newIndexError("index " + index + " out of array");
        }
        
        return values[begin + (int) index];
    }

    /** rb_ary_fetch
    *
    */
   @JRubyMethod(name = "fetch", frame = true)
   public IRubyObject fetch(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
       if (block.isGiven()) getRuntime().getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");

       long index = RubyNumeric.num2long(arg0);

       if (index < 0) index += realLength;
       if (index < 0 || index >= realLength) {
           if (block.isGiven()) return block.yield(context, arg0);
           return arg1;
       }
       
       return values[begin + (int) index];
   }    

    /** rb_ary_to_ary
     * 
     */
    private static RubyArray aryToAry(IRubyObject obj) {
        if (obj instanceof RubyArray) return (RubyArray) obj;

        if (obj.respondsTo("to_ary")) return obj.convertToArray();

        RubyArray arr = new RubyArray(obj.getRuntime(), false); // possibly should not in object space
        arr.alloc(1);
        arr.values[0] = obj;
        arr.realLength = 1;
        return arr;
    }

    /** rb_ary_splice
     * 
     */
    private final void splice(long beg, long len, IRubyObject rpl) {
        if (len < 0) throw getRuntime().newIndexError("negative length (" + len + ")");

        if (beg < 0) {
            beg += realLength;
            if (beg < 0) {
                beg -= realLength;
                throw getRuntime().newIndexError("index " + beg + " out of array");
            }
        }
        
        final RubyArray rplArr;
        final int rlen;

        if (rpl == null || rpl.isNil()) {
            rplArr = null;
            rlen = 0;
        } else {
            rplArr = aryToAry(rpl);
            rlen = rplArr.realLength;
        }

        modify();

        if (beg >= realLength) {
            len = beg + rlen;

            if (len >= values.length) {
                int tryNewLength = values.length + (values.length >> 1);
                realloc(len > tryNewLength ? (int)len : tryNewLength);
            }

            Arrays.fill(values, realLength, (int) beg, getRuntime().getNil());
            realLength = (int) len;
        } else {
            if (beg + len > realLength) len = realLength - beg;

            long alen = realLength + rlen - len;
            if (alen >= values.length) {
                int tryNewLength = values.length + (values.length >> 1);
                realloc(alen > tryNewLength ? (int)alen : tryNewLength);
            }

            if (len != rlen) {
                System.arraycopy(values, (int) (beg + len), values, (int) beg + rlen, realLength - (int) (beg + len));
                realLength = (int) alen;
            }
        }

        if (rlen > 0) System.arraycopy(rplArr.values, rplArr.begin, values, (int) beg, rlen);
    }

    /** rb_ary_splice
     * 
     */
    private final void spliceOne(long beg, long len, IRubyObject rpl) {
        if (len < 0) throw getRuntime().newIndexError("negative length (" + len + ")");

        if (beg < 0) {
            beg += realLength;
            if (beg < 0) {
                beg -= realLength;
                throw getRuntime().newIndexError("index " + beg + " out of array");
            }
        }

        modify();

        if (beg >= realLength) {
            len = beg + 1;

            if (len >= values.length) {
                int tryNewLength = values.length + (values.length >> 1);
                realloc(len > tryNewLength ? (int)len : tryNewLength);
            }

            Arrays.fill(values, realLength, (int) beg, getRuntime().getNil());
            realLength = (int) len;
        } else {
            if (beg + len > realLength) len = realLength - beg;

            int alen = realLength + 1 - (int)len;
            if (alen >= values.length) {
                int tryNewLength = values.length + (values.length >> 1);
                realloc(alen > tryNewLength ? alen : tryNewLength);
            }

            if (len != 1) {
                System.arraycopy(values, (int) (beg + len), values, (int) beg + 1, realLength - (int) (beg + len));
                realLength = alen;
            }
        }

        values[(int)beg] = rpl;
    }

    /** rb_ary_insert
     * 
     */
    @JRubyMethod
    public IRubyObject insert(IRubyObject arg) {
        return this;
    }

    /** rb_ary_insert
     * 
     */
    @JRubyMethod
    public IRubyObject insert(IRubyObject arg1, IRubyObject arg2) {
        long pos = RubyNumeric.num2long(arg1);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;
        
        spliceOne(pos, 0, arg2); // rb_ary_new4
        
        return this;
    }

    /** rb_ary_insert
     * 
     */
    @JRubyMethod(name = "insert", required = 1, rest = true)
    public IRubyObject insert(IRubyObject[] args) {
        if (args.length == 1) return this;

        long pos = RubyNumeric.num2long(args[0]);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;

        RubyArray inserted = new RubyArray(getRuntime(), false);
        inserted.values = args;
        inserted.begin = 1;
        inserted.realLength = args.length - 1;
        
        splice(pos, 0, inserted); // rb_ary_new4
        
        return this;
    }

    /** rb_ary_dup
     * 
     */
    public final RubyArray aryDup() {
        RubyArray dup = new RubyArray(getRuntime(), getMetaClass(), this);
        dup.flags |= flags & TAINTED_F; // from DUP_SETUP
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
        int end = begin + alen;
        for (int i = begin; i < end; i++) {
            tmp = elt(i).convertToArray();
            if (elen < 0) {
                elen = tmp.realLength;
                result = new RubyArray(runtime, elen);
                for (int j = 0; j < elen; j++) {
                    result.store(j, new RubyArray(runtime, alen));
                }
            } else if (elen != tmp.realLength) {
                throw runtime.newIndexError("element size differs (" + tmp.realLength
                        + " should be " + elen + ")");
            }
            for (int j = 0; j < elen; j++) {
                ((RubyArray) result.elt(j)).store(i - begin, tmp.elt(j));
            }
        }
        return result;
    }

    /** rb_values_at (internal)
     * 
     */
    private final IRubyObject values_at(long olen, IRubyObject[] args) {
        RubyArray result = new RubyArray(getRuntime(), args.length);

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
                int end = begin + len;
                for (int j = begin; j < end; j++) {
                    result.append(entry(j + beg));
                }
                continue;
            }
            result.append(entry(RubyNumeric.num2long(args[i])));
        }

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
        if (beg > realLength || beg < 0 || len < 0) return getRuntime().getNil();

        if (beg + len > realLength) {
            len = realLength - beg;
            
            if (len < 0) len = 0;
        }
        
        if (len == 0) return new RubyArray(getRuntime(), getMetaClass(), 0);

        return makeShared(begin + (int) beg, (int) len, getMetaClass());
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseqLight(long beg, long len) {
        if (beg > realLength || beg < 0 || len < 0) return getRuntime().getNil();

        if (beg + len > realLength) {
            len = realLength - beg;
            
            if (len < 0) len = 0;
        }
        
        if (len == 0) return new RubyArray(getRuntime(), getMetaClass(), 0, false);

        return makeShared(begin + (int) beg, (int) len, getMetaClass(), false);
    }

    /** rb_ary_length
     *
     */
    @JRubyMethod(name = "length", alias = "size")
    public RubyFixnum length() {
        return getRuntime().newFixnum(realLength);
    }

    /** rb_ary_push - specialized rb_ary_store 
     *
     */
    @JRubyMethod(name = "<<", required = 1)
    public RubyArray append(IRubyObject item) {
        modify();
        
        if (realLength == values.length) {
        if (realLength == Integer.MAX_VALUE) throw getRuntime().newArgumentError("index too big");
            
            long newLength = values.length + (values.length >> 1);
            if ( newLength > Integer.MAX_VALUE ) {
                newLength = Integer.MAX_VALUE;
            }else if ( newLength < ARRAY_DEFAULT_SIZE ) {
                newLength = ARRAY_DEFAULT_SIZE;
            }

            realloc((int) newLength);
        }
        
        values[realLength++] = item;
        return this;
    }

    /** rb_ary_push_m
     * FIXME: Whis is this named "push_m"?
     */
    @JRubyMethod(name = "push", rest = true)
    public RubyArray push_m(IRubyObject[] items) {
        for (int i = 0; i < items.length; i++) {
            append(items[i]);
        }
        
        return this;
    }

    /** rb_ary_pop
     *
     */
    @JRubyMethod(name = "pop")
    public IRubyObject pop() {
        modifyCheck();
        
        if (realLength == 0) return getRuntime().getNil();

        if (!isShared) {
            int index = begin + --realLength;
            IRubyObject obj = values[index];
            values[index] = null;
            return obj;
        } 

        return values[begin + --realLength];
    }

    /** rb_ary_shift
     *
     */
    @JRubyMethod(name = "shift")
    public IRubyObject shift() {
        modify();

        if (realLength == 0) return getRuntime().getNil();

        IRubyObject obj = values[begin];
        values[begin] = null;

        isShared = true;

        begin++;
        realLength--;

        return obj;
    }

    /** rb_ary_unshift
     *
     */
    public RubyArray unshift(IRubyObject item) {
        modify();

        if (realLength == values.length) {
            int newLength = values.length >> 1;
            if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

            newLength += values.length;
            realloc(newLength);
        }
        System.arraycopy(values, 0, values, 1, realLength);

        realLength++;
        values[0] = item;

        return this;
    }

    /** rb_ary_unshift_m
     *
     */
    @JRubyMethod(name = "unshift", rest = true)
    public RubyArray unshift_m(IRubyObject[] items) {
        long len = realLength;

        if (items.length == 0) return this;

        store(len + items.length - 1, getRuntime().getNil());

        // it's safe to use zeroes here since modified by store()
        System.arraycopy(values, 0, values, items.length, (int) len);
        System.arraycopy(items, 0, values, 0, items.length);
        
        return this;
    }

    /** rb_ary_includes
     * 
     */
    @JRubyMethod(name = "include?", required = 1)
    public RubyBoolean include_p(ThreadContext context, IRubyObject item) {
        return context.getRuntime().newBoolean(includes(context, item));
    }

    /** rb_ary_frozen_p
     *
     */
    @JRubyMethod(name = "frozen?")
    @Override
    public RubyBoolean frozen_p(ThreadContext context) {
        return context.getRuntime().newBoolean(isFrozen() || (flags & TMPLOCK_ARR_F) != 0);
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
        if (arg0 instanceof RubyFixnum) return entry(((RubyFixnum)arg0).getLongValue());
        if (arg0 instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as array index");
            
        long[] beglen;
        if (!(arg0 instanceof RubyRange)) {
        } else if ((beglen = ((RubyRange) arg0).begLen(realLength, 0)) == null) {
            return getRuntime().getNil();
        } else {
            return subseq(beglen[0], beglen[1]);
        }
        return entry(RubyNumeric.num2long(arg0));            
    }        

    /** rb_ary_aref
     */
    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject aref(IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as array index");

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

    /** rb_ary_aset
     *
     */
    @JRubyMethod(name = "[]=")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof RubyFixnum) {
            store(((RubyFixnum)arg0).getLongValue(), arg1);
            return arg1;
        }
        if (arg0 instanceof RubyRange) {
            long[] beglen = ((RubyRange) arg0).begLen(realLength, 1);
            splice(beglen[0], beglen[1], arg1);
            return arg1;
        }
        if (arg0 instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as array index");

        store(RubyNumeric.num2long(arg0), arg1);
        return arg1;
    }

    /** rb_ary_aset
    *
    */
    @JRubyMethod(name = "[]=")
    public IRubyObject aset(IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        if (arg0 instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as array index");
        if (arg1 instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as subarray length");
        splice(RubyNumeric.num2long(arg0), RubyNumeric.num2long(arg1), arg2);
        return arg2;
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
        RubyArray ary = obj.convertToArray();
        
        if (ary.realLength > 0) splice(realLength, 0, ary);

        return this;
    }

    /** inspect_ary
     * 
     */
    private IRubyObject inspectAry(ThreadContext context) {
        ByteList buffer = new ByteList();
        buffer.append('[');
        boolean tainted = isTaint();

        for (int i = 0; i < realLength; i++) {
            if (i > 0) buffer.append(',').append(' ');

            RubyString str = inspect(context, values[begin + i]);
            if (str.isTaint()) tainted = true;
            buffer.append(str.getByteList());
        }
        buffer.append(']');

        RubyString str = getRuntime().newString(buffer);
        if (tainted) str.setTaint(true);

        return str;
    }

    /** rb_ary_inspect
    *
    */
    @JRubyMethod(name = "inspect")
    @Override
    public IRubyObject inspect() {
        if (realLength == 0) return getRuntime().newString("[]");
        if (getRuntime().isInspecting(this)) return  getRuntime().newString("[...]");

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
        return values[begin];
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
        }

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
        return values[begin + realLength - 1];
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
        }

        return makeShared(begin + realLength - (int) n, (int) n, getRuntime().getArray());
    }

    /** rb_ary_each
     *
     */
    @JRubyMethod(name = "each", frame = true)
    public IRubyObject each(ThreadContext context, Block block) {
        for (int i = 0; i < realLength; i++) {
            block.yield(context, values[begin + i]);
        }
        return this;
    }

    /** rb_ary_each_index
     *
     */
    @JRubyMethod(name = "each_index", frame = true)
    public IRubyObject each_index(ThreadContext context, Block block) {
        Ruby runtime = getRuntime();
        for (int i = 0; i < realLength; i++) {
            block.yield(context, runtime.newFixnum(i));
        }
        return this;
    }

    /** rb_ary_reverse_each
     *
     */
    @JRubyMethod(name = "reverse_each", frame = true)
    public IRubyObject reverse_each(ThreadContext context, Block block) {
        int len = realLength;
        
        while(len-- > 0) {
            block.yield(context, values[begin + len]);
            
            if (realLength < len) len = realLength;
        }
        
        return this;
    }

    private IRubyObject inspectJoin(ThreadContext context, RubyArray tmp, IRubyObject sep) {
        Ruby runtime = getRuntime();

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
    public RubyString join(ThreadContext context, IRubyObject sep) {
        if (realLength == 0) return RubyString.newEmptyString(getRuntime());

        boolean taint = isTaint() || sep.isTaint();

        long len = 1;
        for (int i = begin; i < begin + realLength; i++) {            
            IRubyObject tmp = values[i].checkStringType();
            len += tmp.isNil() ? 10 : ((RubyString) tmp).getByteList().length();
        }

        RubyString strSep = null;
        if (!sep.isNil()) {
            sep = strSep = sep.convertToString();
            len += strSep.getByteList().length() * (realLength - 1);
        }

        ByteList buf = new ByteList((int)len);
        Ruby runtime = getRuntime();
        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject tmp = values[i];
            if (tmp instanceof RubyString) {
                // do nothing
            } else if (tmp instanceof RubyArray) {
                if (runtime.isInspecting(tmp)) {
                    tmp = runtime.newString("[...]");
                } else {
                    tmp = inspectJoin(context, (RubyArray)tmp, sep);
                }
            } else {
                tmp = RubyString.objAsString(context, tmp);
            }

            if (i > begin && !sep.isNil()) buf.append(strSep.getByteList());

            buf.append(tmp.asString().getByteList());
            if (tmp.isTaint()) taint = true;
        }

        RubyString result = runtime.newString(buf); 

        if (taint) result.setTaint(true);

        return result;
    }

    /** rb_ary_join_m
     *
     */
    @JRubyMethod(name = "join", optional = 1)
    public RubyString join_m(ThreadContext context, IRubyObject[] args) {
        int argc = args.length;
        IRubyObject sep = (argc == 1) ? args[0] : getRuntime().getGlobalVariables().get("$,");
        
        return join(context, sep);
    }

    /** rb_ary_to_a
     *
     */
    @JRubyMethod(name = "to_a")
    @Override
    public RubyArray to_a() {
        if(getMetaClass() != getRuntime().getArray()) {
            RubyArray dup = new RubyArray(getRuntime(), getRuntime().isObjectSpaceEnabled());

            isShared = true;
            dup.isShared = true;
            dup.values = values;
            dup.realLength = realLength; 
            dup.begin = begin;
            
            return dup;
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
        if (this == obj) return getRuntime().getTrue();

        if (!(obj instanceof RubyArray)) {
            if (!obj.respondsTo("to_ary")) {
                return getRuntime().getFalse();
            } else {
                if (equalInternal(context, obj.callMethod(context, "to_ary"), this)) return getRuntime().getTrue();
                return getRuntime().getFalse();                
            }
        }

        RubyArray ary = (RubyArray) obj;
        if (realLength != ary.realLength) return getRuntime().getFalse();

        Ruby runtime = getRuntime();
        for (long i = 0; i < realLength; i++) {
            if (!equalInternal(context, elt(i), ary.elt(i))) return runtime.getFalse();            
        }
        return runtime.getTrue();
    }

    /** rb_ary_eql
     *
     */
    @JRubyMethod(name = "eql?", required = 1)
    public RubyBoolean eql_p(ThreadContext context, IRubyObject obj) {
        if (this == obj) return getRuntime().getTrue();
        if (!(obj instanceof RubyArray)) return getRuntime().getFalse();

        RubyArray ary = (RubyArray) obj;

        if (realLength != ary.realLength) return getRuntime().getFalse();

        Ruby runtime = getRuntime();
        for (int i = 0; i < realLength; i++) {
            if (!eqlInternal(context, elt(i), ary.elt(i))) return runtime.getFalse();
        }
        return runtime.getTrue();
    }

    /** rb_ary_compact_bang
     *
     */
    @JRubyMethod(name = "compact!")
    public IRubyObject compact_bang() {
        modify();

        int p = 0;
        int t = 0;
        int end = p + realLength;

        while (t < end) {
            if (values[t].isNil()) {
                t++;
            } else {
                values[p++] = values[t++];
            }
        }

        if (realLength == p) return getRuntime().getNil();

        realloc(p);
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

        if(isShared) {
            alloc(ARRAY_DEFAULT_SIZE);
            isShared = true;
        } else if (values.length > ARRAY_DEFAULT_SIZE << 1){
            alloc(ARRAY_DEFAULT_SIZE << 1);
        } else {
            for (int i = 0; i < realLength; i++) {
                values[begin + i] = null;
            }
        }

        begin = 0;
        realLength = 0;
        return this;
    }

    /** rb_ary_fill
     *
     */
    @JRubyMethod(name = "fill", optional = 3, frame = true)
    public IRubyObject fill(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject item = null;
        IRubyObject begObj = null;
        IRubyObject lenObj = null;
        int argc = args.length;

        if (block.isGiven()) {
            Arity.checkArgumentCount(getRuntime(), args, 0, 2);
            item = null;
        	begObj = argc > 0 ? args[0] : null;
        	lenObj = argc > 1 ? args[1] : null;
        	argc++;
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 1, 3);
            item = args[0];
        	begObj = argc > 1 ? args[1] : null;
        	lenObj = argc > 2 ? args[2] : null;
        }

        int beg = 0, end = 0, len = 0;
        switch (argc) {
        case 1:
            beg = 0;
            len = realLength;
            break;
        case 2:
            if (begObj instanceof RubyRange) {
                long[] beglen = ((RubyRange) begObj).begLen(realLength, 1);
                beg = (int) beglen[0];
                len = (int) beglen[1];
                break;
            }
            /* fall through */
        case 3:
            beg = begObj.isNil() ? 0 : RubyNumeric.num2int(begObj);
            if (beg < 0) {
                beg = realLength + beg;
                if (beg < 0) beg = 0;
            }
            len = (lenObj == null || lenObj.isNil()) ? realLength - beg : RubyNumeric.num2int(lenObj);
            // TODO: In MRI 1.9, an explicit check for negative length is
            // added here. IndexError is raised when length is negative.
            // See [ruby-core:12953] for more details.
            //
            // New note: This is actually under re-evaluation,
            // see [ruby-core:17483].
            break;
        }

        modify();

        // See [ruby-core:17483]
        if (len < 0) {
            return this;
        }

        if (len > Integer.MAX_VALUE - beg) {
            throw getRuntime().newArgumentError("argument too big");
        }

        end = beg + len;
        if (end > realLength) {
            if (end >= values.length) realloc(end);

            Arrays.fill(values, realLength, end, getRuntime().getNil());
            realLength = end;
        }

        if (block.isGiven()) {
            Ruby runtime = getRuntime();
            for (int i = beg; i < end; i++) {
                IRubyObject v = block.yield(context, runtime.newFixnum(i));
                if (i >= realLength) break;

                values[i] = v;
            }
        } else {
            if(len > 0) Arrays.fill(values, beg, beg + len, item);
        }

        return this;
    }

    /** rb_ary_index
     *
     */
    @JRubyMethod(name = "index", required = 1)
    public IRubyObject index(ThreadContext context, IRubyObject obj) {
        Ruby runtime = getRuntime();
        for (int i = begin; i < begin + realLength; i++) {
            if (equalInternal(context, values[i], obj)) return runtime.newFixnum(i - begin);            
        }

        return runtime.getNil();
    }

    /** rb_ary_rindex
     *
     */
    @JRubyMethod(name = "rindex", required = 1)
    public IRubyObject rindex(ThreadContext context, IRubyObject obj) {
        Ruby runtime = getRuntime();
        int i = realLength;

        while (i-- > 0) {
            if (i > realLength) {
                i = realLength;
                continue;
            }
            if (equalInternal(context, values[begin + i], obj)) return getRuntime().newFixnum(i);
        }

        return runtime.getNil();
    }

    /** rb_ary_indexes
     * 
     */
    @JRubyMethod(name = {"indexes", "indices"}, required = 1, rest = true)
    public IRubyObject indexes(IRubyObject[] args) {
        getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "Array#indexes is deprecated; use Array#values_at", "Array#indexes", "Array#values_at");

        RubyArray ary = new RubyArray(getRuntime(), args.length);

        IRubyObject[] arefArgs = new IRubyObject[1];
        for (int i = 0; i < args.length; i++) {
            arefArgs[0] = args[i];
            ary.append(aref(arefArgs));
        }

        return ary;
    }

    /** rb_ary_reverse_bang
     *
     */
    @JRubyMethod(name = "reverse!")
    public IRubyObject reverse_bang() {
        modify();

        IRubyObject tmp;
        if (realLength > 1) {
            int p1 = 0;
            int p2 = p1 + realLength - 1;

            while (p1 < p2) {
                tmp = values[p1];
                values[p1++] = values[p2];
                values[p2--] = tmp;
            }
        }
        return this;
    }

    /** rb_ary_reverse_m
     *
     */
    @JRubyMethod(name = "reverse")
    public IRubyObject reverse() {
        return aryDup().reverse_bang();
    }

    /** rb_ary_collect
     *
     */
    @JRubyMethod(name = {"collect", "map"}, frame = true)
    public RubyArray collect(ThreadContext context, Block block) {
        Ruby runtime = getRuntime();
        
        if (!block.isGiven()) return new RubyArray(getRuntime(), runtime.getArray(), this);
        
        RubyArray collect = new RubyArray(runtime, realLength);
        
        for (int i = begin; i < begin + realLength; i++) {
            collect.append(block.yield(context, values[i]));
        }
        
        return collect;
    }

    /** rb_ary_collect_bang
     *
     */
    @JRubyMethod(name = {"collect!", "map!"}, frame = true)
    public RubyArray collect_bang(ThreadContext context, Block block) {
        modify();
        for (int i = 0, len = realLength; i < len; i++) {
            store(i, block.yield(context, values[begin + i]));
        }
        return this;
    }

    /** rb_ary_select
     *
     */
    @JRubyMethod(name = "select", frame = true)
    public RubyArray select(ThreadContext context, Block block) {
        Ruby runtime = getRuntime();
        RubyArray result = new RubyArray(runtime, realLength);

        if (isShared) {
            for (int i = begin; i < begin + realLength; i++) {
                if (block.yield(context, values[i]).isTrue()) result.append(elt(i - begin));
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (block.yield(context, values[i]).isTrue()) result.append(elt(i));
            }
        }
        return result;
    }

    /** rb_ary_delete
     *
     */
    @JRubyMethod(name = "delete", required = 1, frame = true)
    public IRubyObject delete(ThreadContext context, IRubyObject item, Block block) {
        int i2 = 0;

        Ruby runtime = getRuntime();
        for (int i1 = 0; i1 < realLength; i1++) {
            IRubyObject e = values[begin + i1];
            if (equalInternal(context, e, item)) continue;
            if (i1 != i2) store(i2, e);
            i2++;
        }

        if (realLength == i2) {
            if (block.isGiven()) return block.yield(context, item);

            return runtime.getNil();
        }

        modify();

        if (realLength > i2) {
            for (int i = i2; i < realLength; i++) {
                values[begin + i] = null;
            }
            realLength = i2;
            if (i2 << 1 < values.length && values.length > ARRAY_DEFAULT_SIZE) realloc(i2 << 1);
        }
        return item;
    }

    /** rb_ary_delete_at
     *
     */
    private final IRubyObject delete_at(int pos) {
        int len = realLength;

        if (pos >= len) return getRuntime().getNil();

        if (pos < 0) pos += len;

        if (pos < 0) return getRuntime().getNil();

        modify();

        IRubyObject obj = values[pos];
        System.arraycopy(values, pos + 1, values, pos, len - (pos + 1));

        realLength--;
        values[realLength] = null;

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
    @JRubyMethod(name = "reject", frame = true)
    public IRubyObject reject(ThreadContext context, Block block) {
        RubyArray ary = aryDup();
        ary.reject_bang(context, block);
        return ary;
    }

    /** rb_ary_reject_bang
     *
     */
    @JRubyMethod(name = "reject!", frame = true)
    public IRubyObject reject_bang(ThreadContext context, Block block) {
        int i2 = 0;
        modify();

        for (int i1 = 0; i1 < realLength; i1++) {
            IRubyObject v = values[i1];
            if (block.yield(context, v).isTrue()) continue;

            if (i1 != i2) store(i2, v);
            i2++;
        }
        if (realLength == i2) return getRuntime().getNil();

        if (i2 < realLength) {
            for (int i = i2; i < realLength; i++) {
                values[i] = null;
            }
            realLength = i2;
        }

        return this;
    }

    /** rb_ary_delete_if
     *
     */
    @JRubyMethod(name = "delete_if", frame = true)
    public IRubyObject delete_if(ThreadContext context, Block block) {
        reject_bang(context, block);
        return this;
    }

    /** rb_ary_zip
     * 
     */
    @JRubyMethod(name = "zip", optional = 1, rest = true, frame = true)
    public IRubyObject zip(ThreadContext context, IRubyObject[] args, Block block) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].convertToArray();
        }

        Ruby runtime = getRuntime();
        if (block.isGiven()) {
            for (int i = 0; i < realLength; i++) {
                RubyArray tmp = new RubyArray(runtime, args.length + 1);
                tmp.append(elt(i));
                for (int j = 0; j < args.length; j++) {
                    tmp.append(((RubyArray) args[j]).elt(i));
                }
                block.yield(context, tmp);
            }
            return runtime.getNil();
        }
        
        int len = realLength;
        RubyArray result = new RubyArray(runtime, len);
        for (int i = 0; i < len; i++) {
            RubyArray tmp = new RubyArray(runtime, args.length + 1);
            tmp.append(elt(i));
            for (int j = 0; j < args.length; j++) {
                tmp.append(((RubyArray) args[j]).elt(i));
            }
            result.append(tmp);
        }
        return result;
    }

    /** rb_ary_cmp
     *
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject obj) {
        RubyArray ary2 = obj.convertToArray();

        int len = realLength;

        if (len > ary2.realLength) len = ary2.realLength;

        Ruby runtime = getRuntime();
        for (int i = 0; i < len; i++) {
            IRubyObject v = elt(i).callMethod(context, MethodIndex.OP_SPACESHIP, "<=>", ary2.elt(i));
            if (!(v instanceof RubyFixnum) || ((RubyFixnum) v).getLongValue() != 0) return v;
        }
        len = realLength - ary2.realLength;

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

    /** rb_ary_slice_bang
     *
     */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0) {
        if (arg0 instanceof RubyRange) {
            long[] beglen = ((RubyRange) arg0).begLen(realLength, 1);
            long pos = beglen[0];
            long len = beglen[1];

            if (pos < 0) pos = realLength + pos;

            arg0 = subseq(pos, len);
            splice(pos, len, null);
            return arg0;
        }
        return delete_at((int) RubyNumeric.num2long(arg0));
    }

    /** rb_ary_slice_bang
    *
    */
    @JRubyMethod(name = "slice!")
    public IRubyObject slice_bang(IRubyObject arg0, IRubyObject arg1) {
        long pos = RubyNumeric.num2long(arg0);
        long len = RubyNumeric.num2long(arg1);

        if (pos < 0) pos = realLength + pos;

        arg1 = subseq(pos, len);
        splice(pos, len, null);

        return arg1;
    }    

    /** rb_ary_assoc
     *
     */
    @JRubyMethod(name = "assoc", required = 1)
    public IRubyObject assoc(ThreadContext context, IRubyObject key) {
        Ruby runtime = getRuntime();

        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject v = values[i];
            if (v instanceof RubyArray) {
                RubyArray arr = (RubyArray)v;
                if (arr.realLength > 0 && equalInternal(context, arr.values[arr.begin], key)) return arr;
            }
        }

        return runtime.getNil();
    }

    /** rb_ary_rassoc
     *
     */
    @JRubyMethod(name = "rassoc", required = 1)
    public IRubyObject rassoc(ThreadContext context, IRubyObject value) {
        Ruby runtime = getRuntime();

        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject v = values[i];
            if (v instanceof RubyArray) {
                RubyArray arr = (RubyArray)v;
                if (arr.realLength > 1 && equalInternal(context, arr.values[arr.begin + 1], value)) return arr;
            }
        }

        return runtime.getNil();
    }

    /** flatten
     * 
     */
    private final int flatten(ThreadContext context, int index, RubyArray ary2, RubyArray memo) {
        int i = index;
        int n;
        int lim = index + ary2.realLength;

        IRubyObject id = ary2.id();

        if (memo.includes(context, id)) throw getRuntime().newArgumentError("tried to flatten recursive array");

        memo.append(id);
        splice(index, 1, ary2);
        while (i < lim) {
            IRubyObject tmp = elt(i).checkArrayType();
            if (!tmp.isNil()) {
                n = flatten(context, i, (RubyArray) tmp, memo);
                i += n;
                lim += n;
            }
            i++;
        }
        memo.pop();
        return lim - index - 1; /* returns number of increased items */
    }

    /** rb_ary_flatten_bang
     *
     */
    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(ThreadContext context) {
        int i = 0;
        RubyArray memo = null;

        while (i < realLength) {
            IRubyObject ary2 = values[begin + i];
            IRubyObject tmp = ary2.checkArrayType();
            if (!tmp.isNil()) {
                if (memo == null) {
                    memo = new RubyArray(getRuntime(), false);
                    memo.values = reserve(ARRAY_DEFAULT_SIZE);
                }

                i += flatten(context, i, (RubyArray) tmp, memo);
            }
            i++;
        }
        if (memo == null) return getRuntime().getNil();

        return this;
    }

    /** rb_ary_flatten
     *
     */
    @JRubyMethod(name = "flatten")
    public IRubyObject flatten(ThreadContext context) {
        RubyArray ary = aryDup();
        ary.flatten_bang(context);
        return ary;
    }

    /** rb_ary_nitems
     *
     */
    @JRubyMethod(name = "nitems")
    public IRubyObject nitems() {
        int n = 0;

        for (int i = begin; i < begin + realLength; i++) {
            if (!values[i].isNil()) n++;
        }
        
        return getRuntime().newFixnum(n);
    }

    /** rb_ary_plus
     *
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject obj) {
        RubyArray y = obj.convertToArray();
        int len = realLength + y.realLength;
        RubyArray z = new RubyArray(getRuntime(), len);
        System.arraycopy(values, begin, z.values, 0, realLength);
        System.arraycopy(y.values, y.begin, z.values, realLength, y.realLength);
        z.realLength = len;
        return z;
    }

    /** rb_ary_times
     *
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_times(ThreadContext context, IRubyObject times) {
        IRubyObject tmp = times.checkStringType();

        if (!tmp.isNil()) return join(context, tmp);

        long len = RubyNumeric.num2long(times);
        if (len == 0) return new RubyArray(getRuntime(), getMetaClass(), 0);
        if (len < 0) throw getRuntime().newArgumentError("negative argument");

        if (Long.MAX_VALUE / len < realLength) {
            throw getRuntime().newArgumentError("argument too big");
        }

        len *= realLength;

        RubyArray ary2 = new RubyArray(getRuntime(), getMetaClass(), len);
        ary2.realLength = (int) len;

        for (int i = 0; i < len; i += realLength) {
            System.arraycopy(values, begin, ary2.values, i, realLength);
        }

        ary2.infectBy(this);

        return ary2;
    }

    /** ary_make_hash
     * 
     */
    private final RubyHash makeHash(RubyArray ary2) {
        RubyHash hash = new RubyHash(getRuntime(), false);
        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            hash.fastASet(values[i], NEVER);
        }

        if (ary2 != null) {
            begin = ary2.begin;            
            for (int i = begin; i < begin + ary2.realLength; i++) {
                hash.fastASet(ary2.values[i], NEVER);
            }
        }
        return hash;
    }

    /** rb_ary_uniq_bang
     *
     */
    @JRubyMethod(name = "uniq!")
    public IRubyObject uniq_bang() {
        RubyHash hash = makeHash(null);

        if (realLength == hash.size()) return getRuntime().getNil();

        int j = 0;
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (hash.fastDelete(v)) store(j++, v);
        }
        realLength = j;
        return this;
    }

    /** rb_ary_uniq
     *
     */
    @JRubyMethod(name = "uniq")
    public IRubyObject uniq() {
        RubyArray ary = aryDup();
        ary.uniq_bang();
        return ary;
    }

    /** rb_ary_diff
     *
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_diff(IRubyObject other) {
        RubyHash hash = other.convertToArray().makeHash(null);
        RubyArray ary3 = new RubyArray(getRuntime());

        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            if (hash.fastARef(values[i]) != null) continue;
            ary3.append(elt(i - begin));
        }

        return ary3;
    }

    /** rb_ary_and
     *
     */
    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and(IRubyObject other) {
        RubyArray ary2 = other.convertToArray();
        RubyHash hash = ary2.makeHash(null);
        RubyArray ary3 = new RubyArray(getRuntime(), 
                realLength < ary2.realLength ? realLength : ary2.realLength);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (hash.fastDelete(v)) ary3.append(v);
        }

        return ary3;
    }

    /** rb_ary_or
     *
     */
    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or(IRubyObject other) {
        RubyArray ary2 = other.convertToArray();
        RubyHash set = makeHash(ary2);

        RubyArray ary3 = new RubyArray(getRuntime(), realLength + ary2.realLength);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (set.fastDelete(v)) ary3.append(v);
        }
        for (int i = 0; i < ary2.realLength; i++) {
            IRubyObject v = ary2.elt(i);
            if (set.fastDelete(v)) ary3.append(v);
        }
        return ary3;
    }

    /** rb_ary_sort
     *
     */
    @JRubyMethod(name = "sort", frame = true)
    public RubyArray sort(Block block) {
        RubyArray ary = aryDup();
        ary.sort_bang(block);
        return ary;
    }

    /** rb_ary_sort_bang
     *
     */
    @JRubyMethod(name = "sort!", frame = true)
    public RubyArray sort_bang(Block block) {
        modify();
        if (realLength > 1) {
            flags |= TMPLOCK_ARR_F;
            try {
                if (block.isGiven()) {
                    Arrays.sort(values, 0, realLength, new BlockComparator(block));
                } else {
                    Arrays.sort(values, 0, realLength, new DefaultComparator());
                }
            } finally {
                flags &= ~TMPLOCK_ARR_F;
            }
        }
        return this;
    }

    final class BlockComparator implements Comparator {
        private Block block;

        public BlockComparator(Block block) {
            this.block = block;
        }

        public int compare(Object o1, Object o2) {
            ThreadContext context = getRuntime().getCurrentContext();
            IRubyObject obj1 = (IRubyObject) o1;
            IRubyObject obj2 = (IRubyObject) o2;
            IRubyObject ret = block.yield(context, getRuntime().newArray(obj1, obj2), null, null, true);
            int n = RubyComparable.cmpint(context, ret, obj1, obj2);
            //TODO: ary_sort_check should be done here
            return n;
        }
    }

    static final class DefaultComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                return compareFixnums(o1, o2);
            }
            if (o1 instanceof RubyString && o2 instanceof RubyString) {
                return ((RubyString) o1).op_cmp((RubyString) o2);
            }
            //TODO: ary_sort_check should be done here
            return compareOthers((IRubyObject)o1, (IRubyObject)o2);
        }

        private int compareFixnums(Object o1, Object o2) {
            long a = ((RubyFixnum) o1).getLongValue();
            long b = ((RubyFixnum) o2).getLongValue();
            if (a > b) {
                return 1;
            }
            if (a < b) {
                return -1;
            }
            return 0;
        }

        private int compareOthers(IRubyObject o1, IRubyObject o2) {
            ThreadContext context = o1.getRuntime().getCurrentContext();
            IRubyObject ret = o1.callMethod(context, MethodIndex.OP_SPACESHIP, "<=>", o2);
            int n = RubyComparable.cmpint(context, ret, o1, o2);
            //TODO: ary_sort_check should be done here
            return n;
        }
    }

    public static void marshalTo(RubyArray array, MarshalStream output) throws IOException {
        output.registerLinkTarget(array);
        output.writeInt(array.getList().size());
        for (Iterator iter = array.getList().iterator(); iter.hasNext();) {
            output.dumpObject((IRubyObject) iter.next());
        }
    }

    public static RubyArray unmarshalFrom(UnmarshalStream input) throws IOException {
        RubyArray result = input.getRuntime().newArray();
        input.registerLinkTarget(result);
        int size = input.unmarshalInt();
        for (int i = 0; i < size; i++) {
            result.append(input.unmarshalObject());
        }
        return result;
    }

    /**
     * @see org.jruby.util.Pack#pack
     */
    @JRubyMethod(name = "pack", required = 1)
    public RubyString pack(ThreadContext context, IRubyObject obj) {
        RubyString iFmt = RubyString.objAsString(context, obj);
        return Pack.pack(getRuntime(), this, iFmt.getByteList());
    }

    @Override
    public Class getJavaClass() {
        return List.class;
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
        Object[] array = new Object[realLength];
        for (int i = begin; i < realLength; i++) {
            array[i - begin] = JavaUtil.convertRubyToJava(values[i]);
        }
        return array;
    }

    public Object[] toArray(final Object[] arg) {
        Object[] array = arg;
        if (array.length < realLength) {
            Class type = array.getClass().getComponentType();
            array = (Object[]) Array.newInstance(type, realLength);
        }
        int length = realLength - begin;

        for (int i = 0; i < length; i++) {
            array[i] = JavaUtil.convertRubyToJava(values[i + begin]);
        }
        return array;
    }

    public boolean add(Object element) {
        append(JavaUtil.convertJavaToRuby(getRuntime(), element));
        return true;
    }

    public boolean remove(Object element) {
        IRubyObject deleted = delete(getRuntime().getCurrentContext(), JavaUtil.convertJavaToRuby(getRuntime(), element), Block.NULL_BLOCK);
        return deleted.isNil() ? false : true; // TODO: is this correct ?
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
            if (remove(iter.next())) {
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
        return JavaUtil.convertRubyToJava((IRubyObject) elt(index), Object.class);
    }

    public Object set(int index, Object element) {
        return store(index, JavaUtil.convertJavaToRuby(getRuntime(), element));
    }

    // TODO: make more efficient by not creating IRubyArray[]
    public void add(int index, Object element) {
        insert(new IRubyObject[]{RubyFixnum.newFixnum(getRuntime(), index), JavaUtil.convertJavaToRuby(getRuntime(), element)});
    }

    public Object remove(int index) {
        return JavaUtil.convertRubyToJava(delete_at(index), Object.class);
    }

    public int indexOf(Object element) {
        int begin = this.begin;

        if (element == null) {
            for (int i = begin; i < begin + realLength; i++) {
                if (values[i] == null) {
                    return i;
                }
            }
        } else {
            IRubyObject convertedElement = JavaUtil.convertJavaToRuby(getRuntime(), element);

            for (int i = begin; i < begin + realLength; i++) {
                if (convertedElement.equals(values[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int lastIndexOf(Object element) {
        int begin = this.begin;

        if (element == null) {
            for (int i = begin + realLength - 1; i >= begin; i--) {
                if (values[i] == null) {
                    return i;
                }
            }
        } else {
            IRubyObject convertedElement = JavaUtil.convertJavaToRuby(getRuntime(), element);

            for (int i = begin + realLength - 1; i >= begin; i--) {
                if (convertedElement.equals(values[i])) {
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
            return JavaUtil.convertRubyToJava(element, Object.class);
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
            return JavaUtil.convertRubyToJava((IRubyObject) elt(last = --index), Object.class);
		}

		public int nextIndex() {
            return index;
		}

		public int previousIndex() {
            return index - 1;
		}

        public void set(Object obj) {
            if (last == -1) throw new IllegalStateException();

            store(last, JavaUtil.convertJavaToRuby(getRuntime(), obj));
        }

        public void add(Object obj) {
            insert(new IRubyObject[] { RubyFixnum.newFixnum(getRuntime(), index++), JavaUtil.convertJavaToRuby(getRuntime(), obj) });
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
        
        IRubyObject subList = subseq(fromIndex, toIndex - fromIndex + 1);

        return subList.isNil() ? null : (List) subList;
    }

    public void clear() {
        rb_clear();
    }
}
