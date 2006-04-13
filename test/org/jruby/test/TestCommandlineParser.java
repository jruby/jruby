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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2005 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

import junit.framework.TestCase;

import org.jruby.Main;
import org.jruby.util.CommandlineParser;

public class TestCommandlineParser extends TestCase {
	private PrintStream out;
	private PrintStream err;

	public void setUp() {
		out = new PrintStream(new ByteArrayOutputStream());
		err = new PrintStream(new ByteArrayOutputStream());
	}
    public void testParsing() {
        CommandlineParser c = new CommandlineParser(new Main(System.in, out, err), new String[] { "-e", "hello", "-e", "world" });
        assertEquals("hello\nworld\n", c.inlineScript());
        assertNull(c.getScriptFileName());
        assertEquals("-e", c.displayedFileName());

        c = new CommandlineParser(new Main(System.in, out, err), new String[] { "--version" });
        assertTrue(c.isShowVersion());

        c = new CommandlineParser(new Main(System.in, out, err), new String[] { "-n", "myfile.rb" });
        assertTrue(c.isAssumeLoop());
        assertEquals("myfile.rb", c.getScriptFileName());
        assertEquals("myfile.rb", c.displayedFileName());

        c = new CommandlineParser(new Main(System.in, out, err), new String[0]);
        assertEquals("-", c.displayedFileName());
    }


    
    public void testParsingWithDashDash() {
      class TestableCommandlineParser extends CommandlineParser {

        public TestableCommandlineParser(String[] arguments) {
          super(new Main(System.in, out, err), arguments);
        }
      }      
      CommandlineParser c = new TestableCommandlineParser(new String[] { "-I", "someLoadPath", "--", "simple.rb", "-v", "--version" });
      assertEquals("someLoadPath", c.loadPaths().get(0));
      assertEquals("simple.rb",c.getScriptFileName());
      assertEquals("simple.rb", c.displayedFileName());
      assertTrue("Should not be verbose. The -v flag should be a parameter to the script, not the jruby interpreter", !c.isVerbose());
      assertEquals("Script should have two parameters",2,c.getScriptArguments().length);
      assertEquals("-v",c.getScriptArguments()[0]);
      assertEquals("--version",c.getScriptArguments()[1]);
    }    
    
    public void testPrintVersionDoesNotRunInterpreter() {
        String[] args = new String[] { "-v" };
        CommandlineParser parser = new CommandlineParser(new Main(System.in, out, err), args);
        assertTrue(parser.isShowVersion());
        assertFalse(parser.isShouldRunInterpreter());

        args = new String[] { "--version" };
        parser = new CommandlineParser(new Main(System.in, out, err), args);
        assertTrue(parser.isShowVersion());
        assertFalse(parser.isShouldRunInterpreter());
    }
    
    public void testHelpDoesNotRunIntepreter() {
        String[] args = new String[] { "-h" };
        CommandlineParser parser = new CommandlineParser(new Main(System.in, out, err), args);
        assertFalse(parser.isShouldRunInterpreter());
    	
    }
}
