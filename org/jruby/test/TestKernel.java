/*
 * TestKernel.java - No description
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

/**
 * @author Benoit
 * @version $Revision$
 */


package org.jruby.test;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
/**
 * Unit test for the kernel class.
 **/
public class TestKernel extends TestRubyBase {

    public TestKernel(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(null);
        ruby.initLoad(new ArrayList());
    }

    public void testLoad() throws Exception {
        //load should work several times in a row
        assertEquals("0", eval("load 'test/loadTest'"));
        assertEquals("load did not load the same file several times", "1", eval("load 'test/loadTest'"));
    }

    public void testRequire() throws Exception {
        //reset the $loadTestvar
        eval("$loadTest = nil");
        assertEquals("failed to load the file test/loadTest", "0", eval("require 'test/loadTest'"));
        assertEquals("incorrectly reloaded the file test/loadTest", "", eval("require 'test/loadTest'"));

        assertEquals("incorrect value for $\" variable", "test/loadTest", eval("print $\""));
    }

    public void testPrintf() throws Exception {
        assertEquals("hello", eval("printf(\"%s\", \"hello\")"));
        assertEquals("", eval("printf(\"%s\", nil)"));
    }

    private void assertTrue(IRubyObject iObj) {
        assertTrue(iObj.isTrue());
    }
    public void tearDown() {
        super.tearDown();
    }

}
