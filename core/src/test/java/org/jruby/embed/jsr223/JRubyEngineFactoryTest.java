/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2010 Yoko Harada <yokolet@gmail.com>
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

package org.jruby.embed.jsr223;

import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.jruby.runtime.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Yoko Harada
 */
public class JRubyEngineFactoryTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getEngineName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetEngineName() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "JSR 223 JRuby Engine";
        String result = instance.getEngineName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEngineVersion method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetEngineVersion() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = org.jruby.runtime.Constants.VERSION;
        String result = instance.getEngineVersion();
        assertEquals(expResult, result);
    }

    /**
     * Test of getExtensions method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetExtensions() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List result = instance.getExtensions();
        assertEquals(Arrays.asList("rb"), result);
    }

    /**
     * Test of getLanguageName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetLanguageName() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "ruby";
        String result = instance.getLanguageName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getLanguageVersion method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetLanguageVersion() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "jruby " + Constants.VERSION;
        String result = instance.getLanguageVersion();
        assertTrue(result.startsWith(expResult));
    }

    /**
     * Test of getMethodCallSyntax method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetMethodCallSyntax() {
        String obj = "receiver";
        String m = "establish_connection";
        String[] args = {"localhost", "1099"};
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "receiver.establish_connection(localhost, 1099)";
        String result = instance.getMethodCallSyntax(obj, m, args);
        assertEquals(expResult, result);
    }

    /**
     * Test of getMimeTypes method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetMimeTypes() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List result = instance.getMimeTypes();
        assertEquals(Arrays.asList("application/x-ruby"), result);
    }

    /**
     * Test of getNames method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetNames() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        assertEquals(Arrays.asList("ruby", "jruby"), instance.getNames());
    }

    /**
     * Test of getOutputStatement method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetOutputStatement() {
        String toDisplay = "abc";
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "puts abc\nor\nprint abc";
        assertEquals(expResult, instance.getOutputStatement(toDisplay));
    }

    /**
     * Test of getParameter method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetParameter() {
        String key = "";
        JRubyEngineFactory instance = new JRubyEngineFactory();
        Object expResult = null;
        Object result = instance.getParameter(key);
        assertEquals(expResult, result);

        key = ScriptEngine.ENGINE;
        expResult = "JSR 223 JRuby Engine";
        result = instance.getParameter(key);
        assertEquals(expResult, result);

        key = ScriptEngine.ENGINE_VERSION;
        expResult = org.jruby.runtime.Constants.VERSION;
        result = instance.getParameter(key);
        assertEquals(expResult, result);

        key = ScriptEngine.NAME;
        expResult = "JSR 223 JRuby Engine";
        result = instance.getParameter(key);
        assertEquals(expResult, result);

        key = ScriptEngine.LANGUAGE;
        expResult = "ruby";
        result = instance.getParameter(key);
        assertEquals(expResult, result);

        key = ScriptEngine.LANGUAGE_VERSION;
        expResult = "jruby " + Constants.VERSION;
        result = instance.getParameter(key);
        assertTrue(((String)result).startsWith((String) expResult));

        key = "THREADING";
        expResult = "THREAD-ISOLATED";
        result = instance.getParameter(key);
        assertEquals(expResult, result);
    }

    /**
     * Test of getProgram method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetProgram() {
        String[] statements = {
                "1.upto(7) {|i| print i, \" \"}",
                "hh = {\"p\" => 3.14, \"e\" => 2.22}"
        };
        System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "1.upto(7) {|i| print i, \" \"}\nhh = {\"p\" => 3.14, \"e\" => 2.22}\n";
        String result = instance.getProgram(statements);
        assertEquals(expResult, result);
    }

    /**
     * Test of getScriptEngine method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetScriptEngine() {
        JRubyEngineFactory instance = new JRubyEngineFactory();
        ScriptEngine engine = instance.getScriptEngine();
        assertSame(instance, engine.getFactory());
    }

    @Test
    public void testEmbedIntegration() throws ScriptException {
        //System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");

        JRubyEngineFactory instance = new JRubyEngineFactory();
        final ScriptEngine engine = instance.getScriptEngine();

        final StringBuilder $this = new StringBuilder();
        $this.append("first\n");
        engine.put("this", $this);

        assertSame($this, engine.eval("this"));
        engine.eval("this.append( \"2\n\" )");

        Object command = engine.eval("$command = this.java_method :append, [java.lang.String]");

        assertTrue( command instanceof org.jruby.RubyMethod );

        engine.eval("$command.call \"third to pass\n\"");

        assertEquals("first\n2\nthird to pass\n", $this.toString());
    }

}
