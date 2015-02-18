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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;

/**
 * Simple test to make sure require works properly in conjunction with jars
 * in the classpath.
 */
public class TestRequire extends TestRubyBase {
    public TestRequire(String name) {
        super(name);
    }

    public void testRubyRequire() throws Exception {
        String result = eval("require 'A/C'; puts A::C.new.meth");
        assertEquals("ok", result);
        // the current working directory is core/
        result = eval("$: << 'src/test/ruby/A'; require 'B'; puts B.new.meth");
        assertEquals("ok", result);
    }

    public void testLoadErrorsDuringRequireShouldRaise() throws Exception {
        try {
            eval("require '../test/load_error'");
            fail("should have raised LoadError");
        } catch (RaiseException re) {
            assertTrue(re.getException().toString().indexOf("bogus_missing_lib") >= 0);
            assertEquals("LoadError", re.getException().getMetaClass().toString());
        }
    }
    
    public void testFailedRequireInRescueClauseStillRaisesException() throws Exception {
        try {
            eval(
            "begin\n"
            + "require '../test/load_error'\n" +
            "rescue LoadError => e\n"
            + " require '../test/load_error'\n" +
            "end");
            fail("should raise exception");
        } catch (RaiseException re) {
            assertEquals("LoadError", re.getException().getMetaClass().toString());
            assertTrue(re.getException().toString().indexOf("bogus_missing_lib") >= 0);
        }
    }

    public void testParseErrorsDuringRequireShouldRaise() throws Exception {
        try {
            eval("require '../test/parse_error'");
            fail("should have raised SyntaxError");
        } catch (RaiseException re) {
            assertEquals("SyntaxError", re.getException().getMetaClass().toString());
        }
    }
}
