/*
 * TestRuby.java - TestClass for the Ruby class
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
 * Test submitted as part of bug report 502036.
 * @author 
 * @version $Revision$
 **/

package org.jruby.test; 
 
import org.jruby.Ruby; 
 
public class TestRubyCollect extends TestRubyBase { 
	public TestRubyCollect(String name) { 
		super(name); 
	} 
	
	public void setUp() { 
		runtime = Ruby.getDefaultInstance();
	} 
	
	public void tearDown() { 
		super.tearDown(); 
	} 
	
	public void testRubyCollect() throws Exception { 
		String result = eval("a = ['a', 'b'].collect {|x| \"#{x}\"}; p a"); 
		assertEquals("Bug: [ #502036 ]", "[\"a\", \"b\"]", result); 
	} 
}