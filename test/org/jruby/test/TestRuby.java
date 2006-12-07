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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Aslak Hellesoy <rinkrank@codehaus.org>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyException;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;

/**
 * Unit test for the ruby class.
 * 
 * @author Benoit
*/
public class TestRuby extends TestRubyBase {

    public TestRuby(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.getDefaultInstance();
    }
    
    public void testVarAndMet() throws Exception {
        runtime.getLoadService().init(new ArrayList());
        eval("load './test/testVariableAndMethod.rb'");
        assertEquals("Hello World", eval("puts($a)"));
        assertEquals("dlroW olleH", eval("puts $b"));
        assertEquals("Hello World", eval("puts $d.reverse, $c, $e.reverse"));
        assertEquals("135 20 3", eval("puts $f, \" \", $g, \" \",  $h"));
    }
    
    public void testPrintErrorWithNilBacktrace() throws Exception {
        testPrintErrorWithBacktrace("nil");
    }

    public void testPrintErrorWithStringBacktrace() throws Exception {
        testPrintErrorWithBacktrace("\"abc\"");
    }
    
    private void testPrintErrorWithBacktrace(String backtrace) throws Exception {
        RubyIO oldStderr = (RubyIO)runtime.getGlobalVariables().get("$stderr");
        try {
            ByteArrayOutputStream stderrOutput = new ByteArrayOutputStream();
            RubyIO newStderr = new RubyIO(runtime, stderrOutput);
            runtime.getGlobalVariables().set("$stderr", newStderr);
            
            try {
                eval("class MyError < StandardError ; def backtrace ; " + backtrace + " ; end ; end ; raise MyError.new ");
                fail("Expected MyError to be raised");
            } catch (RaiseException re) {
                //No ClassCastException!
                runtime.printError(re.getException());
            }
        } finally {
            runtime.getGlobalVariables().set("$stderr", oldStderr);
        }
    }
    
    public void testPrintErrorShouldPrintErrorMessageAndStacktraceWhenBacktraceIsPresent() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        IRuby ruby = Ruby.newInstance(System.in, System.out, new PrintStream(err), false);
        RubyException exception = new RubyException(ruby, ruby.getClass("NameError"), "A message");
        RubyString[] lines = new RubyString[]{
            RubyString.newString(ruby, "Line 1"),
            RubyString.newString(ruby, "Line 2"),
        };
        RubyArray backtrace = RubyArray.newArray(ruby, Arrays.asList(lines));
        exception.set_backtrace(backtrace);
        ruby.printError(exception);
        assertEquals("Line 1: A message (NameError)\n\tfrom Line 2\n", err.toString());
    }
    
    public void testPrintErrorShouldOnlyPrintErrorMessageWhenBacktraceIsNil() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        IRuby ruby = Ruby.newInstance(System.in, System.out, new PrintStream(err), false);
        RubyException exception = new RubyException(ruby, ruby.getClass("NameError"), "A message");
        ruby.printError(exception);
        //        assertEquals(":[0,0]:[0,7]: A message (NameError)\n", err.toString());
    }
}
