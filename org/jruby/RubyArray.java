/*
 * RubyArray.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyArray extends RubyObject {
    private ArrayList list;
    private boolean tmpLock;

    private RubyId equals;

    public RubyArray(Ruby ruby) {
        this(ruby, new ArrayList());
    }

    public RubyArray(Ruby ruby, List array) {
        this(ruby, new ArrayList(array), false);
    }
    
    public RubyArray(Ruby ruby, ArrayList array, boolean notCopy) {
        super(ruby, ruby.getRubyClass("Array"));
        this.list = array;
        equals = ruby.intern("==");
    }

    /** Getter for property list.
     * @return Value of property list.
     */
    public ArrayList getList() {
        return list;
    }
    
    public RubyObject[] toJavaArray() {
        return (RubyObject[])list.toArray(new RubyObject[length()]);
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
    
    public int length() {
        return list.size();
    }
    
    /** rb_ary_modify
     *
     */
    public void modify() {
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "Array");
        }
        if (isTmpLock()) {
            throw new RubyTypeException(getRuby(), "can't modify array during sort");
        }
        if (isTaint() && getRuby().getSecurityLevel() >= 4 ) {
            throw new RubySecurityException(getRuby(), "Insecure: can't modify array");
        }
    }

    /* if list's size is not at least 'toLength', add nil's until it is */
    private void autoExpand(long toLength) {
        list.ensureCapacity((int)toLength);
        for (int i = length(); i < toLength; i++) {
            list.add(getRuby().getNil());
        }
    }
    
    /** rb_ary_store
     *
     */
    public void store(long idx, RubyObject value) {
        modify();
        if (idx < 0) {
            idx += length();
            if (idx < 0) {
                throw new RubyIndexException(getRuby(), 
                    "index " + (idx - length()) + " out of array");
            }
        }
        autoExpand(idx + 1);
        list.set((int)idx, value);
    }


    /** rb_ary_entry
     *
     */
    public RubyObject entry(long offset) {
        if (length() == 0) {
            return getRuby().getNil();
        }
        
        if (offset < 0) {
            offset += length();
        }
        
        if (offset < 0 || length() <= offset) {
            return getRuby().getNil();
        }
        return (RubyObject)list.get((int)offset);
    }

    
    /** rb_ary_push
     *
     */
    public RubyArray push(RubyObject item) {
        list.add(item);
        return this;
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
        int length = length();

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
            return m_newArray(getRuby(), 0);
        }
        
        RubyArray ary2 = m_newArray(getRuby(), list.subList((int)beg, (int)(len + beg)));
        
        return ary2;
    }
    
    /** rb_ary_replace
     *
     */
    public void replace(long beg, long len, RubyObject repl) {
        int length = length();

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

        for (int i = 0; beg < length && i < len; i++) {
            list.remove((int)beg);
        }
        autoExpand(beg);
        if (repl instanceof RubyArray) {
            List repList = ((RubyArray)repl).getList();
            list.ensureCapacity(length + repList.size());
            list.addAll((int)beg, repList);
        } else if (!repl.isNil()) {
            list.add((int)beg, repl);
        }
    }
    
    /** to_ary
     *
     */
    public static RubyArray arrayValue(RubyObject other) {
        if (other instanceof RubyArray) {
            return (RubyArray)other;
        } else {
            try {
                return (RubyArray)other.convertType(RubyArray.class, "Array", "to_ary");
            } catch (Exception ex) {
                throw new RubyArgumentException(other.getRuby(), 
                    "can't convert arg to Array: " + ex.getMessage());
            }
        }
    }

    //
    // Methods of the Array Class (rb_ary_*):
    //
    
    /** rb_ary_new2
     *
     */
    public static RubyArray m_newArray(Ruby ruby, long len) {
        return new RubyArray(ruby, new ArrayList((int)len));
    }
    
    /** rb_ary_new
     *
     */
    public static RubyArray m_newArray(Ruby ruby) {
        /* Ruby arrays default to holding 16 elements, so we create an
         * ArrayList of the same size if we're not told otherwise
         */
        return new RubyArray(ruby, new ArrayList(16));
    }
    
    /** 
     *
     */
    public static RubyArray m_newArray(Ruby ruby, RubyObject obj) {
        return new RubyArray(ruby, Collections.singletonList(obj));
    }
    
    /** rb_assoc_new
     *
     */
    public static RubyArray m_newArray(Ruby ruby, RubyObject car, RubyObject cdr) {
        return new RubyArray(ruby, Arrays.asList(new RubyObject[] {car, cdr}));
    }
    
    public static RubyArray m_newArray(Ruby ruby, List list) {
        return new RubyArray(ruby, list);
    }

    /** rb_ary_s_new
     *
     */
    public static RubyArray m_new(Ruby ruby, RubyObject[] args) {
        RubyArray array = m_newArray(ruby);
        
        array.callInit(args);
        
        return array;
    }
    
    /** rb_ary_s_create
     *
     */
    public static RubyArray m_create(Ruby ruby, RubyObject[] args) {
        return m_newArray(ruby, Arrays.asList(args));
    }
    
    /** rb_ary_hash
     *
     */
    public RubyFixnum m_hash() {
        return new RubyFixnum(getRuby(), list.hashCode());
    }

    /** rb_ary_length
     *
     */
    public RubyFixnum m_length() {
        return new RubyFixnum(getRuby(), length());
    }

    /** rb_ary_push_m
     *
     */
    public RubyArray m_push(RubyObject[] items) {
        // Performance
        int length = items.length;

        if (length == 0) {
            throw new RubyArgumentException(getRuby(), "wrong # of arguments(at least 1)");
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
    
    public RubyArray m_push(RubyObject value) {
        modify();
        list.add(value);
        infectObject(value);
        return this;
    }

    /** rb_ary_pop
     *
     */
    public RubyObject m_pop() {
        modify();
        if (length() == 0) {
            return getRuby().getNil();
        }
        return (RubyObject)list.remove(length() - 1);
    }
    
    /** rb_ary_shift
     *
     */
    public RubyObject m_shift() {
        modify();
        if (length() == 0) {
            return getRuby().getNil();
        }
        
        return (RubyObject)list.remove(0);
    }
    
    /** rb_ary_unshift_m
     *
     */
    public RubyArray m_unshift(RubyObject[] items) {
        if (items.length == 0) {
            throw new RubyArgumentException(getRuby(), "wrong # of arguments(at least 1)");
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
    public RubyBoolean m_includes(RubyObject item) {
        for (int i = 0, n = length(); i < n; i++) {
            if (item.funcall(equals, entry(i)).isTrue()) {
                return getRuby().getTrue();
            }
        }
        return getRuby().getFalse();
    }
    
    /** rb_ary_frozen_p
     *
     */
    public RubyBoolean m_frozen() {
        return super.m_frozen().op_or(new RubyBoolean(getRuby(), isTmpLock()));
    }

    /** rb_ary_initialize
     *
     */
    public RubyObject m_initialize(RubyObject[] args) {
        int argc = argCount(args, 1, 2);
        long len = RubyNumeric.fix2long(args[0]);
        
        modify();
        
        if (len < 0) {
            throw new RubyArgumentException(getRuby(), "negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw new RubyArgumentException(getRuby(), "array size too big");
        }
        list = new ArrayList((int)len);
        if (len > 0) {
            RubyObject obj = (argc == 2) ? args[1] : (RubyObject)getRuby().getNil();
            Collections.fill(list, obj);
        }
        return this;
    }

    public RubyObject m_dup() {
        return m_aref(new RubyObject[] { RubyFixnum.zero(getRuby()), m_length() });
    }

    /** rb_ary_aref
     *
     */
    public RubyObject m_aref(RubyObject[] args) {
        int argc = argCount(args, 1, 2);
        if (argc == 2) {
            long beg = RubyNumeric.fix2long(args[0]);
            long len = RubyNumeric.fix2long(args[1]);
            if (beg < 0) {
                beg += length();
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
            long[] begLen = ((RubyRange)args[0]).getBeginLength(length(), true, false);
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
    public RubyObject m_aset(RubyObject[] args) {
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
            long[] begLen = ((RubyRange)args[0]).getBeginLength(length(), false, true);
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
    public RubyObject m_at(RubyFixnum pos) {
        return entry(pos.getValue());
    }
    
    /** rb_ary_concat
     *
     */
    public RubyArray m_concat(RubyObject obj) {
        modify();
        RubyArray other = arrayValue(obj);
        list.addAll(other.getList());
        infectObject(other);
        return this;
    }
    
    /** rb_ary_inspect
     *
     */
    public RubyString m_inspect() {
        // Performance
        int length = length();

        // HACK +++
        if (length == 0) {
            return RubyString.m_newString(getRuby(), "[]");
        }
        RubyString result = RubyString.m_newString(getRuby(), "[");
        
        for (int i = 0; i < length; i++)  {
            if (i > 0) {
                result.m_append(RubyString.m_newString(getRuby(), ", "));
            }
            result.m_append(entry(i).funcall(getRuby().intern("inspect")));
        }
        result.m_cat("]");
        return result;
        // HACK ---
    }

    /** rb_ary_first
     *
     */
    public RubyObject m_first() {
        if (length() == 0) {
            return getRuby().getNil();
        }
        return entry(0);
    }

    /** rb_ary_last
     *
     */
    public RubyObject m_last() {
        if (length() == 0) {
            return getRuby().getNil();
        }
        return entry(length() - 1);
    }
    
    /** rb_ary_each
     *
     */
    public RubyObject m_each() {
        for (int i = 0; i < length(); i++) {
            getRuby().yield(entry(i));
        }
        return this;
    }
    
    /** rb_ary_each_index
     *
     */
    public RubyObject m_each_index() {
        for (int i = 0; i < length(); i++) {
            getRuby().yield(RubyFixnum.m_newFixnum(getRuby(), i));
        }
        return this;
    }
    
    /** rb_ary_reverse_each
     *
     */
    public RubyObject m_reverse_each() {
        for (long i = length(); i > 0; i--) {
            getRuby().yield(entry(i-1));
        }
        return this;
    }
    
    /** rb_ary_join
     *
     */
    RubyString join(RubyString sep) {
        int length = length();
        if (length == 0) {
            RubyString.m_newString(getRuby(), "");
        }
        StringBuffer sbuf = new StringBuffer();
        boolean taint = isTaint() || sep.isTaint();
        RubyString str;
        RubyObject tmp = entry(0);
        taint |= tmp.isTaint();
        if (tmp instanceof RubyString) {
            str = (RubyString)tmp.m_dup();
        } else if (tmp instanceof RubyArray) {
            str = (RubyString)((RubyArray)tmp).join(sep);
        } else {
            str = RubyString.objAsString(getRuby(), tmp);
        }
        for (long i = 1; i < length; i++) {
            tmp = entry(i);
            taint |= tmp.isTaint();
            if (tmp instanceof RubyArray) {
                tmp = ((RubyArray)tmp).join(sep);
            } else if (!(tmp instanceof RubyString)) {
                tmp = RubyString.objAsString(getRuby(), tmp);
            }
            str.m_append(sep.op_plus(tmp));
        }
        str.setTaint(taint);
        return str;
    }
    
    /** rb_ary_join_m
     *
     */
    public RubyString m_join(RubyObject[] args) {
        int argc = argCount(args, 0, 1);
        RubyObject sep = (argc == 1)  ? args[0] : getRuby().getGlobalVar("$,");
        return join(sep.isNil() ? RubyString.m_newString(getRuby(), "") 
                                : RubyString.stringValue(sep));
    }
    
    /** rb_ary_to_s
     *
     */
    public RubyString m_to_s() {
        RubyObject sep = getRuby().getGlobalVar("$,");
        return join(sep.isNil() ? RubyString.m_newString(getRuby(), "") 
                                : RubyString.stringValue(sep));
    }
    
    /** rb_ary_to_a
     *
     */
    public RubyArray m_to_a() {
        return this;
    }
    
    /** rb_ary_equal
     *
     */
    public RubyBoolean m_equal(RubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            return getRuby().getFalse();
        }
        int length = length();

        RubyArray ary = (RubyArray)obj;
        if (length != ary.length()) {
            return getRuby().getFalse();
        }

        for (long i = 0; i < length; i++) {
            RubyBoolean result = (RubyBoolean)entry(i).funcall(equals, ary.entry(i));
            if (result.isFalse()) {
                return result;
            }
        }
        return getRuby().getTrue();
    }
    
    /** rb_ary_eql
     *
     */
    public RubyBoolean m_eql(RubyObject obj) {
        if (!(obj instanceof RubyArray)) {
            return getRuby().getFalse();
        }
        int length = length();

        RubyArray ary = (RubyArray)obj;
        if (length != ary.length()) {
            return getRuby().getFalse();
        }
        RubyId equals = getRuby().intern("eql?");
        for (long i = 0; i < length; i++) {
            RubyBoolean result = (RubyBoolean)entry(i).funcall(equals, ary.entry(i));
            if (result.isFalse()) {
                return result;
            }
        }
        return getRuby().getTrue();
    }
    
    /** rb_ary_compact_bang
     *
     */
    public RubyObject m_compact_bang() {
        modify();
        boolean changed = false;
        int length = length();
        ArrayList newList = new ArrayList(length);

        for (int i = 0; i < length; i++) {
            if (!entry(i).isNil()) {
                newList.add(entry(i));
            }
            else {
                changed = true;
             }
        }
        list = newList;
        return changed ? (RubyObject)this : (RubyObject)getRuby().getNil();
    }

    /** rb_ary_compact
     *
     */
    public RubyObject m_compact() {
        RubyArray ary = (RubyArray)m_dup();
        return ary.m_compact_bang();
    }

    /** rb_ary_empty_p
     *
     */
    public RubyObject m_empty_p() {
        return length() == 0 ? getRuby().getTrue() : getRuby().getFalse();
    }

    /** rb_ary_clear
     *
     */
    public RubyObject m_clear() {
        modify();
        list.clear();
        return this;
    }

    /** rb_ary_fill
     *
     */
    public RubyObject m_fill(RubyObject[] args) {
        int argc = argCount(args, 1, 3);
        int beg = 0;
        int len = length();
        switch (argc) {
            case 1:
                break;
            case 2:
                if (args[1] instanceof RubyRange) {
                    long[] begLen = ((RubyRange)args[1]).getBeginLength(len, false, true);
                    beg = (int)begLen[0];
                    len = (int)begLen[1];
                    break;
                }
                /* fall through */
            default:
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
    public RubyObject m_index(RubyObject obj) {
        for (int i = 0; i < length(); i++) {
            if (obj.funcall(equals, entry(i)).isTrue()) {
                return RubyFixnum.m_newFixnum(getRuby(), i);
            }
        }
        return getRuby().getNil();
    }
    
    /** rb_ary_rindex
     *
     */
    public RubyObject m_rindex(RubyObject obj) {
        for (int i = length()-1; i >= 0; i--) {
            if (obj.funcall(equals, entry(i)).isTrue()) {
                return RubyFixnum.m_newFixnum(getRuby(), i);
            }
        }
        return getRuby().getNil();
    }
    
    /** rb_ary_indexes
     *
     */
    public RubyArray m_indexes(RubyObject[] args) {
        RubyObject[] result = new RubyObject[args.length];
        boolean taint = false;
        for (int i = 0; i < args.length; i++) {
            result[i] = entry(RubyNumeric.fix2int(args[i]));
            taint |= result[i].isTaint();
        }
        RubyArray ary = m_create(getRuby(), result);
        ary.setTaint(taint);
        return ary;
    }
    
    /** rb_ary_clone
     *
     */
    public RubyObject m_clone() {
        RubyArray ary = m_newArray(getRuby(), list);
        ary.infectObject(this);
        return ary;
    }
    
    /** rb_ary_reverse_bang
     *
     */
    public RubyObject m_reverse_bang() {
        modify();
        Collections.reverse(list);
        return this;
    }
    
    /** rb_ary_reverse_m
     *
     */
    public RubyObject m_reverse() {
        RubyArray ary = (RubyArray)m_dup();
        ary.m_reverse_bang();
        return ary;
    }
    
    /** rb_ary_collect
     *
     */
    public RubyArray m_collect() {
        if (!getRuby().isBlockGiven()) {
            return (RubyArray)m_dup();
        }
        ArrayList ary = new ArrayList();
        for (int i = 0; i < length(); i++) {
            ary.add(getRuby().yield(entry(i)));
        }
        return new RubyArray(getRuby(), ary);
    }
    
    /** rb_ary_collect_bang
     *
     */
    public RubyArray m_collect_bang() {
        modify();
        for (int i = 0; i < length(); i++) {
            list.set(i, getRuby().yield(entry(i)));
        }
        return this;
    }
    
    /** rb_ary_delete
     *
     */
    public RubyObject m_delete(RubyObject obj) {
        modify();
        RubyObject retVal = getRuby().getNil();
        for (int i = length() - 1; i >= 0; i--) {
            if (obj.funcall(equals, entry(i)).isTrue()) {
                retVal = (RubyObject)list.remove(i);
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
    public RubyObject m_delete_at(RubyObject obj) {
        modify();
        int pos = (int)RubyNumeric.num2long(obj);
        int len = length();
        if (pos >= len) {
            return getRuby().getNil();
        }
        if (pos < 0 && (pos += len) < 0) {
            return getRuby().getNil();
        }
        return (RubyObject)list.remove(pos);
    }
    
    /** rb_ary_reject_bang
     *
     */
    public RubyObject m_reject_bang() {
        modify();
        RubyObject retVal = getRuby().getNil();
        for (int i = length() - 1; i >= 0; i--) {
            if (getRuby().yield(entry(i)).isTrue()) {
                retVal = (RubyObject)list.remove(i);
            }
        }
        return retVal.isNil() ? (RubyObject)retVal : (RubyObject)this;
    }
    
    /** rb_ary_delete_if
     *
     */
    public RubyObject m_delete_if() {
        m_reject_bang();
        return this;
    }
    
    /** rb_ary_replace
     *
     */
    public RubyObject m_replace(RubyObject other) {
        replace(0, length(), arrayValue(other));
        return this;
    }
    
    /** rb_ary_cmp
     *
     */
    public RubyObject op_cmp(RubyObject other) {
        RubyArray ary = arrayValue(other);
        int otherLen = ary.length();
        int len = length();
        int lesser = Math.min(len, otherLen);
        RubyFixnum result = RubyFixnum.zero(getRuby());
        for (int i = 0; i < lesser; i++) {
            result = (RubyFixnum)entry(i).funcall(getRuby().intern("<=>"), ary.entry(i));
            if (result.getValue() != 0) {
                return result;
            }
        }
        if (len != otherLen) {
            return (len > otherLen) ? RubyFixnum.one(getRuby())
                                    : RubyFixnum.minus_one(getRuby());
        }
        return result;
    }
    
    /** rb_ary_slice_bang
     *
     */
    public RubyObject m_slice_bang(RubyObject[] args) {
        int argc = argCount(args, 1, 2);
        RubyObject result = m_aref(args);
        if (argc == 2) {
            long beg = RubyNumeric.fix2long(args[0]);
            long len = RubyNumeric.fix2long(args[1]);
            replace(beg, len, getRuby().getNil());
        } else if ((args[0] instanceof RubyFixnum) && 
                   ((RubyFixnum)args[0]).getValue() < length()) {
            replace(RubyNumeric.fix2long(args[0]), 1, getRuby().getNil());
        } else if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange)args[0]).getBeginLength(length(), false, true);
            replace(begLen[0], begLen[1], getRuby().getNil());
        }
        return result;
    }

    /** rb_ary_assoc
     *
     */
    public RubyObject m_assoc(RubyObject arg) {
        int len = length();
        for (int i = 0; i < len; i++) {
            if (!((entry(i) instanceof RubyArray) && ((RubyArray)entry(i)).length() > 0)) {
                continue;
            }
            RubyArray ary = (RubyArray)entry(i);
            if (arg.funcall(equals, ary.entry(0)).isTrue()) {
                return ary;
            }
        }
        return getRuby().getNil();
    }
    
    /** rb_ary_rassoc
     *
     */
    public RubyObject m_rassoc(RubyObject arg) {
        int len = length();
        for (int i = 0; i < len; i++) {
            if (!((entry(i) instanceof RubyArray) && 
                  ((RubyArray)entry(i)).length() > 1)) {
                continue;
            }
            RubyArray ary = (RubyArray)entry(i);
            if (arg.funcall(equals, ary.entry(1)).isTrue()) {
                return ary;
            }
        }
        return getRuby().getNil();
    }

    private boolean flatten(ArrayList ary) {
        boolean mod = false;
        int len = ary.size();
        for (int i = len - 1; i >= 0; i--) {
            RubyObject obj = (RubyObject)ary.get(i);
            if (ary.get(i) instanceof RubyArray) {
                ArrayList ary2 = ((RubyArray)ary.remove(i)).getList();
                flatten(ary2);
                ary.addAll(i, ary2);
                mod = true;
            }
        }
        return mod;
    }
    
    /** rb_ary_flatten_bang
     *
     */
    public RubyObject m_flatten_bang() {
        modify();
        if (flatten(list)) {
            return this;
        }
        return getRuby().getNil();
    }
    
    /** rb_ary_flatten
     *
     */
    public RubyObject m_flatten() {
        RubyArray rubyArray = (RubyArray)m_dup();
        rubyArray.m_flatten_bang();
        return rubyArray;
    }
    
    /** rb_ary_nitems
     *
     */
    public RubyObject m_nitems() {
        int count = 0;
        for (int i = 0, len = length(); i < len; i++) {
            count += entry(i).isNil() ? 0 : 1;
        }
        return RubyFixnum.m_newFixnum(getRuby(), count);
    }
    
    /** rb_ary_plus
     *
     */
    public RubyObject op_plus(RubyObject other) {
        ArrayList otherList = arrayValue(other).getList();
        ArrayList newList = new ArrayList(length() + otherList.size());
        newList.addAll(list);
        newList.addAll(otherList);
        return new RubyArray(getRuby(), newList);
    }
    
    /** rb_ary_times
     *
     */
    public RubyObject op_times(RubyObject arg) {
        if (arg instanceof RubyString) {
            return join((RubyString)arg);
        }

        int len = (int)RubyNumeric.num2long(arg);
        if (len < 0) {
            throw new RubyArgumentException(getRuby(), "negative argument");
        }
        ArrayList newList = new ArrayList(length() * len);
        for (int i = 0; i < len; i++) {
            newList.addAll(list);
        }
        return new RubyArray(getRuby(), newList);
    }
    
    private ArrayList uniq(ArrayList ary1) {
        int len1 = ary1.size();
        ArrayList ary2 = new ArrayList(len1);
        int len2 = 0;
        boolean found = false;
        for (int i = 0; i < len1; i++) {
            RubyObject obj = (RubyObject)ary1.get(i);
            len2 = ary2.size();
            found = false;
            for (int j = 0; j < len2; j++) {
                if (obj.funcall(equals, (RubyObject)ary1.get(j)).isTrue()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ary2.add(obj);
            }                
        }
        ary2.trimToSize();
        return ary2;
    }
    
    /** rb_ary_uniq_bang
     *
     */
    public RubyObject m_uniq_bang() {
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
    public RubyObject m_uniq() {
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
            RubyObject obj = (RubyObject)ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.funcall(equals, (RubyObject)ary2.get(j)).isTrue()) {
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
            RubyObject obj = (RubyObject)ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.funcall(equals, (RubyObject)ary2.get(j)).isTrue()) {
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
    public RubyArray m_sort() {
        RubyArray rubyArray = (RubyArray)m_dup();
        rubyArray.m_sort_bang();
        return rubyArray;
    }
    
    /** rb_ary_sort_bang
     *
     */
    public RubyObject m_sort_bang() {
        if (length() <= 1) {
            return getRuby().getNil();
        }
        modify();
        setTmpLock(true);
        Collections.sort(list, getRuby().isBlockGiven()
                               ? (Comparator)new BlockComparator()
                               : (Comparator)new DefaultComparator());
        setTmpLock(false);
        return this;
    }

    class BlockComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            RubyObject result = getRuby().yield(RubyArray.m_newArray(getRuby(), 
                                                (RubyObject)o1, (RubyObject)o2));
            return (int)((RubyNumeric)result).getLongValue();
        }
        
        public boolean equals(Object other) {
            return this == other;
        }
    }

    class DefaultComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            RubyObject obj1 = (RubyObject)o1;
            RubyObject obj2 = (RubyObject)o2;
            if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                return (int)(RubyNumeric.fix2long(obj1) - RubyNumeric.fix2long(obj2));
            }
            
            if (o1 instanceof RubyString && o2 instanceof RubyString) {
                return RubyNumeric.fix2int(((RubyString)o1).op_cmp((RubyObject)o2));
            }
            
            return RubyNumeric.fix2int(obj1.funcall(getRuby().intern("<=>"), obj2));
        }
        
        public boolean equals(Object other) {
            return this == other;
        }
    }
}
