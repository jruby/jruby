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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
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

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;

/**
 * Unit test for the kernel class.
 **/
public class TestKernel extends TestRubyBase {

    public TestKernel(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.getDefaultInstance();
        runtime.getLoadService().init(new ArrayList());
    }

    public void testLoad() throws Exception {
        //load should work several times in a row
        assertEquals("0", eval("load 'test/loadTest.rb'"));
        assertEquals("load did not load the same file several times", "1", eval("load 'test/loadTest.rb'"));
    }

    public void testRequire() throws Exception {
        //reset the $loadTestvar
        eval("$loadTest = nil");
        assertEquals("failed to load the file test/loadTest", "0", eval("require 'test/loadTest'"));
        assertEquals("incorrectly reloaded the file test/loadTest", "", eval("require 'test/loadTest'"));

        assertEquals("incorrect value for $\" variable", "test/loadTest.rbbuiltin/etc.rb.ast.ser", eval("print $\""));
    }

    public void testPrintf() throws Exception {
        assertEquals("hello", eval("printf(\"%s\", \"hello\")"));
        assertEquals("", eval("printf(\"%s\", nil)"));
    }
	
	public void testReturn() throws Exception {
		assertEquals("returned", runtime.evalScript("return 'returned'").toString());
	}

    public void testExit() throws Exception {
        verifyExit(RubyFixnum.zero(runtime),   "true");
        verifyExit(RubyFixnum.one(runtime),    "false");
        verifyExit(RubyFixnum.one(runtime),    "");
        verifyExit(new RubyFixnum(runtime, 7), "7");
    }
        
    private void verifyExit(RubyObject expectedStatus, String argument) throws Exception {
        try {
            eval("exit " + argument);
            fail("Expected a SystemExit to be thrown by calling exit.");
        } catch (RaiseException re) {
        	RubyException raisedException = re.getException();
        	if (raisedException.isKindOf(runtime.getClass("SystemExit"))) {
	            RubyObject status = (RubyObject)raisedException.getInstanceVariable("status");
	            assertEquals(expectedStatus, status);
        	} else {
        		throw re;
        	}
        }
    }
        
    public void tearDown() {
        super.tearDown();
    }

}
