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

/**
 *
 * @author  jpetersen
 */
public class RubyArray extends RubyObject {
	private ArrayList list;
	private boolean tmpLock;

	// private RubyId equals; RubyId will replaced by String next.

	public RubyArray(Ruby ruby) {
		this(ruby, new ArrayList());
	}

	public RubyArray(Ruby ruby, List array) {
		this(ruby, new ArrayList(array), false);
	}

	public RubyArray(Ruby ruby, ArrayList array, boolean notCopy) {
		super(ruby, ruby.getClasses().getArrayClass());
		this.list = array;

		// equals = ruby.intern("==");
	}

	public static RubyArray nilArray(Ruby ruby) {
		return new RubyArray(ruby) {
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

	public static RubyClass createArrayClass(Ruby ruby) {
		RubyClass arrayClass = ruby.defineClass("Array", ruby.getClasses().getObjectClass());

		arrayClass.includeModule(ruby.getRubyModule("Enumerable"));

		arrayClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyArray.class, "newInstance"));
		arrayClass.defineSingletonMethod("[]", CallbackFactory.getOptSingletonMethod(RubyArray.class, "create"));
		arrayClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyArray.class, "initialize"));

		arrayClass.defineMethod("inspect", CallbackFactory.getMethod(RubyArray.class, "inspect"));
		arrayClass.defineMethod("to_s", CallbackFactory.getMethod(RubyArray.class, "to_s"));
		arrayClass.defineMethod("to_a", CallbackFactory.getMethod(RubyArray.class, "to_a"));
		arrayClass.defineMethod("to_ary", CallbackFactory.getMethod(RubyArray.class, "to_a"));
		arrayClass.defineMethod("frozen?", CallbackFactory.getMethod(RubyArray.class, "frozen"));

		arrayClass.defineMethod("==", CallbackFactory.getMethod(RubyArray.class, "equal", RubyObject.class));
		arrayClass.defineMethod("eql?", CallbackFactory.getMethod(RubyArray.class, "eql", RubyObject.class));
		arrayClass.defineMethod("hash", CallbackFactory.getMethod(RubyArray.class, "hash"));
		arrayClass.defineMethod("===", CallbackFactory.getMethod(RubyArray.class, "equal", RubyObject.class));

		arrayClass.defineMethod("[]", CallbackFactory.getOptMethod(RubyArray.class, "aref"));
		arrayClass.defineMethod("[]=", CallbackFactory.getOptMethod(RubyArray.class, "aset"));
		arrayClass.defineMethod("at", CallbackFactory.getMethod(RubyArray.class, "at", RubyFixnum.class));

		arrayClass.defineMethod("first", CallbackFactory.getMethod(RubyArray.class, "first"));
		arrayClass.defineMethod("last", CallbackFactory.getMethod(RubyArray.class, "last"));
		arrayClass.defineMethod("concat", CallbackFactory.getMethod(RubyArray.class, "concat", RubyObject.class));

		arrayClass.defineMethod("<<", CallbackFactory.getMethod(RubyArray.class, "push", RubyObject.class));
		arrayClass.defineMethod("push", CallbackFactory.getOptMethod(RubyArray.class, "push"));
		arrayClass.defineMethod("pop", CallbackFactory.getMethod(RubyArray.class, "pop"));

		arrayClass.defineMethod("shift", CallbackFactory.getMethod(RubyArray.class, "shift"));
		arrayClass.defineMethod("unshift", CallbackFactory.getOptMethod(RubyArray.class, "unshift"));
		arrayClass.defineMethod("each", CallbackFactory.getMethod(RubyArray.class, "each"));
		arrayClass.defineMethod("each_index", CallbackFactory.getMethod(RubyArray.class, "each_index"));
		arrayClass.defineMethod("reverse_each", CallbackFactory.getMethod(RubyArray.class, "reverse_each"));

		arrayClass.defineMethod("length", CallbackFactory.getMethod(RubyArray.class, "length"));
		arrayClass.defineMethod("size", CallbackFactory.getMethod(RubyArray.class, "length"));
		arrayClass.defineMethod("empty?", CallbackFactory.getMethod(RubyArray.class, "empty_p"));
		arrayClass.defineMethod("index", CallbackFactory.getMethod(RubyArray.class, "index", RubyObject.class));
		arrayClass.defineMethod("rindex", CallbackFactory.getMethod(RubyArray.class, "rindex", RubyObject.class));
		arrayClass.defineMethod("indexes", CallbackFactory.getOptMethod(RubyArray.class, "indexes"));
		arrayClass.defineMethod("indices", CallbackFactory.getOptMethod(RubyArray.class, "indexes"));
		arrayClass.defineMethod("clone", CallbackFactory.getMethod(RubyArray.class, "rbClone"));
		arrayClass.defineMethod("join", CallbackFactory.getOptMethod(RubyArray.class, "join"));
		arrayClass.defineMethod("reverse", CallbackFactory.getMethod(RubyArray.class, "reverse"));
		arrayClass.defineMethod("reverse!", CallbackFactory.getMethod(RubyArray.class, "reverse_bang"));

		arrayClass.defineMethod("sort", CallbackFactory.getMethod(RubyArray.class, "sort"));
		arrayClass.defineMethod("sort!", CallbackFactory.getMethod(RubyArray.class, "sort_bang"));

		arrayClass.defineMethod("collect", CallbackFactory.getMethod(RubyArray.class, "collect"));
		arrayClass.defineMethod("collect!", CallbackFactory.getMethod(RubyArray.class, "collect_bang"));
		arrayClass.defineMethod("map!", CallbackFactory.getMethod(RubyArray.class, "collect_bang"));
		arrayClass.defineMethod("filter", CallbackFactory.getMethod(RubyArray.class, "collect_bang"));
		arrayClass.defineMethod("delete", CallbackFactory.getMethod(RubyArray.class, "delete", RubyObject.class));
		arrayClass.defineMethod("delete_at", CallbackFactory.getMethod(RubyArray.class, "delete_at", RubyObject.class));
		arrayClass.defineMethod("delete_if", CallbackFactory.getMethod(RubyArray.class, "delete_if"));
		arrayClass.defineMethod("reject!", CallbackFactory.getMethod(RubyArray.class, "reject_bang"));
		arrayClass.defineMethod("replace", CallbackFactory.getMethod(RubyArray.class, "replace", RubyObject.class));
		arrayClass.defineMethod("clear", CallbackFactory.getMethod(RubyArray.class, "clear"));
		arrayClass.defineMethod("fill", CallbackFactory.getOptMethod(RubyArray.class, "fill"));

		arrayClass.defineMethod("include?", CallbackFactory.getMethod(RubyArray.class, "includes", RubyObject.class));

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
	public RubyObject subseq(long beg, long len) {
		int length = getLength();

		if (beg > length) {
			return getRuby().getNil();
		}
		if (beg < 0 || len < 0) {
			return getRuby().getNil();
		}

		if (beg + len > length) {
			len = length - beg;
		}
		if (len < 0) {
			len = 0;
		}
		if (len == 0) {
			return newArray(getRuby(), 0);
		}

		RubyArray ary2 = newArray(getRuby(), list.subList((int) beg, (int) (len + beg)));

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
			RubyObject obj = (RubyObject) ary.get(i);
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
	public static RubyArray newArray(Ruby ruby, long len) {
		return new RubyArray(ruby, new ArrayList((int) len));
	}

	/** rb_ary_new
	 *
	 */
	public static RubyArray newArray(Ruby ruby) {
		/* Ruby arrays default to holding 16 elements, so we create an
		 * ArrayList of the same size if we're not told otherwise
		 */
		return new RubyArray(ruby, new ArrayList(16));
	}

	/**
	 *
	 */
	public static RubyArray newArray(Ruby ruby, RubyObject obj) {
		return new RubyArray(ruby, Collections.singletonList(obj));
	}

	/** rb_assoc_new
	 *
	 */
	public static RubyArray newArray(Ruby ruby, RubyObject car, RubyObject cdr) {
		return new RubyArray(ruby, Arrays.asList(new RubyObject[] { car, cdr }));
	}

	public static RubyArray newArray(Ruby ruby, List list) {
		return new RubyArray(ruby, list);
	}

	public static RubyArray newArray(Ruby ruby, RubyObject[] args) {
		return new RubyArray(ruby, Arrays.asList(args));
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
	public static RubyArray create(Ruby ruby, RubyObject recv, RubyObject[] args) {
		RubyArray array = newArray(ruby, Arrays.asList(args));
		array.setRubyClass((RubyClass) recv);

		return array;
	}

	/** rb_ary_hash
	 *
	 */
	public RubyFixnum hash() {
		return new RubyFixnum(getRuby(), list.hashCode());
	}

	/** rb_ary_length
	 *
	 */
	public RubyFixnum length() {
		return new RubyFixnum(getRuby(), getLength());
	}

	/** rb_ary_push_m
	 *
	 */
	public RubyArray push(RubyObject[] items) {
		// Performance
		int length = items.length;

		if (length == 0) {
			throw new ArgumentError(getRuby(), "wrong # of arguments(at least 1)");
		}
		modify();
		boolean taint = false;
		for (int i = 0; i < length; i++) {
			taint |= items[i].isTaint();
			list.add(items[i]);
		}
		setTaint(isTaint() || taint);
		return this;
	}

	public RubyArray push(RubyObject value) {
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

	/** rb_ary_includes
	 *
	 */
	public RubyBoolean includes(RubyObject item) {
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

	/*public RubyObject dup() {
		return aref(new RubyObject[] { RubyFixnum.zero(getRuby()), length()});
		}*/

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
		StringBuffer sbuf = new StringBuffer();
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

	/** rb_ary_indexes
	 *
	 */
	public RubyArray indexes(RubyObject[] args) {
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
	public RubyObject reverse_bang() {
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
			result.push(input.unmarshalObject());
		}
		return result;
	}

	private String convert2String(RubyObject l2Conv)
	{
		if (l2Conv.getRubyClass() != ruby.getClasses().getStringClass())
		{
			l2Conv = l2Conv.convertToType("String", "to_s", true);	//we may need a false here, not sure
		}
		return ((RubyString)l2Conv).getValue();
	}
	public static final String sSp10 = "          ";
	public static final String sNil10 = "\000\000\000\000\000\000\000\000\000\000";
	/** Native pack type.
	 **/
	public static final String sNatStr = "sSiIlL";
	private static final String sTooFew = "too few arguments";
	static char[] uu_table ="`!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_".toCharArray();
	static char[] b64_table="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	/**
	 * encodes a String in base64 or its uuencode variant.
	 * appends the result of the encoding in a StringBuffer
	 * @param io2Append The StringBuffer which should receive the result
	 * @param i2Encode The String to encode
	 * @param iLength The max number of characters to encode
	 * @param iType the type of encoding required (this is the same type as used by the pack method)
	 * @return the io2Append buffer
	 **/
	private StringBuffer encodes( StringBuffer io2Append, String i2Encode, int iLength, char iType)
	{
		iLength = iLength < i2Encode.length() ?  iLength : i2Encode.length();
		io2Append.ensureCapacity( iLength * 4 / 3 + 6);
		int i = 0;
		char[] lTranslationTable = iType == 'u' ? uu_table : b64_table;
		char lPadding;
		char[] l2Encode = i2Encode.toCharArray();
		if (iType == 'u') {
			if (iLength >= lTranslationTable.length)
				throw new ArgumentError(ruby, "" + iLength + " is not a correct value for the number of bytes per line in a u directive.  Correct values range from 0 to " + lTranslationTable.length);
			io2Append.append(lTranslationTable[iLength]);
			lPadding = '`';
		}
		else {
			lPadding = '=';
		}
		while (iLength >= 3) {
			char lCurChar = l2Encode[i++];
			char lNextChar = l2Encode[i++];
			char lNextNextChar = l2Encode[i++];
			io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
			io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >>> 4) & 017))]);
			io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | ((lNextNextChar >>> 6) & 03))]);
			io2Append.append(lTranslationTable[077 & lNextNextChar]);
			iLength -= 3;
		}
		if (iLength == 2) {
			char lCurChar = l2Encode[i++];
			char lNextChar = l2Encode[i++];
			io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
			io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >> 4) & 017))]);
			io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | (('\0' >> 6) & 03))]);
			io2Append.append(lPadding);
		}
		else if (iLength == 1) {
			char lCurChar = l2Encode[i++];
			io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
			io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | (('\0' >>> 4) & 017))]);
			io2Append.append(lPadding);
			io2Append.append(lPadding);
		}
		io2Append.append('\n');
		return io2Append;
	}

	public static char hex_table[] = "0123456789ABCDEF".toCharArray();
	/**
	 * encodes a String with the Quoted printable, MIME encoding (see RFC2045).
	 * appends the result of the encoding in a StringBuffer
	 * @param io2Append The StringBuffer which should receive the result
	 * @param i2Encode The String to encode
	 * @param iLength The max number of characters to encode
	 * @return the io2Append buffer
	 **/
	private StringBuffer qpencode( StringBuffer io2Append, String i2Encode, int iLength)
	{
		io2Append.ensureCapacity( 1024);
	    int lCurLineLength = 0;
		int	lPrevChar = -1;
		char [] l2Encode = i2Encode.toCharArray();
		try
		{
			for(int i = 0;;i++)
			{
				char lCurChar = l2Encode[i];
				if ((lCurChar > 126) ||
						(lCurChar < 32 && lCurChar != '\n' && lCurChar != '\t') ||
						(lCurChar == '=')) {
					io2Append.append('=');
					io2Append.append(hex_table[lCurChar >> 4]);
					io2Append.append(hex_table[lCurChar & 0x0f]);
					lCurLineLength += 3;
					lPrevChar = -1;
				}
				else if (lCurChar == '\n') {
					if (lPrevChar == ' ' || lPrevChar == '\t') {
						io2Append.append('=');
						io2Append.append(lCurChar);
					}
					io2Append.append(lCurChar);
					lCurLineLength = 0;
					lPrevChar = lCurChar;
				}
				else {
					io2Append.append(lCurChar);
					lCurLineLength++;
					lPrevChar = lCurChar;
				}
				if (lCurLineLength > iLength) {
					io2Append.append('=');
					io2Append.append('\n');
					lCurLineLength = 0;
					lPrevChar = '\n';
				}
			}
		} catch (ArrayIndexOutOfBoundsException e)
		{
			//normal exit, this should be faster than a test at each iterations for string with more than
			//about 40 char
		}
		

		if (lCurLineLength > 0) {
			io2Append.append('=');
			io2Append.append('\n');
		}
		return io2Append;
	}
	


	/**
	 * pack_pack
	 *
	 * Template characters for Array#pack Directive  Meaning
 * 	         <table class="codebox" cellspacing="0" border="0" cellpadding="3">
 * <tr bgcolor="#ff9999">
 *   <td valign="top">
 *                     <b>Directive</b>
 *                   </td>
 *   <td valign="top">
 *                     <b>Meaning</b>
 *                   </td>
 * </tr>
 * <tr>
 *   <td valign="top">@</td>
 *   <td valign="top">Moves to absolute position</td>
 * </tr>
 * <tr>
 *   <td valign="top">A</td>
 *   <td valign="top">ASCII string (space padded, count is width)</td>
 * </tr>
 * <tr>
 *   <td valign="top">a</td>
 *   <td valign="top">ASCII string (null padded, count is width)</td>
 * </tr>
 * <tr>
 *   <td valign="top">B</td>
 *   <td valign="top">Bit string (descending bit order)</td>
 * </tr>
 * <tr>
 *   <td valign="top">b</td>
 *   <td valign="top">Bit string (ascending bit order)</td>
 * </tr>
 * <tr>
 *   <td valign="top">C</td>
 *   <td valign="top">Unsigned char</td>
 * </tr>
 * <tr>
 *   <td valign="top">c</td>
 *   <td valign="top">Char</td>
 * </tr>
 * <tr>
 *   <td valign="top">d</td>
 *   <td valign="top">Double-precision float, native format</td>
 * </tr>
 * <tr>
 *   <td valign="top">E</td>
 *   <td valign="top">Double-precision float, little-endian byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">e</td>
 *   <td valign="top">Single-precision float, little-endian byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">f</td>
 *   <td valign="top">Single-precision float, native format</td>
 * </tr>
 * <tr>
 *   <td valign="top">G</td>
 *   <td valign="top">Double-precision float, network (big-endian) byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">g</td>
 *   <td valign="top">Single-precision float, network (big-endian) byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">H</td>
 *   <td valign="top">Hex string (high nibble first)</td>
 * </tr>
 * <tr>
 *   <td valign="top">h</td>
 *   <td valign="top">Hex string (low nibble first)</td>
 * </tr>
 * <tr>
 *   <td valign="top">I</td>
 *   <td valign="top">Unsigned integer</td>
 * </tr>
 * <tr>
 *   <td valign="top">i</td>
 *   <td valign="top">Integer</td>
 * </tr>
 * <tr>
 *   <td valign="top">L</td>
 *   <td valign="top">Unsigned long</td>
 * </tr>
 * <tr>
 *   <td valign="top">l</td>
 *   <td valign="top">Long</td>
 * </tr>
 * <tr>
 *   <td valign="top">M</td>
 *   <td valign="top">Quoted printable, MIME encoding (see RFC2045)</td>
 * </tr>
 * <tr>
 *   <td valign="top">m</td>
 *   <td valign="top">Base64 encoded string</td>
 * </tr>
 * <tr>
 *   <td valign="top">N</td>
 *   <td valign="top">Long, network (big-endian) byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">n</td>
 *   <td valign="top">Short, network (big-endian) byte-order</td>
 * </tr>
 * <tr>
 *   <td valign="top">P</td>
 *   <td valign="top">Pointer to a structure (fixed-length string)</td>
 * </tr>
 * <tr>
 *   <td valign="top">p</td>
 *   <td valign="top">Pointer to a null-terminated string</td>
 * </tr>
 * <tr>
 *   <td valign="top">S</td>
 *   <td valign="top">Unsigned short</td>
 * </tr>
 * <tr>
 *   <td valign="top">s</td>
 *   <td valign="top">Short</td>
 * </tr>
 * <tr>
 *   <td valign="top">U</td>
 *   <td valign="top">UTF-8</td>
 * </tr>
 * <tr>
 *   <td valign="top">u</td>
 *   <td valign="top">UU-encoded string</td>
 * </tr>
 * <tr>
 *   <td valign="top">V</td>
 *   <td valign="top">Long, little-endian byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">v</td>
 *   <td valign="top">Short, little-endian byte order</td>
 * </tr>
 * <tr>
 *   <td valign="top">X</td>
 *   <td valign="top">Back up a byte</td>
 * </tr>
 * <tr>
 *   <td valign="top">x</td>
 *   <td valign="top">Null byte</td>
 * </tr>
 * <tr>
 *   <td valign="top">Z</td>
 *   <td valign="top">Same as ``A''</td>
 * </tr>
 * <tr>
 *                   <td colspan="9" bgcolor="#ff9999" height="2"><img src="dot.gif" width="1" height="1"></td>
 *                 </tr>
 *               </table>
 * 	 
	 *
	 * Packs the contents of arr into a binary sequence according to the directives in
	 * aTemplateString (see preceding table).
	 * Directives ``A,'' ``a,'' and ``Z'' may be followed by a count, which gives the
	 * width of the resulting field.
	 * The remaining directives also may take a count, indicating the number of array
	 * elements to convert.
	 * If the count is an asterisk (``*''), all remaining array elements will be
	 * converted.
	 * Any of the directives ``sSiIlL'' may be followed by an underscore (``_'') to use
	 * the underlying platform's native size for the specified type; otherwise, they
	 * use a platform-independent size. Spaces are ignored in the template string.
	 * @see RubyString#unpack
	 **/
	public RubyString pack(RubyString iFmt)
	{
		char[] lFmt = iFmt.getValue().toCharArray();
		int lFmtLength = lFmt.length;
		int idx = 0;
		int lLeftInArray = list.size();
		StringBuffer lResult = new StringBuffer();
		RubyObject lFrom;
		String lCurElemString;
		for(int i = 0; i < lFmtLength; )
		{
			int lLength = 1;
			//first skip all spaces
			char lType = lFmt[i++];
			if (Character.isWhitespace(lType))
				continue;
			char lNext = i < lFmtLength ? lFmt[i] : 0;
			if(lNext == '!' || lNext == '_')
			{
				if (sNatStr.indexOf(lType) != -1)
				{
					lNext = ++i < lFmtLength ? lFmt[i]:0;
				}
				else
					throw new ArgumentError(ruby, "'" + lNext +"' allowed only after types " + sNatStr);
			}
			if (lNext == '*')
			{
				lLength = "@Xxu".indexOf(lType) == -1 ?  lLeftInArray : 0;
				lNext = ++i < lFmtLength ? lFmt[i]:0;
			} else if (Character.isDigit(lNext))
			{
				int lEndIndex = i;
				for (; lEndIndex < lFmtLength ; lEndIndex++)
					if (!Character.isDigit(lFmt[lEndIndex]))
						break;
				lLength = Integer.parseInt(new String(lFmt, i, lEndIndex-i));		//an exception may occur here if an int can't hold this but ...
				i = lEndIndex;
				lNext = i  < lFmtLength ? lFmt[i] : 0;
			}		//no else, the original value of length is correct
			switch(lType)
			{
				case '%':
					throw new ArgumentError(ruby, "% is not supported");

				case 'A': case 'a': case 'Z':
				case 'B': case 'b':
				case 'H': case 'h':
					if (lLeftInArray-- > 0)
						lFrom = (RubyObject)list.get(idx++);
					else
						throw new ArgumentError(ruby, sTooFew);
					if(lFrom == ruby.getNil())
						lCurElemString = "";
					else
						lCurElemString = convert2String(lFrom);
					if (lFmt[i-1] == '*')
						lLength = lCurElemString.length();
					switch(lType)
					{
						case 'a':
						case 'A':
						case 'Z':
							if ( lCurElemString.length() >= lLength)
								lResult.append(lCurElemString.toCharArray(), 0, lLength);
							else 		//need padding
							{			//I'm fairly sure there is a library call to create a
								//string filled with a given char with a given length but I couldn't find it
								lResult.append(lCurElemString);
								lLength -= lCurElemString.length();
								grow(lResult, (lType == 'a')?sNil10:sSp10 , lLength);
							}
							break;

							//I believe there is a bug in the b and B case we skip a char too easily
						case 'b':
							{
								int lByte = 0;
								int lIndex = 0;
								char lCurChar;
								int lPadLength = 0;
								if (lLength > lCurElemString.length())
								{	//I don't understand this, why divide by 2
									lPadLength = (lLength - lCurElemString.length()+1)/2;
									lLength = lCurElemString.length();
								}
								for(lIndex = 0; lIndex < lLength; )
								{
									lCurChar = lCurElemString.charAt(lIndex++);
									if ((lCurChar & 1) != 0)	//if the low bit of the current char is set
										lByte |= 128;	//set the high bit of the result
									if ((lIndex & 7) != 0)		//if the index is not a multiple of 8, we are not on a byte boundary
										lByte >>= 1;	//shift the byte
									else
									{		//we are done with one byte, append it to the result and go for the next
										lResult.append((char)(lByte & 0xff));
										lByte = 0;
									}
								}
								if ((lLength & 7) != 0)	//if the length is not a multiple of 8
								{									//we need to pad the last byte
									lByte >>=7 - (lLength & 7);
									lResult.append((char)(lByte & 0xff));
								}
								//do some padding, I don't understand the padding strategy
								lLength = lResult.length();
								lResult.setLength(lLength+lPadLength);
							}
							break;

						case 'B':
							{
								int lByte = 0;
								int lIndex = 0;
								char lCurChar;
								int lPadLength = 0;
								if (lLength > lCurElemString.length())
								{	//I don't understand this, why divide by 2
									lPadLength = (lLength - lCurElemString.length()+1)/2;
									lLength = lCurElemString.length();
								}
								for(lIndex = 0; lIndex < lLength; )
								{
									lCurChar = lCurElemString.charAt(lIndex++);
									lByte |= lCurChar & 1;
									if ((lIndex & 7) != 0)		//if the index is not a multiple of 8, we are not on a byte boundary
										lByte <<= 1;	//shift the byte
									else
									{		//we are done with one byte, append it to the result and go for the next
										lResult.append((char)(lByte & 0xff));
										lByte = 0;
									}
								}
								if ((lLength & 7) != 0)	//if the length is not a multiple of 8
								{									//we need to pad the last byte
									lByte <<=7 - (lLength & 7);
									lResult.append((char)(lByte & 0xff));
								}
								//do some padding, I don't understand the padding strategy
								lLength = lResult.length();
								lResult.setLength(lLength+lPadLength);
							}
							break;

						case 'h':
							{
								int lByte = 0;
								int lIndex = 0;
								char lCurChar;
								int lPadLength = 0;
								if (lLength > lCurElemString.length())
								{	//I don't undestand this why divide by 2
									lPadLength = (lLength - lCurElemString.length()+1)/2;
									lLength = lCurElemString.length();
								}
								for(lIndex = 0; lIndex < lLength; )
								{
									lCurChar = lCurElemString.charAt(lIndex++);
									if (Character.isJavaIdentifierStart(lCurChar))	//this test may be too lax but it is the same as in MRI
										lByte |= (((lCurChar & 15) + 9) & 15) << 4;
									else
										lByte  |= (lCurChar & 15) << 4;
									if ((lIndex & 1) != 0)
										lByte >>= 4;
									else
									{
										lResult.append((char)(lByte & 0xff));
										lByte = 0;
									}
								}
								if ((lLength & 1) != 0)
								{
									lResult.append((char)(lByte & 0xff));
								}

								//do some padding, I don't understand the padding strategy
								lLength = lResult.length();
								lResult.setLength(lLength+lPadLength);
							}
							break;

						case 'H':
							{
								int lByte = 0;
								int lIndex = 0;
								char lCurChar;
								int lPadLength = 0;
								if (lLength > lCurElemString.length())
								{	//I don't undestand this why divide by 2
									lPadLength = (lLength - lCurElemString.length()+1)/2;
									lLength = lCurElemString.length();
								}
								for(lIndex = 0; lIndex < lLength;)
								{
									lCurChar = lCurElemString.charAt(lIndex++);
									if (Character.isJavaIdentifierStart(lCurChar))	//this test may be too lax but it is the same as in MRI
										lByte |= ((lCurChar & 15) + 9) & 15;
									else
										lByte  |= (lCurChar & 15);
									if ((lIndex & 1) != 0)
										lByte <<= 4;
									else
									{
										lResult.append((char)(lByte & 0xff));
										lByte = 0;
									}
								}
								if ((lLength & 1) != 0)
								{
									lResult.append((char)(lByte & 0xff));
								}

								//do some padding, I don't understand the padding strategy
								lLength = lResult.length();
								lResult.setLength(lLength+lPadLength);
							}
							break;
					}
					break;



				case 'x':
					grow(lResult, sNil10, lLength);
					break;

				case 'X':
					shrink(lResult, lLength);
					break;

				case '@':
					lLength -= lResult.length();
					if (lLength > 0) grow(lResult, sNil10, lLength);
					lLength = -lLength;
					if (lLength > 0) shrink(lResult, lLength);
					break;

				case 'c':
				case 'C':
					while (lLength-- > 0) {
						char c;
						if (lLeftInArray-- > 0)
							lFrom = (RubyObject)list.get(idx++);
						else
							throw new ArgumentError(ruby, sTooFew);
						if (lFrom == ruby.getNil()) c = 0;
						else {
							c = (char)(RubyNumeric.num2long(lFrom) & 0xff);
						}
						lResult.append(c);
					}
					break;

				case 'u':
				case 'm':
					if (lLeftInArray-- > 0)
						lFrom = (RubyObject)list.get(idx++);
					else
						throw new ArgumentError(ruby, sTooFew);
					if(lFrom == ruby.getNil())
						lCurElemString = "";
					else
						lCurElemString = convert2String(lFrom);

					if (lLength <= 2)
						lLength = 45;
					else
						lLength = lLength / 3 * 3;
					for (;;) 
					{
						int lTodo;
						encodes(lResult, lCurElemString, lLength, lType);
						if (lLength < lCurElemString.length())
							lCurElemString = lCurElemString.substring(lLength);
						else 
							break;
					}
					break;

				case 'M':
					if (lLeftInArray-- > 0)
						lFrom = (RubyObject)list.get(idx++);
					else
						throw new ArgumentError(ruby, sTooFew);
					if(lFrom == ruby.getNil())
						lCurElemString = "";
					else
						lCurElemString = convert2String(lFrom);
					
					if (lLength <= 1)
						lLength = 72;
					qpencode(lResult, lCurElemString, lLength);
					break;
					
				case 'U':
					char[] c = new char[lLength];
					for(int lCurCharIdx = 0;lLength-- > 0; lCurCharIdx++) {
						long l;
						if (lLeftInArray-- > 0)
							lFrom = (RubyObject)list.get(idx++);
						else
							throw new ArgumentError(ruby, sTooFew);

						if (lFrom == ruby.getNil()) l = 0;
						else {
							l = RubyNumeric.num2long(lFrom);
						}
						c[lCurCharIdx] = (char)l;
					}
					String s = new String(c);
					try
                    {
                        lResult.append(RubyString.bytesToString(s.getBytes("UTF-8")));
					} catch (java.io.UnsupportedEncodingException e)
					{
						throw new RubyBugException( "can't convert to UTF8");
					}
					break;
			}
		}
		return RubyString.newString(ruby, lResult.toString());
	}

	/**
	 * shrinks a stringbuffer.
	 * shrinks a stringbuffer by a number of characters.
	 * @param i2Shrink the stringbuffer
	 * @param iLength how much to shrink
	 * @return the stringbuffer
	 **/
	private final StringBuffer shrink(StringBuffer i2Shrink, int iLength)
	{
		iLength = i2Shrink.length() - iLength;
		if (iLength < 0)
			throw new ArgumentError(ruby, "X outside of string" );
		i2Shrink.setLength(iLength);
		return i2Shrink;
	}
	/**
	 * grows a stringbuffer.
	 * uses the Strings to pad the buffer for a certain length
	 * @param i2Grow the buffer to grow
	 * @param iPads the string used as padding
	 * @param iLength how much padding is needed
	 * @return the padded buffer
	 **/
	private final StringBuffer grow(StringBuffer i2Grow, String iPads, int iLength)
	{
		int lPadLength = iPads.length();
		while(iLength >= lPadLength)
		{
			i2Grow.append(iPads);
			iLength -= lPadLength;
		}
		i2Grow.append(iPads.substring(0, iLength));
		return i2Grow;
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
