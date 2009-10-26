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

import java.io.StringWriter;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
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
public class JRubyCompiledScriptTest {

    public JRubyCompiledScriptTest() {
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
     * Test of eval method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testEval() throws Exception {
        System.out.println("eval");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        String script = "puts \"Hello World!!!\"";

        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);
        Object expResult = null;
        Object result = instance.eval();
        assertEquals(expResult, result);
    }

    /**
     * Test of eval method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testEval_context() throws Exception {
        System.out.println("eval with context");
        System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        ScriptContext context = new SimpleScriptContext();
        StringWriter writer = new StringWriter();
        StringWriter errorWriter = new StringWriter();
        context.setWriter(writer);
        context.setErrorWriter(errorWriter);

        context.setAttribute("message", "Hello World!!!!!", ScriptContext.ENGINE_SCOPE);
        engine.setContext(context);
        String script = "puts message";
        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);
        Object expResult = "Hello World!!!!!";
        instance.eval(context);
        Object result = writer.toString().trim();
        assertEquals(expResult, result);
        writer.close();

        writer = new StringWriter();
        context.setWriter(writer);
        context.setAttribute("@message", "Say Hey.", ScriptContext.ENGINE_SCOPE);
        engine.setContext(context);
        script = "puts @message";
        instance = (JRubyCompiledScript) engine.compile(script);
        expResult = "Say Hey.";
        instance.eval(context);
        result = writer.toString().trim();
        assertEquals(expResult, result);

        context.setAttribute("@message", "Yeah!", ScriptContext.ENGINE_SCOPE);
        engine.setContext(context);
        expResult = "Say Hey.\nYeah!";
        instance.eval(context);
        result = writer.toString().trim();
        assertEquals(expResult, result);
        writer.close();

        writer = new StringWriter();
        context.setWriter(writer);
        context.setAttribute("$message", "Hiya.", ScriptContext.ENGINE_SCOPE);
        engine.setContext(context);
        script = "puts $message";
        instance = (JRubyCompiledScript) engine.compile(script);
        expResult = "Hiya.";
        instance.eval(context);
        result = writer.toString().trim();
        assertEquals(expResult, result);
        writer.close();
        errorWriter.close();
    }

    /**
     * Test of eval method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testEval_bindings() throws Exception {
        System.out.println("eval with bindings");
        System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        Bindings bindings = new SimpleBindings();
        bindings.put("message", "Helloooo Woooorld!");
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        String script = "\"I heard, \"#{message}\"\"";
        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);
        Object expResult = "I heard, Helloooo Woooorld!";
        Object result = instance.eval(bindings);
        // Bug? a local variable isn't shared.
        //assertEquals(expResult, result);

        bindings.put("@message", "Saaaay Heeeey.");
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        script = "\"I heard, #{@message}\"";
        instance = (JRubyCompiledScript) engine.compile(script);
        expResult = "I heard, Saaaay Heeeey.";
        result = instance.eval(bindings);
        assertEquals(expResult, result);

        bindings.put("$message", "Hiya, hiya, hiya");
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        script = "\"I heard, #{$message}\"";
        instance = (JRubyCompiledScript) engine.compile(script);
        expResult = "I heard, Hiya, hiya, hiya";
        result = instance.eval(bindings);
        assertEquals(expResult, result);
    }

    /**
     * Test of getEngine method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testGetEngine() throws ScriptException {
        System.out.println("getEngine");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        String script = "puts \"Hello World!!!\"";
        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);

        Object expResult = "JSR 223 JRuby Engine";
        Object result = instance.getEngine().getFactory().getEngineName();
        assertEquals(expResult, result);
    }

}