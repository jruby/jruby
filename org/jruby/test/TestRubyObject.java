/*
 * TestRubyObject.java - description
 * Created on 10.03.2002, 17:46:15
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.*;

public class TestRubyObject extends TestCase {
    private Ruby ruby;
    private RubyObject rubyObject;

    public TestRubyObject(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(null);
        rubyObject = new RubyObject(ruby, ruby.getClasses().getObjectClass());
    }

    public void testNil() {
        assertTrue(!rubyObject.isNil());
    }

    public void testTrue() {
        assertTrue(rubyObject.isTrue());
    }

    public void testFalse() {
        assertTrue(!rubyObject.isFalse());
    }

    public void testEqual() {
        assertTrue(rubyObject.equal(rubyObject).isTrue());
    }

    public void testEquals() {
        assertTrue(rubyObject.equals(rubyObject));
    }

    public void testClone() {
        assertTrue(rubyObject.rbClone().type() == rubyObject.type());
    }

    public void testDup() {
        assertTrue(rubyObject.dup().type() == rubyObject.type());
    }

    public void testType() {
        assertEquals("Object", rubyObject.type().name().toString());
    }

    public void testFreeze() {
        assertTrue(rubyObject.frozen().isFalse());
        rubyObject.freeze();
        assertTrue(rubyObject.frozen().isTrue());
    }

    public void testTaint() {
        assertTrue(rubyObject.tainted().isFalse());
        rubyObject.taint();
        assertTrue(rubyObject.tainted().isTrue());
        rubyObject.untaint();
        assertTrue(rubyObject.tainted().isFalse());
    }

    public void test_to_s() {
        assertTrue(rubyObject.to_s().toString().startsWith("#<Object:0x"));
    }

    public void test_instance_of() {
        assertTrue(rubyObject.instance_of(ruby.getClasses().getObjectClass()).isTrue());
        assertTrue(rubyObject.instance_of(ruby.getClasses().getStringClass()).isFalse());
    }

    public void test_kind_of() {
        assertTrue(rubyObject.kind_of(ruby.getClasses().getObjectClass()).isTrue());
        // assertTrue(rubyObject.kind_of(ruby.getClasses().getStringClass()).isFalse());
    }
}