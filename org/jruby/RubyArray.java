/*
 * RubyArray.java - The Array class.
 * Created on 04. Juli 2001, 22:53
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jruby;

import java.util.*;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.marshal.*;
import org.jruby.util.Pack;
import org.jruby.util.Asserts;

/**
 *
 * @author  jpetersen
 */
public class RubyArray extends RubyObject implements IndexCallable {
	private ArrayList list;
	private boolean tmpLock;

	public RubyArray(final Ruby ruby, final ArrayList list) {
		super(ruby, ruby.getClasses().getArrayClass());

		this.list = list;
	}

	public static RubyArray nilArray(Ruby ruby) {
		return new RubyArray(ruby, null) {
			public boolean isNil() {
				return true;
			}
		};
	}

	/** Getter for property list.
	 * @return Value of property list.
	 */
	public ArrayList getList() {
		return list;
	}

	public RubyObject[] toJavaArray() {
		return (RubyObject[]) list.toArray(new RubyObject[getLength()]);
	}

	/** Getter for property tmpLock.
	 * @return Value of property tmpLock.
	 */
	public boolean isTmpLock() {
		return tmpLock;
	}

	/** Setter for property tmpLock.
	 * @param tmpLock New value of property tmpLock.
	 */
	public void setTmpLock(boolean tmpLock) {
		this.tmpLock = tmpLock;
	}

	public int getLength() {
		return list.size();
	}

    public boolean includes(RubyObject item) {
        return include_p(item).isTrue();
    }

    private static final int M_INSPECT = 2;
    private static final int M_TO_S = 3;
    private static final int M_FROZEN = 4;
    private static final int M_EQUAL = 5;
    private static final int M_AREF = 101;
    private static final int M_ASET = 102;
    private static final int M_EQL = 6;
    private static final int M_FIRST = 10;
    private static final int M_LAST = 11;
    private static final int M_CONCAT = 12;
    private static final int M_APPEND = 201;
    private static final int M_PUSH = 202;
    private static final int M_POP = 14;
    private static final int M_SHIFT = 15;
    private static final int M_UNSHIFT = 16;
    private static final int M_EACH = 20;
    private static final int M_EACH_INDEX = 21;
    private static final int M_REVERSE_EACH = 22;
    private static final int M_LENGTH = 23;
    private static final int M_EMPTY_P = 24;
    private static final int M_INDEX = 25;
    private static final int M_RINDEX = 26;
    private static final int M_CLONE = 260;
    private static final int M_JOIN = 261;
    private static final int M_INDICES = 27;
    private static final int M_REVERSE = 30;
    private static final int M_REVERSE_BANG = 31;
    private static final int M_SORT = 32;
    private static final int M_SORT_BANG = 33;
    private static final int M_COLLECT = 34;
    private static final int M_COLLECT_BANG = 35;
    private static final int M_DELETE = 40;
    private static final int M_DELETE_AT = 41;
    private static final int M_DELETE_IF = 42;
    private static final int M_REJECT_BANG = 43;
    private static final int M_REPLACE = 50;
    private static final int M_CLEAR = 51;
    private static final int M_INCLUDE_P = 53;

	public static RubyClass createArrayClass(Ruby ruby) {
		RubyClass arrayClass = ruby.defineClass("Array", ruby.getClasses().getObjectClass());

		arrayClass.includeModule(ruby.getRubyModule("Enumerable"));

		arrayClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyArray.class, "newInstance"));
		arrayClass.defineSingletonMethod("[]", CallbackFactory.getOptSingletonMethod(RubyArray.class, "create"));
		arrayClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyArray.class, "initialize"));

		arrayClass.defineMethod("inspect", IndexedCallback.create(M_INSPECT, 0));
		arrayClass.defineMethod("to_s", IndexedCallback.create(M_TO_S, 0));
		arrayClass.defineMethod("to_a", CallbackFactory.getSelfMethod(0));
		arrayClass.defineMethod("to_ary", CallbackFactory.getSelfMethod(0));
		arrayClass.defineMethod("frozen?", IndexedCallback.create(M_FROZEN, 0));
		arrayClass.defineMethod("==", IndexedCallback.create(M_EQUAL, 1));
		arrayClass.defineMethod("eql?", IndexedCallback.create(M_EQL, 1));
		arrayClass.defineMethod("===", IndexedCallback.create(M_EQUAL, 1));
		arrayClass.defineMethod("[]", IndexedCallback.createOptional(M_AREF));
		arrayClass.defineMethod("[]=", IndexedCallback.createOptional(M_ASET));
		arrayClass.defineMethod("at", CallbackFactory.getMethod(RubyArray.class, "at", RubyFixnum.class));
		arrayClass.defineMethod("first", IndexedCallback.create(M_FIRST, 0));
		arrayClass.defineMethod("last", IndexedCallback.create(M_LAST, 0));
		arrayClass.defineMethod("concat", IndexedCallback.create(M_CONCAT, 1));
		arrayClass.defineMethod("<<", IndexedCallback.create(M_APPEND, 1));
		arrayClass.defineMethod("push", IndexedCallback.createOptional(M_PUSH, 1));
		arrayClass.defineMethod("pop", IndexedCallback.create(M_POP, 0));
		arrayClass.defineMethod("shift", IndexedCallback.create(M_SHIFT, 0));
		arrayClass.defineMethod("unshift", IndexedCallback.createOptional(M_UNSHIFT));
		arrayClass.defineMethod("each", IndexedCallback.create(M_EACH, 0));
		arrayClass.defineMethod("each_index", IndexedCallback.create(M_EACH_INDEX, 0));
		arrayClass.defineMethod("reverse_each", IndexedCallback.create(M_REVERSE_EACH, 0));
		arrayClass.defineMethod("length", IndexedCallback.create(M_LENGTH, 0));
		arrayClass.defineMethod("size", IndexedCallback.create(M_LENGTH, 0));
		arrayClass.defineMethod("empty?", IndexedCallback.create(M_EMPTY_P, 0));
		arrayClass.defineMethod("index", IndexedCallback.create(M_INDEX, 1));
		arrayClass.defineMethod("rindex", IndexedCallback.create(M_RINDEX, 1));
		arrayClass.defineMethod("indexes", IndexedCallback.createOptional(M_INDICES));
		arrayClass.defineMethod("indices", IndexedCallback.createOptional(M_INDICES));
		arrayClass.defineMethod("clone", IndexedCallback.create(M_CLONE, 0));
		arrayClass.defineMethod("join", IndexedCallback.createOptional(M_JOIN));
		arrayClass.defineMethod("reverse", IndexedCallback.create(M_REVERSE, 0));
		arrayClass.defineMethod("reverse!", IndexedCallback.create(M_REVERSE_BANG, 0));
		arrayClass.defineMethod("sort", IndexedCallback.create(M_SORT, 0));
		arrayClass.defineMethod("sort!", IndexedCallback.create(M_SORT_BANG, 0));
		arrayClass.defineMethod("collect", IndexedCallback.create(M_COLLECT, 0));
		arrayClass.defineMethod("collect!", IndexedCallback.create(M_COLLECT_BANG, 0));
		arrayClass.defineMethod("map!", IndexedCallback.create(M_COLLECT_BANG, 0));
		arrayClass.defineMethod("filter", IndexedCallback.create(M_COLLECT_BANG, 0));
		arrayClass.defineMethod("delete", IndexedCallback.create(M_DELETE, 1));
		arrayClass.defineMethod("delete_at", IndexedCallback.create(M_DELETE_AT, 1));
		arrayClass.defineMethod("delete_if", IndexedCallback.create(M_DELETE_IF, 0));
		arrayClass.defineMethod("reject!", IndexedCallback.create(M_REJECT_BANG, 0));
		arrayClass.defineMethod("replace", IndexedCallback.create(M_REPLACE, 1));
		arrayClass.defineMethod("clear", IndexedCallback.create(M_CLEAR, 0));
		arrayClass.defineMethod("fill", CallbackFactory.getOptMethod(RubyArray.class, "fill"));
		arrayClass.defineMethod("include?", IndexedCallback.create(M_INCLUDE_P, 1));
		arrayClass.defineMethod("<=>", CallbackFactory.getMethod(RubyArray.class, "op_cmp", RubyObject.class));

		arrayClass.defineMethod("slice", CallbackFactory.getOptMethod(RubyArray.class, "aref"));
		arrayClass.defineMethod("slice!", CallbackFactory.getOptMethod(RubyArray.class, "slice_bang"));

		arrayClass.defineMethod("assoc", CallbackFactory.getMethod(RubyArray.class, "assoc", RubyObject.class));
		arrayClass.defineMethod("rassoc", CallbackFactory.getMethod(RubyArray.class, "rassoc", RubyObject.class));

		arrayClass.defineMethod("+", CallbackFactory.getMethod(RubyArray.class, "op_plus", RubyObject.class));
		arrayClass.defineMethod("*", CallbackFactory.getMethod(RubyArray.class, "op_times", RubyObject.class));

		arrayClass.defineMethod("-", CallbackFactory.getMethod(RubyArray.class, "op_diff", RubyObject.class));
		arrayClass.defineMethod("&", CallbackFactory.getMethod(RubyArray.class, "op_and", RubyObject.class));
		arrayClass.defineMethod("|", CallbackFactory.getMethod(RubyArray.class, "op_or", RubyObject.class));

		arrayClass.defineMethod("uniq", CallbackFactory.getMethod(RubyArray.class, "uniq"));
		arrayClass.defineMethod("uniq!", CallbackFactory.getMethod(RubyArray.class, "uniq_bang"));
		arrayClass.defineMethod("compact", CallbackFactory.getMethod(RubyArray.class, "compact"));
		arrayClass.defineMethod("compact!", CallbackFactory.getMethod(RubyArray.class, "compact_bang"));
		arrayClass.defineMethod("flatten", CallbackFactory.getMethod(RubyArray.class, "flatten"));
		arrayClass.defineMethod("flatten!", CallbackFactory.getMethod(RubyArray.class, "flatten_bang"));
		arrayClass.defineMethod("nitems", CallbackFactory.getMethod(RubyArray.class, "nitems"));
		arrayClass.defineMethod("pack", CallbackFactory.getMethod(RubyArray.class, "pack", RubyString.class));

		return arrayClass;
	}

    public RubyObject callIndexed(int index, RubyObject[] args) {
        switch (index) {
        case M_INSPECT:
            return inspect();
        case M_TO_S:
            return to_s();
        case M_FROZEN:
            return frozen();
        case M_EQUAL:
            return equal(args[0]);
        case M_EQL:
            return eql(args[0]);
        case M_AREF:
            return aref(args);
        case M_ASET:
            return aset(args);
        case M_FIRST:
            return first();
        case M_LAST:
            return last();
        case M_CONCAT:
            return concat(args[0]);
        case M_APPEND:
            return append(args[0]);
        case M_PUSH:
            return push(args);
        case M_POP:
            return pop();
        case M_SHIFT:
            return shift();
        case M_UNSHIFT:
            return unshift(args);
        case M_EACH:
            return each();
        case M_EACH_INDEX:
            return each_index();
        case M_REVERSE_EACH:
            return reverse_each();
        case M_LENGTH:
            return length();
        case M_EMPTY_P:
            return empty_p();
        case M_INDEX:
            return index(args[0]);
        case M_RINDEX:
            return rindex(args[0]);
        case M_INDICES:
            return indices(args);
        case M_CLONE:
            return rbClone();
        case M_JOIN:
            return join(args);
        case M_REVERSE:
            return reverse();
        case M_REVERSE_BANG:
            return reverse_bang();
        case M_SORT:
            return sort();
        case M_SORT_BANG:
            return sort_bang();
        case M_COLLECT:
            return collect();
        case M_COLLECT_BANG:
            return collect_bang();
        case M_DELETE:
            return delete(args[0]);
        case M_DELETE_AT:
            return delete_at(args[0]);
        case M_DELETE_IF:
            return delete_if();
        case M_REJECT_BANG:
            return reject_bang();
        case M_REPLACE:
            return replace(args[0]);
        case M_CLEAR:
            return clear();
        case M_INCLUDE_P:
            return include_p(args[0]);
        }
        Asserts.assertNotReached();
        return null;
    }

    public int hashCode() {
        return list.hashCode();
    }

	/** rb_ary_modify
	 *
	 */
	public void modify() {
		if (isFrozen()) {
			throw new RubyFrozenException(getRuby(), "Array");
		}
		if (isTmpLock()) {
			throw new TypeError(getRuby(), "can't modify array during sort");
		}
		if (isTaint() && getRuby().getSafeLevel() >= 4) {
			throw new RubySecurityException(getRuby(), "Insecure: can't modify array");
		}
	}

	/* if list's size is not at least 'toLength', add nil's until it is */
	private void autoExpand(long toLength) {
		list.ensureCapacity((int) toLength);
		for (int i = getLength(); i < toLength; i++) {
			list.add(getRuby().getNil());
		}
	}

	/** rb_ary_store
	 *
	 */
	public void store(long idx, RubyObject value) {
		modify();
		if (idx < 0) {
			idx += getLength();
			if (idx < 0) {
				throw new RubyIndexException(getRuby(), "index " + (idx - getLength()) + " out of array");
			}
		}
		autoExpand(idx + 1);
		list.set((int) idx, value);
	}

	/** rb_ary_entry
	 *
	 */
	public RubyObject entry(long offset) {
		if (getLength() == 0) {
			return getRuby().getNil();
		}

		if (offset < 0) {
			offset += getLength();
		}

		if (offset < 0 || getLength() <= offset) {
			return getRuby().getNil();
		}

		return (RubyObject) list.get((int) offset);
	}

	/** rb_ary_unshift
	 *
	 */
	public RubyArray unshift(RubyObject item) {
		modify();
		list.add(0, item);

		return this;
	}

	/** rb_ary_subseq
	 *
	 */
	public final RubyObject subseq(final long beg, long len) {
		final int length = getLength();

		if (beg > length || beg < 0 || len < 0) {
			return getRuby().getNil();
		}

		if (beg + len > length) {
			len = length - beg;
		}
		if (len <= 0) {
			return newArray(getRuby(), 0);
		}

		RubyArray ary2 = newArray(getRuby(), new ArrayList(list.subList((int) beg, (int) (len + beg))));

		return ary2;
	}

	/** rb_ary_replace
	 *	@todo change the algorythm to make it efficient
	 *			there should be no need to do any deletion or addition
	 *			when the replacing object is an array of the same length
	 *			and in any case we should minimize them, they are costly
	 */
	public void replace(long beg, long len, RubyObject repl) {
		int length = getLength();

		if (len < 0) {
			throw new RubyIndexException(getRuby(), "Negative array length: " + len);
		}
		if (beg < 0) {
			beg += length;
		}
		if (beg < 0) {
			throw new RubyIndexException(getRuby(), "Index out of bounds: " + beg);
		}

		modify();

		for (int i = 0; beg < getLength() && i < len; i++) {
			list.remove((int) beg);
		}
		autoExpand(beg);
		if (repl instanceof RubyArray) {
			List repList = ((RubyArray) repl).getList();
			list.ensureCapacity(getLength() + repList.size());
			list.addAll((int) beg, new ArrayList(repList));
		} else if (!repl.isNil()) {
			list.add((int) beg, repl);
		}
	}

	/** to_ary
	 *
	 */
	public static RubyArray arrayValue(RubyObject other) {
		if (other instanceof RubyArray) {
			return (RubyArray) other;
		} else {
			try {
				return (RubyArray) other.convertType(RubyArray.class, "Array", "to_ary");
			} catch (Exception ex) {
				throw new ArgumentError(other.getRuby(), "can't convert arg to Array: " + ex.getMessage());
			}
		}
	}

	private boolean flatten(ArrayList ary) {
		boolean mod = false;
		for (int i = ary.size() - 1; i >= 0; i--) {
			if (ary.get(i) instanceof RubyArray) {
				ArrayList ary2 = ((RubyArray) ary.remove(i)).getList();
				flatten(ary2);
				ary.addAll(i, ary2);
				mod = true;
			}
		}
		return mod;
	}

	//
	// Methods of the Array Class (rb_ary_*):
	//

	/** rb_ary_new2
	 *
	 */
	public final static RubyArray newArray(final Ruby ruby, final long len) {
		return new RubyArray(ruby, new ArrayList((int) len));
	}

	/** rb_ary_new
	 *
	 */
	public final static RubyArray newArray(final Ruby ruby) {
		/* Ruby arrays default to holding 16 elements, so we create an
		 * ArrayList of the same size if we're not told otherwise
		 */
		return new RubyArray(ruby, new ArrayList(16));
	}

	/**
	 *
	 */
	public final static RubyArray newArray(final Ruby ruby, final RubyObject obj) {
        final ArrayList list = new ArrayList(1);
        list.add(obj);
		return new RubyArray(ruby, list);
	}

	/** rb_assoc_new
	 *
	 */
	public final static RubyArray newArray(final Ruby ruby, final RubyObject car, final RubyObject cdr) {
		final ArrayList list = new ArrayList(2);
        list.add(car);
        list.add(cdr);
        return new RubyArray(ruby, list);
	}

    public final static RubyArray newArray(final Ruby ruby, final ArrayList list) {
        return new RubyArray(ruby, list);
    }

	public final static RubyArray newArray(final Ruby ruby, final RubyObject[] args) {
        final int size = args.length;
        final ArrayList list = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            list.add(args[i]);
        }
		return new RubyArray(ruby, list);
	}

	/** rb_ary_s_new
	 *
	 */
	public static RubyArray newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
		RubyArray array = newArray(ruby);
		array.setRubyClass((RubyClass) recv);

		array.callInit(args);

		return array;
	}

	/** rb_ary_s_create
	 *
	 */
	public final static RubyArray create(final Ruby ruby, final RubyObject recv, final RubyObject[] args) {
		final RubyArray array = newArray(ruby, args);
		array.setRubyClass((RubyClass) recv);

		return array;
	}

	/** rb_ary_length
	 *
	 */
	public RubyFixnum length() {
		return RubyFixnum.newFixnum(getRuby(), getLength());
	}

	/** rb_ary_push_m
	 *
	 */
    public RubyArray push(RubyObject[] items) {
		modify();
		boolean tainted = false;
		for (int i = 0; i < items.length; i++) {
			tainted |= items[i].isTaint();
			list.add(items[i]);
		}
		setTaint(isTaint() || tainted);
		return this;
	}

    public RubyArray append(RubyObject value) {
		modify();
		list.add(value);
		infectObject(value);
		return this;
	}

	/** rb_ary_pop
	 *
	 */
	public RubyObject pop() {
		modify();
		if (getLength() == 0) {
			return getRuby().getNil();
		}
		return (RubyObject) list.remove(getLength() - 1);
	}

	/** rb_ary_shift
	 *
	 */
	public RubyObject shift() {
		modify();
		if (getLength() == 0) {
			return getRuby().getNil();
		}

		return (RubyObject) list.remove(0);
	}

	/** rb_ary_unshift_m
	 *
	 */
	public RubyArray unshift(RubyObject[] items) {
		if (items.length == 0) {
			throw new ArgumentError(getRuby(), "wrong # of arguments(at least 1)");
		}
		modify();
		boolean taint = false;
		for (int i = 0; i < items.length; i++) {
			taint |= items[i].isTaint();
			list.add(i, items[i]);
		}
		setTaint(isTaint() || taint);
		return this;
	}

	public RubyBoolean include_p(RubyObject item) {
		for (int i = 0, n = getLength(); i < n; i++) {
			if (item.funcall("==", entry(i)).isTrue()) {
				return getRuby().getTrue();
			}
		}
		return getRuby().getFalse();
	}

	/** rb_ary_frozen_p
	 *
	 */
	public RubyBoolean frozen() {
		return RubyBoolean.newBoolean(getRuby(), isFrozen() || isTmpLock());
	}

	/** rb_ary_initialize
	 *
	 */
	public RubyObject initialize(RubyObject[] args) {
		int argc = argCount(args, 0, 2);
		long len = 0;
		if (argc != 0)
			len = RubyNumeric.fix2long(args[0]);

		modify();

		if (len < 0) {
			throw new ArgumentError(getRuby(), "negative array size");
		}
		if (len > Integer.MAX_VALUE) {
			throw new ArgumentError(getRuby(), "array size too big");
		}
		list = new ArrayList((int) len);
		if (len > 0) {
			RubyObject obj = (argc == 2) ? args[1] : (RubyObject) getRuby().getNil();
			Collections.fill(list, obj);
		}
		return this;
	}

	/** rb_ary_aref
	 *
	 */
	public RubyObject aref(RubyObject[] args) {
		int argc = argCount(args, 1, 2);
		if (argc == 2) {
			long beg = RubyNumeric.fix2long(args[0]);
			long len = RubyNumeric.fix2long(args[1]);
			if (beg < 0) {
				beg += getLength();
			}
			return subseq(beg, len);
		}
		if (args[0] instanceof RubyFixnum) {
			return entry(RubyNumeric.fix2long(args[0]));
		}
		if (args[0] instanceof RubyBignum) {
			throw new RubyIndexException(getRuby(), "index too big");
		}
		if (args[0] instanceof RubyRange) {
			long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), true, false);
			if (begLen == null) {
				return getRuby().getNil();
			}
			return subseq(begLen[0], begLen[1]);
		}
		return entry(RubyNumeric.num2long(args[0]));
	}

	/** rb_ary_aset
	 *
	 */
	public RubyObject aset(RubyObject[] args) {
		int argc = argCount(args, 2, 3);
		if (argc == 3) {
			long beg = RubyNumeric.fix2long(args[0]);
			long len = RubyNumeric.fix2long(args[1]);
			replace(beg, len, args[2]);
			return args[2];
		}
		if (args[0] instanceof RubyFixnum) {
			store(RubyNumeric.fix2long(args[0]), args[1]);
			return args[1];
		}
		if (args[0] instanceof RubyRange) {
			long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), false, true);
			replace(begLen[0], begLen[1], args[1]);
			return args[1];
		}
		if (args[0] instanceof RubyBignum) {
			throw new RubyIndexException(getRuby(), "Index too large");
		}
		store(RubyNumeric.num2long(args[0]), args[1]);
		return args[1];
	}

	/** rb_ary_at
	 *
	 */
	public RubyObject at(RubyFixnum pos) {
		return entry(pos.getLongValue());
	}

	/** rb_ary_concat
	 *
	 */
	public RubyArray concat(RubyObject obj) {
		modify();
		RubyArray other = arrayValue(obj);
		list.addAll(other.getList());
		infectObject(other);
		return this;
	}

	/** rb_ary_inspect
	 *
	 */
	public RubyString inspect() {
		// Performance
		int length = getLength();

		// HACK +++
		if (length == 0) {
			return RubyString.newString(getRuby(), "[]");
		}
		RubyString result = RubyString.newString(getRuby(), "[");
		RubyString sep = RubyString.newString(getRuby(), ", ");
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				result.append(sep);
			}
			result.append(entry(i).funcall("inspect"));
		}
		result.cat("]");
		return result;
		// HACK ---
	}

	/** rb_ary_first
	 *
	 */
	public RubyObject first() {
		if (getLength() == 0) {
			return getRuby().getNil();
		}
		return entry(0);
	}

	/** rb_ary_last
	 *
	 */
	public RubyObject last() {
		if (getLength() == 0) {
			return getRuby().getNil();
		}
		return entry(getLength() - 1);
	}

	/** rb_ary_each
	 *
	 */
	public RubyObject each() {
		for (int i = 0, len = getLength(); i < len; i++) {
			getRuby().yield(entry(i));
		}
		return this;
	}

	/** rb_ary_each_index
	 *
	 */
	public RubyObject each_index() {
		for (int i = 0, len = getLength(); i < len; i++) {
			getRuby().yield(RubyFixnum.newFixnum(getRuby(), i));
		}
		return this;
	}

	/** rb_ary_reverse_each
	 *
	 */
	public RubyObject reverse_each() {
		for (long i = getLength(); i > 0; i--) {
			getRuby().yield(entry(i - 1));
		}
		return this;
	}

	/** rb_ary_join
	 *
	 */
	RubyString join(RubyString sep) {
		int length = getLength();
		if (length == 0) {
			RubyString.newString(getRuby(), "");
		}
		boolean taint = isTaint() || sep.isTaint();
		RubyString str;
		RubyObject tmp = entry(0);
		taint |= tmp.isTaint();
		if (tmp instanceof RubyString) {
			str = (RubyString) tmp.dup();
		} else if (tmp instanceof RubyArray) {
			str = (RubyString) ((RubyArray) tmp).join(sep);
		} else {
			str = RubyString.objAsString(getRuby(), tmp);
		}
		for (long i = 1; i < length; i++) {
			tmp = entry(i);
			taint |= tmp.isTaint();
			if (tmp instanceof RubyArray) {
				tmp = ((RubyArray) tmp).join(sep);
			} else if (!(tmp instanceof RubyString)) {
				tmp = RubyString.objAsString(getRuby(), tmp);
			}
			str.append(sep.op_plus(tmp));
		}
		str.setTaint(taint);
		return str;
	}

	/** rb_ary_join_m
	 *
	 */
	public RubyString join(RubyObject[] args) {
		int argc = argCount(args, 0, 1);
		RubyObject sep = (argc == 1) ? args[0] : getRuby().getGlobalVar("$,");
		return join(sep.isNil() ? RubyString.newString(getRuby(), "") : RubyString.stringValue(sep));
	}

	/** rb_ary_to_s
	 *
	 */
	public RubyString to_s() {
		RubyObject sep = getRuby().getGlobalVar("$,");
		return join(sep.isNil() ? RubyString.newString(getRuby(), "") : RubyString.stringValue(sep));
	}

	/** rb_ary_to_a
	 *
	 */
	public RubyArray to_a() {
		return this;
	}

	/** rb_ary_equal
	 *
	 */
	public RubyBoolean equal(RubyObject obj) {
		if (this == obj) {
			return getRuby().getTrue();
		}

		if (!(obj instanceof RubyArray)) {
			return getRuby().getFalse();
		}
		int length = getLength();

		RubyArray ary = (RubyArray) obj;
		if (length != ary.getLength()) {
			return getRuby().getFalse();
		}

		for (long i = 0; i < length; i++) {
			if (entry(i).funcall("==", ary.entry(i)).isFalse()) {
				return getRuby().getFalse();
			}
		}
		return getRuby().getTrue();
	}

	/** rb_ary_eql
	 *
	 */
	public RubyBoolean eql(RubyObject obj) {
		if (!(obj instanceof RubyArray)) {
			return getRuby().getFalse();
		}
		int length = getLength();

		RubyArray ary = (RubyArray) obj;
		if (length != ary.getLength()) {
			return getRuby().getFalse();
		}

		for (long i = 0; i < length; i++) {
			if (entry(i).funcall("eql?", ary.entry(i)).isFalse()) {
				return getRuby().getFalse();
			}
		}
		return getRuby().getTrue();
	}

	/** rb_ary_compact_bang
	 *
	 */
	public RubyObject compact_bang() {
		modify();
		boolean changed = false;

		for (int i = getLength() - 1; i >= 0; i--) {
			if (entry(i).isNil()) {
				list.remove(i);
				changed = true;
			}
		}
		return changed ? (RubyObject) this : (RubyObject) getRuby().getNil();
	}

	/** rb_ary_compact
	 *
	 */
	public RubyObject compact() {
		RubyArray ary = (RubyArray) dup();
		ary.compact_bang();		//Benoit: do not return directly the value of compact_bang, it may be nil
		return ary;
	}

	/** rb_ary_empty_p
	 *
	 */
	public RubyObject empty_p() {
		return getLength() == 0 ? getRuby().getTrue() : getRuby().getFalse();
	}

	/** rb_ary_clear
	 *
	 */
	public RubyObject clear() {
		modify();
		list.clear();
		return this;
	}

	/** rb_ary_fill
	 *
	 */
	public RubyObject fill(RubyObject[] args) {
		int argc = argCount(args, 1, 3);
		int beg = 0;
		int len = getLength();
		switch (argc) {
			case 1 :
				break;
			case 2 :
				if (args[1] instanceof RubyRange) {
					long[] begLen = ((RubyRange) args[1]).getBeginLength(len, false, true);
					beg = (int) begLen[0];
					len = (int) begLen[1];
					break;
				}
				/* fall through */
			default :
				beg = args[1].isNil() ? beg : RubyNumeric.fix2int(args[1]);
				if (beg < 0 && (beg += len) < 0) {
					throw new RubyIndexException(getRuby(), "Negative array index");
				}
				len -= beg;
				if (argc == 3 && !args[2].isNil()) {
					len = RubyNumeric.fix2int(args[2]);
				}
		}

		modify();
		autoExpand(beg + len);
		for (int i = beg; i < (beg + len); i++) {
			list.set(i, args[0]);
		}
		return this;
	}

	/** rb_ary_index
	 *
	 */
	public RubyObject index(RubyObject obj) {
		for (int i = 0, len = getLength(); i < len; i++) {
			if (obj.funcall("==", entry(i)).isTrue()) {
				return RubyFixnum.newFixnum(getRuby(), i);
			}
		}
		return getRuby().getNil();
	}

	/** rb_ary_rindex
	 *
	 */
	public RubyObject rindex(RubyObject obj) {
		for (int i = getLength() - 1; i >= 0; i--) {
			if (obj.funcall("==", entry(i)).isTrue()) {
				return RubyFixnum.newFixnum(getRuby(), i);
			}
		}
		return getRuby().getNil();
	}

	public RubyArray indices(RubyObject[] args) {
		RubyObject[] result = new RubyObject[args.length];
		boolean taint = false;
		for (int i = 0; i < args.length; i++) {
			result[i] = entry(RubyNumeric.fix2int(args[i]));
			taint |= result[i].isTaint();
		}
		RubyArray ary = create(getRuby(), getRubyClass(), result);
		ary.setTaint(taint);
		return ary;
	}

	/** rb_ary_clone
	 *
	 */
	public RubyObject rbClone() {
		RubyArray ary = newArray(getRuby(), list);
		ary.setupClone(this);
		return ary;
	}

	/** rb_ary_reverse_bang
	 *
	 */
	public RubyArray reverse_bang() {
        if (list.size() <= 1) {
            return nilArray(ruby);
        }
        modify();
        Collections.reverse(list);
        return this;
	}

	/** rb_ary_reverse_m
	 *
	 */
	public RubyObject reverse() {
		RubyArray ary = (RubyArray) dup();
		ary.reverse_bang();
		return ary;
	}

	/** rb_ary_collect
	 *
	 */
	public RubyArray collect() {
		if (!getRuby().isBlockGiven()) {
			return (RubyArray) dup();
		}
		ArrayList ary = new ArrayList();
		for (int i = 0, len = getLength(); i < len; i++) {
			ary.add(getRuby().yield(entry(i)));
		}
		return new RubyArray(getRuby(), ary);
	}

	/** rb_ary_collect_bang
	 *
	 */
	public RubyArray collect_bang() {
		modify();
		for (int i = 0, len = getLength(); i < len; i++) {
			list.set(i, getRuby().yield(entry(i)));
		}
		return this;
	}

	/** rb_ary_delete
	 *
	 */
	public RubyObject delete(RubyObject obj) {
		modify();
		RubyObject retVal = getRuby().getNil();
		for (int i = getLength() - 1; i >= 0; i--) {
			if (obj.funcall("==", entry(i)).isTrue()) {
				retVal = (RubyObject) list.remove(i);
			}
		}
		if (retVal.isNil() && getRuby().isBlockGiven()) {
			retVal = getRuby().yield(entry(0));
		}
		return retVal;
	}

	/** rb_ary_delete_at
	 *
	 */
	public RubyObject delete_at(RubyObject obj) {
		modify();
		int pos = (int) RubyNumeric.num2long(obj);
		int len = getLength();
		if (pos >= len) {
			return getRuby().getNil();
		}
		if (pos < 0 && (pos += len) < 0) {
			return getRuby().getNil();
		}
		return (RubyObject) list.remove(pos);
	}

	/** rb_ary_reject_bang
	 *
	 */
	public RubyObject reject_bang() {
		modify();
		RubyObject retVal = getRuby().getNil();
		for (int i = getLength() - 1; i >= 0; i--) {
			if (getRuby().yield(entry(i)).isTrue()) {
				retVal = (RubyObject) list.remove(i);
			}
		}
		return retVal.isNil() ? (RubyObject) retVal : (RubyObject) this;
	}

	/** rb_ary_delete_if
	 *
	 */
	public RubyObject delete_if() {
		reject_bang();
		return this;
	}

	/** rb_ary_replace
	 *
	 */
	public RubyObject replace(RubyObject other) {
		replace(0, getLength(), arrayValue(other));
		return this;
	}

	/** rb_ary_cmp
	 *
	 */
	public RubyObject op_cmp(RubyObject other) {
		RubyArray ary = arrayValue(other);
		int otherLen = ary.getLength();
		int len = getLength();

		if (len != otherLen) {
			return (len > otherLen) ? RubyFixnum.one(getRuby()) : RubyFixnum.minus_one(getRuby());
		}

		for (int i = 0; i < len; i++) {
			RubyFixnum result = (RubyFixnum) entry(i).funcall("<=>", ary.entry(i));
			if (result.getLongValue() != 0) {
				return result;
			}
		}

		return RubyFixnum.zero(getRuby());
	}

	/** rb_ary_slice_bang
	 *
	 */
	public RubyObject slice_bang(RubyObject[] args) {
		int argc = argCount(args, 1, 2);
		RubyObject result = aref(args);
		if (argc == 2) {
			long beg = RubyNumeric.fix2long(args[0]);
			long len = RubyNumeric.fix2long(args[1]);
			replace(beg, len, getRuby().getNil());
		} else if ((args[0] instanceof RubyFixnum) && (RubyNumeric.fix2long(args[0]) < getLength())) {
			replace(RubyNumeric.fix2long(args[0]), 1, getRuby().getNil());
		} else if (args[0] instanceof RubyRange) {
			long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), false, true);
			replace(begLen[0], begLen[1], getRuby().getNil());
		}
		return result;
	}

	/** rb_ary_assoc
	 *
	 */
	public RubyObject assoc(RubyObject arg) {
		for (int i = 0, len = getLength(); i < len; i++) {
			if (!((entry(i) instanceof RubyArray) && ((RubyArray) entry(i)).getLength() > 0)) {
				continue;
			}
			RubyArray ary = (RubyArray) entry(i);
			if (arg.funcall("==", ary.entry(0)).isTrue()) {
				return ary;
			}
		}
		return getRuby().getNil();
	}

	/** rb_ary_rassoc
	 *
	 */
	public RubyObject rassoc(RubyObject arg) {
		for (int i = 0, len = getLength(); i < len; i++) {
			if (!((entry(i) instanceof RubyArray) && ((RubyArray) entry(i)).getLength() > 1)) {
				continue;
			}
			RubyArray ary = (RubyArray) entry(i);
			if (arg.funcall("==", ary.entry(1)).isTrue()) {
				return ary;
			}
		}
		return getRuby().getNil();
	}

	/** rb_ary_flatten_bang
	 *
	 */
	public RubyObject flatten_bang() {
		modify();
		if (flatten(list)) {
			return this;
		}
		return getRuby().getNil();
	}

	/** rb_ary_flatten
	 *
	 */
	public RubyObject flatten() {
		RubyArray rubyArray = (RubyArray) dup();
		rubyArray.flatten_bang();
		return rubyArray;
	}

	/** rb_ary_nitems
	 *
	 */
	public RubyObject nitems() {
		int count = 0;
		for (int i = 0, len = getLength(); i < len; i++) {
			count += entry(i).isNil() ? 0 : 1;
		}
		return RubyFixnum.newFixnum(getRuby(), count);
	}

	/** rb_ary_plus
	 *
	 */
	public RubyObject op_plus(RubyObject other) {
		ArrayList otherList = arrayValue(other).getList();
		ArrayList newList = new ArrayList(getLength() + otherList.size());
		newList.addAll(list);
		newList.addAll(otherList);
		return new RubyArray(getRuby(), newList);
	}

	/** rb_ary_times
	 *
	 */
	public RubyObject op_times(RubyObject arg) {
		if (arg instanceof RubyString) {
			return join((RubyString) arg);
		}

		int len = (int) RubyNumeric.num2long(arg);
		if (len < 0) {
			throw new ArgumentError(getRuby(), "negative argument");
		}
		ArrayList newList = new ArrayList(getLength() * len);
		for (int i = 0; i < len; i++) {
			newList.addAll(list);
		}
		return new RubyArray(getRuby(), newList);
	}

	private ArrayList uniq(List oldList) {
		int oldLength = oldList.size();
		ArrayList newList = new ArrayList(oldLength);

		for (int i = 0; i < oldLength; i++) {
			RubyObject obj = (RubyObject)oldList.get(i);

			boolean found = false;
			int newLength = newList.size();
			for (int j = 0; j < newLength; j++) {
				if (obj.funcall("==", (RubyObject)newList.get(j)).isTrue()) {
					found = true;
					break;
				}
			}
			if (!found) {
				newList.add(obj);
			}
		}

		newList.trimToSize();
		return newList;
	}

	/** rb_ary_uniq_bang
	 *
	 */
	public RubyObject uniq_bang() {
		modify();
		ArrayList newList = uniq(list);
		if (newList.equals(list)) {
			return getRuby().getNil();
		}
		list = newList;
		return this;
	}

	/** rb_ary_uniq
	 *
	 */
	public RubyObject uniq() {
		return new RubyArray(getRuby(), uniq(list));
	}

	/** rb_ary_diff
	 *
	 */
	public RubyObject op_diff(RubyObject other) {
		ArrayList ary1 = uniq(list);
		ArrayList ary2 = arrayValue(other).getList();
		int len2 = ary2.size();
		for (int i = ary1.size() - 1; i >= 0; i--) {
			RubyObject obj = (RubyObject) ary1.get(i);
			for (int j = 0; j < len2; j++) {
				if (obj.funcall("==", (RubyObject) ary2.get(j)).isTrue()) {
					ary1.remove(i);
					break;
				}
			}
		}
		return new RubyArray(getRuby(), ary1);
	}

	/** rb_ary_and
	 *
	 */
	public RubyObject op_and(RubyObject other) {
		ArrayList ary1 = uniq(list);
		int len1 = ary1.size();
		ArrayList ary2 = arrayValue(other).getList();
		int len2 = ary2.size();
		ArrayList ary3 = new ArrayList(len1);
		for (int i = 0; i < len1; i++) {
			RubyObject obj = (RubyObject) ary1.get(i);
			for (int j = 0; j < len2; j++) {
				if (obj.funcall("==", (RubyObject) ary2.get(j)).isTrue()) {
					ary3.add(obj);
					break;
				}
			}
		}
		ary3.trimToSize();
		return new RubyArray(getRuby(), ary3);
	}

	/** rb_ary_or
	 *
	 */
	public RubyObject op_or(RubyObject other) {
		ArrayList ary1 = new ArrayList(list);
		ArrayList ary2 = arrayValue(other).getList();
		ary1.addAll(ary2);
		return new RubyArray(getRuby(), uniq(ary1));
	}

	/** rb_ary_sort
	 *
	 */
	public RubyArray sort() {
		RubyArray rubyArray = (RubyArray) dup();
		rubyArray.sort_bang();
		return rubyArray;
	}

	/** rb_ary_sort_bang
	 *
	 */
	public RubyObject sort_bang() {
		if (getLength() <= 1) {
			return getRuby().getNil();
		}
		modify();
		setTmpLock(true);

		if (getRuby().isBlockGiven()) {
			Collections.sort(list, new BlockComparator());
		} else {
			Collections.sort(list, new DefaultComparator());
		}

		setTmpLock(false);
		return this;
	}


	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('[');
		output.dumpInt(getList().size());
		Iterator iter = getList().iterator();
		while (iter.hasNext()) {
			output.dumpObject((RubyObject) iter.next());
		}
	}

	public static RubyArray unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
		RubyArray result = newArray(input.getRuby());
		int size = input.unmarshalInt();
		for (int i = 0; i < size; i++) {
			result.append(input.unmarshalObject());
		}
		return result;
	}

    /**
     * @see org.jruby.util.Pack#pack
     */
	public RubyString pack(RubyString iFmt) {
        return Pack.pack(this.list, iFmt);
    }

    class BlockComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			RubyObject result = getRuby().yield(RubyArray.newArray(getRuby(), (RubyObject) o1, (RubyObject) o2));
			return (int) ((RubyNumeric) result).getLongValue();
		}

		public boolean equals(Object other) {
			return this == other;
		}
	}

	class DefaultComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			RubyObject obj1 = (RubyObject) o1;
			RubyObject obj2 = (RubyObject) o2;
			if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
				return (int) (RubyNumeric.fix2long(obj1) - RubyNumeric.fix2long(obj2));
			}

			if (o1 instanceof RubyString && o2 instanceof RubyString) {
				return RubyNumeric.fix2int(((RubyString) o1).op_cmp((RubyObject) o2));
			}

			return RubyNumeric.fix2int(obj1.funcall("<=>", obj2));
		}

		public boolean equals(Object other) {
			return this == other;
		}
	}
}
