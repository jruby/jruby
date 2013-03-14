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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Aslak Hellesoy <rinkrank@codehaus.org>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyException;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import jnr.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Unit test for the ruby class.
 * 
 * @author Benoit
*/
public class TestRuby extends TestRubyBase {
    
    public TestRuby(String name) {
        super(name);
    }
    
    public void testArgvIsNonNil() throws Exception {
        assert(!runtime.getObject().getConstant("ARGV").isNil());
        assert(!runtime.getGlobalVariables().get("$*").isNil());
    }
    
    public void testNativeENVSetting() throws Exception {
        if (Platform.IS_WINDOWS) {
            return;             // posix.getenv() not currently implemented
        }
        runtime = Ruby.newInstance();
        runtime.evalScriptlet("ENV['ham'] = 'biscuit'");
        assertEquals("biscuit", runtime.getPosix().getenv("ham"));
    }
    
    public void testNativeENVSettingWhenItIsDisabledOnTheRuntime() throws Exception {
        if (Platform.IS_WINDOWS) {
            return;             // posix.getenv() not currently implemented
        }
        RubyInstanceConfig cfg = new RubyInstanceConfig();
        cfg.setUpdateNativeENVEnabled(false);
        runtime = Ruby.newInstance(cfg);
        runtime.evalScriptlet("ENV['biscuit'] = 'gravy'");
        assertNull(runtime.getPosix().getenv("biscuit"));
    }
    
    public void testNativeENVSettingWhenNativeIsDisabledGlobally() throws Exception {
        RubyInstanceConfig cfg = new RubyInstanceConfig();
        cfg.setNativeEnabled(false);
        runtime = Ruby.newInstance(cfg);
        runtime.evalScriptlet("ENV['gravy'] = 'with sausage'");
        assertNull(runtime.getPosix().getenv("gravy"));
    }
    
    public void testNativeENVSettingWhenNativeIsDisabledGloballyButExplicitlyEnabledOnTheRuntime() throws Exception {
        RubyInstanceConfig cfg = new RubyInstanceConfig();
        cfg.setNativeEnabled(false);
        cfg.setUpdateNativeENVEnabled(true);
        runtime = Ruby.newInstance(cfg);
        runtime.evalScriptlet("ENV['sausage'] = 'biscuits'");
        assertNull(runtime.getPosix().getenv("sausage"));
    }
    
    public void testRequireCextNotAllowedWhenCextIsDisabledGlobally() throws Exception {
        RubyInstanceConfig cfg = new RubyInstanceConfig();
        cfg.setCextEnabled(false);
        runtime = Ruby.newInstance(cfg);
        
        String extensionSuffix;
        if (Platform.IS_WINDOWS) {
            extensionSuffix = ".dll";
        } else if (Platform.IS_MAC) { // TODO: BSD also?
            extensionSuffix = ".bundle";
        } else {
            extensionSuffix = ".so";
        }

        try {
            runtime.evalScriptlet("require 'tempfile'; file = Tempfile.open(['foo', '" + extensionSuffix + "']); file.close; require file.path");
            fail();
        } catch (RaiseException re) {
            assertEquals(re.getException().getType(), runtime.getLoadError());
        }
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
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        RubyInstanceConfig config = new RubyInstanceConfig() {{
            setInput(System.in); setOutput(System.out); setError(new PrintStream(err)); setObjectSpaceEnabled(false);
            setTraceType(TraceType.traceTypeFor("mri"));
        }};
        Ruby ruby = Ruby.newInstance(config);
        RubyException exception = (RubyException)runtime.getClass("NameError").newInstance(ruby.getCurrentContext(), new IRubyObject[]{ruby.newString("A message")},  Block.NULL_BLOCK);
        RubyString[] lines = new RubyString[]{
            RubyString.newString(ruby, "Line 1"),
            RubyString.newString(ruby, "Line 2"),
        };
        RubyArray backtrace = RubyArray.newArray(ruby, Arrays.<IRubyObject>asList(lines));
        exception.set_backtrace(backtrace);
        ruby.printError(exception);
        assertEquals("Line 1: A message (NameError)\n\tfrom Line 2\n", err.toString());
    }
    
    public void testPrintErrorShouldOnlyPrintErrorMessageWhenBacktraceIsNil() {
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        // use MRI formatting, since JRuby formatting is a bit different
        RubyInstanceConfig config = new RubyInstanceConfig() {{
            setInput(System.in); setOutput(System.out); setError(new PrintStream(err)); setObjectSpaceEnabled(false);
            setTraceType(TraceType.traceTypeFor("mri"));
        }};
        Ruby ruby = Ruby.newInstance(config);
        RubyException exception = (RubyException)runtime.getClass("NameError").newInstance(ruby.getCurrentContext(), new IRubyObject[]{ruby.newString("A message")},  Block.NULL_BLOCK);
        ruby.printError(exception);
        //        assertEquals(":[0,0]:[0,7]: A message (NameError)\n", err.toString());
    }
}
