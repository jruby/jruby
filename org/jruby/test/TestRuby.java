/*
 * TestRuby.java - TestClass for the Ruby class
 * Created on 28. Nov 2001, 15:18
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.Ruby;

/**
 * Unit test for the ruby class.
 * 
 * @author Benoit
 * @version $Revision$
 */
public class TestRuby extends TestRubyBase {

    public TestRuby(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance();
    }
    
    public void testVarAndMet() throws Exception {
        ruby.getLoadService().init(ruby, new ArrayList());
        eval("load './test/testVariableAndMethod.rb'");
        assertEquals("Hello World", eval("puts($a)"));
        assertEquals("dlroW olleH", eval("puts $b"));
        assertEquals("Hello World", eval("puts $d.reverse, $c, $e.reverse"));
        assertEquals("135 20 3", eval("puts $f, \" \", $g, \" \",  $h"));
    }
}
