/**
 * **** BEGIN LICENSE BLOCK *****
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.jsr223;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.jruby.runtime.Constants;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Yoko Harada
 */
public class JRubyEngineFactoryTest {
    String basedir = System.getProperty("user.dir");

    static Logger logger0 = Logger.getLogger(JRubyEngineFactoryTest.class.getName());
    static Logger logger1 = Logger.getLogger(JRubyEngineFactoryTest.class.getName());
    static OutputStream outStream = null;

    public JRubyEngineFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        outStream.close();
    }

    @Before
    public void setUp() throws FileNotFoundException {
        System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");

        outStream = new FileOutputStream(basedir + "/target/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getEngineName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetEngineName() {
        logger1.info("getEngineName");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "JSR 223 JRuby Engine";
        String result = instance.getEngineName();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getEngineVersion method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetEngineVersion() {
        logger1.info("getEngineVersion");
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
        logger1.info("getExtensions");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List result = instance.getExtensions();
        assertEquals(Arrays.asList("rb"), result);
    }

    /**
     * Test of getLanguageName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetLanguageName() {
        logger1.info("getLanguageName");
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
        logger1.info("getLanguageVersion");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "jruby " + Constants.VERSION;
        String result = instance.getLanguageVersion();
        assertTrue(result.startsWith(expResult));
        logger1.info(result);
    }

    /**
     * Test of getMethodCallSyntax method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetMethodCallSyntax() {
        logger1.info("getMethodCallSyntax");
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
        logger1.info("getMimeTypes");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List result = instance.getMimeTypes();
        assertEquals(Arrays.asList("application/x-ruby"), result);
    }

    /**
     * Test of getNames method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetNames() {
        logger1.info("getNames");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        assertEquals(Arrays.asList("ruby", "jruby"), instance.getNames());
    }

    /**
     * Test of getOutputStatement method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetOutputStatement() {
        logger1.info("getOutputStatement");
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
        logger1.info("getParameter");
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
        logger1.info("getProgram");
        String[] statements =
            {"1.upto(7) {|i| print i, \" \"}",
             "hh = {\"p\" => 3.14, \"e\" => 2.22}"};
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
        logger1.info("getScriptEngine");
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

        assertSame($this, engine.eval("$this"));
        engine.eval("$this.append( \"2\n\" )");

        Object command = engine.eval("$command = $this.java_method :append, [java.lang.String]");

        assertTrue( command instanceof org.jruby.RubyMethod );
        //assertEquals( "append", ((Method) command).getName() );
        //assertEquals( String.class, ((Method) command).getParameterTypes()[0] );

        Object result = engine.eval(
            "result = $command.call \"third to pass\n\"; " +
            "# puts result \n" +
            "result.to_s.each_line do |line|\n" +
            "  puts line\n" +
            "end"
        );

        //System.out.println(result);
        //System.out.println(result.getClass());
    }

}