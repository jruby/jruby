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
    private RubyPointer list;
    private boolean tmpLock;

    public RubyArray(Ruby ruby) {
        this(ruby, null);
    }

    public RubyArray(Ruby ruby, List array) {
        super(ruby, ruby.getRubyClass("Array"));
        
        this.list = new RubyPointer(new ArrayList(array));
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
            idx += list.size();
            if (idx < 0) {
                throw new RubyIndexException("index " + (idx - list.size()) + " out of array");
            }
        } else if (idx > list.size()) {
            // list.ensureCapacity((int)idx + 1);
            for (int i = list.size(); i < idx; i++) {
                list.add(getRuby().getNil());
            }
        }
        if (idx == list.size()) {
            list.add(value);
        } else {
            list.set((int)idx, value);
        }
    }


    /** rb_ary_entry
     *
     */
    public RubyObject entry(long offset) {
        if (list.size() == 0) {
            return getRuby().getNil();
        }
        
        if (offset < 0) {
            offset += list.size();
        }
        
        if (offset < 0 || list.size() <= offset) {
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
    public RubyArray subseq(long beg, long len) {
        /*if (beg > array.size()) {
            return getRuby().getNil();
        }*/
        /* if (beg < 0 || len < 0) {
            return getRuby().getNil();
        }*/
        
        if (beg + len > list.size()) {
            len = list.size() - beg;
        }
        if (len < 0) {
            len = 0;
        }
        if (len == 0) {
            return m_newArray(getRuby());
        }
        
        RubyArray ary2 = m_newArray(getRuby(), list.subList((int)beg, (int)(len + beg)));
        
        return ary2;
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
        
        if (items.length == 0) {
            throw new RubyArgumentException("wrong # of arguments(at least 1)");
        }
        
        modify();
        
        for (int i = 0; i < length; i++) {
            list.add(items[i]);
        }
        return this;
    }
    
    public RubyArray m_push(RubyObject value) {
        modify();
        
        list.add(value);
        
        return this;
    }

    /** rb_ary_pop
     *
     */
    public RubyObject m_pop() {
        modify();
        if (list.size() == 0) {
            return getRuby().getNil();
        }
        return (RubyObject)list.remove(list.size() - 1);
    }
    
    /** rb_ary_shift
     *
     */
    public RubyObject m_shift() {
        modify();
        if (list.size() == 0) {
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
        list.addAll(0, Arrays.asList(items));
                
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
        if (args.length < 2) {
            return this;
        }
        
        RubyFixnum size = (RubyFixnum)args[0];
        
        modify();
        
        long len = size.getValue();
        if (len < 0) {
            throw new RubyArgumentException("negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw new RubyArgumentException("array size too big");
        }
        Collections.fill(list, args[1]);
        for (int i = list.size(); i < len; i++) {
            list.add(args[1]);
        }
        return this;
    }

    public RubyObject m_dup() {
      return m_slice(new RubyObject[] { new RubyFixnum(getRuby(), 0), m_length() });
    }

    /** rb_ary_aref
     *
     */
    public RubyObject m_slice(RubyObject[] args) {
        if (args.length == 2) {
            long beg = ((RubyFixnum)args[0]).getValue();
            long len = ((RubyFixnum)args[1]).getValue();
            if (beg < 0) {
                beg += list.size();
            }
            return subseq(beg, len);
        }
        if (args.length == 1) {
            if (args[0] instanceof RubyFixnum) {
                return entry(((RubyFixnum)args[0]).getValue());
            }
            if (args[0] instanceof RubyBignum) {
                throw new RubyIndexException("index too big");
            }
            if (args[0] instanceof RubyRange) {
                long[] begLength = ((RubyRange)args[0]).getBeginLength(length());
                if (begLength == null) {
                    return getRuby().getNil();
                } else {
                    return subseq(begLength[0], begLength[1]);
                }
            }
        }
        return getRuby().getNil();
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
        // obj.toArray();
        
        return this;
    }
    
    /** rb_ary_inspect
     *
     */
    public RubyString m_inspect() {
        // HACK +++
        StringBuffer sb = new StringBuffer(100);
        sb.append("[");
        
        // Performance
        int length = length();
        for (int i = 0; i < length; i++)  {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(((RubyString)entry(i).funcall(getRuby().intern("inspect"))).getValue());
        }
        sb.append("]");
        return RubyString.m_newString(getRuby(), sb.toString());
        // HACK ---
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
        
        Comparator blockComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                RubyObject result = getRuby().yield(RubyArray.m_newArray(getRuby(), 
                                                    (RubyObject)o1, (RubyObject)o2));
                return (int)((RubyNumeric)result).getLongValue();
            }
            
            public boolean equals(Object other) {
                return this == other;
            }
        };
        
        Comparator defaultComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
                    return (int)(((RubyFixnum)o1).getLongValue() - ((RubyFixnum)o2).getLongValue());
                }
                
                if (o1 instanceof RubyString && o2 instanceof RubyString) {
                    return (int)((RubyString)o1).op_cmp((RubyObject)o2).getLongValue();
                }
                
                return (int)((RubyFixnum)((RubyObject)o1).funcall(getRuby().intern("<=>"), (RubyObject)o2)).getLongValue();
            }
            
            public boolean equals(Object other) {
                return this == other;
            }
        };
        
        Collections.sort(list, getRuby().isBlockGiven() ? blockComparator : 
                                                          defaultComparator);
        
        setTmpLock(false);
        
        return this;
    }
    
    /** Getter for property list.
     * @return Value of property list.
     */
    public RubyPointer getList() {
        return list;
    }
}