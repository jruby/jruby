/*
 * TestJavaUtil.java - No description
 * Created on 11.01.2002, 12:54:12
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

import junit.framework.TestCase;

import org.jruby.javasupport.JavaUtil;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class TestJavaUtil extends TestCase {
    private Ruby ruby;

    public TestJavaUtil(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance();
    }

    public void testConvertJavaToRuby() {
        assertEquals(JavaUtil.convertJavaToRuby(ruby, null).getType().name().toString(), "NilClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Integer(1000)).getType().name().toString(), "Fixnum");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Double(1.0)).getType().name().toString(), "Float");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.TRUE).getType().name().toString(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.FALSE).getType().name().toString(), "FalseClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, "AString").getType().name().toString(), "String");
    }

    public void testCompatible() {
        assertTrue(JavaUtil.isCompatible(RubyString.newString(ruby, "hello"),
                                         String.class));
        assertTrue(JavaUtil.isCompatible(RubyHash.newHash(ruby),
                                         java.util.Map.class));
        assertTrue(JavaUtil.isCompatible(RubyArray.newArray(ruby),
                                         java.util.List.class));

        assertTrue(JavaUtil.isCompatible(RubyArray.newArray(ruby),
                                         String[].class));
        RubyArray array = RubyArray.newArray(ruby);
        array.append(RubyString.newString(ruby, "hello"));
        assertTrue(JavaUtil.isCompatible(array, String[].class));
        array.append(RubyHash.newHash(ruby));
        assertTrue(! JavaUtil.isCompatible(array, String[].class));
    }
}
