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

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* @author Anders
*/
public class TestObjectSpace extends TestCase {

    private Ruby runtime;

    public TestObjectSpace(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.getDefaultInstance();
    }

    public void testObjectSpace() {
        ObjectSpace os = new ObjectSpace();

        IRubyObject o1 = runtime.newFixnum(10);
        IRubyObject o2 = runtime.newFixnum(20);
        IRubyObject o3 = runtime.newFixnum(30);
        IRubyObject o4 = runtime.newString("hello");

        os.add(o1);
        os.add(o2);
        os.add(o3);
        os.add(o4);

        List storedFixnums = new ArrayList(3);
        storedFixnums.add(o1);
        storedFixnums.add(o2);
        storedFixnums.add(o3);

        Iterator strings = os.iterator(runtime.getClasses().getStringClass());
        assertTrue(strings.hasNext());
        assertSame(o4, strings.next());
        assertTrue(! strings.hasNext());

        Iterator numerics = os.iterator(runtime.getClasses().getNumericClass());
        for (int i = 0; i < 3; i++) {
            assertTrue(numerics.hasNext());
            Object item = numerics.next();
            assertTrue(storedFixnums.contains(item));
            storedFixnums.remove(item);
        }
        assertTrue(! numerics.hasNext());
    }
}
