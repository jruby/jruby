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

import org.jruby.Ruby;
import org.jruby.javasupport.JavaUtil;

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
        ruby = Ruby.getDefaultInstance(null);
    }

    public void testConvertJavaToRuby() {
        assertEquals(JavaUtil.convertJavaToRuby(ruby, null).type().toName(), "NilClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Integer(1000)).type().toName(), "Fixnum");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, new Double(1.0)).type().toName(), "Float");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.TRUE).type().toName(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, Boolean.FALSE).type().toName(), "FalseClass");
        assertEquals(JavaUtil.convertJavaToRuby(ruby, "AString").type().toName(), "String");
    }
}