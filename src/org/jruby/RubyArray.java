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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.ConversionIterator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.ArrayMetaClass;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Pack;
import org.jruby.util.collections.IdentitySet;

/**
 * The implementation of the built-in class Array in Ruby.
 */
public class RubyArray extends RubyObject implements List {
    private List list;
    private boolean tmpLock;

	private RubyArray(IRuby runtime, List list) {
		super(runtime, runtime.getClass("Array"));
        this.list = list;
    }

    /** Getter for property list.
     * @return Value of property list.
     */
    public List getList() {
        return list;
    }

    public IRubyObject[] toJavaArray() {
        return (IRubyObject[])list.toArray(new IRubyObject[getLength()]);
    }
    
    public RubyArray convertToArray() {
    	return this;
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
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0, n = getLength(); i < n; i++) {
            if (item.callMethod(context, "==", entry(i)).isTrue()) {
                return true;
            }
        }
        return false;
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(list.hashCode());
    }

    /** rb_ary_modify
     *
     */
    public void modify() {
    	testFrozen("Array");
        if (isTmpLock()) {
            throw getRuntime().newTypeError("can't modify array during sort");
        }
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify array");
        }
    }

    /* if list's size is not at least 'toLength', add nil's until it is */
    private void autoExpand(long toLength) {
        //list.ensureCapacity((int) toLength);
        for (int i = getLength(); i < toLength; i++) {
            list.add(getRuntime().getNil());
        }
    }

    /** rb_ary_store
     *
     */
    private IRubyObject store(long index, IRubyObject value) {
        modify();
        if (index < 0) {
            index += getLength();
            if (index < 0) {
                throw getRuntime().newIndexError("index " + (index - getLength()) + " out of array");
            }
        }
        autoExpand(index + 1);
        list.set((int) index, value);
        return value;
    }

    public IRubyObject entry(long offset) {
    	return entry(offset, false);
    }
    
    /** rb_ary_entry
     *
     */
    public IRubyObject entry(long offset, boolean throwException) {
        if (getLength() == 0) {
        	if (throwException) {
        		throw getRuntime().newIndexError("index " + offset + " out of array");
        	} 
        	return getRuntime().getNil();
        }
        if (offset < 0) {
            offset += getLength();
        }
        if (offset < 0 || getLength() <= offset) {
        	if (throwException) {
        		throw getRuntime().newIndexError("index " + offset + " out of array");
        	} 
            return getRuntime().getNil();
        }
        return (IRubyObject) list.get((int) offset);
    }
    
    public IRubyObject fetch(IRubyObject[] args) {
    	checkArgumentCount(args, 1, 2);

    	RubyInteger index = args[0].convertToInteger();
    	try {
    		return entry(index.getLongValue(), true);
    	} catch (RaiseException e) {
            ThreadContext tc = getRuntime().getCurrentContext();
    		// FIXME: use constant or method for IndexError lookup?
    		RubyException raisedException = e.getException();
    		if (raisedException.isKindOf(getRuntime().getClassFromPath("IndexError"))) {
	    		if (args.length > 1) {
	    			return args[1];
	    		} else if (tc.isBlockGiven()) {
	    			return tc.yield(index);
	    		}
    		}
    		
    		throw e;
    	}
    }
    
    public IRubyObject insert(IRubyObject[] args) {
    	checkArgumentCount(args, 1, -1);
    	// ruby does not bother to bounds check index, if no elements are to be added.
    	if (args.length == 1) {
    	    return this;
    	}
    	
    	// too negative of an offset will throw an IndexError
    	long offset = args[0].convertToInteger().getLongValue();
    	if (offset < 0 && getLength() + offset < -1) {
    		throw getRuntime().newIndexError("index " + 
    				(getLength() + offset) + " out of array");
    	}
    	
    	// An offset larger than the current length will pad with nils
    	// to length
    	if (offset > getLength()) {
    		long difference = offset - getLength();
    		IRubyObject nil = getRuntime().getNil();
    		for (long i = 0; i < difference; i++) {
    			list.add(nil);
    		}
    	}
    	
    	if (offset < 0) {
    		offset += getLength() + 1;
    	}
    	
    	for (int i = 1; i < args.length; i++) {
    		list.add((int) (offset + i - 1), args[i]);
    	}
    	
    	return this;
    }

    public RubyArray transpose() {
    	RubyArray newArray = getRuntime().newArray();
    	int length = getLength();
    	
    	if (length == 0) {
    		return newArray;
    	}

    	for (int i = 0; i < length; i++) {
    	    if (!(entry(i) instanceof RubyArray)) {
    		    throw getRuntime().newTypeError("Some error");
    	    }
    	}
    	
    	int width = ((RubyArray) entry(0)).getLength();

		for (int j = 0; j < width; j++) {
    		RubyArray columnArray = getRuntime().newArray(length);
    		
			for (int i = 0; i < length; i++) {
				try {
				    columnArray.append((IRubyObject) ((RubyArray) entry(i)).list.get(j));
				} catch (IndexOutOfBoundsException e) {
					throw getRuntime().newIndexError("element size differ (" + i +
							" should be " + width + ")");
				}
    		}
			
			newArray.append(columnArray);
    	}
    	
    	return newArray;
    }

    public IRubyObject values_at(IRubyObject[] args) {
    	RubyArray newArray = getRuntime().newArray();

    	for (int i = 0; i < args.length; i++) {
    		IRubyObject o = aref(new IRubyObject[] {args[i]});
    		if (args[i] instanceof RubyRange) {
    			if (o instanceof RubyArray) {
    				for (Iterator j = ((RubyArray) o).getList().iterator(); j.hasNext();) {
    					newArray.append((IRubyObject) j.next());
    				}
    			}
    		} else {
    			newArray.append(o);    			
    		}
    	}
    	return newArray;
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
        return len <= 0 ? getRuntime().newArray(0) :
        	getRuntime().newArray( 
        			new ArrayList(list.subList((int)beg, (int) (len + beg))));
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
            throw getRuntime().newIndexError("Negative array length: " + len);
        }
        if (beg < 0) {
            beg += length;
        }
        if (beg < 0) {
            throw getRuntime().newIndexError("Index out of bounds: " + beg);
        }

        modify();

        for (int i = 0; beg < getLength() && i < len; i++) {
            list.remove((int) beg);
        }
        autoExpand(beg);
        if (repl instanceof RubyArray) {
            List repList = ((RubyArray) repl).getList();
            //list.ensureCapacity(getLength() + repList.size());
            list.addAll((int) beg, new ArrayList(repList));
        } else if (!repl.isNil()) {
            list.add((int) beg, repl);
        }
    }

    private boolean flatten(List array) {
        return flatten(array, new IdentitySet(), null, -1);
    }

    private boolean flatten(List array, IdentitySet visited, List toModify, int index) {
        if (visited.contains(array)) {
            throw getRuntime().newArgumentError("tried to flatten recursive array");
        }
        visited.add(array);
        boolean isModified = false;
        for (int i = array.size() - 1; i >= 0; i--) {
            Object elem = array.get(i);
            if (elem instanceof RubyArray) {
                if (toModify == null) { // This is the array to flatten
                    array.remove(i);
                    flatten(((RubyArray) elem).getList(), visited, array, i);
                } else { // Sub-array, recurse
                    flatten(((RubyArray) elem).getList(), visited, toModify, index);
                }
                isModified = true;
            } else if (toModify != null) { // Add sub-list element to flattened array
                toModify.add(index, elem);
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
    public static final RubyArray newArray(final IRuby runtime, final long len) {
        return new RubyArray(runtime, new ArrayList((int) len));
    }

    /** rb_ary_new
     *
     */
    public static final RubyArray newArray(final IRuby runtime) {
        /* Ruby arrays default to holding 16 elements, so we create an
         * ArrayList of the same size if we're not told otherwise
         */
    	
        return new RubyArray(runtime, new ArrayList(16));
    }

    /**
     *
     */
    public static RubyArray newArray(IRuby runtime, IRubyObject obj) {
        ArrayList list = new ArrayList(1);
        list.add(obj);
        return new RubyArray(runtime, list);
    }

    /** rb_assoc_new
     *
     */
    public static RubyArray newArray(IRuby runtime, IRubyObject car, IRubyObject cdr) {
        ArrayList list = new ArrayList(2);
        list.add(car);
        list.add(cdr);
        return new RubyArray(runtime, list);
    }

    public static final RubyArray newArray(final IRuby runtime, final List list) {
        return new RubyArray(runtime, list);
    }

    public static RubyArray newArray(IRuby runtime, IRubyObject[] args) {
        final ArrayList list = new ArrayList(args.length);
        for (int i = 0; i < args.length; i++) {
            list.add(args[i]);
        }
        return new RubyArray(runtime, list);
    }

    /** rb_ary_length
     *
     */
    public RubyFixnum length() {
        return getRuntime().newFixnum(getLength());
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
        int length = getLength();
        return length == 0 ? getRuntime().getNil() : 
        	(IRubyObject) list.remove(length - 1);
    }

    /** rb_ary_shift
     *
     */
    public IRubyObject shift() {
        modify();
        return getLength() == 0 ? getRuntime().getNil() : 
        	(IRubyObject) list.remove(0);
    }

    /** rb_ary_unshift_m
     *
     */
    public RubyArray unshift(IRubyObject[] items) {
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
        return getRuntime().newBoolean(includes(item));
    }

    /** rb_ary_frozen_p
     *
     */
    public RubyBoolean frozen() {
        return getRuntime().newBoolean(isFrozen() || isTmpLock());
    }

    /** rb_ary_initialize
     */
    public IRubyObject initialize(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 0, 2);
        RubyArray arrayInitializer = null;
        long len = 0;
        if (argc > 0) {
        	if (args[0] instanceof RubyArray) {
        		arrayInitializer = (RubyArray)args[0];
        	} else {
        		len = convertToLong(args[0]);
        	}
        }

        modify();

        // Array initializer is provided
        if (arrayInitializer != null) {
        	list = new ArrayList(arrayInitializer.list);
        	return this;
        }
        
        // otherwise, continue with Array.new(fixnum, obj)
        if (len < 0) {
            throw getRuntime().newArgumentError("negative array size");
        }
        if (len > Integer.MAX_VALUE) {
            throw getRuntime().newArgumentError("array size too big");
        }
        list = new ArrayList((int) len);
        ThreadContext tc = getRuntime().getCurrentContext();
        if (len > 0) {
        	if (tc.isBlockGiven()) {
        		// handle block-based array initialization
                for (int i = 0; i < len; i++) {
                    list.add(tc.yield(new RubyFixnum(getRuntime(), i)));
                }
        	} else {
        		IRubyObject obj = (argc == 2) ? args[1] : getRuntime().getNil();
        		list.addAll(Collections.nCopies((int)len, obj));
        	}
        }
        return this;
    }

    /** rb_ary_aref
     */
    public IRubyObject aref(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 1, 2);
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
            throw getRuntime().newIndexError("index too big");
        }
        if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), true, false);

            return begLen == null ? newArray(getRuntime()) : subseq(begLen[0], begLen[1]);
        }
        if(args[0] instanceof RubySymbol) {
            throw getRuntime().newTypeError("Symbol as array index");
        }
        return entry(args[0].convertToInteger().getLongValue());
    }

    /** rb_ary_aset
     *
     */
    public IRubyObject aset(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 2, 3);
        if (argc == 3) {
            long beg = args[0].convertToInteger().getLongValue();
            long len = args[1].convertToInteger().getLongValue();
            replace(beg, len, args[2]);
            return args[2];
        }
        if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).getBeginLength(getLength(), false, true);
            replace(begLen[0], begLen[1], args[1]);
            return args[1];
        }
        if (args[0] instanceof RubyBignum) {
            throw getRuntime().newIndexError("Index too large");
        }
        return store(args[0].convertToInteger().getLongValue(), args[1]);
    }

    /** rb_ary_at
     *
     */
    public IRubyObject at(IRubyObject pos) {
        return entry(convertToLong(pos));
    }

	private long convertToLong(IRubyObject pos) {
		if (pos instanceof RubyNumeric) {
			return ((RubyNumeric) pos).getLongValue();
		}
		throw getRuntime().newTypeError("cannot convert " + pos.getType().getBaseName() + " to Integer");
	}

	/** rb_ary_concat
     *
     */
    public RubyArray concat(IRubyObject obj) {
        modify();
        RubyArray other = obj.convertToArray();
        list.addAll(other.getList());
        infectBy(other);
        return this;
    }

    /** rb_ary_inspect
     *
     */
    public IRubyObject inspect() {
        int length = getLength();

        if (length == 0) {
            return getRuntime().newString("[]");
        }
        RubyString result = getRuntime().newString("[");
        RubyString separator = getRuntime().newString(", ");
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                result.append(separator);
            }
            result.append(entry(i).callMethod(context, "inspect"));
        }
        result.cat("]");
        return result;
    }

    /** rb_ary_first
     *
     */
    public IRubyObject first(IRubyObject[] args) {
    	checkArgumentCount(args, 0, 1);

    	if (args.length == 0) {
    		return getLength() == 0 ? getRuntime().getNil() : entry(0);
    	}
    	
    	// TODO: See if enough integer-only conversions to make this
    	// convenience function (which could replace RubyNumeric#fix2long).
    	if (!(args[0] instanceof RubyInteger)) {
            throw getRuntime().newTypeError("Cannot convert " + 
            		args[0].getType() + " into Integer");
    	}
    	
    	long length = ((RubyInteger)args[0]).getLongValue();
    	
    	if (length < 0) {
    		throw getRuntime().newArgumentError(
    				"negative array size (or size too big)");
    	}
    	
    	return subseq(0, length);
    }

    /** rb_ary_last
     *
     */
    public IRubyObject last(IRubyObject[] args) {
        int count = checkArgumentCount(args, 0, 1);
    	int length = getLength();
    	
    	int listSize = list.size();
    	int sublistSize = 0;
    	int startIndex = 0;
    		
    	switch (count) {
        case 0:
            return length == 0 ? getRuntime().getNil() : entry(length - 1);
        case 1:
            sublistSize = RubyNumeric.fix2int(args[0]);
            if (sublistSize == 0) {
                return getRuntime().newArray();
            }
            if (sublistSize < 0) {
                throw getRuntime().newArgumentError("negative array size (or size too big)");
            }

            startIndex = (sublistSize > listSize) ? 0 : listSize - sublistSize;
            return getRuntime().newArray(list.subList(startIndex, listSize));
        default:
            assert false;
        	return null;
        }
    }

    /** rb_ary_each
     *
     */
    public IRubyObject each() {
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0, len = getLength(); i < len; i++) {
            context.yield(entry(i));
        }
        return this;
    }

    /** rb_ary_each_index
     *
     */
    public IRubyObject each_index() {
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0, len = getLength(); i < len; i++) {
            context.yield(getRuntime().newFixnum(i));
        }
        return this;
    }

    /** rb_ary_reverse_each
     *
     */
    public IRubyObject reverse_each() {
        ThreadContext context = getRuntime().getCurrentContext();
        for (long i = getLength(); i > 0; i--) {
            context.yield(entry(i - 1));
        }
        return this;
    }

    /** rb_ary_join
     *
     */
    public RubyString join(RubyString sep) {
        StringBuffer buf = new StringBuffer();
        int length = getLength();
        if (length == 0) {
            getRuntime().newString("");
        }
        boolean taint = isTaint() || sep.isTaint();
        RubyString str;
        IRubyObject tmp = null;
        for (long i = 0; i < length; i++) {
            tmp = entry(i);
            taint |= tmp.isTaint();
            if (tmp instanceof RubyString) {
                // do nothing
            } else if (tmp instanceof RubyArray) {
                tmp = ((RubyArray) tmp).join(sep);
            } else {
                tmp = RubyString.objAsString(tmp);
            }
            
            if (i > 0 && !sep.isNil()) {
                buf.append(sep.toString());
            }
            buf.append(((RubyString)tmp).toString());
        }
        str = RubyString.newString(getRuntime(), buf.toString());
        str.setTaint(taint);
        return str;
    }

    /** rb_ary_join_m
     *
     */
    public RubyString join(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 0, 1);
        IRubyObject sep = (argc == 1) ? args[0] : getRuntime().getGlobalVariables().get("$,");
        return join(sep.isNil() ? getRuntime().newString("") : RubyString.stringValue(sep));
    }

    /** rb_ary_to_s
     *
     */
    public IRubyObject to_s() {
        IRubyObject separatorObject = getRuntime().getGlobalVariables().get("$,");
        RubyString separator;
        if (separatorObject.isNil()) {
            separator = getRuntime().newString("");
        } else {
            separator = RubyString.stringValue(separatorObject);
        }
        return join(separator);
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

    /** rb_ary_equal
     *
     */
    public IRubyObject array_op_equal(IRubyObject obj) {
        if (this == obj) {
            return getRuntime().getTrue();
        }

        RubyArray ary;
        
        if (!(obj instanceof RubyArray)) {
            if (obj.respondsTo("to_ary")) {
                ary = obj.convertToArray();
            } else {
                return getRuntime().getFalse();
            }
        } else {
        	ary = (RubyArray) obj;
        }
        
        int length = getLength();

        if (length != ary.getLength()) {
            return getRuntime().getFalse();
        }

        for (long i = 0; i < length; i++) {
            if (!entry(i).callMethod(getRuntime().getCurrentContext(), "==", ary.entry(i)).isTrue()) {
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
        
        ThreadContext context = getRuntime().getCurrentContext();

        for (long i = 0; i < length; i++) {
            if (!entry(i).callMethod(context, "eql?", ary.entry(i)).isTrue()) {
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
        boolean isChanged = false;
        for (int i = getLength() - 1; i >= 0; i--) {
            if (entry(i).isNil()) {
                list.remove(i);
                isChanged = true;
            }
        }
        return isChanged ? (IRubyObject) this : (IRubyObject) getRuntime().getNil();
    }

    /** rb_ary_compact
     *
     */
    public IRubyObject compact() {
        RubyArray ary = (RubyArray) dup();
        ary.compact_bang();
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
    public IRubyObject rb_clear() {
        modify();
        list.clear();
        return this;
    }

    /** rb_ary_fill
     *
     */
    public IRubyObject fill(IRubyObject[] args) {
        int beg = 0;
        int len = getLength();
        int argc;
        IRubyObject filObj;
        IRubyObject begObj;
        IRubyObject lenObj;
        IRuby runtime = getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        if (tc.isBlockGiven()) {
        	argc = checkArgumentCount(args, 0, 2);
        	filObj = null;
        	begObj = argc > 0 ? args[0] : null;
        	lenObj = argc > 1 ? args[1] : null;
        	argc++;
        } else {
        	argc = checkArgumentCount(args, 1, 3);
        	filObj = args[0];
        	begObj = argc > 1 ? args[1] : null;
        	lenObj = argc > 2 ? args[2] : null;
        }
        switch (argc) {
            case 1 :
                break;
            case 2 :
                if (begObj instanceof RubyRange) {
                    long[] begLen = ((RubyRange) begObj).getBeginLength(len, false, true);
                    beg = (int) begLen[0];
                    len = (int) begLen[1];
                    break;
                }
                /* fall through */
            default :
                beg = begObj.isNil() ? beg : RubyNumeric.fix2int(begObj);
                if (beg < 0 && (beg += len) < 0) {
                    throw getRuntime().newIndexError("Negative array index");
                }
                len -= beg;
                if (argc == 3 && !lenObj.isNil()) {
                    len = RubyNumeric.fix2int(lenObj);
                }
        }

        modify();
        autoExpand(beg + len);
        for (int i = beg; i < beg + len; i++) {
        	if (filObj == null) {
        		list.set(i, tc.yield(runtime.newFixnum(i)));
        	} else {
        		list.set(i, filObj);
        	}
        }
        return this;
    }

    /** rb_ary_index
     *
     */
    public IRubyObject index(IRubyObject obj) {
        ThreadContext context = getRuntime().getCurrentContext();
        int len = getLength();
        for (int i = 0; i < len; i++) {
            if (entry(i).callMethod(context, "==", obj).isTrue()) {
                return getRuntime().newFixnum(i);
            }
        }
        return getRuntime().getNil();
    }

    /** rb_ary_rindex
     *
     */
    public IRubyObject rindex(IRubyObject obj) {
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = getLength() - 1; i >= 0; i--) {
            if (entry(i).callMethod(context, "==", obj).isTrue()) {
                return getRuntime().newFixnum(i);
            }
        }
        return getRuntime().getNil();
    }

    public IRubyObject indices(IRubyObject[] args) {
        IRubyObject[] result = new IRubyObject[args.length];
        boolean taint = false;
        for (int i = 0; i < args.length; i++) {
            result[i] = entry(RubyNumeric.fix2int(args[i]));
            taint |= result[i].isTaint();
        }
        IRubyObject ary = ((ArrayMetaClass) getMetaClass()).create(result);
        ary.setTaint(taint);
        return ary;
    }

    /** rb_ary_clone
     *
     */
    public IRubyObject rbClone() {
        RubyArray result = getRuntime().newArray(new ArrayList(list));
        result.setTaint(isTaint());
        result.initCopy(this);
        result.setFrozen(isFrozen());
        return result;
    }

    /** rb_ary_reverse_bang
     *
     */
    public IRubyObject reverse_bang() {
        modify();
        Collections.reverse(list);
        return this;
    }

    /** rb_ary_reverse_m
     *
     */
    public IRubyObject reverse() {
        RubyArray result = (RubyArray) dup();
        result.reverse_bang();
        return result;
    }

    /** rb_ary_collect
     *
     */
    public RubyArray collect() {
        ThreadContext tc = getRuntime().getCurrentContext();
        if (!tc.isBlockGiven()) {
            return (RubyArray) dup();
        }
        ArrayList ary = new ArrayList();
        for (int i = 0, len = getLength(); i < len; i++) {
            ary.add(tc.yield(entry(i)));
        }
        return new RubyArray(getRuntime(), ary);
    }

    /** rb_ary_collect_bang
     *
     */
    public RubyArray collect_bang() {
        modify();
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = 0, len = getLength(); i < len; i++) {
            list.set(i, context.yield(entry(i)));
        }
        return this;
    }

    /** rb_ary_delete
     *
     */
    public IRubyObject delete(IRubyObject obj) {
        modify();
        ThreadContext tc = getRuntime().getCurrentContext();
        IRubyObject result = getRuntime().getNil();
        for (int i = getLength() - 1; i >= 0; i--) {
            if (obj.callMethod(tc, "==", entry(i)).isTrue()) {
                result = (IRubyObject) list.remove(i);
            }
        }
        if (result.isNil() && tc.isBlockGiven()) {
            result = tc.yield(entry(0));
        }
        return result;
    }

    /** rb_ary_delete_at
     *
     */
    public IRubyObject delete_at(IRubyObject obj) {
        modify();
        int pos = (int) obj.convertToInteger().getLongValue();
        int len = getLength();
        if (pos >= len) {
            return getRuntime().getNil();
        }
        
        return pos < 0 && (pos += len) < 0 ?
            getRuntime().getNil() : (IRubyObject) list.remove(pos);
    }

    /** rb_ary_reject_bang
     *
     */
    public IRubyObject reject_bang() {
        modify();
        IRubyObject retVal = getRuntime().getNil();
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i = getLength() - 1; i >= 0; i--) {
            if (context.yield(entry(i)).isTrue()) {
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
        replace(0, getLength(), other.convertToArray());
        return this;
    }

    /** rb_ary_cmp
     *
     */
    public IRubyObject op_cmp(IRubyObject other) {
        RubyArray ary = other.convertToArray();
        int otherLen = ary.getLength();
        int len = getLength();
        int minCommon = Math.min(len, otherLen);
        ThreadContext context = getRuntime().getCurrentContext();
        RubyClass fixnumClass = getRuntime().getClass("Fixnum");
        for (int i = 0; i < minCommon; i++) {
        	IRubyObject result = entry(i).callMethod(context, "<=>", ary.entry(i));
            if (! result.isKindOf(fixnumClass) || RubyFixnum.fix2int(result) != 0) {
                return result;
            }
        }
        if (len != otherLen) {
            return len < otherLen ? RubyFixnum.minus_one(getRuntime()) : RubyFixnum.one(getRuntime());
        }
        return RubyFixnum.zero(getRuntime());
    }

    /** rb_ary_slice_bang
     *
     */
    public IRubyObject slice_bang(IRubyObject[] args) {
        int argc = checkArgumentCount(args, 1, 2);
        IRubyObject result = aref(args);
        if (argc == 2) {
            long beg = RubyNumeric.fix2long(args[0]);
            long len = RubyNumeric.fix2long(args[1]);
            replace(beg, len, getRuntime().getNil());
        } else if (args[0] instanceof RubyFixnum && RubyNumeric.fix2long(args[0]) < getLength()) {
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
            if (!(entry(i) instanceof RubyArray && ((RubyArray) entry(i)).getLength() > 0)) {
                continue;
            }
            RubyArray ary = (RubyArray) entry(i);
            if (arg.callMethod(getRuntime().getCurrentContext(), "==", ary.entry(0)).isTrue()) {
                return ary;
            }
        }
        return getRuntime().getNil();
    }

    /** rb_ary_rassoc
     *
     */
    public IRubyObject rassoc(IRubyObject arg) {
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (int i = 0, len = getLength(); i < len; i++) {
            if (!(entry(i) instanceof RubyArray && ((RubyArray) entry(i)).getLength() > 1)) {
                continue;
            }
            RubyArray ary = (RubyArray) entry(i);
            if (arg.callMethod(context, "==", ary.entry(1)).isTrue()) {
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
        return flatten(list) ? this : getRuntime().getNil();
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
        return getRuntime().newFixnum(count);
    }

    /** rb_ary_plus
     *
     */
    public IRubyObject op_plus(IRubyObject other) {
        List otherList = other.convertToArray().getList();
        List newList = new ArrayList(getLength() + otherList.size());
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

        int len = (int) arg.convertToInteger().getLongValue();
        if (len < 0) {
            throw getRuntime().newArgumentError("negative argument");
        }
        ArrayList newList = new ArrayList(getLength() * len);
        for (int i = 0; i < len; i++) {
            newList.addAll(list);
        }
        return new RubyArray(getRuntime(), newList);
    }

    private static ArrayList uniq(List oldList) {
        ArrayList newList = new ArrayList(oldList.size());
        Set passed = new HashSet(oldList.size());

        for (Iterator iter = oldList.iterator(); iter.hasNext();) {
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
        List ary1 = new ArrayList(list);
        List ary2 = other.convertToArray().getList();
        int len2 = ary2.size();
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (int i = ary1.size() - 1; i >= 0; i--) {
            IRubyObject obj = (IRubyObject) ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.callMethod(context, "==", (IRubyObject) ary2.get(j)).isTrue()) {
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
    	RubyClass arrayClass = getRuntime().getClass("Array");
    	
    	// & only works with array types
    	if (!other.isKindOf(arrayClass)) {
    		throw getRuntime().newTypeError(other, arrayClass);
    	}
        List ary1 = uniq(list);
        int len1 = ary1.size();
        List ary2 = other.convertToArray().getList();
        int len2 = ary2.size();
        ArrayList ary3 = new ArrayList(len1);
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (int i = 0; i < len1; i++) {
            IRubyObject obj = (IRubyObject) ary1.get(i);
            for (int j = 0; j < len2; j++) {
                if (obj.callMethod(context, "eql?", (IRubyObject) ary2.get(j)).isTrue()) {
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
        List newArray = new ArrayList(list);
        
        newArray.addAll(other.convertToArray().getList());
        
        return new RubyArray(getRuntime(), uniq(newArray));
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
        modify();
        setTmpLock(true);

        Comparator comparator;
        if (getRuntime().getCurrentContext().isBlockGiven()) {
            comparator = new BlockComparator();
        } else {
            comparator = new DefaultComparator();
        }
        Collections.sort(list, comparator);

        setTmpLock(false);
        return this;
    }

    public void marshalTo(MarshalStream output) throws IOException {
        output.write('[');
        output.dumpInt(getList().size());
        for (Iterator iter = getList().iterator(); iter.hasNext(); ) {
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
        return Pack.pack(this.list, iFmt);
    }

    class BlockComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IRubyObject result = getRuntime().getCurrentContext().yieldCurrentBlock(getRuntime().newArray((IRubyObject) o1, (IRubyObject) o2), null, null, true);
            return (int) ((RubyNumeric) result).getLongValue();
        }
    }

    static class DefaultComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IRubyObject obj1 = (IRubyObject) o1;
            IRubyObject obj2 = (IRubyObject) o2;
            if (o1 instanceof RubyFixnum && o2 instanceof RubyFixnum) {
            	long diff = RubyNumeric.fix2long(obj1) - RubyNumeric.fix2long(obj2);

            	return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }

            if (o1 instanceof RubyString && o2 instanceof RubyString) {
                StringMetaClass stringMC = (StringMetaClass)((RubyObject)o1).getMetaClass();
                return RubyNumeric.fix2int(
                        stringMC.op_cmp.call(stringMC.getRuntime().getCurrentContext(), (RubyString)o1, stringMC, "<=>", new IRubyObject[] {(RubyString)o2}, false));
            }

            return RubyNumeric.fix2int(obj1.callMethod(obj1.getRuntime().getCurrentContext(), "<=>", obj2));
        }
    }


    public Class getJavaClass() {
        return List.class;
    }
    
    // Satisfy java.util.List interface (for Java integration)
    
	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Object element) {
		return list.contains(JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public Iterator iterator() {
		return new ConversionIterator(list.iterator());
	}

	public Object[] toArray() {
		Object[] array = new Object[getLength()];
		Iterator iter = iterator();
		
		for (int i = 0; iter.hasNext(); i++) {
			array[i] = iter.next();
		}

		return array;
	}

	public Object[] toArray(final Object[] arg) {
        Object[] array = arg;
        int length = getLength();
            
        if(array.length < length) {
            Class type = array.getClass().getComponentType();
            array = (Object[])Array.newInstance(type, length);
        }

        Iterator iter = iterator();
        for (int i = 0; iter.hasNext(); i++) {
            array[i] = iter.next();
        }
        
        return array;
	}

	public boolean add(Object element) {
		return list.add(JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public boolean remove(Object element) {
		return list.remove(JavaUtil.convertJavaToRuby(getRuntime(), element));
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
		for (Iterator iter = c.iterator(); iter.hasNext(); ) {
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
		boolean changed = false;
		
		for (Iterator iter = c.iterator(); iter.hasNext();) {
			if (remove(iter.next())) {
				changed = true;
			}
		}

		return changed;
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
		return JavaUtil.convertRubyToJava((IRubyObject) list.get(index), Object.class);
	}

	public Object set(int index, Object element) {
		return list.set(index, JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public void add(int index, Object element) {
		list.add(index, JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public Object remove(int index) {
		return JavaUtil.convertRubyToJava((IRubyObject) list.remove(index), Object.class);
	}

	public int indexOf(Object element) {
		return list.indexOf(JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public int lastIndexOf(Object element) {
		return list.lastIndexOf(JavaUtil.convertJavaToRuby(getRuntime(), element));
	}

	public ListIterator listIterator() {
		return new ConversionListIterator(list.listIterator());
	}

	public ListIterator listIterator(int index) {
		return new ConversionListIterator(list.listIterator(index));
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
		list.clear();
	}

	class ConversionListIterator implements ListIterator {
		private ListIterator iterator;

		public ConversionListIterator(ListIterator iterator) {
			this.iterator = iterator;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public Object next() {
			return JavaUtil.convertRubyToJava((IRubyObject) iterator.next(), Object.class);
		}

		public boolean hasPrevious() {
			return iterator.hasPrevious();
		}

		public Object previous() {
			return JavaUtil.convertRubyToJava((IRubyObject) iterator.previous(), Object.class);
		}

		public int nextIndex() {
			return iterator.nextIndex();
		}

		public int previousIndex() {
			return iterator.previousIndex();
		}

		public void remove() {
			iterator.remove();
		}

		public void set(Object arg0) {
			// TODO Auto-generated method stub
		}

		public void add(Object arg0) {
			// TODO Auto-generated method stub
		}
	}
}
