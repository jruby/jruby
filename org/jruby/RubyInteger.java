/*
 * RubyInteger.java - No description
 * Created on 10. September 2001, 17:49
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
public abstract class RubyInteger extends RubyNumeric {

    public RubyInteger(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }
 
    public RubyString m_chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            // throw new RubyRangeException();
            // HACK +++
            throw new RuntimeException();
            // HACK ---
        }
        
        return RubyString.m_newString(getRuby(), new String(new char[]{(char)getLongValue()}));
    }
    
    public RubyObject m_downto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (((RubyBoolean)i.funcall(getRuby().intern("<"), to)).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric)i.funcall(getRuby().intern("-"), RubyFixnum.m_newFixnum(getRuby(), 1));
        }
        return this;
    }
    
    public RubyBoolean m_int_p() {
        return getRuby().getTrue();
    }
    
    public RubyObject m_step(RubyNumeric to, RubyNumeric step) {
        RubyNumeric i = this;
        if (step.getLongValue() == 0) {
            throw new RubyArgumentException("step cannot be 0");
        }
        
        RubyId cmp = getRuby().intern("<");
        if (((RubyBoolean)step.funcall(cmp, RubyFixnum.m_newFixnum(getRuby(), 0))).isFalse()) {
            cmp = getRuby().intern(">");
        }
        
        while (true) {
            if (((RubyBoolean)i.funcall(cmp, to)).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric)i.funcall(getRuby().intern("+"), step);
        }
        return this;
    }
    
    public RubyObject m_times() {
        RubyNumeric i = RubyFixnum.m_newFixnum(getRuby(), 0);
        while (true) {
            if (i.funcall(getRuby().intern("<"), this).isFalse()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric)i.funcall(getRuby().intern("+"), RubyFixnum.m_newFixnum(getRuby(), 1));
        }
        return this;
    }
    
    public RubyObject m_succ() {
        return funcall(getRuby().intern("+"), RubyFixnum.m_newFixnum(getRuby(), 1));
    }
    
    public RubyObject m_upto(RubyNumeric to) {
        RubyNumeric i = this;
        while (true) {
            if (i.funcall(getRuby().intern(">"), to).isTrue()) {
                break;
            }
            getRuby().yield(i);
            i = (RubyNumeric)i.funcall(getRuby().intern("+"), RubyFixnum.m_newFixnum(getRuby(), 1));
        }
        return this;
    }
    
    public RubyInteger m_to_i() {
        return this;
    }
}