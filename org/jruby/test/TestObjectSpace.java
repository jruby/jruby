/*
 * TestObjectSpace.java
 * Created on 27 May 2002
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore,
 * Benoit Cerrina, Chad Fowler, Anders Bengtsson
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;

/**
* @author Anders
*/
public class TestObjectSpace extends TestCase {

    private Ruby ruby;

    public TestObjectSpace(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(null);
    }

    public void testObjectSpace() {
        ObjectSpace os = new ObjectSpace();

        IRubyObject o1 = RubyFixnum.newFixnum(ruby, 10);
        IRubyObject o2 = RubyFixnum.newFixnum(ruby, 20);
        IRubyObject o3 = RubyFixnum.newFixnum(ruby, 30);
        IRubyObject o4 = RubyString.newString(ruby, "hello");

        os.add(o1);
        os.add(o2);
        os.add(o3);
        os.add(o4);

        List storedFixnums = new ArrayList(3);
        storedFixnums.add(o1);
        storedFixnums.add(o2);
        storedFixnums.add(o3);

        Iterator strings = os.iterator(ruby.getClasses().getStringClass());
        assertTrue(strings.hasNext());
        assertSame(o4, strings.next());
        assertTrue(! strings.hasNext());

        Iterator numerics = os.iterator(ruby.getClasses().getNumericClass());
        for (int i = 0; i < 3; i++) {
            assertTrue(numerics.hasNext());
            Object item = numerics.next();
            assertTrue(storedFixnums.contains(item));
            storedFixnums.remove(item);
        }
        assertTrue(! numerics.hasNext());
    }
}
