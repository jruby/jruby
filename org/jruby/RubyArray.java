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

    public RubyArray(Ruby ruby) {
        this(ruby, new ArrayList());
    }

    public RubyArray(Ruby ruby, List array) {
        super(ruby, ruby.getRubyClass("Array"));
        
        this.list = new ArrayList(array);
    }
    
    /** Getter for property list.
     * @return Value of property list.
     */
    public List getList() {
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
            throw new RubyFrozenException("Array");
        }
        if (isTmpLock()) {
            throw new RubyTypeException("can't modify array during sort");
        }
        if (isTaint() && getRuby().getSecurityLevel() >= 4 ) {
            throw new RubySecurityException("Insecure: can't modify array");
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
                throw new RubyIndexException("index " + (idx - length()) + " out of array");
            }
        } else if (idx > length()) {
            list.ensureCapacity((int)idx + 1);
            for (int i = length(); i < idx; i++) {
                list.add(getRuby().getNil());
            }
        }
        if (idx == length()) {
            list.add(value);
        } else {
            list.set((int)idx, value);
        }
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
        if (beg > length()) {
            return getRuby().getNil();
        }
        if (beg < 0 || len < 0) {
            return getRuby().getNil();
        }
        
        if (beg + len > length()) {
            len = length() - beg;
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
        if (len < 0) {
            throw new RubyIndexException("Negative array length: " + len);
        }
        if (beg < 0) {
            beg += length();
        }
        if (beg < 0) {
            throw new RubyIndexException("Index out of bounds: " + beg);
        }

        modify();

        for (int i = 0; beg < length() && i < len; i++) {
            list.remove((int)beg);
        }
        if (beg > length()) {
            list.ensureCapacity((int)beg + 1);
            for (int i = length(); i < beg; i++) {
                list.add(getRuby().getNil());
            }
        }
        if (repl instanceof RubyArray) {
            List repList = ((RubyArray)repl).getList();
            list.ensureCapacity(length() + repList.size());
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
                throw new RubyArgumentException("can't convert arg to Array: " + ex.getMessage());
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
            throw new RubyArgumentException("wrong # of arguments(at least 1)");
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
            throw new RubyArgumentException("wrong # of arguments(at least 1)");
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
        if (list.contains(item)) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
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
            throw new RubyArgumentException("negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw new RubyArgumentException("array size too big");
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
            throw new RubyIndexException("index too big");
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
            throw new RubyIndexException("Index too large");
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
        // HACK +++
        if (length() == 0) {
            return RubyString.m_newString(getRuby(), "[]");
        }
        RubyString result = RubyString.m_newString(getRuby(), "[");
        
        // Performance
        int length = length();
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
        if (length() == 0) {
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
        for (long i = 1; i < length(); i++) {
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
        RubyArray ary = (RubyArray)obj;
        if (length() != ary.length()) {
            return getRuby().getFalse();
        }
        RubyId equals = getRuby().intern("==");
        for (long i = 0; i < length(); i++) {
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
        RubyArray ary = (RubyArray)obj;
        if (length() != ary.length()) {
            return getRuby().getFalse();
        }
        RubyId equals = getRuby().intern("eql?");
        for (long i = 0; i < length(); i++) {
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
        for (int i = 0; i < length(); i++) {
            if (entry(i).isNil()) {
                list.remove(i);
                changed = true;
            }
        }
        return changed ? (RubyObject)this : (RubyObject)getRuby().getNil();
    }

    /** rb_ary_compact
     *
     */
    public RubyObject m_compact() {
        RubyArray ary = (RubyArray)m_dup();
        return ary.m_compact_bang();
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
        Collections.sort(getList(), getRuby().isBlockGiven()
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
