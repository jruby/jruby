/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.globalVariables;

/**
 * @author Benoit
 */
public class Base extends junit.framework.TestCase {
    protected ThreadContext context;
    private PrintStream out;

    public Base() {
    }
    
    public Base(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = Ruby.newInstance().getCurrentContext();
    }

    /**
     * evaluate a string and returns the standard output.
     * @param script the String to eval as a String
     * @return the value printed out on  stdout and stderr by 
     **/
    protected String eval(String script) {
        return eval(script, "test");
    }

    protected final String eval(String script, String fileName) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        out = new PrintStream(result);
        RubyIO lStream = new RubyIO(context.runtime, out);
        lStream.getOpenFileChecked().setSync(true);
        var globals = globalVariables(context);
        globals.set("$stdout", lStream);
        globals.set("$>", lStream);
        globals.set("$stderr", lStream);

        context.runtime.runFromMain(new ByteArrayInputStream(script.getBytes()), fileName);
        StringBuffer sb = new StringBuffer(new String(result.toByteArray()));
        for (int idx = sb.indexOf("\n"); idx != -1; idx = sb.indexOf("\n")) {
            sb.deleteCharAt(idx);
        }

        return sb.toString();
    }

    @Override
    protected void tearDown() throws Exception {
        if (out != null) {
            out.close();
        }
        super.tearDown();
    }

    public static String CRLFToNL(String line) {
        return line.replaceAll("\r\n", "\n");
    }
}
