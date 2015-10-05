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

import javax.script.Invocable;
import javax.script.Compilable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.jruby.embed.PositionFunction;
import org.jruby.embed.RadioActiveDecay;
import org.jruby.embed.internal.BiVariableMap;
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
public class JRubyEngineTest {
    private String basedir = new File(System.getProperty("user.dir")).getParent();

    static Logger logger0 = Logger.getLogger(JRubyEngineTest.class.getName());
    static Logger logger1 = Logger.getLogger(JRubyEngineTest.class.getName());
    static OutputStream outStream = null;
    FileWriter writer = null;

    public JRubyEngineTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        outStream.close();
    }

    @Before
    public void setUp() throws FileNotFoundException, IOException {
        outStream = new FileOutputStream(basedir + "/core/target/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);

        writer = new FileWriter(basedir + "/core/target/run-junit-embed.txt", true);
    }

    @After
    public void tearDown() throws IOException {
        writer.close();
    }

    /**
     * Test of compile method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testCompile_String() throws Exception {
        logger1.info("[compile string]");
        ScriptEngine instance;
        synchronized (this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "global");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String script =
            "def norman_window(x, y)\n" +
               "return get_area(x, y), get_perimeter(x, y)\n" +
            "end\n" +
            "def get_area(x, y)\n" +
              "x * y + Math::PI / 8.0 * x ** 2.0\n" +
            "end\n" +
            "def get_perimeter(x, y)\n" +
              "x + 2.0 * y + Math::PI / 2.0 * x\n" +
            "end\n" +
            "norman_window(2, 1)";
        CompiledScript cs = ((Compilable)instance).compile(script);
        List<Double> result = (List<Double>) cs.eval();
        assertEquals(3.570796327, result.get(0), 0.000001);
        assertEquals(7.141592654, result.get(1), 0.000001);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of compile method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testCompile_Reader() throws Exception {
        logger1.info("[compile reader]");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/proverbs_of_the_day.rb";
        Reader reader = new FileReader(filename);

        instance.put("$day", -1);
        CompiledScript cs = ((Compilable)instance).compile(reader);
        String result = (String) cs.eval();
        String expResult = "A rolling stone gathers no moss.";
        assertEquals(expResult, result);
        result = (String) cs.eval();
        expResult = "A friend in need is a friend indeed.";
        assertEquals(expResult, result);
        result = (String) cs.eval();
        expResult = "Every garden may have some weeds.";
        assertEquals(expResult, result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;

    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_String_ScriptContext() throws Exception {
        logger1.info("[eval String with ScriptContext]");
        ScriptEngine instance;
        synchronized (this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "global");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String script =
            "def norman_window(x, y)\n" +
               "return get_area(x, y), get_perimeter(x, y)\n" +
            "end\n" +
            "def get_area(x, y)\n" +
              "x * y + Math::PI / 8.0 * x ** 2.0\n" +
            "end\n" +
            "def get_perimeter(x, y)\n" +
              "x + 2.0 * y + Math::PI / 2.0 * x\n" +
            "end\n" +
            "norman_window(1, 3)";
        ScriptContext context = new SimpleScriptContext();
        List<Double> expResult = new ArrayList();
        expResult.add(3.392);
        expResult.add(8.571);
        List<Double> result = (List<Double>) instance.eval(script, context);
        for (int i=0; i<result.size(); i++) {
            assertEquals(expResult.get(i), result.get(i), 0.01);
        }

        script =
            "def get_area\n" +
              "$x * $y + Math::PI / 8.0 * $x ** 2.0\n" +
            "end\n" +
            "get_area";
        context.setAttribute("x", 1.0, ScriptContext.ENGINE_SCOPE);
        context.setAttribute("y", 3.0, ScriptContext.ENGINE_SCOPE);
        Double result2 = (Double) instance.eval(script, context);
        assertEquals(expResult.get(0), result2, 0.01);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_String_ScriptContext2() throws Exception {
        logger1.info("[eval String with ScriptContext 2]");
        ScriptEngine instance = null;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        ScriptContext context = new SimpleScriptContext();

        String script =
            "def get_area\n" +
              "@x * @y + Math::PI / 8.0 * @x ** 2.0\n" +
            "end\n" +
            "get_area";
        context.setAttribute("@x", 1.0, ScriptContext.ENGINE_SCOPE);
        context.setAttribute("@y", 3.0, ScriptContext.ENGINE_SCOPE);
        Double expResult = 3.392;
        Double result = (Double) instance.eval(script, context);
        assertEquals(expResult, result, 0.01);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_Reader_ScriptContext() throws Exception {
        logger1.info("[eval Reader with ScriptContext]");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/list_printer.rb";
        Reader reader = new FileReader(filename);
        ScriptContext context = new SimpleScriptContext();

        String[] big5 = {"Alaska", "Texas", "California", "Montana", "New Mexico"};
        context.setAttribute("@list", Arrays.asList(big5), ScriptContext.ENGINE_SCOPE);
        StringWriter sw = new StringWriter();
        context.setWriter(sw);
        instance.eval(reader, context);
        String expResult = "Alaska >> Texas >> California >> Montana >> New Mexico: 5 in total";
        assertEquals(expResult, sw.toString().trim());

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_String() throws Exception {
        logger1.info("eval String");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        instance.getContext().setWriter(writer);
        instance.eval("p=9.0");
        instance.eval("q = Math.sqrt p");
        instance.eval("puts \"square root of #{p} is #{q}\"");
        Double expResult = 3.0;
        Double result = (Double) instance.get("q");
        assertEquals(expResult, result, 0.01);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_Reader() throws Exception {
        logger1.info("eval Reader");
        ScriptEngine instance;
        synchronized (this) {
            System.setProperty("org.jruby.embed.class.path", basedir + "/lib/ruby/1.9");
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "global");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/next_year.rb";
        Reader reader = new FileReader(filename);

        long expResult = getNextYear();
        Object result = instance.eval(reader);
        assertEquals(expResult, result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    private int getNextYear() {
        Calendar calendar = Calendar.getInstance();
        int this_year = calendar.get(Calendar.YEAR);
        return this_year + 1;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_String_Bindings() throws Exception {
        logger1.info("eval String with Bindings");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String script =
            "def get_perimeter(x, y)\n" +
              "x + 2.0 * y + PI / 2.0 * x\n" +
            "end\n" +
            "get_perimeter(1.5, 1.5)";
        Bindings bindings = new SimpleBindings();
        Double expResult = 6.856;
        bindings.put("PI", 3.1415);
        Double result = (Double) instance.eval(script, bindings);
        assertEquals(expResult, result, 0.01);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of eval method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testEval_Reader_Bindings() throws Exception {
        logger1.info("eval Reader with Bindings");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.class.path", basedir + "/lib/ruby/1.9");
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/count_down.rb";
        Reader reader = new FileReader(filename);
        Bindings bindings = new SimpleBindings();
        bindings.put("@month", 6);
        bindings.put("@day", 2);
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        String result = (String) instance.eval(reader, bindings);
        assertTrue(result.startsWith("Happy") || result.startsWith("You have"));

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of get method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGet() {
        logger1.info("get");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }

        instance.put("abc", "aabc");
        instance.put("@abc", "abbc");
        instance.put("$abc", "abcc");
        String key = "abc";
        Object expResult = "aabc";
        Object result = instance.get(key);
        assertEquals(expResult, result);
        List list = new ArrayList(); list.add("aabc");
        instance.put("abc", list);
        Map map = new HashMap(); map.put("Ruby", "Rocks");
        instance.put("@abc", map);
        result = instance.get(key);
        assertEquals(expResult, ((List)result).get(0));
        key = "@abc";
        expResult = "Rocks";
        result = instance.get(key);
        assertEquals(expResult, ((Map)result).get("Ruby"));

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of put method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testPut() {
        logger1.info("put");
        String key = "";
        Object value = null;
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine instance = manager.getEngineByName("jruby");
        try {
            instance.put(key, value);
        } catch (IllegalArgumentException e) {
            String expResult = "key can not be empty";
            assertEquals(expResult, e.getMessage());
        }

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of getBindings method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGetBindings() throws ScriptException {
        logger1.info("getBindings");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }

        instance.eval("p = 9.0");
        instance.eval("q = Math.sqrt p");
        Double expResult = 9.0;
        int scope = ScriptContext.ENGINE_SCOPE;
        Bindings result = instance.getBindings(scope);
        assertEquals(expResult, (Double)result.get("p"), 0.01);
        expResult = 3.0;
        assertEquals(expResult, (Double)result.get("q"), 0.01);

        scope = ScriptContext.GLOBAL_SCOPE;
        result = instance.getBindings(scope);
        // Bug of livetribe javax.script package impl.
        //assertTrue(result instanceof SimpleBindings);
        //assertEquals(0, result.size());
        JRubyScriptEngineManager manager2 = new JRubyScriptEngineManager();
        instance = (JRubyEngine) manager2.getEngineByName("jruby");
        result = instance.getBindings(scope);
        assertTrue(result instanceof SimpleBindings);
        assertEquals(0, result.size());

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of setBindings method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testSetBindings() throws ScriptException {
        logger1.info("setBindings");
        ScriptEngine instance;
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
            ScriptEngineManager manager = new ScriptEngineManager();
            instance = manager.getEngineByName("jruby");
        }
        String script =
            "def message\n" +
                "\"message: #{@message}\"\n" +
            "end\n" +
            "message";
        Bindings bindings = new SimpleBindings();
        bindings.put("@message", "What's up?");
        int scope = ScriptContext.ENGINE_SCOPE;
        Object expResult = "message: What's up?";
        instance.setBindings(bindings, scope);
        Object result = instance.eval(script);
        assertEquals(expResult, result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        instance = null;
    }

    /**
     * Test of createBindings method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testCreateBindings() {
        logger1.info("createBindings");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine instance = manager.getEngineByName("jruby");
        Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
        Bindings result = instance.createBindings();
        assertNotSame(bindings, result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of getContext method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGetContext() {
        logger1.info("getContext");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine instance = manager.getEngineByName("jruby");
        ScriptContext result = instance.getContext();
        assertNotNull(result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of setContext method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testSetContext() {
        logger1.info("setContext");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine instance = manager.getEngineByName("jruby");
        ScriptContext ctx = new SimpleScriptContext();
        StringWriter sw = new StringWriter();
        sw.write("Have a great summer!");
        ctx.setWriter(sw);
        instance.setContext(ctx);
        ScriptContext result = instance.getContext();
        Writer w = result.getWriter();
        Object expResult = "Have a great summer!";
        assertTrue(sw == result.getWriter());
        assertEquals(expResult, (result.getWriter()).toString());

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of getFactory method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGetFactory() {
        logger1.info("getFactory");
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine instance = manager.getEngineByName("jruby");
        ScriptEngineFactory result = instance.getFactory();
        assertTrue(result instanceof JRubyEngineFactory);
        String expResult = "JSR 223 JRuby Engine";
        String ret = result.getEngineName();
        assertEquals(expResult, ret);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of invokeMethod method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testInvokeMethod() throws Exception {
        logger1.info("invokeMethod");
        ScriptEngine instance = newScriptEngine();
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/tree.rb";
        Reader reader = new FileReader(filename);
        Object receiver = instance.eval(reader);
        String method = "to_s";
        Object[] args = null;
        String expResult = "Cherry blossom is a round shaped,";
        String result = (String) ((Invocable)instance).invokeMethod(receiver, method, args);
        assertTrue(result.startsWith(expResult));

        Bindings bindings = new SimpleBindings();
        bindings.put("name", "cedar");
        bindings.put("shape", "pyramidal");
        bindings.put("foliage", "evergreen");
        bindings.put("color", "nondescript");
        bindings.put("bloomtime", "April - May");
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/tree_given_localvars.rb";
        reader = new FileReader(filename);
        receiver = instance.eval(reader);
        expResult = "Cedar is a pyramidal shaped,";
        result = (String) ((Invocable)instance).invokeMethod(receiver, method, args);
        assertTrue(result.startsWith(expResult));

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of invokeFunction method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testInvokeFunction() throws Exception {
        logger1.info("invokeFunction");
        ScriptEngine instance = newScriptEngine();
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/count_down.rb";
        Reader reader = new FileReader(filename);
        Bindings bindings = new SimpleBindings();
        bindings.put("@month", 6);
        bindings.put("@day", 3);
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        instance.eval(reader, bindings);

        String method = "count_down_birthday";
        bindings.put("@month", 12);
        bindings.put("@day", 3);
        instance.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        Object[] args = null;
        Object result = ((Invocable)instance).invokeFunction(method, args);
        assertTrue(((String)result).startsWith("Happy") || ((String) result).startsWith("You have"));

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of getInterface method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGetInterface_Class() throws FileNotFoundException, ScriptException {
        logger1.info("getInterface (no receiver)");
        ScriptEngine instance = newScriptEngine();
        Class returnType = RadioActiveDecay.class;
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/radioactive_decay.rb";
        Reader reader = new FileReader(filename);
        Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("$h", 5715); // half-life of Carbon
        instance.eval(reader);
        double expResult = 8.857809480593293;
        RadioActiveDecay result = (RadioActiveDecay) ((Invocable)instance).getInterface(returnType);
        assertEquals(expResult, result.amountAfterYears(10.0, 1000), 0.000001);
        expResult = 18984.81906228128;
        assertEquals(expResult, result.yearsToAmount(10.0, 1.0), 0.000001);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /**
     * Test of getInterface method, of class Jsr223JRubyEngine.
     */
    //@Test
    public void testGetInterface_Object_Class() throws FileNotFoundException, ScriptException {
        logger1.info("getInterface (with receiver)");
        ScriptEngine instance = newScriptEngine();
        String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/position_function.rb";
        Reader reader = new FileReader(filename);
        Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("initial_velocity", 30.0);
        bindings.put("initial_height", 30.0);
        bindings.put("system", "metric");
        Object receiver = instance.eval(reader, bindings);
        Class returnType = PositionFunction.class;
        PositionFunction result = (PositionFunction) ((Invocable)instance).getInterface(receiver, returnType);
        double expResult = 75.9;
        double t = 3.0;
        assertEquals(expResult, result.getPosition(t), 0.1);

        expResult = 20.2;
        t = 1.0;
        assertEquals(expResult, result.getVelocity(t), 0.1);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    }

    /*
     * Test of ScriptEngine.ARGV, JRUBY-4090
     */
    //@Test
    public void testARGV() throws ScriptException {
        logger1.info("ScriptEngine.ARGV");
        ScriptEngine instance = newScriptEngine();
        instance.getContext().setErrorWriter(writer);
        String script = "" +
//            "ARGV << 'foo' \n" +
            "if ARGV.length == 0\n" +
            "  raise 'Error No argv passed'\n" +
            "end";
        instance.put(ScriptEngine.ARGV,new String[]{"one param"});
        instance.eval(script);
    }

    /*
     * Test of ScriptEngine.ARGV, JRUBY-4090
     */
    //@Test
    public void testARGV_2() throws ScriptException {
        logger1.info("ScriptEngine.ARGV before initialization");
        ScriptEngine instance = newScriptEngine();
        instance.getContext().setErrorWriter(writer);
        Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(ScriptEngine.ARGV, new String[]{"init params"});
        String script = "" +
//            "ARGV << 'foo' \n" +
            "if ARGV.length == 0\n" +
            "  raise 'Error No argv passed'\n" +
            "end";
        instance.eval(script);
    }

    // This code worked successfully on command-line but never as JUnit test
    // <script>:1: undefined method `+' for nil:NilClass (NoMethodError)
    // raised at "Object obj1 = engine1.eval("$Value + 2010.to_s");"
    //@Test
    public void testMultipleEngineStates() throws ScriptException {
        logger1.info("Multiple Engine States");
        ScriptEngine engine1;
        ScriptEngine engine2;
        synchronized (this) {
            System.setProperty("org.jruby.embed.localcontext.scope", "singlethread");
            System.setProperty("org.jruby.embed.localvariable.behavior", "global");
            ScriptEngineManager manager = new ScriptEngineManager();
            List<ScriptEngineFactory> factories = manager.getEngineFactories();
            ScriptEngineFactory factory = null;
            while (factories.iterator().hasNext()) {
                factory = factories.iterator().next();
                if ("ruby".equals(factory.getLanguageName())) {
                    break;
                }
            }
            engine1 = factory.getScriptEngine();
            engine2 = factory.getScriptEngine();
        }
        engine1.put("Value", "value of the first engine");
        engine2.put("Value", new Double(-0.0149));
        Object obj1 = engine1.eval("$Value + 2010.to_s");
        Object obj2 = engine2.eval("$Value + 2010");
        assertNotSame(obj1, obj2);
    }

    //@Test
    public void testTermination() throws ScriptException {
        logger1.info("Termination Test");
        ScriptEngineManager manager = new ScriptEngineManager();
        JRubyEngine instance = (JRubyEngine) manager.getEngineByName("jruby");
        StringWriter sw = new StringWriter();
        instance.getContext().setWriter(sw);
        instance.eval("x = 'GVar'; at_exit { puts \"#{x} in an at_exit block\" }");
        String expResult = "";
        assertEquals(expResult, sw.toString().trim());

        sw = new StringWriter();
        instance.getContext().setWriter(sw);
        instance.getContext().setAttribute("org.jruby.embed.termination", true, ScriptContext.ENGINE_SCOPE);
        instance.eval("");
        expResult = "GVar in an at_exit block";
        assertEquals(expResult, sw.toString().trim());
        instance.getContext().setAttribute("org.jruby.embed.termination", false, ScriptContext.ENGINE_SCOPE);
    }

    //@Test
    public void testClearVariables() throws ScriptException {
        logger1.info("Clear Variables Test");
        final ScriptEngine instance = newScriptEngine("singlethread", "global");

        instance.put("gvar", ":Gvar");
        String result = (String) instance.eval("$gvar");
        assertEquals(":Gvar", result);

        instance.getBindings(ScriptContext.ENGINE_SCOPE).remove("gvar");
        instance.getContext().setAttribute("org.jruby.embed.clear.variables", true, ScriptContext.ENGINE_SCOPE);

        instance.eval("");
        instance.getContext().setAttribute("org.jruby.embed.clear.variables", false, ScriptContext.ENGINE_SCOPE);

        result = (String) instance.eval("$gvar");
        assertNull(result);
        assertNull( getVarMap(instance).getVariable("$gvar") );

        assertNotNull( instance.eval("ARGV") );
        assertNotNull( getVarMap(instance).getVariable("ARGV") );
    }

    private static BiVariableMap getVarMap(final ScriptEngine engine) {
        return ((JRubyEngine) engine).container.getVarMap();
    }

    private ScriptEngine newScriptEngine() {
        return newScriptEngine("singlethread", "transient");
    }

    private ScriptEngine newScriptEngine(final String scope, final String varBehavior) {
        synchronized(this) {
            System.setProperty("org.jruby.embed.localcontext.scope", scope);
            System.setProperty("org.jruby.embed.localvariable.behavior", varBehavior);
            return new ScriptEngineManager().getEngineByName("jruby");
        }
    }

}