/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.jsr223;

import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
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

    public JRubyEngineFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getEngineName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetEngineName() {
        System.out.println("getEngineName");
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
        System.out.println("getEngineVersion");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "0.1.2";
        String result = instance.getEngineVersion();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getExtensions method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetExtensions() {
        System.out.println("getExtensions");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List expResult = new ArrayList();
        expResult.add("rb");
        List result = instance.getExtensions();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getLanguageName method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetLanguageName() {
        System.out.println("getLanguageName");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "ruby";
        String result = instance.getLanguageName();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getLanguageVersion method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetLanguageVersion() {
        System.out.println("getLanguageVersion");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "jruby 1.5.0";
        String result = instance.getLanguageVersion();
        assertTrue(result.startsWith(expResult));
        System.out.println(result);

        instance = null;
    }

    /**
     * Test of getMethodCallSyntax method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetMethodCallSyntax() {
        System.out.println("getMethodCallSyntax");
        String obj = "receiver";
        String m = "establish_connection";
        String[] args = {"localhost", "1099"};
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "receiver.establish_connection(localhost, 1099)";
        String result = instance.getMethodCallSyntax(obj, m, args);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getMimeTypes method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetMimeTypes() {
        System.out.println("getMimeTypes");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List expResult = new ArrayList();
        expResult.add("application/x-ruby");
        List result = instance.getMimeTypes();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getNames method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetNames() {
        System.out.println("getNames");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        List expResult = new ArrayList();
        expResult.add("ruby");
        expResult.add("jruby");
        List result = instance.getNames();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getOutputStatement method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetOutputStatement() {
        System.out.println("getOutputStatement");
        String toDisplay = "abc";
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "puts abc\nor\nprint abc";
        String result = instance.getOutputStatement(toDisplay);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getParameter method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetParameter() {
        System.out.println("getParameter");
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
        expResult = "0.1.2";
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
        expResult = "jruby 1.5.0";
        result = instance.getParameter(key);
        assertTrue(((String)result).startsWith((String) expResult));

        key = "THREADING";
        expResult = "THREAD-ISOLATED";
        result = instance.getParameter(key);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getProgram method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetProgram() {
        System.out.println("getProgram");
        String[] statements =
            {"1.upto(7) {|i| print i, \" \"}",
             "hh = {\"p\" => 3.14, \"e\" => 2.22}"};
        System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        String expResult = "1.upto(7) {|i| print i, \" \"}\nhh = {\"p\" => 3.14, \"e\" => 2.22}\n";
        String result = instance.getProgram(statements);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getScriptEngine method, of class Jsr223JRubyEngineFactory.
     */
    @Test
    public void testGetScriptEngine() {
        System.out.println("getScriptEngine");
        JRubyEngineFactory instance = new JRubyEngineFactory();
        Object expResult = instance;
        ScriptEngine engine = instance.getScriptEngine();
        Object result = engine.getFactory();
        assertEquals(expResult, result);

        instance = null;
    }

}