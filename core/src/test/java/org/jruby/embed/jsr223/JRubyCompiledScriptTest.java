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
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
import java.io.StringWriter;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.script.ScriptEngineManager;
import org.jruby.embed.AttributeName;
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
    String basedir = System.getProperty("user.dir");
    static Logger logger0 = Logger.getLogger(JRubyCompiledScriptTest.class.getName());
    static Logger logger1 = Logger.getLogger(JRubyCompiledScriptTest.class.getName());
    static OutputStream outStream = null;

    public JRubyCompiledScriptTest() {
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
     * Test of eval method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testEval() throws Exception {
        logger1.info("eval");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        String script = "return \"Hello World!!!\"";

        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);
        Object expResult = "Hello World!!!";
        Object result = instance.eval();
        assertEquals(expResult, result);
    }

    /**
     * Test of eval method, of class Jsr223JRubyCompiledScript.
     */
    @Test
    public void testEval_context() throws Exception {
        logger1.info("eval with context");
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
        logger1.info("eval with bindings");
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
        logger1.info("getEngine");
        JRubyEngineFactory factory = new JRubyEngineFactory();
        JRubyEngine engine = (JRubyEngine) factory.getScriptEngine();
        String script = "puts \"Hello World!!!\"";
        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);

        Object expResult = "JSR 223 JRuby Engine";
        Object result = instance.getEngine().getFactory().getEngineName();
        assertEquals(expResult, result);
    }

    @Test
    public void testTerminate() throws ScriptException {
        logger1.info("termination");
        JRubyEngine engine = null;
        synchronized (this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            JRubyEngineFactory factory = new JRubyEngineFactory();
            engine = (JRubyEngine) factory.getScriptEngine();
        }
        engine.eval("$x='GVar'");
        StringWriter writer = new StringWriter();
        engine.getContext().setWriter(writer);
        String script = "at_exit { puts \"#{$x} in an at_exit block\" }";
        engine.getContext().setAttribute(AttributeName.TERMINATION.toString(), false, ScriptContext.ENGINE_SCOPE);
        JRubyCompiledScript instance = (JRubyCompiledScript) engine.compile(script);

        instance.eval();
        Object expResult = "";
        assertEquals(expResult, writer.toString().trim());

        writer = new StringWriter();
        engine.getContext().setWriter(writer);
        engine.getContext().setAttribute(AttributeName.TERMINATION.toString(), true, ScriptContext.ENGINE_SCOPE);
        expResult = "GVar in an at_exit block";
        engine.compile("").eval();
        assertEquals(expResult, writer.toString().trim());
        engine.getContext().setAttribute(AttributeName.TERMINATION.toString(), false, ScriptContext.ENGINE_SCOPE);
    }
}