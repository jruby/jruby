/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 uid41545 <uid41545@users.sourceforge.net>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2003 Joey Gibson <joey@joeygibson.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author Anders
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
            IRuby runtime = setupInterpreter();
            
            suite.addTest(new ScriptTest(runtime, line));
        }

        return suite;
    }

    private static IRuby setupInterpreter() {
        IRuby runtime = Ruby.getDefaultInstance();
        
        runtime.getLoadService().init(new ArrayList());
        
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
        private final IRuby runtime;
        private final String filename;

        public ScriptTest(IRuby runtime, String filename) {
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
        	script.append("$silentTests = true").append('\n');
        	script.append("test_load('").append(scriptName()).append("')").append('\n');
            script.append("$failed").append('\n');

            RubyArray lastFailed = (RubyArray)runtime.evalScript(script.toString());
            
            if (!lastFailed.isEmpty()) {
				RubyString message = (RubyString) lastFailed.callMethod("to_s");
                fail(scriptName() + " failed, complete failure list follows:\n" + message.toString());
            }

            System.out.flush(); // Without a flush Ant will miss some of our output
        }
    }
}
