/***** BEGIN LICENSE BLOCK *****
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Pack;

/**
 * The implementation of the built-in class Array in Ruby.
 */
public class RubyArray extends RubyObject implements List {

    public static RubyClass createArrayClass(Ruby runtime) {
        RubyClass arrayc = runtime.defineClass("Array", runtime.getObject(), ARRAY_ALLOCATOR);
        arrayc.index = ClassIndex.ARRAY;
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyArray.class);

        arrayc.includeModule(runtime.getModule("Enumerable"));
        arrayc.getMetaClass().defineMethod("[]", callbackFactory.getOptSingletonMethod("create"));

        arrayc.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        arrayc.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("replace", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s")); 
        arrayc.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        arrayc.defineFastMethod("to_a", callbackFactory.getFastMethod("to_a"));
        arrayc.defineFastMethod("to_ary", callbackFactory.getFastMethod("to_ary"));
        arrayc.defineFastMethod("frozen?", callbackFactory.getFastMethod("frozen"));

        arrayc.defineFastMethod("==", callbackFactory.getFastMethod("op_equal", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("eql?", callbackFactory.getFastMethod("eql", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));

        arrayc.defineFastMethod("[]", callbackFactory.getFastOptMethod("aref"));
        arrayc.defineFastMethod("[]=", callbackFactory.getFastOptMethod("aset"));
        arrayc.defineFastMethod("at", callbackFactory.getFastMethod("at", RubyKernel.IRUBY_OBJECT));
        arrayc.defineMethod("fetch", callbackFactory.getOptMethod("fetch"));
        arrayc.defineFastMethod("first", callbackFactory.getFastOptMethod("first"));
        arrayc.defineFastMethod("last", callbackFactory.getFastOptMethod("last"));
        arrayc.defineFastMethod("concat", callbackFactory.getFastMethod("concat", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("<<", callbackFactory.getFastMethod("append", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("push", callbackFactory.getFastOptMethod("push_m"));
        arrayc.defineFastMethod("pop", callbackFactory.getFastMethod("pop"));
        arrayc.defineFastMethod("shift", callbackFactory.getFastMethod("shift"));
        arrayc.defineFastMethod("unshift", callbackFactory.getFastOptMethod("unshift_m"));
        arrayc.defineFastMethod("insert", callbackFactory.getFastOptMethod("insert"));
        arrayc.defineMethod("each", callbackFactory.getMethod("each"));
        arrayc.defineMethod("each_index", callbackFactory.getMethod("each_index"));
        arrayc.defineMethod("reverse_each", callbackFactory.getMethod("reverse_each"));
        arrayc.defineFastMethod("length", callbackFactory.getFastMethod("length"));
        arrayc.defineAlias("size", "length");
        arrayc.defineFastMethod("empty?", callbackFactory.getFastMethod("empty_p"));
        arrayc.defineFastMethod("index", callbackFactory.getFastMethod("index", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("rindex", callbackFactory.getFastMethod("rindex", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("indexes", callbackFactory.getFastOptMethod("indexes"));
        arrayc.defineFastMethod("indices", callbackFactory.getFastOptMethod("indexes"));
        arrayc.defineFastMethod("join", callbackFactory.getFastOptMethod("join_m"));
        arrayc.defineFastMethod("reverse", callbackFactory.getFastMethod("reverse"));
        arrayc.defineFastMethod("reverse!", callbackFactory.getFastMethod("reverse_bang"));
        arrayc.defineMethod("sort", callbackFactory.getMethod("sort"));
        arrayc.defineMethod("sort!", callbackFactory.getMethod("sort_bang"));
        arrayc.defineMethod("collect", callbackFactory.getMethod("collect"));
        arrayc.defineMethod("collect!", callbackFactory.getMethod("collect_bang"));
        arrayc.defineMethod("map", callbackFactory.getMethod("collect"));
        arrayc.defineMethod("map!", callbackFactory.getMethod("collect_bang"));
        arrayc.defineMethod("select", callbackFactory.getMethod("select"));
        arrayc.defineFastMethod("values_at", callbackFactory.getFastOptMethod("values_at"));
        arrayc.defineMethod("delete", callbackFactory.getMethod("delete", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("delete_at", callbackFactory.getFastMethod("delete_at", RubyKernel.IRUBY_OBJECT));
        arrayc.defineMethod("delete_if", callbackFactory.getMethod("delete_if"));
        arrayc.defineMethod("reject", callbackFactory.getMethod("reject"));
        arrayc.defineMethod("reject!", callbackFactory.getMethod("reject_bang"));
        arrayc.defineMethod("zip", callbackFactory.getOptMethod("zip"));
        arrayc.defineFastMethod("transpose", callbackFactory.getFastMethod("transpose"));
        arrayc.defineFastMethod("replace", callbackFactory.getFastMethod("replace", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("clear", callbackFactory.getFastMethod("rb_clear"));
        arrayc.defineMethod("fill", callbackFactory.getOptMethod("fill"));
        arrayc.defineFastMethod("include?", callbackFactory.getFastMethod("include_p", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("<=>", callbackFactory.getFastMethod("op_cmp", RubyKernel.IRUBY_OBJECT));

        arrayc.defineFastMethod("slice", callbackFactory.getFastOptMethod("aref"));
        arrayc.defineFastMethod("slice!", callbackFactory.getFastOptMethod("slice_bang"));

        arrayc.defineFastMethod("assoc", callbackFactory.getFastMethod("assoc", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("rassoc", callbackFactory.getFastMethod("rassoc", RubyKernel.IRUBY_OBJECT));

        arrayc.defineFastMethod("+", callbackFactory.getFastMethod("op_plus", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("*", callbackFactory.getFastMethod("op_times", RubyKernel.IRUBY_OBJECT));

        arrayc.defineFastMethod("-", callbackFactory.getFastMethod("op_diff", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("&", callbackFactory.getFastMethod("op_and", RubyKernel.IRUBY_OBJECT));
        arrayc.defineFastMethod("|", callbackFactory.getFastMethod("op_or", RubyKernel.IRUBY_OBJECT));

        arrayc.defineFastMethod("uniq", callbackFactory.getFastMethod("uniq"));
        arrayc.defineFastMethod("uniq!", callbackFactory.getFastMethod("uniq_bang"));
        arrayc.defineFastMethod("compact", callbackFactory.getFastMethod("compact"));
        arrayc.defineFastMethod("compact!", callbackFactory.getFastMethod("compact_bang"));

        arrayc.defineFastMethod("flatten", callbackFactory.getFastMethod("flatten"));
        arrayc.defineFastMethod("flatten!", callbackFactory.getFastMethod("flatten_bang"));

        arrayc.defineFastMethod("nitems", callbackFactory.getFastMethod("nitems"));

        arrayc.defineFastMethod("pack", callbackFactory.getFastMethod("pack", RubyKernel.IRUBY_OBJECT));

        return arrayc;
    }

    private static ObjectAllocator ARRAY_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyArray(runtime, klass);
        }
    };
    
    public static final byte OP_PLUS_SWITCHVALUE = 1;
    public static final byte AREF_SWITCHVALUE = 2;
    public static final byte ASET_SWITCHVALUE = 3;
    public static final byte POP_SWITCHVALUE = 4;
    public static final byte PUSH_SWITCHVALUE = 5;
    public static final byte NIL_P_SWITCHVALUE = 6;
    public static final byte EQUALEQUAL_SWITCHVALUE = 7;
    public static final byte UNSHIFT_SWITCHVALUE = 8;
    public static final byte OP_LSHIFT_SWITCHVALUE = 9;
    public static final byte EMPTY_P_SWITCHVALUE = 10;

    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, byte switchvalue,
            String name, IRubyObject[] args, CallType callType, Block block) {
        switch (switchvalue) {
            case OP_PLUS_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
                return op_plus(args[0]);
            case AREF_SWITCHVALUE:
                Arity.optional().checkArity(context.getRuntime(), args);
                return aref(args);
            case ASET_SWITCHVALUE:
                Arity.optional().checkArity(context.getRuntime(), args);
                return aset(args);
            case POP_SWITCHVALUE:
                Arity.noArguments().checkArity(context.getRuntime(), args);
                return pop();
            case PUSH_SWITCHVALUE:
                Arity.optional().checkArity(context.getRuntime(), args);
            return push_m(args);
            case NIL_P_SWITCHVALUE:
                Arity.noArguments().checkArity(context.getRuntime(), args);
                return nil_p();
            case EQUALEQUAL_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
            return op_equal(args[0]);
            case UNSHIFT_SWITCHVALUE:
                Arity.optional().checkArity(context.getRuntime(), args);
            return unshift_m(args);
            case OP_LSHIFT_SWITCHVALUE:
                Arity.singleArgument().checkArity(context.getRuntime(), args);
                return append(args[0]);
            case EMPTY_P_SWITCHVALUE:
                Arity.noArguments().checkArity(context.getRuntime(), args);
                return empty_p();
            case 0:
            default:
                return super.callMethod(context, rubyclass, name, args, callType, block);
        }
    }    

    /** rb_ary_s_create
     * 
     */
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

    /** rb_assoc_new
     *
     */
    public static RubyArray newArray(Ruby runtime, IRubyObject car, IRubyObject cdr) {
        return new RubyArray(runtime, new IRubyObject[] { car, cdr });
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

    public static RubyArray newArray(Ruby runtime, List list) {
        RubyArray arr = new RubyArray(runtime, list.size());
        list.toArray(arr.values);
        arr.realLength = arr.values.length;
        return arr;
    }

    public static final int ARRAY_DEFAULT_SIZE = 16;    

    private IRubyObject[] values;
    private boolean tmpLock = false;
    private boolean shared = false;

    private int begin = 0;
    private int realLength = 0;

    /* 
     * plain internal array assignment
     */
    public RubyArray(Ruby runtime, IRubyObject[]vals){
        super(runtime, runtime.getArray());
        values = vals;
        realLength = vals.length;
    }
    
    /* rb_ary_new2
     * just allocates the internal array
     */
    private RubyArray(Ruby runtime, long length) {
        super(runtime, runtime.getArray());
        checkLength(length);
        alloc((int) length);
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

    /* rb_ary_new3, rb_ary_new4, with begin
     * allocates the internal array of size length and copies the 'length' elements from 'vals' starting from 'beg'
     */
    private RubyArray(Ruby runtime, int beg, long length, IRubyObject[] vals) {
        super(runtime, runtime.getArray());
        checkLength(length);
        int ilength = (int) length;
        alloc(ilength);
        if (ilength > 0 && vals.length > 0) System.arraycopy(vals, beg, values, 0, ilength);

        realLength = ilength;
    }

    /*
     * just allocates the internal array, with optional objectspace
     */
    private RubyArray(Ruby runtime, long length, boolean objectSpace) {
        super(runtime, runtime.getArray(), objectSpace);
        checkLength(length);
        alloc((int) length);
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

    public int getNativeTypeIndex() {
        return ClassIndex.ARRAY;
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
        return !shared ? values : toJavaArray();
    }    

    /** rb_ary_make_shared
     *
     */
    private final RubyArray makeShared(int beg, int len){
        RubyArray sharedArray = new RubyArray(getRuntime(), true);
        shared = true;
        sharedArray.values = values;
        sharedArray.shared = true;
        sharedArray.begin = beg;
        sharedArray.realLength = len;
        return sharedArray;        
    }

    /** rb_ary_modify_check
     *
     */
    private final void modifyCheck() {
        testFrozen("array");

        if (tmpLock) {
            throw getRuntime().newTypeError("can't modify array during iteration");
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
        if (shared) {
            IRubyObject[] vals = reserve(realLength);
            shared = false;
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
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        int argc = checkArgumentCount(args, 0, 2);
        Ruby runtime = getRuntime();

        if (argc == 0) {
            realLength = 0;
            if (block.isGiven()) runtime.getWarnings().warn("given block not used");

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
                runtime.getWarnings().warn("block supersedes default value argument");
            }

            ThreadContext context = runtime.getCurrentContext();
            for (int i = 0; i < ilen; i++) {
                store(i, context.yield(new RubyFixnum(runtime, i), block));
                realLength = i + 1;
            }
        } else {
            Arrays.fill(values, 0, ilen, (argc == 2) ? args[1] : runtime.getNil());
            realLength = ilen;
        }
    	return this;
    }

    /** rb_ary_replace
     *
     */
    public IRubyObject replace(IRubyObject orig) {
        modifyCheck();

        RubyArray origArr = orig.convertToArray();

        if (this == orig) return this;

        origArr.shared = true;
        values = origArr.values;
        realLength = origArr.realLength;
        begin = origArr.begin;
        shared = true;

        return this;
    }

    /** rb_ary_to_s
     *
     */
    public IRubyObject to_s() {
        if (realLength == 0) return getRuntime().newString("");

        return join(getRuntime().getGlobalVariables().get("$,"));
    }

    public boolean includes(IRubyObject item) {
        ThreadContext context = getRuntime().getCurrentContext();
        int begin = this.begin;
        
        for (int i = begin; i < begin + realLength; i++) {
            if (item.callMethod(context, "==", values[i]).isTrue()) return true;
    	}
        
        return false;
    }

    /** rb_ary_hash
     * 
     */
    public RubyFixnum hash() {
        int h = realLength;

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            h = (h << 1) | (h < 0 ? 1 : 0);
            h ^= RubyNumeric.num2long(values[i].callMethod(context, "hash"));
        }

        return runtime.newFixnum(h);
    }

    /** rb_ary_store
     *
     */
    private final IRubyObject store(long index, IRubyObject value) {
        if (index < 0) {
            index += realLength;
            if (index < 0) {
                throw getRuntime().newIndexError("index " + (index - realLength) + " out of array");
            }
        }

        modify();

        if (index >= values.length) {
            long newLength = values.length / 2;

            if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

            newLength += index;
            if (newLength >= Integer.MAX_VALUE) {
                throw getRuntime().newArgumentError("index too big");
            }
            realloc((int) newLength);
        }
        if (index > realLength) {
            Arrays.fill(values, realLength, (int) index + 1, getRuntime().getNil());
        }
        
        if (index >= realLength) realLength = (int) index + 1;

        values[(int) index] = value;
        return value;
    }

    /** rb_ary_elt - faster
     *
     */
    private final IRubyObject elt(long offset) {
        if (realLength == 0 || offset < 0 || offset >= realLength) return getRuntime().getNil();

        return values[begin + (int) offset];
    }

    /** rb_ary_elt - faster
     *
     */
    private final IRubyObject elt(int offset) {
        if (realLength == 0 || offset < 0 || offset >= realLength) return getRuntime().getNil();

        return values[begin + offset];
    }

    /** rb_ary_elt - faster
     *
     */
    private final IRubyObject elt_f(long offset) {
        if (realLength == 0 || offset >= realLength) return getRuntime().getNil();

        return values[begin + (int) offset];
    }

    /** rb_ary_elt - faster
     *
     */
    private final IRubyObject elt_f(int offset) {
        if (realLength == 0 || offset >= realLength) return getRuntime().getNil();

        return values[begin + offset];
    }

    /** rb_ary_entry
     *
     */
    public final IRubyObject entry(long offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt_f(offset);
    }


    /** rb_ary_entry
     *
     */
    public final IRubyObject entry(int offset) {
        return (offset < 0 ) ? elt(offset + realLength) : elt_f(offset);
    }

    /** rb_ary_fetch
     *
     */
    public IRubyObject fetch(IRubyObject[] args, Block block) {
        checkArgumentCount(args, 1, 2);
        IRubyObject pos = args[0];

        if (block.isGiven() && args.length == 2) {
            getRuntime().getWarnings().warn("block supersedes default value argument");
        }

        long index = RubyNumeric.num2long(pos);

        if (index < 0) index += realLength;

        if (index < 0 || index >= realLength) {
            if (block.isGiven()) return getRuntime().getCurrentContext().yield(pos, block);

            if (args.length == 1) {
                throw getRuntime().newIndexError("index " + index + " out of array");
            }
            
            return args[1];
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
        long rlen;

        if (len < 0) throw getRuntime().newIndexError("negative length (" + len + ")");

        if (beg < 0) {
            beg += realLength;
            if (beg < 0) {
                beg -= realLength;
                throw getRuntime().newIndexError("index " + beg + " out of array");
            }
        }
        
        if (beg + len > realLength) len = realLength - beg;

        RubyArray rplArr;
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
                int tryNewLength = values.length + values.length / 2;
                
                realloc(len > tryNewLength ? (int)len : tryNewLength);
            }
            
            Arrays.fill(values, realLength, (int) beg, getRuntime().getNil());
            if (rlen > 0) {
                System.arraycopy(rplArr.values, rplArr.begin, values, (int) beg, (int) rlen);
            }
            realLength = (int) len;
        } else {
            long alen;

            if (beg + len > realLength) len = realLength - beg;

            alen = realLength + rlen - len;
            if (alen >= values.length) {
                int tryNewLength = values.length + values.length / 2;
                realloc(alen > tryNewLength ? (int)alen : tryNewLength);
            }
            
            if (len != rlen) {
                System.arraycopy(values, (int) (beg + len), values, (int) (beg + rlen), realLength - (int) (beg + len));
                realLength = (int) alen;
            }
            
            if (rlen > 0) {
                System.arraycopy(rplArr.values, rplArr.begin, values, (int) beg, (int) rlen);
            }
        }
    }

    /** rb_ary_insert
     * 
     */
    public IRubyObject insert(IRubyObject[] args) {
        if (args.length == 1) return this;

        if (args.length < 1) {
            throw getRuntime().newArgumentError("wrong number of arguments (at least 1)");
        }

        long pos = RubyNumeric.num2long(args[0]);

        if (pos == -1) pos = realLength;
        if (pos < 0) pos++;

        splice(pos, 0, new RubyArray(getRuntime(), 1, args.length - 1, args)); // rb_ary_new4
        
        return this;
    }

    public final IRubyObject dup() {
        return aryDup();
    }

    /** rb_ary_dup
     * 
     */
    private final RubyArray aryDup() {
        RubyArray dup = new RubyArray(getRuntime(), realLength);
        dup.setTaint(isTaint()); // from DUP_SETUP
        // rb_copy_generic_ivar from DUP_SETUP here ...unlikely..
        System.arraycopy(values, begin, dup.values, 0, realLength);
        dup.realLength = realLength;
        return dup;
    }

    /** rb_ary_transpose
     * 
     */
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
                result.append(entry(RubyNumeric.fix2long(args[i])));
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
        
        // MRI does klass = rb_obj_class(ary); here, what for ?
        if (len == 0) return new RubyArray(getRuntime(), 0);

        return makeShared(begin + (int) beg, (int) len);
    }

    /** rb_ary_length
     *
     */
    public RubyFixnum length() {
        return getRuntime().newFixnum(realLength);
    }

    /** rb_ary_push - specialized rb_ary_store 
     *
     */
    public final RubyArray append(IRubyObject item) {
        modify();
        
        if (realLength == Integer.MAX_VALUE) throw getRuntime().newArgumentError("index too big");

        if (realLength == values.length){
            long newLength = values.length + values.length / 2;
            if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

            realloc((int) newLength);
        }
        values[realLength++] = item;
        return this;
    }

    /** rb_ary_push_m
     *
     */
    public RubyArray push_m(IRubyObject[] items) {
        for (int i = 0; i < items.length; i++) {
            append(items[i]);
        }
        
        return this;
    }

    /** rb_ary_pop
     *
     */
    public IRubyObject pop() {
        modifyCheck();
        
        if (realLength == 0) return getRuntime().getNil();

        if (!shared) {
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
    public IRubyObject shift() {
        modifyCheck();

        if (realLength == 0) return getRuntime().getNil();

        IRubyObject obj = values[begin];

        if (!shared) shared = true;

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
            long newLength = values.length / 2;
            if (newLength < ARRAY_DEFAULT_SIZE) newLength = ARRAY_DEFAULT_SIZE;

            newLength += values.length;
            realloc((int) newLength);
        }
        System.arraycopy(values, 0, values, 1, realLength);

        realLength++;
        values[0] = item;

        return this;
    }

    /** rb_ary_unshift_m
     *
     */
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
    public RubyBoolean include_p(IRubyObject item) {
        return getRuntime().newBoolean(includes(item));
    }

    /** rb_ary_frozen_p
     *
     */
    public RubyBoolean frozen() {
        return getRuntime().newBoolean(isFrozen() || tmpLock);
    }

    /** rb_ary_aref
     */
    public IRubyObject aref(IRubyObject[] args) {
        long beg, len;
        if (args.length == 2) {
            if (args[0] instanceof RubySymbol) {
                throw getRuntime().newTypeError("Symbol as array index");
            }
            beg = RubyNumeric.num2long(args[0]);
            len = RubyNumeric.num2long(args[1]);

            if (beg < 0) beg += realLength;

            return subseq(beg, len);
        }

        if (args.length != 1) checkArgumentCount(args, 1, 1);

        IRubyObject arg = args[0];

        if (arg instanceof RubyFixnum) return entry(RubyNumeric.fix2long(arg));
        if (arg instanceof RubySymbol) throw getRuntime().newTypeError("Symbol as array index");

        long[] beglen;
        if (!(arg instanceof RubyRange)) {
        } else if ((beglen = ((RubyRange) arg).begLen(realLength, 0)) == null) {
            return getRuntime().getNil();
        } else {
            beg = beglen[0];
            len = beglen[1];
            return subseq(beg, len);
        }

        return entry(RubyNumeric.num2long(arg));
    }

    /** rb_ary_aset
     *
     */
    public IRubyObject aset(IRubyObject[] args) {
        if (args.length == 3) {
            if (args[0] instanceof RubySymbol) {
                throw getRuntime().newTypeError("Symbol as array index");
            }
            if (args[1] instanceof RubySymbol) {
                throw getRuntime().newTypeError("Symbol as subarray length");
            }
            splice(RubyNumeric.num2long(args[0]), RubyNumeric.num2long(args[1]), args[2]);
            return args[2];
        }

        if (args.length != 2) {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + 
                    " for 2)");
        }
        
        if (args[0] instanceof RubyFixnum) {
            store(RubyNumeric.fix2long(args[0]), args[1]);
            return args[1];
        }
        
        if (args[0] instanceof RubySymbol) {
            throw getRuntime().newTypeError("Symbol as array index");
        }

        if (args[0] instanceof RubyRange) {
            long[] beglen = ((RubyRange) args[0]).begLen(realLength, 1);
            splice(beglen[0], beglen[1], args[1]);
            return args[1];
        }
        
        store(RubyNumeric.num2long(args[0]), args[1]);
        return args[1];
    }

    /** rb_ary_at
     *
     */
    public IRubyObject at(IRubyObject pos) {
        return entry(RubyNumeric.num2long(pos));
    }

	/** rb_ary_concat
     *
     */
    public RubyArray concat(IRubyObject obj) {
        RubyArray ary = obj.convertToArray();
        
        if (ary.realLength > 0) splice(realLength, 0, ary);

        return this;
    }

    /** rb_ary_inspect
     *
     */
    public IRubyObject inspect() {
        if (realLength == 0) return getRuntime().newString("[]");

        if (!getRuntime().registerInspecting(this)) return getRuntime().newString("[...]");

        RubyString s;
        try {
            StringBuffer buffer = new StringBuffer("[");
            Ruby runtime = getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            boolean tainted = isTaint();
            for (int i = 0; i < realLength; i++) {
                s = RubyString.objAsString(values[begin + i].callMethod(context, "inspect"));
                
                if (s.isTaint()) tainted = true;

                if (i > 0) buffer.append(", ");

                buffer.append(s.toString());
            }
            buffer.append("]");
            if (tainted) setTaint(true);

            return runtime.newString(buffer.toString());
        } finally {
            getRuntime().unregisterInspecting(this);
        }
    }

    /** rb_ary_first
     *
     */
    public IRubyObject first(IRubyObject[] args) {
    	if (args.length == 0) {
            if (realLength == 0) return getRuntime().getNil();

            return values[begin];
        } 
            
        checkArgumentCount(args, 0, 1);
        int n = (int)RubyNumeric.num2long(args[0]);
        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw getRuntime().newArgumentError("negative array size (or size too big)");
    	}
    	
        return makeShared(begin, n);
    }

    /** rb_ary_last
     *
     */
    public IRubyObject last(IRubyObject[] args) {
        if (args.length == 0) {
            if (realLength == 0) return getRuntime().getNil();

            return values[begin + realLength - 1];
        } 
            
        checkArgumentCount(args, 0, 1);

        int n = (int)RubyNumeric.num2long(args[0]);
        if (n > realLength) {
            n = realLength;
        } else if (n < 0) {
            throw getRuntime().newArgumentError("negative array size (or size too big)");
        }

        return makeShared(begin + realLength - n, n);
    }

    /** rb_ary_each
     *
     */
    public IRubyObject each(Block block) {
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = begin; i < begin + realLength; i++) {
            context.yield(values[i], block);
        }
        return this;
    }

    /** rb_ary_each_index
     *
     */
    public IRubyObject each_index(Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (int i = 0; i < realLength; i++) {
            context.yield(runtime.newFixnum(i), block);
        }
        return this;
    }

    /** rb_ary_reverse_each
     *
     */
    public IRubyObject reverse_each(Block block) {
        ThreadContext context = getRuntime().getCurrentContext();
        
        int len = realLength;
        
        while(len-- > 0) {
            context.yield(values[begin + len], block);
            
            if (realLength < len) len = realLength;
        }
        
        return this;
    }

    private final IRubyObject inspectJoin(IRubyObject sep) {
        IRubyObject result = join(sep);
        getRuntime().unregisterInspecting(this);
        return result;
    }

    /** rb_ary_join
     *
     */
    public RubyString join(IRubyObject sep) {
        if (realLength == 0) return getRuntime().newString("");

        boolean taint = isTaint() || sep.isTaint();

        long len = 1;
        for (int i = begin; i < begin + realLength; i++) {            
            IRubyObject tmp = values[i].checkStringType();
            len += tmp.isNil() ? 10 : ((RubyString) tmp).getByteList().length();
        }

        if (!sep.isNil()) len += sep.convertToString().getByteList().length() * (realLength - 1);

        StringBuffer buf = new StringBuffer((int) len);
        Ruby runtime = getRuntime();
        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject tmp = values[i];
            if (tmp instanceof RubyString) {
                // do nothing
            } else if (tmp instanceof RubyArray) {
                if (!runtime.registerInspecting(tmp)) {
                    tmp = runtime.newString("[...]");
                } else {
                    tmp = ((RubyArray) tmp).inspectJoin(sep);
                }
            } else {
                tmp = RubyString.objAsString(tmp);
            }

            if (i > begin && !sep.isNil()) buf.append(sep.toString());

            buf.append(((RubyString) tmp).toString());
            taint |= tmp.isTaint();
        }

        RubyString result = RubyString.newString(runtime, buf.toString());
        
        if (taint) result.setTaint(taint);

        return result;
    }

    /** rb_ary_join_m
     *
     */
    public RubyString join_m(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 0, 1);
        IRubyObject sep = (argc == 1) ? args[0] : getRuntime().getGlobalVariables().get("$,");
        
        return join(sep);
    }

    /** rb_ary_to_a
     *
     */
    public RubyArray to_a() {
        return this;
    }

    public IRubyObject to_ary() {
    	return this;
    }

    public RubyArray convertToArray() {
        return this;
    }

    /** rb_ary_equal
     *
     */
    public IRubyObject op_equal(IRubyObject obj) {
        if (this == obj) return getRuntime().getTrue();

        if (!(obj instanceof RubyArray)) {
            if (!obj.respondsTo("to_ary")) {
                return getRuntime().getFalse();
            } else {
                return obj.callMethod(getRuntime().getCurrentContext(), "==", this);
            }
        }

        RubyArray ary = (RubyArray) obj;
        if (realLength != ary.realLength) return getRuntime().getFalse();

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (long i = 0; i < realLength; i++) {
            if (!elt(i).callMethod(context, "==", ary.elt(i)).isTrue()) return runtime.getFalse();
        }
        return runtime.getTrue();
    }

    /** rb_ary_eql
     *
     */
    public RubyBoolean eql(IRubyObject obj) {
        if (this == obj) return getRuntime().getTrue();
        if (!(obj instanceof RubyArray)) return getRuntime().getFalse();

        RubyArray ary = (RubyArray) obj;

        if (realLength != ary.realLength) return getRuntime().getFalse();

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (long i = 0; i < realLength; i++) {
            if (!elt(i).callMethod(context, "eql?", ary.elt(i)).isTrue()) return runtime.getFalse();
        }
        return runtime.getTrue();
    }

    /** rb_ary_compact_bang
     *
     */
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
    public IRubyObject compact() {
        RubyArray ary = aryDup();
        ary.compact_bang();
        return ary;
    }

    /** rb_ary_empty_p
     *
     */
    public IRubyObject empty_p() {
        return realLength == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** rb_ary_clear
     *
     */
    public IRubyObject rb_clear() {
        modifyCheck();

        if (ARRAY_DEFAULT_SIZE * 2 < values.length || shared) alloc(ARRAY_DEFAULT_SIZE * 2);
        
        begin = 0;
        shared = false;
        
        realLength = 0;
        return this;
    }

    /** rb_ary_fill
     *
     */
    public IRubyObject fill(IRubyObject[] args, Block block) {
        IRubyObject item = null;
        IRubyObject begObj = null;
        IRubyObject lenObj = null;
        int argc = args.length;

        if (block.isGiven()) {
            checkArgumentCount(args, 0, 2);
            item = null;
        	begObj = argc > 0 ? args[0] : null;
        	lenObj = argc > 1 ? args[1] : null;
        	argc++;
        } else {
            checkArgumentCount(args, 1, 3);
            item = args[0];
        	begObj = argc > 1 ? args[1] : null;
        	lenObj = argc > 2 ? args[2] : null;
        }

        long beg = 0, end = 0, len = 0;
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
            beg = begObj.isNil() ? 0 : RubyNumeric.num2long(begObj);
            if (beg < 0) {
                beg = realLength + beg;
                if (beg < 0) beg = 0;
            }
            len = (lenObj == null || lenObj.isNil()) ? realLength - beg : RubyNumeric.num2long(lenObj);
            break;
        }

        modify();

        end = beg + len;
        if (end > realLength) {
            if (end >= values.length) realloc((int) end);

            Arrays.fill(values, realLength, (int) end, getRuntime().getNil());
            realLength = (int) end;
        }

        if (block.isGiven()) {
            Ruby runtime = getRuntime();
            ThreadContext context = runtime.getCurrentContext();
            for (int i = (int) beg; i < (int) end; i++) {
                IRubyObject v = context.yield(runtime.newFixnum(i), block);
                if (i >= realLength) break;

                values[i] = v;
            }
        } else {
            Arrays.fill(values, (int) beg, (int) (beg + len), item);
        }
        
        return this;
    }

    /** rb_ary_index
     *
     */
    public IRubyObject index(IRubyObject obj) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (int i = begin; i < begin + realLength; i++) {
            if (values[i].callMethod(context, "==", obj).isTrue()) return runtime.newFixnum(i - begin);
        }

        return runtime.getNil();
    }

    /** rb_ary_rindex
     *
     */
    public IRubyObject rindex(IRubyObject obj) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        int i = realLength;

        while (i-- > 0) {
            if (i > realLength) {
                i = realLength;
                continue;
            }
            if (values[begin + i].callMethod(context, "==", obj).isTrue()) {
                return getRuntime().newFixnum(i);
            }
        }

        return runtime.getNil();
    }

    /** rb_ary_indexes
     * 
     */
    public IRubyObject indexes(IRubyObject[] args) {
        getRuntime().getWarnings().warn("Array#indexes is deprecated; use Array#values_at");

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
    public IRubyObject reverse_bang() {
        modify();

        int p1, p2;
        IRubyObject tmp;
        if (realLength > 1) {
            p1 = 0;
            p2 = p1 + realLength - 1;

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
    public IRubyObject reverse() {
        return aryDup().reverse_bang();
    }

    /** rb_ary_collect
     *
     */
    public RubyArray collect(Block block) {
        if (!block.isGiven()) return new RubyArray(getRuntime(), realLength, values);

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyArray collect = new RubyArray(runtime, realLength);
        
        for (int i = begin; i < begin + realLength; i++) {
            collect.append(context.yield(values[i], block));
        }
        
        return collect;
    }

    /** rb_ary_collect_bang
     *
     */
    public RubyArray collect_bang(Block block) {
        modify();
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0, len = realLength; i < len; i++) {
            store(i, context.yield(values[begin + i], block));
        }
        return this;
    }

    /** rb_ary_select
     *
     */
    public RubyArray select(Block block) {
        Ruby runtime = getRuntime();
        RubyArray result = new RubyArray(runtime, realLength);

        ThreadContext context = runtime.getCurrentContext();
        if (shared) {
            for (int i = begin; i < begin + realLength; i++) {
                if (context.yield(values[i], block).isTrue()) result.append(elt(i - begin));
            }
        } else {
            for (int i = 0; i < realLength; i++) {
                if (context.yield(values[i], block).isTrue()) result.append(elt(i));
            }
        }
        return result;
    }

    /** rb_ary_delete
     *
     */
    public IRubyObject delete(IRubyObject item, Block block) {
        int i2 = 0;

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (int i1 = 0; i1 < realLength; i1++) {
            IRubyObject e = values[begin + i1];
            if (e.callMethod(context, "==", item).isTrue()) continue;
            if (i1 != i2) store(i2, e);
            i2++;
        }
        
        if (realLength == i2) {
            if (block.isGiven()) return context.yield(item, block);

            return runtime.getNil();
        }

        modify();

        if (realLength > i2) {
            realLength = i2;
            if (i2 * 2 < values.length && values.length > ARRAY_DEFAULT_SIZE) realloc(i2 * 2);
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

        return obj;
    }

    /** rb_ary_delete_at_m
     * 
     */
    public IRubyObject delete_at(IRubyObject obj) {
        return delete_at((int) RubyNumeric.num2long(obj));
    }

    /** rb_ary_reject_bang
     * 
     */
    public IRubyObject reject(Block block) {
        RubyArray ary = aryDup();
        ary.reject_bang(block);
        return ary;
    }

    /** rb_ary_reject_bang
     *
     */
    public IRubyObject reject_bang(Block block) {
        int i2 = 0;
        modify();

        ThreadContext context = getRuntime().getCurrentContext();
        for (int i1 = 0; i1 < realLength; i1++) {
            IRubyObject v = values[i1];
            if (context.yield(v, block).isTrue()) continue;

            if (i1 != i2) store(i2, v);
            i2++;
        }
        if (realLength == i2) return getRuntime().getNil();

        if (i2 < realLength) realLength = i2;

        return this;
    }

    /** rb_ary_delete_if
     *
     */
    public IRubyObject delete_if(Block block) {
        reject_bang(block);
        return this;
    }

    /** rb_ary_zip
     * 
     */
    public IRubyObject zip(IRubyObject[] args, Block block) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].convertToArray();
        }

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        if (block.isGiven()) {
            for (int i = 0; i < realLength; i++) {
                RubyArray tmp = new RubyArray(runtime, args.length + 1);
                tmp.append(elt(i));
                for (int j = 0; j < args.length; j++) {
                    tmp.append(((RubyArray) args[j]).elt(i));
                }
                context.yield(tmp, block);
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
    public IRubyObject op_cmp(IRubyObject obj) {
        RubyArray ary2 = obj.convertToArray();

        int len = realLength;

        if (len > ary2.realLength) len = ary2.realLength;

        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (int i = 0; i < len; i++) {
            IRubyObject v = elt(i).callMethod(context, "<=>", ary2.elt(i));
            if (!(v instanceof RubyFixnum) || ((RubyFixnum) v).getLongValue() != 0) return v;
        }
        len = realLength - ary2.realLength;

        if (len == 0) return RubyFixnum.zero(runtime);
        if (len > 0) return RubyFixnum.one(runtime);

        return RubyFixnum.minus_one(runtime);
    }

    /** rb_ary_slice_bang
     *
     */
    public IRubyObject slice_bang(IRubyObject[] args) {
        if (checkArgumentCount(args, 1, 2) == 2) {
            int pos = (int) RubyNumeric.num2long(args[0]);
            int len = (int) RubyNumeric.num2long(args[1]);
            
            if (pos < 0) pos = realLength + pos;

            args[1] = subseq(pos, len);
            splice(pos, len, null);
            
            return args[1];
        }
        
        IRubyObject arg = args[0];
        if (arg instanceof RubyRange) {
            long[] beglen = ((RubyRange) arg).begLen(realLength, 1);
            int pos = (int) beglen[0];
            int len = (int) beglen[1];

            if (pos < 0) {
                pos = realLength + pos;
            }
            arg = subseq(pos, len);
            splice(pos, len, null);
            return arg;
        }

        return delete_at((int) RubyNumeric.num2long(args[0]));
    }

    /** rb_ary_assoc
     *
     */
    public IRubyObject assoc(IRubyObject key) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject v = values[i];
            if (v instanceof RubyArray && ((RubyArray) v).realLength > 0
                    && ((RubyArray) v).values[0].callMethod(context, "==", key).isTrue()) {
                return v;
            }
        }
        return runtime.getNil();
    }

    /** rb_ary_rassoc
     *
     */
    public IRubyObject rassoc(IRubyObject value) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        for (int i = begin; i < begin + realLength; i++) {
            IRubyObject v = values[i];
            if (v instanceof RubyArray && ((RubyArray) v).realLength > 1
                    && ((RubyArray) v).values[1].callMethod(context, "==", value).isTrue()) {
                return v;
            }
        }

        return runtime.getNil();
    }

    /** flatten
     * 
     */
    private final long flatten(long index, RubyArray ary2, RubyArray memo) {
        long i = index;
        long n;
        long lim = index + ary2.realLength;

        IRubyObject id = ary2.id();

        if (memo.includes(id)) throw getRuntime().newArgumentError("tried to flatten recursive array");

        memo.append(id);
        splice(index, 1, ary2);
        while (i < lim) {
            IRubyObject tmp = elt(i).checkArrayType();
            if (!tmp.isNil()) {
                n = flatten(i, (RubyArray) tmp, memo);
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
    public IRubyObject flatten_bang() {
        int i = 0;
        boolean mod = false;
        IRubyObject memo = getRuntime().getNil();

        while (i < realLength) {
            IRubyObject ary2 = values[begin + i];
            IRubyObject tmp = ary2.checkArrayType();
            if (!tmp.isNil()) {
                if (memo.isNil()) memo = new RubyArray(getRuntime());

                i += flatten(i, (RubyArray) tmp, (RubyArray) memo);
                mod = true;
            }
            i++;
        }
        if (!mod) return getRuntime().getNil();

        return this;
    }

    /** rb_ary_flatten
     *
     */
    public IRubyObject flatten() {
        RubyArray ary = aryDup();
        ary.flatten_bang();
        return ary;
    }

    /** rb_ary_nitems
     *
     */
    public IRubyObject nitems() {
        int n = 0;

        for (int i = begin; i < begin + realLength; i++) {
            if (!values[i].isNil()) n++;
        }
        
        return getRuntime().newFixnum(n++);
    }

    /** rb_ary_plus
     *
     */
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
    public IRubyObject op_times(IRubyObject times) {
        IRubyObject tmp = times.checkStringType();

        if (!tmp.isNil()) return join(tmp);

        long len = RubyNumeric.num2long(times);
        if (len == 0) return new RubyArray(getRuntime(), 0);
        if (len < 0) throw getRuntime().newArgumentError("negative argument");

        if (Long.MAX_VALUE / len < realLength) {
            throw getRuntime().newArgumentError("argument too big");
        }

        len *= realLength;

        RubyArray ary2 = new RubyArray(getRuntime(), len);
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
    private final Set makeSet(RubyArray ary2) {
        final Set set = new HashSet();
        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            set.add(values[i]);
        }

        if (ary2 != null) {
            begin = ary2.begin;            
            for (int i = begin; i < begin + ary2.realLength; i++) {
                set.add(ary2.values[i]);
            }
        }
        return set;
    }

    /** rb_ary_uniq_bang
     *
     */
    public IRubyObject uniq_bang() {
        Set set = makeSet(null);

        if (realLength == set.size()) return getRuntime().getNil();

        int j = 0;
        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (set.remove(v)) store(j++, v);
        }
        realLength = j;
        return this;
    }

    /** rb_ary_uniq
     *
     */
    public IRubyObject uniq() {
        RubyArray ary = aryDup();
        ary.uniq_bang();
        return ary;
    }

    /** rb_ary_diff
     *
     */
    public IRubyObject op_diff(IRubyObject other) {
        Set set = other.convertToArray().makeSet(null);
        RubyArray ary3 = new RubyArray(getRuntime());

        int begin = this.begin;
        for (int i = begin; i < begin + realLength; i++) {
            if (set.contains(values[i])) continue;

            ary3.append(elt(i - begin));
        }

        return ary3;
    }

    /** rb_ary_and
     *
     */
    public IRubyObject op_and(IRubyObject other) {
        RubyArray ary2 = other.convertToArray();
        Set set = ary2.makeSet(null);
        RubyArray ary3 = new RubyArray(getRuntime(), 
                realLength < ary2.realLength ? realLength : ary2.realLength);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (set.remove(v)) ary3.append(v);
        }

        return ary3;
    }

    /** rb_ary_or
     *
     */
    public IRubyObject op_or(IRubyObject other) {
        RubyArray ary2 = other.convertToArray();
        Set set = makeSet(ary2);

        RubyArray ary3 = new RubyArray(getRuntime(), realLength + ary2.realLength);

        for (int i = 0; i < realLength; i++) {
            IRubyObject v = elt(i);
            if (set.remove(v)) ary3.append(v);
        }
        for (int i = 0; i < ary2.realLength; i++) {
            IRubyObject v = ary2.elt(i);
            if (set.remove(v)) ary3.append(v);
        }
        return ary3;
    }

    /** rb_ary_sort
     *
     */
    public RubyArray sort(Block block) {
        RubyArray ary = aryDup();
        ary.sort_bang(block);
        return ary;
    }

    /** rb_ary_sort_bang
     *
     */
    public RubyArray sort_bang(Block block) {
        modify();
        if (realLength > 1) {
            tmpLock = true;
            try {
                if (block.isGiven()) {
                    Arrays.sort(values, 0, realLength, new BlockComparator(block));
                } else {
                    Arrays.sort(values, 0, realLength, new DefaultComparator());
                }
            } finally {
                tmpLock = false;
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
            int n = RubyComparable.cmpint(ret, obj1, obj2);
            //TODO: ary_sort_check should be done here
            return n;
        }
    }

    final class DefaultComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                long a = ((RubyFixnum) o1).getLongValue();
                long b = ((RubyFixnum) o2).getLongValue();
                if (a > b) return 1;
                if (a < b) return -1;
                return 0;
            }
            if (o1 instanceof RubyString && o2 instanceof RubyString) {
                return ((RubyString) o1).cmp((RubyString) o2);
            }

            IRubyObject obj1 = (IRubyObject) o1;
            IRubyObject obj2 = (IRubyObject) o2;

            IRubyObject ret = obj1.callMethod(obj1.getRuntime().getCurrentContext(), "<=>", obj2);
            int n = RubyComparable.cmpint(ret, obj1, obj2);
            //TODO: ary_sort_check should be done here
            return n;
        }
    }

    public static void marshalTo(RubyArray array, MarshalStream output) throws IOException {
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
    public RubyString pack(IRubyObject obj) {
        RubyString iFmt = RubyString.objAsString(obj);
        return Pack.pack(getRuntime(), Arrays.asList(toJavaArray()), iFmt.getBytes());
    }

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
        IRubyObject deleted = delete(JavaUtil.convertJavaToRuby(getRuntime(), element), Block.NULL_BLOCK);
        return deleted.isNil() ? false : true; // TODO: is this correct ?
	}

	public boolean containsAll(Collection c) {
		for (Iterator iter = c.iterator(); iter.hasNext();) {
			if (indexOf(iter.next()) == -1) return false;
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
        insert(new IRubyObject[] { RubyFixnum.newFixnum(getRuntime(), index), JavaUtil.convertJavaToRuby(getRuntime(), element) });
	}

	public Object remove(int index) {
        return JavaUtil.convertRubyToJava(delete_at(index), Object.class);
	}

	public int indexOf(Object element) {
        int begin = this.begin;
        
        if (element == null) {
            for (int i = begin; i < begin + realLength; i++) {
                if (values[i] == null) return i;
            }
        } else {
            IRubyObject convertedElement = JavaUtil.convertJavaToRuby(getRuntime(), element);
            
            for (int i = begin; i < begin + realLength; i++) {
                if (convertedElement.equals(values[i])) return i;
            }
        }
        return -1;
    }

	public int lastIndexOf(Object element) {
        int begin = this.begin;

        if (element == null) {
            for (int i = begin + realLength - 1; i >= begin; i--) {
                if (values[i] == null) return i;
            }
        } else {
            IRubyObject convertedElement = JavaUtil.convertJavaToRuby(getRuntime(), element);
            
            for (int i = begin + realLength - 1; i >= begin; i--) {
                if (convertedElement.equals(values[i])) return i;
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
