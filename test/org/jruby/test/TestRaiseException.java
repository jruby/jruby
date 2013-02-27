/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2007 Nick Sieger
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.test;

import org.jruby.exceptions.RaiseException;
import org.jruby.Ruby;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

public class TestRaiseException extends TestRubyBase {
    public static class ThrowFromJava {
        public void throwIt() {
            throw new RuntimeException("here");
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();
    }

    public void testJavaExceptionTraceIncludesRubys() throws Exception {
        String script = 
        "def one\n" +
        "  two\n" + 
        "end\n" +
        "def two\n" +
        "  raise 'here'\n" +
        "end\n" +
        "one\n";
        try {
            eval(script);
            fail("should have raised an exception");
        } catch (RaiseException re) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            re.printStackTrace(new PrintStream(baos));
            String trace = baos.toString();
            // System.out.println(trace);
            assertTrue(trace.indexOf("here") >= 0);
            assertTrue(trace.indexOf("one") >= 0);
            assertTrue(trace.indexOf("two") >= 0);
            // removed this line because we don't include the interpreter in
            // traces (for now)
            //assertTrue(trace.indexOf("evaluator") == -1);
        }
    }
    
    public void testRubyExceptionTraceIncludesJavas() throws Exception {
        String script =
        "require 'java'\n" +
        "java_import('org.jruby.test.TestRaiseException$ThrowFromJava') {|p,c| 'ThrowFromJava' }\n" +
        "def throw_it\n" +
        "tfj = ThrowFromJava.new\n" +
        "tfj.throwIt\n" +
        "end\n" +
        "throw_it\n";
        try {
            eval(script);
            fail("should have raised an exception");
        } catch (Exception re) {
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             re.printStackTrace(new PrintStream(baos));
             //String trace = baos.toString();
             // System.out.println(trace);
        }
    }

    public void testRubyExceptionWithoutCause() throws Exception {
        try {
            RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();

            evaler.eval(runtime, "no_method_with_this_name");
            fail("Expected ScriptException");
        } catch (RaiseException re) {
            assertEquals("(NameError) undefined local variable or method `no_method_with_this_name' for main:Object", re.getMessage());
        }
    }
}