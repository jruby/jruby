/*
 * RubyArray.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby;

import java.util.*;

import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 */
public class RubyArray extends RubyObject {
    private ArrayList array;
    private boolean tmpLock;

    public RubyArray(Ruby ruby) {
        this(ruby, null);
    }

    public RubyArray(Ruby ruby, List array) {
        super(ruby);
        this.array = new ArrayList(array);
    }
    
    public ArrayList getArray() {
        return array;
    }
    
    public void setArray(ArrayList array) {
        this.array = array;
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
    
    public long length() {
        return array.size();
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
            idx += array.size();
            if (idx < 0) {
                throw new RubyIndexException("index " + (idx - array.size()) + " out of array");
            }
        }
        if (idx > array.size()) {
            array.ensureCapacity((int)idx + 1);
            for (int i = array.size(); i < idx; i++) {
                array.add(getRuby().getNil());
            }
        }
        if (idx == array.size()) {
            array.add(value);
        }
        array.set((int)idx, value);
    }


    /** rb_ary_entry
     *
     */
    public RubyObject entry(long offset) {
        if (array.size() == 0) {
            return getRuby().getNil();
        }
        
        if (offset < 0) {
            offset += array.size();
        }
        
        if (offset < 0 || array.size() <= offset) {
            return getRuby().getNil();
        }
        return (RubyObject)array.get((int)offset);
    }

    
    /** rb_ary_push
     *
     */
    public RubyArray push(RubyObject item) {
        array.add(item);
        return this;
    }
    
    /** rb_ary_unshift
     *
     */
    public RubyArray unshift(RubyObject item) {
        modify();
        array.add(0, item);
                
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
        
        if (beg + len > array.size()) {
            len = array.size() - beg;
        }
        if (len < 0) {
            len = 0;
        }
        if (len == 0) {
            return m_newArray(getRuby());
        }
        
        RubyArray ary2 = m_newArray(getRuby(), (int)len);
        ary2.array.addAll(array.subList((int)beg, (int)(len + beg)));
        
        return ary2;
    }
    
    /**
     *
     
    public int length() {
        return array.size();
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
        return new RubyArray(ruby, new ArrayList(0));
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
    
    /** rb_ary_push_m
     *
     */
    public RubyArray m_push(RubyObject[] items) {
        if (items.length == 0) {
            throw new RubyArgumentException("wrong # of arguments(at least 1)");
        }
        
        for (int i = 0; i < items.length; i++) {
            array.add(items[i]);
        }
        return this;
    }

    /** rb_ary_pop
     *
     */
    public RubyObject m_pop() {
        modify();
        if (array.size() == 0) {
            return getRuby().getNil();
        }
        return (RubyObject)array.remove(array.size() - 1);
    }
    
    /** rb_ary_shift
     *
     */
    public RubyObject m_shift() {
        modify();
        if (array.size() == 0) {
            return getRuby().getNil();
        }
        
        return (RubyObject)array.remove(0);
    }
    
    /** rb_ary_unshift_m
     *
     */
    public RubyArray m_unshift(RubyObject[] items) {
        if (items.length == 0) {
            throw new RubyArgumentException("wrong # of arguments(at least 1)");
        }
        modify();
        array.addAll(0, Arrays.asList(items));
                
        return this;
    }
    
    /** rb_ary_includes
     *
     */
    public RubyBoolean m_includes(RubyObject item) {
        if (array.contains(item)) {
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
    public RubyObject m_initialize(RubyFixnum size, RubyObject value) {
        modify();
        
        long len = size.getValue();
        if (len < 0) {
            throw new RubyArgumentException("negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw new RubyArgumentException("array size too big");
        }
        Collections.fill(array, value);
        for (int i = array.size(); i < len; i++) {
            array.add(value);
        }
        return this;
    }

    /** rb_ary_aref
     *
     */
    public RubyObject m_slice(RubyObject[] args) {
        if (args.length == 2) {
            long beg = ((RubyFixnum)args[0]).getValue();
            long len = ((RubyFixnum)args[1]).getValue();
            if (beg < 0) {
                beg += array.size();
            }
            return subseq(beg, len);
        }
        if (args.length == 1) {
            if (args[0] instanceof RubyFixnum) {
                return entry(((RubyFixnum)args[0]).getValue());
            }
/*            if (args[0] instanceof RubyBignum) {
                throw new RubyIndexException("index too big");
            }
            if (args[0] instanceof RubyRange) {
                
            }*/
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
}