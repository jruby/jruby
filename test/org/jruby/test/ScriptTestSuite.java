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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * @author Anders
 * @version $Revision$
 */
public class ScriptTestSuite extends TestSuite {

    private static final String TEST_DIR = "test";
    private static final String TEST_INDEX = "test" + File.separator + "test_index";

    public ScriptTestSuite(String name) {
        super(name);
    }

    public static Test suite() throws java.io.IOException {
        TestSuite suite = new TestSuite();

        File testIndex = new File("test/test_index");

        if (! testIndex.canRead()) {
            // Since we don't have any other error reporting mechanism, we
            // add the error message as an always-failing test to the test suite.
            suite.addTest(new FailingTest("ScriptTestSuite",
                                          "Couldn't locate " + TEST_INDEX +
                                          ". Make sure you run the tests from the base " +
                                          "directory of the JRuby sourcecode."));
            return suite;
        }

        BufferedReader testFiles =
            new BufferedReader(new InputStreamReader(new FileInputStream(testIndex)));
        String line;
        while ((line = testFiles.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || line.length() == 0) {
                continue;
            }
            
            // Ensure we have a new interpreter for each test. Previous we were using the
            //  same interpreter which caused problems as soon as one test failed.
            Ruby runtime = setupInterpreter();
            
            suite.addTest(new ScriptTest(runtime, line));
        }

        return suite;
    }

    private static Ruby setupInterpreter() {
        Ruby runtime = Ruby.getDefaultInstance();
        return runtime;
    }


    private static class FailingTest extends TestCase {
        private final String message;

        public FailingTest(String name, String message) {
            super(name);
            this.message = message;
        }

        public void runTest() throws Throwable {
            fail(message);
        }
    }


    private static class ScriptTest extends TestCase {
        private final Ruby runtime;
        private final String filename;

        public ScriptTest(Ruby runtime, String filename) {
            super(filename);
            this.runtime = runtime;
            this.filename = filename;
        }

		private String scriptName() {
			return new File(TEST_DIR + File.separator + filename).getPath();
		}

        public void runTest() throws Throwable {
        	StringBuffer script = new StringBuffer();
        	
        	script.append("require 'test/minirunit'").append('\n');
        	script.append("test_load('").append(scriptName()).append("')").append('\n');
            script.append("test_get_last_failed()").append('\n');

            IRubyObject lastFailed = runtime.evalScript(script.toString());
            
            if (! lastFailed.isNil()) {
				RubyString message = (RubyString) lastFailed.callMethod("to_s");
                fail(message.getValue());
            }

            System.out.flush(); // Without a flush Ant will miss some of our output
        }
    }
}
