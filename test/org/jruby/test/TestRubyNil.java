/*
 * RubyNil.java - No description
 * Created on 28. Nov 2001, 15:18
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
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
package org.jruby.test;

import java.util.ArrayList;
import junit.framework.TestCase;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.RubyFixnum;

/**
* @author chadfowler
*/
public class TestRubyNil extends TestCase {

    private Ruby ruby;
    private IRubyObject rubyNil;

    public TestRubyNil(String name) {
        super(name);
    } 
    
    public void setUp() {
        ruby = Ruby.getDefaultInstance();
        rubyNil = ruby.getNil();
    }
    
    public void testIsNil() {
        assertTrue(rubyNil.isNil());
    }

    public void testIsFalseOrTrue() {
        assertTrue(!rubyNil.isTrue());
    }

    public void testToI() {
        assertEquals(RubyFixnum.zero(ruby), RubyNil.to_i(rubyNil));
    }

    public void testToS() {
        assertEquals("", RubyNil.to_s(rubyNil).getValue());
    }

    public void testToA() {
        assertEquals(new ArrayList(), RubyNil.to_a(rubyNil).getList());
    }

    public void testInspect() {
        assertEquals("nil", RubyNil.inspect(rubyNil).getValue());
    }

    public void testType() {
        assertEquals("NilClass", RubyNil.type(rubyNil).name().toString());
    }

    public void testOpAnd() {
        assertTrue(RubyNil.op_and(rubyNil, rubyNil).isFalse());
    }
  
    public void testOpOr() {
        assertTrue(RubyNil.op_or(rubyNil, ruby.getTrue()).isTrue());
        assertTrue(RubyNil.op_or(rubyNil, ruby.getFalse()).isFalse());
    }

    public void testOpXOr() {
        assertTrue(RubyNil.op_xor(rubyNil, ruby.getTrue()).isTrue());
        assertTrue(RubyNil.op_xor(rubyNil, ruby.getFalse()).isFalse());
    }
}