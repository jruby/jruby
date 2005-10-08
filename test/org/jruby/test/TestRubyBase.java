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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
import java.io.StringReader;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.RubyIO;

/**
 * @author Benoit
 */
public class TestRubyBase extends TestCase {
    protected IRuby runtime;
    private PrintStream out;

    public TestRubyBase() {
    }
    
    public TestRubyBase(String name) {
        super(name);
    }

    /**
     * evaluate a string and returns the standard output.
     * @param script the String to eval as a String
     * @return the value printed out on  stdout and stderr by 
     **/
    protected String eval(String script) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        out = new PrintStream(result);
        RubyIO lStream = new RubyIO(runtime, out); 
        runtime.getGlobalVariables().set("$stdout", lStream);
        runtime.getGlobalVariables().set("$>", lStream);
        runtime.getGlobalVariables().set("$stderr", lStream);
        
        runtime.loadScript("test", new StringReader(script), false);
        StringBuffer sb = new StringBuffer(new String(result.toByteArray()));
        for (int idx = sb.indexOf("\n"); idx != -1; idx = sb.indexOf("\n")) {
            sb.deleteCharAt(idx);
        }
        
        return sb.toString();
    }

    public void tearDown() {
        if (out != null)
            out.close();
    }
}
