/*
 * RubyHash.java - No description
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
 */


package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyFixnum;
import org.jruby.RubyArray;
import org.jruby.regexp.GNURegexpAdapter;
import java.util.ArrayList;
import java.io.File;

public class TestRuby extends TestRubyBase {

	public TestRuby(String name) {
		super(name);
	}

	public void setUp() {
		ruby = Ruby.getDefaultInstance(GNURegexpAdapter.class);
	}

	public void testInitLoad() {
		ArrayList list = new ArrayList();
		//check without a RubyHome and with one parameter
		list.add("toto");
		ruby.initLoad(list);
		//check that the global vars are correctly valuated
		RubyObject lCol = ruby.getGlobalVar("$:");
		RubyObject lI = ruby.getGlobalVar("$-I");
		RubyObject lLoad = ruby.getGlobalVar("$LOAD_PATH");
		assertTrue(lCol == lI && lI == lLoad && lLoad != null);
		RubyArray lLoadA = (RubyArray)lLoad;
		//check that we have 2 non null element
		assertTrue(((RubyFixnum)lLoadA.nitems()).getValue()== 2);
		//check that it is what we expect, a RubyString of the correct type
		assertTrue(new RubyString(ruby,"toto").equal(lLoadA.shift()));
		assertTrue(new RubyString(ruby,".").equal(lLoadA.shift()));
		//check the case when RubyHome is valuated
		System.setProperty("jruby.home", "RubyHome");
		//MRI result
		/*
C:\dev\jruby>ruby -e "puts $:"
/cygdrive/d/ruby/lib/ruby/site_ruby/1.6
/cygdrive/d/ruby/lib/ruby/site_ruby/1.6/i386-cygwin
/cygdrive/d/ruby/lib/ruby/site_ruby
/cygdrive/d/ruby/lib/ruby/1.6
/cygdrive/d/ruby/lib/ruby/1.6/i386-cygwin
.

		 */
		ruby.initLoad(new ArrayList());
		if (File.separatorChar == '/')
		{
			
			assertEquals("6", eval("$:.size"));
			String wanted = "RubyHome/lib/ruby/site_ruby/1.6" 
				+ "RubyHome/lib/ruby/site_ruby/1.6/JAVA"
				+ "RubyHome/lib/ruby/site_ruby"
				+ "RubyHome/lib/ruby/1.6"
				+ "RubyHome/lib/ruby/1.6/JAVA"
				+ ".\n";
			assertEquals(wanted, eval("puts $:"));
		} else
		{
			String result = eval("puts $:");
			String wanted = "RubyHome\\lib\\ruby\\site_ruby\\1.6" 
				+ "RubyHome\\lib\\ruby\\site_ruby\\1.6\\JAVA"
				+ "RubyHome\\lib\\ruby\\site_ruby"
				+ "RubyHome\\lib\\ruby\\1.6"
				+ "RubyHome\\lib\\ruby\\1.6\\JAVA"
				+ ".";
			assertEquals(wanted, result);

		}
	}

	public void testFindFile() {
		ArrayList list = new ArrayList();
		//should not work right away
		//we do need to run initLoad first so that
		//the $: variable is initialized
		ruby.initLoad(list);
		list.clear();
		File testFile = new File("fib.rb");
		try
		{
			ruby.findFile(testFile);
			fail("should have thrown an exception, the file fib.rb is not 					in the search path");
		}
		catch (Exception e)
		{
		}
		list.add("./samples");
		//now we valuate the path 
		ruby.initLoad(list);
		assertEquals(new File("./samples/fib.rb"), ruby.findFile(testFile));
	}

	
	private void assertTrue(RubyObject iObj)
	{
		assertTrue(iObj.isTrue());
	}
	public void tearDown() {
		super.tearDown();
	}

}
