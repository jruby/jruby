/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.util.collections.IdentitySet;

public class TestIdentitySet extends TestCase {

    public TestIdentitySet(String name) {
        super(name);
    }

    public void testStoring() {
        IdentitySet set = new IdentitySet();
        String s1 = "hello";
        String s2 = new String(s1);
        String s3 = "some other string";

        set.add(s1);
        assertTrue(set.contains(s1));
        assertTrue(! set.contains(s2));
        assertTrue(! set.contains(s3));

        set.add(s2);
        assertTrue(set.contains(s1));
        assertTrue(set.contains(s2));
        assertTrue(! set.contains(s3));

        set.remove(s2);
        assertTrue(set.contains(s1));
        assertTrue(! set.contains(s2));
        assertTrue(! set.contains(s3));
    }
}
