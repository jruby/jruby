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

import junit.framework.*;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.RubyFixnum;
import org.jruby.core.RbNilClass;

/**
* @author chadfowler
*/
public class TestRubyNil extends TestCase {

    private Ruby ruby;
    private RubyNil rubyNil;

    public TestRubyNil(String name) {
        super(name);
    } 
    
    public void setUp() {
        ruby = new Ruby();
        ruby.init();
        rubyNil = new RubyNil(ruby);
    }
    
    public void testIsNil() {
        assertTrue(rubyNil.isNil());
    }

    public void testIsFalseOrTrue() {
        assertTrue(rubyNil.isFalse());
        assertTrue(!rubyNil.isTrue());
    }

    public void testToI() {
        assertEquals(RubyFixnum.m_newFixnum(ruby, 0), rubyNil.m_to_i());
    }

    public void testToS() {
        assertEquals("", rubyNil.m_to_s().getValue());
    }

    public void testToA() {
        assertEquals(new ArrayList(), rubyNil.m_to_a().getList());
    }

    public void testInspect() {
        assertEquals("nil", rubyNil.m_inspect().getValue());
    }

    public void testType() {
        assertEquals(RbNilClass.createNilClass(ruby).getClassname(), rubyNil.m_type().getClassname());
    }

    public void testOpAnd() {
        assertTrue(rubyNil.op_and(rubyNil).isFalse());
    }
  
    public void testOpOr() {
        assertTrue(rubyNil.op_or(ruby.getTrue()).isTrue());
        assertTrue(!rubyNil.op_or(ruby.getFalse()).isTrue());
    }

    public void testOpXOr() {
        assertTrue(rubyNil.op_or(ruby.getTrue()).isTrue());
        assertTrue(!rubyNil.op_or(ruby.getFalse()).isTrue());
    }
}

