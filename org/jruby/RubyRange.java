/*
 * RubyRange.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyRange extends RubyObject {

    public RubyRange(Ruby ruby) {
        super(ruby, ruby.getRubyClass("Range"));
    }

    public void init(RubyObject begin, RubyObject end, RubyBoolean exclusive) {
        if (!(begin instanceof RubyFixnum && end instanceof RubyFixnum)) {
            try {
                begin.funcall(getRuby().intern("<=>"), end);
            } catch (RubyException rExcptn) {
                throw new RubyArgumentException("bad value for range");
            }
        }
        
        setInstanceVar("begin", begin);
        setInstanceVar("end", end);
        setInstanceVar("excl", exclusive);
    }
    
    /** rb_range_beg_len
     *
     */
    public long[] getBeginLength(long length) {
        long begin = ((RubyNumeric)getInstanceVar("begin")).getLongValue();
        long end = ((RubyNumeric)getInstanceVar("end")).getLongValue();
        end -= getInstanceVar("excl").isTrue() ? 1 : 0;
        
        if (begin < 0) {
            begin += length;
        }
        
        if (begin < 0 || begin > length) {
            return null;
        }
        
        if (end > length) {
            end = length;
        }
        
        if (end < 0) {
            end += length;
        }
        
        if (end < 0) {
            return null;
        }
        
        return new long[] {begin, end - begin};
    }
    
    // public Range methods
    
    public static RubyRange m_newRange(Ruby ruby, RubyObject begin, RubyObject end, boolean exclusive) {
        RubyRange range = new RubyRange(ruby);
        range.init(begin, end, exclusive ? ruby.getTrue(): ruby.getFalse());
        return range;
    }
    
    public RubyObject m_initialize(RubyObject[] args) {
        if (isInstanceVarDefined(getRuby().intern("begin"))) {
            throw new RubyNameException("'initialize' called twice.");
        }
        if (args.length == 3) {
            init(args[0], args[1], (RubyBoolean)args[2]);
        } else if (args.length == 2) {
            init(args[0], args[1], getRuby().getFalse());
        } else {
            throw new RubyArgumentException("Wrong arguments. (anObject, anObject, aBoolean = false) excepted");
        }
        return getRuby().getNil();
    }
    
    public RubyObject m_each() {
        RubyObject begin = getInstanceVar("begin");
        RubyObject end = getInstanceVar("end");
        boolean exclusive = getInstanceVar("excl").isTrue();
        
        if (begin instanceof RubyFixnum && end instanceof RubyFixnum) {
            long endLong = ((RubyNumeric)end).getLongValue();
            long i = ((RubyNumeric)begin).getLongValue();
            
            if (!exclusive) {
                endLong += 1;
            }
            
            for (; i < endLong; i++) {
                getRuby().yield(RubyFixnum.m_newFixnum(getRuby(), i));
            }
        } else if (begin instanceof RubyString) {
            // rb_str_upto(b, e, EXCL(range));
        } else if (begin.m_kind_of(getRuby().getNumericClass()).isTrue()) {
        /*
	b = rb_Integer(b);
	e = rb_Integer(e);

	if (!EXCL(range)) e = rb_funcall(e, '+', 1, INT2FIX(1));
	while (RTEST(rb_funcall(b, '<', 1, e))) {
	    rb_yield(b);
	    b = rb_funcall(b, '+', 1, INT2FIX(1));
	}*/
        } else {
            RubyObject v = begin;
            
            if (exclusive) {
                while (v.funcall(getRuby().intern("<"), end).isTrue()) {
                    if (v.m_equal(end).isTrue()) {
                        break;
                    }
                    getRuby().yield(v);
                    v = v.funcall(getRuby().intern("succ"));
                }
            } else {
                while (v.funcall(getRuby().intern("<="), end).isTrue()) {
                    getRuby().yield(v);
                    if (v.m_equal(end).isTrue()) {
                        break;
                    }
                    v = v.funcall(getRuby().intern("succ"));
                }
            }
        }
        
        return this;
    }
}