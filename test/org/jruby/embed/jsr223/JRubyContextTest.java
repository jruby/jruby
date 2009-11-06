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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
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
public class JRubyContextTest {

    public JRubyContextTest() {
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
     * Test of getAttribute method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetAttribute_String() {
        System.out.println("getAttribute");
        String name = "";
        Object expResult = null;
        Object result = null;
        JRubyContext instance = new JRubyContext(new ScriptingContainer(LocalContextScope.THREADSAFE));
        try {
            result = instance.getAttribute(name);
        } catch (IllegalArgumentException e) {
            expResult = "name is empty";
            assertEquals(expResult, e.getMessage());
        }
        instance.setAttribute("abc", "aabc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("$abc", "abbc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("@abc", "abcc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "filename", ScriptContext.ENGINE_SCOPE);
        expResult = "filename";
        result = instance.getAttribute(ScriptEngine.FILENAME);
        assertEquals(expResult, result);
        expResult = "abcc";
        result = instance.getAttribute("@abc");
        assertEquals(expResult, result);
    }

    /**
     * Test of getAttribute method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetAttribute_String_int() throws ScriptException {
        System.out.println("getAttribute");
        // livetribe javax.script impl has a bug to handle global scope.
        //ScriptEngineManager manager = new ScriptEngineManager();
        JRubyScriptEngineManager manager = new JRubyScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        JRubyContext instance = (JRubyContext) engine.getContext();
        int scope = ScriptContext.ENGINE_SCOPE;
        String name = "";
        Object expResult = null;
        Object result = null;
        try {
            result = instance.getAttribute(name, scope);
        } catch (IllegalArgumentException e) {
            expResult = "name is empty";
            assertEquals(expResult, e.getMessage());
        }
        instance.setAttribute("abc", "aabc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("$abc", "abbc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("@abc", "abcc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "filename", ScriptContext.ENGINE_SCOPE);
        /*
         * Global Scope is maintained in ScriptEngineManager.
         */
        instance = (JRubyContext) engine.getContext();
        instance.setAttribute("abc", "xxyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("$abc", "xyyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("@abc", "xyzz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "nameoffile", ScriptContext.GLOBAL_SCOPE);

        expResult = "aabc";
        result = instance.getAttribute("abc", ScriptContext.ENGINE_SCOPE);
        assertEquals(expResult, result);

        expResult = "xxyz";
        result = instance.getAttribute("abc", ScriptContext.GLOBAL_SCOPE);
        assertEquals(expResult, result);

        expResult = "nameoffile";
        result = instance.getAttribute(ScriptEngine.FILENAME, ScriptContext.GLOBAL_SCOPE);
        assertEquals(expResult, result);
    }

    /**
     * Test of getAttributesScope method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetAttributesScope() throws ScriptException {
        System.out.println("getAttributesScope");
        // livetribe javax.script impl has a bug to handle global scope.
        //ScriptEngineManager manager = new ScriptEngineManager();
        JRubyScriptEngineManager manager = new JRubyScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        JRubyContext instance = (JRubyContext) engine.getContext();
        instance.setAttribute("abc", "aabc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("$abc", "abbc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("@abc", "abcc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "filename", ScriptContext.ENGINE_SCOPE);
        /*
         * Global Scope is maintained in ScriptEngineManager.
         */
        instance.setAttribute("abc", "xxyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("$abc", "xyyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("@abc", "xyzz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "nameoffile", ScriptContext.GLOBAL_SCOPE);

        String name = "abc";
        int expResult = ScriptContext.ENGINE_SCOPE;
        int result = instance.getAttributesScope(name);
        assertEquals(expResult, result);

        name = ScriptEngine.FILENAME;
        result = instance.getAttributesScope(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of getBindings method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetBindings() throws ScriptException {
        System.out.println("getBindings");
        ScriptingContainer container = new ScriptingContainer();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        JRubyContext instance = (JRubyContext) engine.getContext();
        JRubyBindings jb = new JRubyBindings(container);
        jb.put("abc", "aabc");
        jb.put("@abc", "abbc");
        jb.put("$abc", "abcc");
        instance.setBindings(jb, ScriptContext.ENGINE_SCOPE);
        
        SimpleBindings sb = new SimpleBindings();
        sb.put("abc", "xxyz");
        sb.put("@abc", "xyyz");
        instance.setBindings(sb, ScriptContext.GLOBAL_SCOPE);

        Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
        Object expResult = "abcc";
        Object result = bindings.get("$abc");
        assertEquals(expResult, result);
        expResult = 3;
        result = bindings.size();
        assertEquals(expResult, result);

        bindings = instance.getBindings(ScriptContext.GLOBAL_SCOPE);
        expResult = 2;
        result = bindings.size();
        assertEquals(expResult, result);
    }

    /**
     * Test of getErrorWriter method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetErrorWriter() {
        System.out.println("getErrorWriter");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        Writer errorWriter = new StringWriter();
        instance.setErrorWriter(errorWriter);
        Writer expResult = errorWriter;
        Writer result = instance.getErrorWriter();
        assertEquals(expResult, result);
    }

    /**
     * Test of getReader method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetReader() {
        System.out.println("getReader");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        Reader reader = new StringReader("");
        instance.setReader(reader);
        Reader expResult = reader;
        Reader result = instance.getReader();
        assertEquals(expResult, result);
    }

    /**
     * Test of getScopes method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetScopes() {
        System.out.println("getScopes");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        List<Integer> expResult = new ArrayList();
        expResult.add(ScriptContext.ENGINE_SCOPE);
        expResult.add(ScriptContext.GLOBAL_SCOPE);

        List<Integer> result = instance.getScopes();
        assertEquals(expResult.get(0), result.get(0));
        assertEquals(expResult.size(), result.size());

        try {
            result.add(1000);
            assertEquals(3, result.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test of getWriter method, of class Jsr223JRubyContext.
     */
    @Test
    public void testGetWriter() {
        System.out.println("getWriter");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        Writer writer = new StringWriter();
        instance.setWriter(writer);
        Writer expResult = writer;
        Writer result = instance.getWriter();
        assertEquals(expResult, result);
    }

    /**
     * Test of removeAttribute method, of class Jsr223JRubyContext.
     */
    @Test
    public void testRemoveAttribute() throws ScriptException {
        System.out.println("removeAttribute");
        String name = "abc";
        int priority = 0;
        // livetribe javax.script impl has a bug to handle global scope.
        //ScriptEngineManager manager = new ScriptEngineManager();
        JRubyScriptEngineManager manager = new JRubyScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        JRubyContext instance = (JRubyContext) engine.getContext();
        Object expResult = null;
        Object result = null;
        try {
            result = instance.removeAttribute(name, priority);
        } catch (IllegalArgumentException e) {
            expResult = "invalid scope";
            assertEquals(expResult, e.getMessage());
        }
        instance.setAttribute("abc", "aabc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("$abc", "abbc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute("@abc", "abcc", ScriptContext.ENGINE_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "filename", ScriptContext.ENGINE_SCOPE);
        
        instance.setAttribute("abc", "xxyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("$abc", "xyyz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute("@abc", "xyzz", ScriptContext.GLOBAL_SCOPE);
        instance.setAttribute(ScriptEngine.FILENAME, "nameoffile", ScriptContext.GLOBAL_SCOPE);

        expResult = 4;
        result = instance.getBindings(ScriptContext.ENGINE_SCOPE).size();
        assertEquals(expResult , result);

        result = instance.getBindings(ScriptContext.GLOBAL_SCOPE).size();
        assertEquals(expResult , result);

        expResult = "abcc";
        result = instance.removeAttribute("@abc", ScriptContext.ENGINE_SCOPE);
        assertEquals(expResult, result);
        expResult = 3;
        result = instance.getBindings(ScriptContext.ENGINE_SCOPE).size();
        assertEquals(expResult , result);

        expResult = 2;
        instance.removeAttribute("abc", ScriptContext.GLOBAL_SCOPE);
        instance.removeAttribute(ScriptEngine.FILENAME, ScriptContext.GLOBAL_SCOPE);
        result = instance.getBindings(ScriptContext.GLOBAL_SCOPE).size();
        assertEquals(expResult , result);
    }

    /**
     * Test of setAttribute method, of class Jsr223JRubyContext.
     */
    @Test
    public void testSetAttribute() {
        System.out.println("setAttribute");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        instance.setAttribute("PAI", 3.1415, ScriptContext.ENGINE_SCOPE);
        Object expResult = 3.1415;
        Object result = instance.getAttribute("PAI", ScriptContext.ENGINE_SCOPE);
        assertEquals(expResult, result);
    }

    /**
     * Test of setBindings method, of class Jsr223JRubyContext.
     */
    @Test
    public void testSetBindings() throws ScriptException {
        System.out.println("setBindings");
        Object expResult = null;
        Object result = null;
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        JRubyScriptEngineManager manager = new JRubyScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        JRubyContext instance = (JRubyContext) engine.getContext();
        Bindings bindings = null;
        int scope = ScriptContext.ENGINE_SCOPE;
        try {
            instance.setBindings(bindings, scope);
        } catch (NullPointerException e) {
            expResult = "null bindings in ENGINE scope";
            assertEquals(expResult, e.getMessage());
        }
        bindings = new JRubyBindings(container);
        bindings.put("abc", "aabc");
        bindings.put(ScriptEngine.LANGUAGE, "jruby");
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        expResult = "aabc";
        result = instance.getAttribute("abc");
        assertEquals(expResult, result);

        expResult = "jruby";
        result = instance.removeAttribute(ScriptEngine.LANGUAGE, ScriptContext.ENGINE_SCOPE);
        assertEquals(expResult, result);

        bindings = new SimpleBindings();
        bindings.put("@abc", "abcc");
        bindings.put(ScriptEngine.FILENAME, "helloworld.txt");
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        expResult = "abcc";
        result = instance.getAttribute("@abc");
        assertEquals(expResult, result);

        expResult = "helloworld.txt";
        result = instance.removeAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
        assertEquals(expResult, result);
        expResult = 1;
        result = instance.getBindings(ScriptContext.ENGINE_SCOPE).size();
        assertEquals(expResult, result);

        instance.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        expResult = 1;
        result = instance.getBindings(ScriptContext.GLOBAL_SCOPE).size();
        assertEquals(expResult, result);
    }

    /**
     * Test of setErrorWriter method, of class Jsr223JRubyContext.
     */
    @Test
    public void testSetErrorWriter() throws IOException {
        System.out.println("setErrorWriter");
        Writer errorWriter = null;
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        instance.setErrorWriter(errorWriter);

        errorWriter = new StringWriter();
        errorWriter.write("check setErrorWriter");
        instance.setErrorWriter(errorWriter);

        Writer ew = instance.getErrorWriter();
        Object expResult = "check setErrorWriter";
        Object result = ew.toString();
        assertTrue(((String)result).startsWith((String) expResult));
    }

    /**
     * Test of setReader method, of class Jsr223JRubyContext.
     */
    @Test
    public void testSetReader() throws IOException {
        System.out.println("setReader");
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        Reader reader = null;
        instance.setReader(reader);

        reader = new StringReader("Hello World from JSR 223.");
        assertTrue(reader.ready());
        instance.setReader(reader);
        assertTrue(reader.ready());

        Reader r = instance.getReader();
        assertTrue(r.ready());
        r.reset();

        Object expResult = "Hello World from JSR 223.";
        char[] target = new char[256];
        int n = r.read(target);
        Object result = new String(target).trim();
        assertEquals(expResult, result);
    }

    /**
     * Test of setWriter method, of class Jsr223JRubyContext.
     */
    @Test
    public void testSetWriter() throws IOException {
        System.out.println("setWriter");
        Writer writer = null;
        JRubyContext instance = new JRubyContext(new ScriptingContainer());
        instance.setWriter(writer);

        writer = new StringWriter();
        writer.write("setWriter test");
        instance.setWriter(writer);

        Writer w = instance.getWriter();
        assertTrue(writer == w);
        Object expResult = "setWriter test";
        Object result = w.toString();
        assertEquals(expResult, result);
    }

}