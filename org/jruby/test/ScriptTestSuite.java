/*
 * ScriptTestSuite.java
 * Created on 2002-03-18 15:20
 * 
 * Copyright (C) 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore,
 *     Benoit Cerrina, Chad Fowler, Anders Bengtsson
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

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.jruby.*;
import org.ablaf.ast.INode;

/**
 * @author Anders
 * @version $Revision$
 */

public class ScriptTestSuite extends TestSuite {

    public ScriptTestSuite(String name) {
		super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

		Ruby ruby = Ruby.getDefaultInstance(null);
		ruby.initLoad(new ArrayList());

		suite.addTest(new ScriptTest(ruby, "testRegexp"));
		suite.addTest(new ScriptTest(ruby, "testStringEval"));
		suite.addTest(new ScriptTest(ruby, "testHereDocument"));
		suite.addTest(new ScriptTest(ruby, "testClass"));
//      suite.addTest(new ScriptTest(ruby, "testArray"));
		suite.addTest(new ScriptTest(ruby, "testVariableAndMethod"));
		suite.addTest(new ScriptTest(ruby, "testIf"));
		suite.addTest(new ScriptTest(ruby, "testLoops"));
		suite.addTest(new ScriptTest(ruby, "testMethods"));
		suite.addTest(new ScriptTest(ruby, "testGlobalVars"));
		suite.addTest(new ScriptTest(ruby, "testClasses"));
		suite.addTest(new ScriptTest(ruby, "testNumber"));
		suite.addTest(new ScriptTest(ruby, "testFloat"));
		suite.addTest(new ScriptTest(ruby, "testBlock"));
		suite.addTest(new ScriptTest(ruby, "testRange"));
		suite.addTest(new ScriptTest(ruby, "testString"));
//  	suite.addTest(new ScriptTest(ruby, "testException"));
//  	suite.addTest(new ScriptTest(ruby, "testSpecialVar"));
		suite.addTest(new ScriptTest(ruby, "testFile"));

        return suite;
    }

    private static class ScriptTest extends TestCase {
		private final Ruby ruby;
		private final String filename;
		private final File script;

		public ScriptTest(Ruby ruby, String filename) {
			super(filename);
			this.ruby = ruby;
			this.filename = filename;
			this.script = new File("test/" + filename + ".rb");
		}

		public void runTest() throws Throwable {
			StringBuffer scriptString = new StringBuffer((int) script.length());
			BufferedReader br = new BufferedReader(new FileReader(script));
			String line;
			while ((line = br.readLine()) != null) {
				scriptString.append(line).append('\n');
			}
			br.close();

			// At the end of the tests we need a value to tell us if they failed.
			scriptString.append("$curtestOK").append('\n');

			RubyBoolean isOk = (RubyBoolean) ruby.evalScript(scriptString.toString());
			if (isOk.isFalse()) {
				fail(filename + " failed");
			}

			System.out.flush(); // Without a flush Ant will miss some of our output
		}
    }
}
