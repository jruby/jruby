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
	private Ruby ruby;

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
		System.setProperty("org.jruby.HOME", "RubyHome");
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
			assertTrue(new RubyString(ruby,"RubyHome/lib/ruby/site_ruby/1.6").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome/lib/ruby/site_ruby/1.6/JAVA").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome/lib/ruby/site_ruby").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome/lib/ruby/1.6").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome/lib/ruby/1.6/JAVA").equal(lLoadA.shift()));
		} else
		{
			assertTrue(new RubyString(ruby,"RubyHome\\lib\\ruby\\site_ruby\\1.6").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome\\lib\\ruby\\site_ruby\\1.6\\JAVA").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome\\lib\\ruby\\site_ruby").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome\\lib\\ruby\\1.6").equal(lLoadA.shift()));
			assertTrue(new RubyString(ruby,"RubyHome\\lib\\ruby\\1.6\\JAVA").equal(lLoadA.shift()));
		}
		assertTrue(new RubyString(ruby,".").equal(lLoadA.shift()));
	}
	private void assertTrue(RubyObject iObj)
	{
		assertTrue(iObj.isTrue());
	}
    public void tearDown() {
	super.tearDown();
    }

}
