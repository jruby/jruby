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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.IndexError;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Pack;
import org.jruby.util.collections.IdentitySet;
import org.jruby.internal.runtime.builtin.definitions.ArrayDefinition;

/**
 *
 * @author  jpetersen
 */
public class RubyArray extends RubyObject implements IndexCallable {
    private ArrayList list;
    private boolean tmpLock;

	private RubyArray(Ruby ruby, ArrayList list) {
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

    public IRubyObject[] toJavaArray() {
        return (IRubyObject[]) list.toArray(new IRubyObject[getLength()]);
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

    public boolean includes(IRubyObject item) {
        for (int i = 0, n = getLength(); i < n; i++) {
            if (item.callMethod("==", entry(i)).isTrue()) {
                return true;
            }
        }
        return false;
    }

    public static RubyClass createArrayClass(Ruby ruby) {
        RubyClass arrayClass = new ArrayDefinition(ruby).getType();

        arrayClass.includeModule(ruby.getRubyModule("Enumerable"));

        arrayClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyArray.class, "initialize"));

        arrayClass.defineMethod("to_a", CallbackFactory.getSelfMethod(0));
        arrayClass.defineMethod("to_ary", CallbackFactory.getSelfMethod(0));
        arrayClass.defineMethod("at", CallbackFactory.getMethod(RubyArray.class, "at", RubyFixnum.class));

        arrayClass.defineMethod("slice", CallbackFactory.getOptMethod(RubyArray.class, "aref"));
        arrayClass.defineMethod("slice!", CallbackFactory.getOptMethod(RubyArray.class, "slice_bang"));

        arrayClass.defineMethod("assoc", CallbackFactory.getMethod(RubyArray.class, "assoc", IRubyObject.class));
        arrayClass.defineMethod("rassoc", CallbackFactory.getMethod(RubyArray.class, "rassoc", IRubyObject.class));

        arrayClass.defineMethod("+", CallbackFactory.getMethod(RubyArray.class, "op_plus", IRubyObject.class));
        arrayClass.defineMethod("*", CallbackFactory.getMethod(RubyArray.class, "op_times", IRubyObject.class));

        arrayClass.defineMethod("-", CallbackFactory.getMethod(RubyArray.class, "op_diff", IRubyObject.class));
        arrayClass.defineMethod("&", CallbackFactory.getMethod(RubyArray.class, "op_and", IRubyObject.class));
        arrayClass.defineMethod("|", CallbackFactory.getMethod(RubyArray.class, "op_or", IRubyObject.class));

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

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case ArrayDefinition.INSPECT :
                return inspect();
            case ArrayDefinition.TO_S :
                return to_s();
            case ArrayDefinition.FROZEN :
                return frozen();
            case ArrayDefinition.EQUAL :
                return equal(args[0]);
            case ArrayDefinition.EQL :
                return eql(args[0]);
            case ArrayDefinition.HASH :
                return hash();
            case ArrayDefinition.AREF :
                return aref(args);
            case ArrayDefinition.ASET :
                return aset(args);
            case ArrayDefinition.FIRST :
                return first();
            case ArrayDefinition.LAST :
                return last();
            case ArrayDefinition.CONCAT :
                return concat(args[0]);
            case ArrayDefinition.APPEND :
                return append(args[0]);
            case ArrayDefinition.PUSH :
                return push(args);
            case ArrayDefinition.POP :
                return pop();
            case ArrayDefinition.SHIFT :
                return shift();
            case ArrayDefinition.UNSHIFT :
                return unshift(args);
            case ArrayDefinition.EACH :
                return each();
            case ArrayDefinition.EACH_INDEX :
                return each_index();
            case ArrayDefinition.REVERSE_EACH :
                return reverse_each();
            case ArrayDefinition.LENGTH :
                return length();
            case ArrayDefinition.EMPTY_P :
                return empty_p();
            case ArrayDefinition.INDEX :
                return index(args[0]);
            case ArrayDefinition.RINDEX :
                return rindex(args[0]);
            case ArrayDefinition.INDICES :
                return indices(args);
            case ArrayDefinition.RBCLONE :
                return rbClone();
            case ArrayDefinition.JOIN :
                return join(args);
            case ArrayDefinition.REVERSE :
                return reverse();
            case ArrayDefinition.REVERSE_BANG :
                return reverse_bang();
            case ArrayDefinition.SORT :
                return sort();
            case ArrayDefinition.SORT_BANG :
                return sort_bang();
            case ArrayDefinition.COLLECT :
                return collect();
            case ArrayDefinition.COLLECT_BANG :
                return collect_bang();
            case ArrayDefinition.DELETE :
                return delete(args[0]);
            case ArrayDefinition.DELETE_AT :
                return delete_at(args[0]);
            case ArrayDefinition.DELETE_IF :
                return delete_if();
            case ArrayDefinition.REJECT_BANG :
                return reject_bang();
            case ArrayDefinition.REPLACE :
                return replace(args[0]);
            case ArrayDefinition.CLEAR :
                return clear();
            case ArrayDefinition.INCLUDE_P :
                return include_p(args[0]);
            case ArrayDefinition.OP_CMP :
                return op_cmp(args[0]);
            case ArrayDefinition.FILL :
                return fill(args);
        }
        return super.callIndexed(index, args);
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, list.hashCode());
    }

    /** rb_ary_modify
     *
     */
    public void modify() {
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "Array");
        }
        if (isTmpLock()) {
            throw new TypeError(getRuntime(), "can't modify array during sort");
        }
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't modify array");
        }
    }

    /* if list's size is not at least 'toLength', add nil's until it is */
    private void autoExpand(long toLength) {
        list.ensureCapacity((int) toLength);
        for (int i = getLength(); i < toLength; i++) {
            list.add(getRuntime().getNil());
        }
    }

    /** rb_ary_store
     *
     */
    public void store(long idx, IRubyObject value) {
        modify();
        if (idx < 0) {
            idx += getLength();
            if (idx < 0) {
                throw new IndexError(getRuntime(), "index " + (idx - getLength()) + " out of array");
            }
        }
        autoExpand(idx + 1);
        list.set((int) idx, value);
    }

    /** rb_ary_entry
     *
     */
    public IRubyObject entry(long offset) {
        if (getLength() == 0) {
            return getRuntime().getNil();
        }

        if (offset < 0) {
            offset += getLength();
        }

        if (offset < 0 || getLength() <= offset) {
            return getRuntime().getNil();
        }

        return (IRubyObject) list.get((int) offset);
    }

    /** rb_ary_unshift
     *
     */
    public RubyArray unshift(IRubyObject item) {
        modify();
        list.add(0, item);

        return this;
    }

    /** rb_ary_subseq
     *
     */
    public IRubyObject subseq(long beg, long len) {
        int length = getLength();

        if (beg > length || beg < 0 || len < 0) {
            return getRuntime().getNil();
        }

        if (beg + len > length) {
            len = length - beg;
        }
        if (len <= 0) {
            return newArray(getRuntime(), 0);
        }

        RubyArray ary2 = newArray(getRuntime(), new ArrayList(list.subList((int) beg, (int) (len + beg))));

        return ary2;
    }

    /** rb_ary_replace
     *	@todo change the algorythm to make it efficient
     *			there should be no need to do any deletion or addition
     *			when the replacing object is an array of the same length
     *			and in any case we should minimize them, they are costly
     */
    public void replace(long beg, long len, IRubyObject repl) {
        int length = getLength();

        if (len < 0) {
            throw new IndexError(getRuntime(), "Negative array length: " + len);
        }
        if (beg < 0) {
            beg += length;
        }
        if (beg < 0) {
            throw new IndexError(getRuntime(), "Index out of bounds: " + beg);
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
    public static RubyArray arrayValue(IRubyObject other) {
        if (other instanceof RubyArray) {
            return (RubyArray) other;
        } else {
            try {
                return (RubyArray) other.convertType(RubyArray.class, "Array", "to_ary");
            } catch (Exception ex) {
                throw new ArgumentError(other.getRuntime(), "can't convert arg to Array: " + ex.getMessage());
            }
        }
    }

    private boolean flatten(ArrayList array) {
        return flatten(array, new IdentitySet());
    }

    private boolean flatten(ArrayList array, IdentitySet visited) {
        if (visited.contains(array)) {
            throw new ArgumentError(runtime, "tried to flatten recursive array");
        }
        visited.add(array);
        boolean isModified = false;
        for (int i = array.size() - 1; i >= 0; i--) {
            if (array.get(i) instanceof RubyArray) {
                ArrayList ary2 = ((RubyArray) array.remove(i)).getList();
                flatten(ary2, visited);
                array.addAll(i, ary2);
                isModified = true;
            }
        }
        visited.remove(array);
        return isModified;
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
    public static RubyArray newArray(Ruby ruby, IRubyObject obj) {
        ArrayList list = new ArrayList(1);
        list.add(obj);
        return new RubyArray(ruby, list);
    }

    /** rb_assoc_new
     *
     */
    public static RubyArray newArray(Ruby ruby, IRubyObject car, IRubyObject cdr) {
        ArrayList list = new ArrayList(2);
        list.add(car);
        list.add(cdr);
        return new RubyArray(ruby, list);
    }

    public final static RubyArray newArray(final Ruby ruby, final ArrayList list) {
        return new RubyArray(ruby, list);
    }

    public static RubyArray newArray(Ruby ruby, IRubyObject[] args) {
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
    public static RubyArray newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyArray array = newArray(recv.getRuntime());
        array.setMetaClass((RubyClass) recv);

        array.callInit(args);

        return array;
    }

    /** rb_ary_s_create
     *
     */
    public static RubyArray create(IRubyObject recv, IRubyObject[] args) {
        RubyArray array = newArray(recv.getRuntime(), args);

        array.setMetaClass((RubyClass) recv);

        return array;
    }

    /** rb_ary_length
     *
     */
    public RubyFixnum length() {
        return RubyFixnum.newFixnum(getRuntime(), getLength());
    }

    /** rb_ary_push_m
     *
     */
    public RubyArray push(IRubyObject[] items) {
        modify();
        boolean tainted = false;
        for (int i = 0; i < items.length; i++) {
            tainted |= items[i].isTaint();
            list.add(items[i]);
        }
        setTaint(isTaint() || tainted);
        return this;
    }

    public RubyArray append(IRubyObject value) {
        modify();
        list.add(value);
        infectBy(value);
        return this;
    }

    /** rb_ary_pop
     *
     */
    public IRubyObject pop() {
        modify();
        if (getLength() == 0) {
            return getRuntime().getNil();
        }
        return (IRubyObject) list.remove(getLength() - 1);
    }

    /** rb_ary_shift
     *
     */
    public IRubyObject shift() {
        modify();
        if (getLength() == 0) {
            return getRuntime().getNil();
        }

        return (IRubyObject) list.remove(0);
    }

    /** rb_ary_unshift_m
     *
     */
    public RubyArray unshift(IRubyObject[] items) {
        if (items.length == 0) {
            throw new ArgumentError(getRuntime(), "wrong # of arguments(at least 1)");
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

    public RubyBoolean include_p(IRubyObject item) {
        return RubyBoolean.newBoolean(runtime, includes(item));
    } /** rb_ary_frozen_p
    	 *
    	 */
    public RubyBoolean frozen() {
        return RubyBoolean.newBoolean(getRuntime(), isFrozen() || isTmpLock());
    }

    /** rb_ary_initialize
     *
     */
    public IRubyObject initialize(IRubyObject[] args) {
        int argc = argCount(args, 0, 2);
        long len = 0;
        if (argc != 0)
            len = RubyNumeric.fix2long(args[0]);

        modify();

        if (len < 0) {
            throw new ArgumentError(getRuntime(), "negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw new ArgumentError(getRuntime(), "array size too big");
        }
        list = new ArrayList((int) len);
        if (len > 0) {
            IRubyObject obj = (argc == 2) ? args[1] : getRuntime().getNil();
            Collections.fill(list, obj);
        }
        return this;
    }

    /** rb_ary_aref
     *
     */
    public IRubyObject aref(IRubyObject[] args) {
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
            throw new IndexError(getRuntime(), "index too big");
        }
        if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), true, false);
            if (begLen == null) {
                return getRuntime().getNil();
            }
            return subseq(begLen[0], begLen[1]);
        }
        return entry(RubyNumeric.num2long(args[0]));
    }

    /** rb_ary_aset
     *
     */
    public IRubyObject aset(IRubyObject[] args) {
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
            throw new IndexError(getRuntime(), "Index too large");
        }
        store(RubyNumeric.num2long(args[0]), args[1]);
        return args[1];
    }

    /** rb_ary_at
     *
     */
    public IRubyObject at(RubyFixnum pos) {
        return entry(pos.getLongValue());
    }

    /** rb_ary_concat
     *
     */
    public RubyArray concat(IRubyObject obj) {
        modify();
        RubyArray other = arrayValue(obj);
        list.addAll(other.getList());
        infectBy(other);
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
            return RubyString.newString(getRuntime(), "[]");
        }
        RubyString result = RubyString.newString(getRuntime(), "[");
        RubyString sep = RubyString.newString(getRuntime(), ", ");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                result.append(sep);
            }
            result.append(entry(i).callMethod("inspect"));
        }
        result.cat("]");
        return result;
        // HACK ---
    }

    /** rb_ary_first
     *
     */
    public IRubyObject first() {
        if (getLength() == 0) {
            return getRuntime().getNil();
        }
        return entry(0);
    }

    /** rb_ary_last
     *
     */
    public IRubyObject last() {
        if (getLength() == 0) {
            return getRuntime().getNil();
        }
        return entry(getLength() - 1);
    }

    /** rb_ary_each
     *
     */
    public IRubyObject each() {
        for (int i = 0, len = getLength(); i < len; i++) {
            getRuntime().yield(entry(i));
        }
        return this;
    }

    /** rb_ary_each_index
     *
     */
    public IRubyObject each_index() {
        for (int i = 0, len = getLength(); i < len; i++) {
            getRuntime().yield(RubyFixnum.newFixnum(getRuntime(), i));
        }
        return this;
    }

    /** rb_ary_reverse_each
     *
     */
    public IRubyObject reverse_each() {
        for (long i = getLength(); i > 0; i--) {
            getRuntime().yield(entry(i - 1));
        }
        return this;
    }

    /** rb_ary_join
     *
     */
    RubyString join(RubyString sep) {
        int length = getLength();
        if (length == 0) {
            RubyString.newString(getRuntime(), "");
        }
        boolean taint = isTaint() || sep.isTaint();
        RubyString str;
        IRubyObject tmp = entry(0);
        taint |= tmp.isTaint();
        if (tmp instanceof RubyString) {
            str = (RubyString) tmp.dup();
        } else if (tmp instanceof RubyArray) {
            str = ((RubyArray) tmp).join(sep);
        } else {
            str = RubyString.objAsString(tmp);
        }
        for (long i = 1; i < length; i++) {
            tmp = entry(i);
            taint |= tmp.isTaint();
            if (tmp instanceof RubyArray) {
                tmp = ((RubyArray) tmp).join(sep);
            } else if (!(tmp instanceof RubyString)) {
                tmp = RubyString.objAsString(tmp);
            }
            str.append(sep.op_plus(tmp));
        }
        str.setTaint(taint);
        return str;
    }

    /** rb_ary_join_m
     *
     */
    public RubyString join(IRubyObject[] args) {
        int argc = argCount(args, 0, 1);
        IRubyObject sep = (argc == 1) ? args[0] : getRuntime().getGlobalVariables().get("$,");
        return join(sep.isNil() ? RubyString.newString(getRuntime(), "") : RubyString.stringValue(sep));
    }

    /** rb_ary_to_s
     *
     */
    public RubyString to_s() {
        IRubyObject sep = getRuntime().getGlobalVariables().get("$,");
        return join(sep.isNil() ? RubyString.newString(getRuntime(), "") : RubyString.stringValue(sep));
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
    public RubyBoolean equal(IRubyObject obj) {
        if (this == obj) {
            return getRuntime().getTrue();
        }

        if (!(obj instanceof RubyArray)) {
            return getRuntime().getFalse();
        }
        int length = getLength();

        RubyArray ary = (RubyArray) obj;
        if (length != ary.getLength()) {
            return getRuntime().getFalse();
        }

        for (long i = 0; i < length; i++) {
            if (!entry(i).callMethod("==", ary.entry(i)).isTrue()) {
                return getRuntime().getFalse();
            }
        }
        return getRuntime().getTrue();
    }

    /** rb_ary_eql
     *
     */
    public RubyBoolean eql(IRubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            return getRuntime().getFalse();
        }
        int length = getLength();

        RubyArray ary = (RubyArray) obj;
        if (length != ary.getLength()) {
            return getRuntime().getFalse();
        }

        for (long i = 0; i < length; i++) {
            if (!entry(i).callMethod("eql?", ary.entry(i)).isTrue()) {
                return getRuntime().getFalse();
            }
        }
        return getRuntime().getTrue();
    }

    /** rb_ary_compact_bang
     *
     */
    public IRubyObject compact_bang() {
        modify();
        boolean changed = false;

        for (int i = getLength() - 1; i >= 0; i--) {
            if (entry(i).isNil()) {
                list.remove(i);
                changed = true;
            }
        }
        return changed ? (IRubyObject) this : (IRubyObject) getRuntime().getNil();
    }

    /** rb_ary_compact
     *
     */
    public IRubyObject compact() {
        RubyArray ary = (RubyArray) dup();
        ary.compact_bang(); //Benoit: do not return directly the value of compact_bang, it may be nil
        return ary;
    }

    /** rb_ary_empty_p
     *
     */
    public IRubyObject empty_p() {
        return getLength() == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** rb_ary_clear
     *
     */
    public IRubyObject clear() {
        modify();
        list.clear();
        return this;
    }

    /** rb_ary_fill
     *
     */
    public IRubyObject fill(IRubyObject[] args) {
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
                    throw new IndexError(getRuntime(), "Negative array index");
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
    public IRubyObject index(IRubyObject obj) {
        for (int i = 0, len = getLength(); i < len; i++) {
            if (obj.callMethod("==", entry(i)).isTrue()) {
                return RubyFixnum.newFixnum(getRuntime(), i);
            }
        }
        return getRuntime().getNil();
    }

    /** rb_ary_rindex
     *
     */
    public IRubyObject rindex(IRubyObject obj) {
        for (int i = getLength() - 1; i >= 0; i--) {
            if (obj.callMethod("==", entry(i)).isTrue()) {
                return RubyFixnum.newFixnum(getRuntime(), i);
            }
        }
        return getRuntime().getNil();
    }

    public RubyArray indices(IRubyObject[] args) {
        IRubyObject[] result = new IRubyObject[args.length];
        boolean taint = false;
        for (int i = 0; i < args.length; i++) {
            result[i] = entry(RubyNumeric.fix2int(args[i]));
            taint |= result[i].isTaint();
        }
        RubyArray ary = create(getMetaClass(), result);
        ary.setTaint(taint);
        return ary;
    }

    /** rb_ary_clone
     *
     */
    public IRubyObject rbClone() {
        RubyArray ary = newArray(getRuntime(), new ArrayList(list));
        ary.setupClone(this);
        return ary;
    }

    /** rb_ary_reverse_bang
     *
     */
    public RubyArray reverse_bang() {
        if (list.size() <= 1) {
            return nilArray(runtime);
        }
        modify();
        Collections.reverse(list);
        return this;
    }

    /** rb_ary_reverse_m
     *
     */
    public IRubyObject reverse() {
        RubyArray ary = (RubyArray) dup();
        ary.reverse_bang();
        return ary;
    }

    /** rb_ary_collect
     *
     */
    public RubyArray collect() {
        if (!getRuntime().isBlockGiven()) {
            return (RubyArray) dup();
        }
        ArrayList ary = new ArrayList();
        for (int i = 0, len = getLength(); i < len; i++) {
            ary.add(getRuntime().yield(entry(i)));
        }
        return new RubyArray(getRuntime(), ary);
    }

    /** rb_ary_collect_bang
     *
     */
    public RubyArray collect_bang() {
        modify();
        for (int i = 0, len = getLength(); i < len; i++) {
            list.set(i, getRuntime().yield(entry(i)));
        }
        return this;
    }

    /** rb_ary_delete
     *
     */
    public IRubyObject delete(IRubyObject obj) {
        modify();
        IRubyObject retVal = getRuntime().getNil();
        for (int i = getLength() - 1; i >= 0; i--) {
            if (obj.callMethod("==", entry(i)).isTrue()) {
                retVal = (IRubyObject) list.remove(i);
            }
        }
        if (retVal.isNil() && getRuntime().isBlockGiven()) {
            retVal = getRuntime().yield(entry(0));
        }
        return retVal;
    }

    /** rb_ary_delete_at
     *
     */
    public IRubyObject delete_at(IRubyObject obj) {
        modify();
        int pos = (int) RubyNumeric.num2long(obj);
        int len = getLength();
        if (pos >= len) {
            return getRuntime().getNil();
        }
        if (pos < 0 && (pos += len) < 0) {
            return getRuntime().getNil();
        }
        return (IRubyObject) list.remove(pos);
    }

    /** rb_ary_reject_bang
     *
     */
    public IRubyObject reject_bang() {
        modify();
        IRubyObject retVal = getRuntime().getNil();
        for (int i = getLength() - 1; i >= 0; i--) {
            if (getRuntime().yield(entry(i)).isTrue()) {
                retVal = (IRubyObject) list.remove(i);
            }
        }
        return retVal.isNil() ? (IRubyObject) retVal : (IRubyObject) this;
    }

    /** rb_ary_delete_if
     *
     */
    public IRubyObject delete_if() {
        reject_bang();
        return this;
    }

    /** rb_ary_replace
     *
     */
    public IRubyObject replace(IRubyObject other) {
        replace(0, getLength(), arrayValue(other));
        return this;
    }

    /** rb_ary_cmp
     *
     */
    public IRubyObject op_cmp(IRubyObject other) {
        RubyArray ary = arrayValue(other);
        int otherLen = ary.getLength();
        int len = getLength();

        if (len != otherLen) {
            return (len > otherLen) ? RubyFixnum.one(getRuntime()) : RubyFixnum.minus_one(getRuntime());
        }

        for (int i = 0; i < len; i++) {
            RubyFixnum result = (RubyFixnum) entry(i).callMethod("<=>", ary.entry(i));
            if (result.getLongValue() != 0) {
                return result;
            }
        }

        return RubyFixnum.zero(getRuntime());
    }

    /** rb_ary_slice_bang
     *
     */
    public IRubyObject slice_bang(IRubyObject[] args) {
        int argc = argCount(args, 1, 2);
        IRubyObject result = aref(args);
        if (argc == 2) {
            long beg = RubyNumeric.fix2long(args[0]);
            long len = RubyNumeric.fix2long(args[1]);
            replace(beg, len, getRuntime().getNil());
        } else if ((args[0] instanceof RubyFixnum) && (RubyNumeric.fix2long(args[0]) < getLength())) {
            replace(RubyNumeric.fix2long(args[0]), 1, getRuntime().getNil());
        } else if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), false, true);
            replace(begLen[0], begLen[1], getRuntime().getNil());
        }
        return result;
    }

    /** rb_ary_assoc
     *
     */
    public IRubyObject assoc(IRubyObject arg) {
        for (int i = 0, len = getLength(); i < len; i++) {
            if (!((entry(i) instanceof RubyArray) && ((RubyArray) entry(i)).getLength() > 0)) {
                continue;
            }
            RubyArray ary = (RubyArray) entry(i);
            if (arg.callMethod("==", ary.entry(0)).isTrue()) {
                return ary;
            }
        }
        return getRuntime().getNil();
    }

    /** rb_ary_rassoc
     *
     */
    public IRubyObject rassoc(IRubyObject arg) {
        for (int i = 0, len = getLength(); i < len; i++) {
            if (!((entry(i) instanceof RubyArray) && ((RubyArray) entry(i)).getLength() > 1)) {
                continue;
            }
            RubyArray ary = (RubyArray) entry(i);
            if (arg.callMethod("==", ary.entry(1)).isTrue()) {
                return ary;
            }
        }
        return getRuntime().getNil();
    }

    /** rb_ary_flatten_bang
     *
     */
    public IRubyObject flatten_bang() {
        modify();
        if (flatten(list)) {
            return this;
        }
        return getRuntime().getNil();
    }

    /** rb_ary_flatten
     *
     */
    public IRubyObject flatten() {
        RubyArray rubyArray = (RubyArray) dup();
        rubyArray.flatten_bang();
        return rubyArray;
    }

    /** rb_ary_nitems
     *
     */
    public IRubyObject nitems() {
        int count = 0;
        for (int i = 0, len = getLength(); i < len; i++) {
            count += entry(i).isNil() ? 0 : 1;
        }
        return RubyFixnum.newFixnum(getRuntime(), count);
    }

    /** rb_ary_plus
     *
     */
    public IRubyObject op_plus(IRubyObject other) {
        ArrayList otherList = arrayValue(other).getList();
        ArrayList newList = new ArrayList(getLength() + otherList.size());
        newList.addAll(list);
        newList.addAll(otherList);
        return new RubyArray(getRuntime(), newList);
    }

    /** rb_ary_times
     *
     */
    public IRubyObject op_times(IRubyObject arg) {
        if (arg instanceof RubyString) {
            return join((RubyString) arg);
        }

        int len = (int) RubyNumeric.num2long(arg);
        if (len < 0) {
            throw new ArgumentError(getRuntime(), "negative argument");
        }
        ArrayList newList = new ArrayList(getLength() * len);
        for (int i = 0; i < len; i++) {
            newList.addAll(list);
        }
        return new RubyArray(getRuntime(), newList);
    }

    private ArrayList uniq(List oldList) {
        ArrayList newList = new ArrayList(oldList.size());
        Set passed = new HashSet(oldList.size());

        Iterator iter = oldList.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            if (! passed.contains(item)) {
                passed.add(item);
                newList.add(item);
            }
        }
        newList.trimToSize();
        return newList;
    }

    /** rb_ary_uniq_bang
     *
     */
    public IRubyObject uniq_bang() {
        modify();
        ArrayList newList = uniq(list);
        if (newList.equals(list)) {
            return getRuntime().getNil();
        }
        list = newList;
        return this;
    }

    /** rb_ary_uniq
     *
     */
    public IRubyObject uniq() {
        return new RubyArray(getRuntime(), uniq(list));
    }

    /** rb_ary_diff
     *
     */
    public IRubyObject op_diff(IRubyObject other) {
        ArrayList ary1 = uniq(list);
        ArrayList ary2 = arrayValue(other).getList();
        int len2 = ary2.size();
        for (int i = ary1.size() - 1; i >= 0; i--) {
            IRubyObject obj = (IRubyObject) ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.callMethod("==", (IRubyObject) ary2.get(j)).isTrue()) {
                    ary1.remove(i);
                    break;
                }
            }
        }
        return new RubyArray(getRuntime(), ary1);
    }

    /** rb_ary_and
     *
     */
    public IRubyObject op_and(IRubyObject other) {
        ArrayList ary1 = uniq(list);
        int len1 = ary1.size();
        ArrayList ary2 = arrayValue(other).getList();
        int len2 = ary2.size();
        ArrayList ary3 = new ArrayList(len1);
        for (int i = 0; i < len1; i++) {
            IRubyObject obj = (IRubyObject) ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.callMethod("==", (IRubyObject) ary2.get(j)).isTrue()) {
                    ary3.add(obj);
                    break;
                }
            }
        }
        ary3.trimToSize();
        return new RubyArray(getRuntime(), ary3);
    }

    /** rb_ary_or
     *
     */
    public IRubyObject op_or(IRubyObject other) {
        ArrayList ary1 = new ArrayList(list);
        ArrayList ary2 = arrayValue(other).getList();
        ary1.addAll(ary2);
        return new RubyArray(getRuntime(), uniq(ary1));
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
    public IRubyObject sort_bang() {
        if (getLength() <= 1) {
            return getRuntime().getNil();
        }
        modify();
        setTmpLock(true);

        if (getRuntime().isBlockGiven()) {
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
            output.dumpObject((IRubyObject) iter.next());
        }
    }

    public static RubyArray unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyArray result = newArray(input.getRuntime());
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
    public RubyString pack(RubyString iFmt) {
        return Pack.pack(this.list, iFmt);
    }

    class BlockComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IRubyObject result = getRuntime().yield(RubyArray.newArray(getRuntime(), (IRubyObject) o1, (IRubyObject) o2));
            return (int) ((RubyNumeric) result).getLongValue();
        }

        public boolean equals(Object other) {
            return this == other;
        }
    }

    class DefaultComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IRubyObject obj1 = (IRubyObject) o1;
            IRubyObject obj2 = (IRubyObject) o2;
            if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                return (int) (RubyNumeric.fix2long(obj1) - RubyNumeric.fix2long(obj2));
            }

            if (o1 instanceof RubyString && o2 instanceof RubyString) {
                return RubyNumeric.fix2int(((RubyString) o1).op_cmp((IRubyObject) o2));
            }

            return RubyNumeric.fix2int(obj1.callMethod("<=>", obj2));
        }

        public boolean equals(Object other) {
            return this == other;
        }
    }
}
