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
package org.jruby.embed;

import org.jruby.Profile;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.RubyInstanceConfig.LoadServiceCreator;
import org.jruby.ast.Node;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.internal.ConcurrentLocalContextProvider;
import org.jruby.embed.internal.LocalContextProvider;
import org.jruby.embed.internal.SingleThreadLocalContextProvider;
import org.jruby.embed.internal.SingletonLocalContextProvider;
import org.jruby.embed.internal.ThreadSafeLocalContextProvider;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KCode;
import org.jruby.util.cli.Options;
import org.jruby.util.io.ChannelDescriptor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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

import static org.junit.Assert.*;

/**
 *
 * @author yoko
 */
public class ScriptingContainerTest {
    static Logger logger0 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static Logger logger1 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static OutputStream outStream = null;
    PrintStream pstream = null;
    FileWriter writer = null;
    String basedir = System.getProperty("jruby.home");

    public ScriptingContainerTest() {
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

        pstream = new PrintStream(outStream, true);
        writer = new FileWriter(basedir + "/core/target/run-junit-embed.txt", true);
    }

    @After
    public void tearDown() throws IOException {
        pstream.close();
        writer.close();
    }

    /**
     * Test of getProperty method, of class ScriptingContainer.
     */
    @Test
    public void testGetProperty() {
        logger1.info("getProperty");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String key = "language.extension";
        String[] extensions = {"rb"};
        String[] result = instance.getProperty(key);
        assertArrayEquals(key, extensions, result);
        key = "language.name";
        String[] names = {"ruby"};
        result = instance.getProperty(key);
        assertArrayEquals(key, names, result);
        instance = null;
    }

    /**
     * Test of getSupportedRubyVersion method, of class ScriptingContainer.
     */
    @Test
    public void testGetSupportedRubyVersion() {
        logger1.info("getSupportedRubyVersion");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String expResult = "jruby " + Constants.VERSION;
        String result = instance.getSupportedRubyVersion();
        assertTrue(result.startsWith(expResult));
        instance = null;
    }

    /**
     * Test of getProvider method, of class ScriptingContainer.
     */
    @Test
    public void testGetProvider() {
        logger1.info("getProvider");
        ScriptingContainer instance = new ScriptingContainer();
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        LocalContextProvider result = instance.getProvider();
        assertTrue(result instanceof SingletonLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        result = instance.getProvider();
        assertTrue(result instanceof ThreadSafeLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        result = instance.getProvider();
        assertTrue(result instanceof SingleThreadLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.SINGLETON);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        result = instance.getProvider();
        assertTrue(result instanceof SingletonLocalContextProvider);
        instance = null;
        
        instance = new ScriptingContainer(LocalContextScope.CONCURRENT);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        result = instance.getProvider();
        assertTrue(result instanceof ConcurrentLocalContextProvider);
        instance = null;
    }

    /**
     * Test of getRuntime method, of class ScriptingContainer.
     */
    @Test
    public void testGetRuntime() {
        logger1.info("getRuntime");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Ruby runtime  = JavaEmbedUtils.initialize(new ArrayList());
        Ruby result = instance.getProvider().getRuntime();
        Class expClazz = runtime.getClass();
        Class resultClazz = result.getClass();
        assertEquals(expClazz, resultClazz);
        instance = null;
    }

    /**
     * Test of getVarMap method, of class ScriptingContainer.
     */
    @Test
    public void testGetVarMap() {
        logger1.info("getVarMap");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        BiVariableMap result = instance.getVarMap();
        result.put("@name", "camellia");
        assertEquals("camellia", instance.getVarMap().get("@name"));
        result.put("COLOR", "red");
        assertEquals("red", instance.getVarMap().get("COLOR"));
        // class variable injection does not work
        //result.put("@@season", "spring");
        //assertEquals("spring", instance.getVarMap().get("@@season"));
        result.put("$category", "flower");
        assertEquals("flower", instance.getVarMap().get("$category"));
        result.put("@name", "maple");
        assertEquals("maple", instance.getVarMap().get("@name"));
        result.put("COLOR", "orangered");
        assertEquals("orangered", instance.getVarMap().get("COLOR"));
        result.put("$category", "tree");
        assertEquals("tree", instance.getVarMap().get("$category"));

        result.put("parameter", 1.2345);
        assertEquals(1.2345, instance.getVarMap().get("parameter"));
        result.put("@coefficient", 4);
        assertEquals(4, instance.getVarMap().get("@coefficient"));
        
        result.clear();
        instance = null;
    }

    /**
     * Test of getAttributeMap method, of class ScriptingContainer.
     */
    @Test
    public void testGetAttributeMap() {
        logger1.info("getAttributeMap");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        
        Map result = instance.getAttributeMap();
        Object obj = result.get(AttributeName.READER);
        assertEquals(obj.getClass(), java.io.InputStreamReader.class);
        obj = result.get(AttributeName.WRITER);
        assertEquals(obj.getClass(), java.io.PrintWriter.class);
        obj = result.get(AttributeName.ERROR_WRITER);
        assertEquals(obj.getClass(), java.io.PrintWriter.class);
        
        result.put(AttributeName.BASE_DIR, "/usr/local/lib");
        assertEquals("/usr/local/lib", result.get(AttributeName.BASE_DIR));

        result.put(AttributeName.LINENUMBER, 5);
        assertEquals(5, result.get(AttributeName.LINENUMBER));

        result.put("むなしいきもち", "虚");
        assertEquals("虚", result.get("むなしいきもち"));

        result.clear();
        instance = null;
    }

    /**
     * Test of getAttribute method, of class ScriptingContainer.
     */
    //@Test
    public void testGetAttribute() {
        logger1.info("getAttribute");
        Object key = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.getAttribute(key);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAttribute method, of class ScriptingContainer.
     */
    //@Test
    public void testSetAttribute() {
        logger1.info("setAttribute");
        Object key = null;
        Object value = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.setAttribute(key, value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of get method, of class ScriptingContainer.
     */
    //@Test
    public void testGet() {
        logger1.info("get");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String key = null;
        try {
            instance.get(key);
        } catch (NullPointerException e) {
            assertEquals("key is null", e.getMessage());
        }
        key = "";
        try {
            instance.get(key);
        } catch (IllegalArgumentException e) {
            assertEquals("key is empty", e.getMessage());
        }
        key = "a";
        Object expResult = null;
        Object result = instance.get(key);
        assertEquals(expResult, result);
        
        instance.put("@name", "camellia");
        assertEquals("camellia", instance.get("@name"));
        instance.put("COLOR", "red");
        assertEquals("red", instance.get("COLOR"));
        // class variables doesn't work
        //varMap.put("@@season", "spring");
        //assertEquals("spring", instance.get("@@season"));
        instance.put("$category", "flower");
        assertEquals("flower", instance.get("$category"));

        // Bug. Can't retrieve instance variables from Ruby.
        instance.runScriptlet("@eular = 2.718281828");
        assertEquals(2.718281828, instance.get("@eular"));
        instance.runScriptlet("@name = \"holly\"");
        assertEquals("holly", instance.get("@name"));
        instance.runScriptlet("$category = \"bush\"");
        assertEquals("bush", instance.get("$category"));

        instance.getVarMap().clear();
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.THREADSAFE, LocalVariableBehavior.PERSISTENT);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.runScriptlet("ivalue = 200000");
        assertEquals(200000L, instance.get("ivalue"));
        
        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of put method, of class ScriptingContainer.
     */
    //@Test
    public void testPut() {
        logger1.info("put");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String key = null;
        try {
            instance.get(key);
        } catch (NullPointerException e) {
            assertEquals("key is null", e.getMessage());
        }
        key = "";
        try {
            instance.get(key);
        } catch (IllegalArgumentException e) {
            assertEquals("key is empty", e.getMessage());
        }
        key = "a";
        Object value = null;
        Object expResult = null;
        Object result = instance.put(key, value);
        Object newValue = "xyz";
        result = instance.put(key, newValue);
        assertEquals(expResult, result);
        expResult = "xyz";
        assertEquals(expResult, instance.get(key));

        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        instance.put("x", 144.0);
        instance.runScriptlet("puts Math.sqrt(x)");
        assertEquals("12.0", sw.toString().trim());

        sw = new StringWriter();
        instance.setWriter(sw);
        instance.put("@x", 256.0);
        instance.runScriptlet("puts Math.sqrt(@x)");
        assertEquals("16.0", sw.toString().trim());

        sw = new StringWriter();
        instance.setWriter(sw);
        instance.put("$x", 9.0);
        instance.runScriptlet("puts Math.sqrt($x)");
        assertEquals("3.0", sw.toString().trim());

        sw = new StringWriter();
        instance.setWriter(sw);
        instance.put("KMTOMI", 0.621);
        instance.runScriptlet("puts \"1 km is #{KMTOMI} miles.\"");
        assertEquals("1 km is 0.621 miles.", sw.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    //@Test
    public void testParse_String_intArr() {
        logger1.info("parse");
        String script = null;
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedEvalUnit expResult = null;
        EmbedEvalUnit result = instance.parse(script, lines);
        assertEquals(expResult, result);

        script = "";
        Ruby runtime = JavaEmbedUtils.initialize(new ArrayList());
        Node node = runtime.parseEval(script, "<script>", null, 0);
        IRubyObject expRet = runtime.runInterpreter(node);
        result = instance.parse(script);
        IRubyObject ret = result.run();
        assertEquals(expRet.toJava(String.class), ret.toJava(String.class));
        // Maybe bug. This returns RubyNil, but it should be ""
        //assertEquals("", ret.toJava(String.class));

        script = "# -*- coding: utf-8 -*-\n" +
                 "def say_something()" +
                   "\"はろ〜、わぁ〜るど！\"\n" +
                 "end\n" +
                 "say_something";
        expRet = runtime.runInterpreter(runtime.parseEval(script, "<script>", null, 0));
        ret = instance.parse(script).run();
        assertEquals(expRet.toJava(String.class), ret.toJava(String.class));

        //sharing variables
        instance.put("what", "Trick or Treat.");
        script = "\"Did you say, #{what}?\"";
        result = instance.parse(script);
        ret = result.run();
        assertEquals("Did you say, Trick or Treat.?", ret.toJava(String.class));

        // line number test
        script = "puts \"Hello World!!!\"\nputs \"Have a nice day!";
        StringWriter sw = new StringWriter();
        instance.setErrorWriter(sw);
        try {
            instance.parse(script, 1);
        } catch (Exception e) {
            assertTrue(sw.toString().contains("<script>:3:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    @Test
    public void testParse_3args_1() throws FileNotFoundException {
        logger1.info("parse(reader, filename, lines)");
        Reader reader = null;
        String filename = "";
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedEvalUnit expResult = null;
        EmbedEvalUnit result = instance.parse(reader, filename, lines);
        assertEquals(expResult, result);

        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/iteration.rb";
        reader = new FileReader(filename);
        instance.put("@t", 2);
        result = instance.parse(reader, filename);
        IRubyObject ret = result.run();
        String expStringResult =
            "Trick or Treat!\nTrick or Treat!\n\nHmmm...I'd like trick.";
        assertEquals(expStringResult, ret.toJava(String.class));

        // line number test
        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/raises_parse_error.rb";
        reader = new FileReader(filename);
        StringWriter sw = new StringWriter();
        instance.setErrorWriter(sw);
        try {
            instance.parse(reader, filename, 2);
        } catch (Exception e) {
            logger1.info(sw.toString());
            assertTrue(sw.toString().contains(filename + ":7:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    @Test
    public void testParse_3args_2() {
        logger1.info("parse(type, filename, lines)");
        PathType type = null;
        String filename = "";
        int[] lines = null;

        String[] paths = {basedir + "/lib", basedir + "/lib/ruby/stdlib"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setLoadPaths(Arrays.asList(paths));
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedEvalUnit result;
        
        try {
            result = instance.parse(type, filename, lines);
        } catch (Throwable t) {
            assertTrue(t.getCause() instanceof FileNotFoundException);
            t.printStackTrace(new PrintStream(outStream));
        }

        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/next_year.rb";
        result = instance.parse(PathType.ABSOLUTE, filename);
        IRubyObject ret = result.run();
        assertEquals(getNextYear(), ret.toJava(Integer.class));

        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        String[] planets = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};
        instance.put("@list", Arrays.asList(planets));
        filename = "/src/test/ruby/org/jruby/embed/ruby/list_printer.rb";
        result = instance.parse(PathType.RELATIVE, filename);
        ret = result.run();
        String expResult = "Mercury >> Venus >> Earth >> Mars >> Jupiter >> Saturn >> Uranus >> Neptune: 8 in total";
        assertEquals(expResult, sw.toString().trim());

        sw = new StringWriter();
        instance.setWriter(sw);
        instance.setAttribute(AttributeName.UNICODE_ESCAPE, true);
        planets = new String[]{"水星", "金星", "地球", "火星", "木星", "土星", "天王星", "海王星"};
        instance.put("@list", Arrays.asList(planets));
        filename = "src/test/ruby/org/jruby/embed/ruby/list_printer.rb";
        result = instance.parse(PathType.RELATIVE, filename);
        ret = result.run();
        expResult = "水星 >> 金星 >> 地球 >> 火星 >> 木星 >> 土星 >> 天王星 >> 海王星: 8 in total";
        assertEquals(expResult, sw.toString().trim());

        filename = "src/test/ruby/org/jruby/embed/ruby/raises_parse_error.rb";
        sw = new StringWriter();
        instance.setErrorWriter(sw);
        try {
            instance.parse(PathType.RELATIVE, filename, 2);
        } catch (Exception e) {
            logger1.info(sw.toString());
            assertTrue(sw.toString().contains(filename + ":7:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    private int getNextYear() {
        Calendar calendar = Calendar.getInstance();
        int this_year = calendar.get(Calendar.YEAR);
        return this_year + 1;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    @Test
    public void testParse_3args_3() throws FileNotFoundException {
        logger1.info("parse(istream, filename, lines)");
        InputStream istream = null;
        String filename = "";
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedEvalUnit expResult = null;
        EmbedEvalUnit result = instance.parse(istream, filename, lines);
        assertEquals(expResult, result);

        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/law_of_cosines.rb";
        istream = new FileInputStream(filename);
        result = instance.parse(istream, filename);
        instance.put("@a", 1);
        instance.put("@b", 1);
        instance.put("@c", 1);
        IRubyObject ret = result.run();
        List<Double> angles = (List) ret.toJava(List.class);
        // this result goes to 60.00000000000001,60.00000000000001,59.99999999999999.
        // these should be 60.0, 60.0, 60.0. conversion precision error?
        for (double angle : angles) {
            assertEquals(60.0, angle, 0.00001);
        }

        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/raises_parse_error.rb";
        StringWriter sw = new StringWriter();
        instance.setErrorWriter(sw);
        istream = new FileInputStream(filename);
        try {
            instance.parse(istream, filename, 2);
        } catch (Exception e) {
            logger1.info(sw.toString());
            assertTrue(sw.toString().contains(filename + ":7:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of runScriptlet method, of class ScriptingContainer.
     */
    @Test
    public void testRunScriptlet_String() {
        logger1.info("runScriptlet(script)");
        String script = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.runScriptlet(script);
        assertEquals(expResult, result);
        script = "";
        expResult = "";
        result = instance.runScriptlet(script);
        // Maybe bug. This should return "", but RubyNil.
        //assertEquals(expResult, result);

        script = "# -*- coding: utf-8 -*-\n" +
                 "def say_something()" +
                   "\"いけてるね! ＞　JRuby\"\n" +
                 "end\n" +
                 "say_something";
        result = instance.runScriptlet(script);
        expResult = "いけてるね! ＞　JRuby";
        assertEquals(expResult, result);

        // unicode escape
        String str = "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c";
        script = "# -*- coding: utf-8 -*-\n" +
                 "given_str = \"" + str + "\"";
        //result = instance.runScriptlet("given_str = \"" + str + "\"");
        result = instance.runScriptlet(script);
        expResult = "こんにちは世界";
        assertEquals(expResult, result);

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of runScriptlet method, of class ScriptingContainer.
     */
    @Test
    public void testRunScriptlet_Reader_String() throws FileNotFoundException {
        logger1.info("runScriptlet(reader, filename)");
        Reader reader = null;
        String filename = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.runScriptlet(reader, filename);
        assertEquals(expResult, result);

        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/iteration.rb";
        reader = new FileReader(filename);
        instance.put("@t", 3);
        result = instance.runScriptlet(reader, filename);
        expResult =
            "Trick or Treat!\nTrick or Treat!\nTrick or Treat!\n\nHmmm...I'd like trick.";
        assertEquals(expResult, result);

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of runScriptlet method, of class ScriptingContainer.
     */
    @Test
    public void testRunScriptlet_InputStream_String() throws FileNotFoundException {
        logger1.info("runScriptlet(istream, filename)");
        InputStream istream = null;
        String filename = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.runScriptlet(istream, filename);
        assertEquals(expResult, result);

        filename = "src/test/ruby/org/jruby/embed/ruby/law_of_cosines.rb";
        instance.put("@a", 2.0);
        instance.put("@b", 2 * Math.sqrt(3.0));
        instance.put("@c", 2.0);
        List<Double> angles = (List<Double>) instance.runScriptlet(PathType.RELATIVE, filename);
        // this result goes to 30.00000000000004,30.00000000000004,120.0.
        // these should be 30.0, 30.0, 120.0. conversion precision error?
        logger1.info(angles.get(0) + ", " + angles.get(1) + ", " +angles.get(2));
        assertEquals(30.0, angles.get(0), 0.00001);
        assertEquals(30.0, angles.get(1), 0.00001);
        assertEquals(120.0, angles.get(2), 0.00001);

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of runScriptlet method, of class ScriptingContainer.
     */
    @Test
    public void testRunScriptlet_PathType_String() {
        logger1.info("runScriptlet(type, filename)");
        PathType type = null;
        String filename = "";
        String[] paths = {basedir + "/lib/ruby/stdlib"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setLoadPaths(Arrays.asList(paths));
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);

        Object expResult = null;
        Object result;
        try {
            result = instance.parse(type, filename);
        } catch (Throwable e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
            e.printStackTrace(new PrintStream(outStream));
        }

        // absolute path
        filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/next_year.rb";
        result = instance.runScriptlet(PathType.ABSOLUTE, filename);
        // perhaps, a return type should be in a method argument
        // since implicit cast results in a Long type
        expResult = new Long(getNextYear());
        assertEquals(expResult, result);

        instance.setAttribute(AttributeName.BASE_DIR, basedir + "/core/src/test/ruby/org/jruby/embed");
        filename = "/ruby/next_year.rb";
        result = instance.runScriptlet(PathType.RELATIVE, filename);
        assertEquals(expResult, result);
        instance.removeAttribute(AttributeName.BASE_DIR);

        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        String[] radioactive_isotopes = {"Uranium", "Plutonium", "Carbon", "Radium", "Einstenium", "Nobelium"};
        instance.put("@list", Arrays.asList(radioactive_isotopes));
        filename = "/src/test/ruby/org/jruby/embed/ruby/list_printer.rb";
        result = instance.runScriptlet(PathType.RELATIVE, filename);
        expResult = "Uranium >> Plutonium >> Carbon >> Radium >> Einstenium >> Nobelium: 6 in total";
        assertEquals(expResult, sw.toString().trim());

        sw = new StringWriter();
        instance.setWriter(sw);
        radioactive_isotopes = new String[]{"ウラン", "プルトニウム", "炭素", "ラジウム", "アインスタイニウム", "ノーベリウム"};
        instance.put("@list", Arrays.asList(radioactive_isotopes));
        filename = "src/test/ruby/org/jruby/embed/ruby/list_printer.rb";
        result = instance.runScriptlet(PathType.RELATIVE, filename);
        expResult = "ウラン >> プルトニウム >> 炭素 >> ラジウム >> アインスタイニウム >> ノーベリウム: 6 in total";
        assertEquals(expResult, sw.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of newRuntimeAdapter method, of class ScriptingContainer.
     */
    @Test
    public void testNewRuntimeAdapter() {
        logger1.info("newRuntimeAdapter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedRubyRuntimeAdapter result = instance.newRuntimeAdapter();
        String script = 
            "def volume\n"+
            "  (Math::PI * (@r ** 2.0) * @h)/3.0\n" +
            "end\n" +
            "def surface_area\n" +
            "  Math::PI * @r * Math.sqrt((@r ** 2.0) + (@h ** 2.0)) + Math::PI * (@r ** 2.0)\n" +
            "end\n" +
            "return volume, surface_area";
        instance.put("@r", 1.0);
        instance.put("@h", Math.sqrt(3.0));
        EmbedEvalUnit unit = result.parse(script);
        IRubyObject ret = unit.run();
        List<Double> rightCircularCone = (List<Double>) ret.toJava(List.class);
        assertEquals(1.813799, rightCircularCone.get(0), 0.000001);
        assertEquals(9.424778, rightCircularCone.get(1), 0.000001);

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of newObjectAdapter method, of class ScriptingContainer.
     */
    @Test
    public void testNewObjectAdapter() {
        logger1.info("newObjectAdapter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        EmbedRubyObjectAdapter result = instance.newObjectAdapter();
        Class[] interfaces = result.getClass().getInterfaces();
        assertEquals(org.jruby.embed.EmbedRubyObjectAdapter.class, interfaces[0]);
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_3args() {
        logger1.info("callMethod(receiver, methodName, returnType)");
        Object receiver = null;
        String methodName = "";
        Class<Object> returnType = null;
        String[] paths = {basedir + "/lib/ruby/stdlib"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setLoadPaths(Arrays.asList(paths));
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);

        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, returnType);
        assertEquals(expResult, result);

        String filename = "src/test/ruby/org/jruby/embed/ruby/next_year_1.rb";
        receiver = instance.runScriptlet(PathType.RELATIVE, filename);
        int next_year = instance.callMethod(receiver, "get_year", Integer.class);
        assertEquals(getNextYear(), next_year);

        String script =
            "def volume\n"+
            "  (Math::PI * (@r ** 2.0) * @h)/3.0\n" +
            "end\n" +
            "def surface_area\n" +
            "  Math::PI * @r * Math.sqrt((@r ** 2.0) + (@h ** 2.0)) + Math::PI * (@r ** 2.0)\n" +
            "end\n" +
            "self";
        receiver = instance.runScriptlet(script);
        instance.put("@r", 1.0);
        instance.put("@h", Math.sqrt(3.0));
        double volume = instance.callMethod(receiver, "volume", Double.class);
        assertEquals(1.813799, volume, 0.000001);
        double surface_area = instance.callMethod(receiver, "surface_area", Double.class);
        assertEquals(9.424778, surface_area, 0.000001);

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_4args_1() {
        logger1.info("callMethod(receiver, methodName, singleArg, returnType)");
        Object receiver = null;
        String methodName = "";
        Object singleArg = null;
        Class<Object> returnType = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, singleArg, returnType);
        assertEquals(expResult, result);

        String filename = "src/test/ruby/org/jruby/embed/ruby/list_printer_1.rb";
        receiver = instance.runScriptlet(PathType.RELATIVE, filename);
        methodName = "print_list";
        String[] hellos = {"你好", "こんにちは", "Hello", "Здравствуйте"};
        singleArg = Arrays.asList(hellos);
        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        instance.callMethod(receiver, methodName, singleArg, null);
        expResult = "Hello >> Здравствуйте >> こんにちは >> 你好: 4 in total";
        assertEquals(expResult, sw.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_4args_2() {
        logger1.info("callMethod(receiver, methodName, args, returnType)");
        Object receiver = null;
        String methodName = "";
        Object[] args = null;
        Class<Object> returnType = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, args, returnType);
        assertEquals(expResult, result);

        String filename = "src/test/ruby/org/jruby/embed/ruby/quadratic_formula.rb";
        receiver = instance.runScriptlet(PathType.RELATIVE, filename);
        methodName = "solve";
        args = new Double[]{12.0, -21.0, -6.0};
        List<Double> solutions = instance.callMethod(receiver, methodName, args, List.class);
        assertEquals(2, solutions.size());
        assertEquals(new Double(-0.25), solutions.get(0));
        assertEquals(new Double(2.0), solutions.get(1));

        args = new Double[]{1.0, 1.0, 1.0};
        try {
            solutions = instance.callMethod(receiver, methodName, args, List.class);
        } catch (RuntimeException e) {
            Throwable t = e.getCause();
            assertTrue(t.getMessage().contains("RangeError"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallMethod_5args_1() {
        logger1.info("callMethod");
        Object receiver = null;
        String methodName = "";
        Object[] args = null;
        Block block = null;
        Class<T> returnType = null;
        ScriptingContainer instance = new ScriptingContainer();
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, args, block, returnType);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_4args_3() {
        // Sharing local variables over method call doesn't work.
        // Should delete methods with unit argument?
        logger1.info("callMethod(receiver, methodName, returnType, unit)");
        Class<Object> returnType = null;
        EmbedEvalUnit unit = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE, LocalVariableBehavior.PERSISTENT);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        
        // Verify that empty message name returns null
        Object result = instance.callMethod(null, "", returnType, unit);
        assertEquals(null, result);

        String text = 
            "songs:\n"+
            "- Hey Soul Sister\n" +
            "- Who Says\n" +
            "- Apologize\n" +
            "podcasts:\n" +
            "- Java Posse\n" +
            "- Stack Overflow";
        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        // local variable doesn't work in this case, so instance variable is used.
        instance.put("@text", text);
        unit = instance.parse(PathType.RELATIVE, "src/test/ruby/org/jruby/embed/ruby/yaml_dump.rb");
        Object receiver = unit.run();
        instance.callMethod(instance.getProvider().getRuntime().getTopSelf(), "dump", null, unit);
        Object expResult =
                "songs: Hey Soul Sister Who Says Apologizepodcasts: Java Posse Stack Overflow\n";
        assertEquals(expResult, sw.toString());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_without_returnType() {
        logger1.info("callMethod no returnType");
        Object receiver = null;
        String methodName = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName);
        assertEquals(expResult, result);
        String script =
                "def say_something\n" +
                  "return \"Oh, well. I'm stucked\"" +
                "end";
        receiver = instance.runScriptlet(script);
        methodName = "say_something";
        String something = (String)instance.callMethod(receiver, methodName);
        assertEquals("Oh, well. I'm stucked", something);

        script =
                "def give_me_foo\n" +
                  "Java::org.jruby.embed.FooArU.new\n" +
                "end";
        receiver = instance.runScriptlet(script);
        methodName = "give_me_foo";
        FooArU foo = (FooArU)instance.callMethod(receiver, methodName);
        assertEquals("May I have your name?", foo.askPolitely());

        script =
                "def give_me_array(*args)\n" +
                  "args\n" +
                "end";
        receiver = instance.runScriptlet(script);
        methodName = "give_me_array";
        List<Double> list =
                (List<Double>)instance.callMethod(receiver, methodName, 3.1415, 2.7182, 1.4142);
        Double[] a = {3.1415, 2.7182, 1.4142};
        List expList = Arrays.asList(a);
        assertEquals(expList, list);
    }

    @Test
    public void test_runRubyMethod_with_non_ruby_receiver() {
        logger1.info("callMethod no returnType");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        assertEquals(true, instance.runRubyMethod(Boolean.class, null, "nil?"));
        assertEquals(true, instance.runRubyMethod(Boolean.class, instance.getProvider().getRuntime().getNil(), "nil?"));
        assertEquals(false, instance.runRubyMethod(Boolean.class, "A Java String", "nil?"));
        String script =
                "ScriptingContainer = Java::org.jruby.embed.ScriptingContainer\n" +
                "class ScriptingContainer\n" +
                  "def say_something\n" +
                    "'Something'\n" +
                  "end\n" +
                "end\n";
        instance.runScriptlet(script);
        String something = instance.runRubyMethod(String.class, instance, "say_something");
        assertEquals("Something", something);
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallMethod_5args_2() {
        logger1.info("callMethod");
        Object receiver = null;
        String methodName = "";
        Object[] args = null;
        Class<T> returnType = null;
        EmbedEvalUnit unit = null;
        ScriptingContainer instance = new ScriptingContainer();
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, args, returnType, unit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallMethod_6args() {
        logger1.info("callMethod");
        Object receiver = null;
        String methodName = "";
        Object[] args = null;
        Block block = null;
        Class<T> returnType = null;
        EmbedEvalUnit unit = null;
        ScriptingContainer instance = new ScriptingContainer();
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, args, block, returnType, unit);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of callSuper method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallSuper_3args() {
        logger1.info("callSuper");
        Object receiver = null;
        Object[] args = null;
        Class<T> returnType = null;
        ScriptingContainer instance = new ScriptingContainer();
        Object expResult = null;
        Object result = instance.callSuper(receiver, args, returnType);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of callSuper method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallSuper_4args() {
        logger1.info("callSuper");
        Object receiver = null;
        Object[] args = null;
        Block block = null;
        Class<T> returnType = null;
        ScriptingContainer instance = new ScriptingContainer();
        Object expResult = null;
        Object result = instance.callSuper(receiver, args, block, returnType);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of getInstance method, of class ScriptingContainer.
     */
    //@Test
    public void testGetInstance() {
        logger1.info("getInstance");
        Object receiver = null;
        Class<Object> clazz = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.getInstance(receiver, clazz);
        assertEquals(expResult, result);

        // calculates Plutonium decay
        instance.put("$h", 24100.0); // half-life of Plutonium is 24100 years.
        String filename = "src/test/ruby/org/jruby/embed/ruby/radioactive_decay.rb";
        receiver = instance.runScriptlet(PathType.RELATIVE, filename);
        result = instance.getInstance(receiver, RadioActiveDecay.class);
        double initial = 10.0; // 10.0 g
        double years = 1000; // 1000 years
        double amount_left = ((RadioActiveDecay)result).amountAfterYears(initial, years);
        assertEquals(9.716483752784367, amount_left, 0.00000000001);
        amount_left = 1.0;
        years = ((RadioActiveDecay)result).yearsToAmount(initial, amount_left);
        assertEquals(80058.46708678544, years, 0.00000000001);

        // calculates position and velocity after some seconds have past
        instance.put("initial_velocity", 16.0);
        instance.put("initial_height", 32.0);
        instance.put("system", "english");
        filename = "src/test/ruby/org/jruby/embed/ruby/position_function.rb";
        receiver = instance.runScriptlet(PathType.RELATIVE, filename);
        result = instance.getInstance(receiver, PositionFunction.class);
        double time = 2.0;
        double position = ((PositionFunction)result).getPosition(time);
        assertEquals(0.0, position, 0.01);
        double velocity = ((PositionFunction)result).getVelocity(time);
        assertEquals(-48.0, velocity, 0.01);
        List<String> units = ((PositionFunction)result).getUnits();
        assertEquals("ft./sec", units.get(0));
        assertEquals("ft.", units.get(1));
    }

    /**
     * Test of setReader method, of class ScriptingContainer.
     */
    @Test
    public void testSetReader() {
        logger1.info("setReader");
        Reader reader = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setReader(reader);

        instance = null;
    }

    /**
     * Test of getReader method, of class ScriptingContainer.
     */
    @Test
    public void testGetReader() {
        logger1.info("getReader");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Reader result = instance.getReader();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of getIn method, of class ScriptingContainer.
     */
    //@Test
    public void testGetIn() {
        logger1.info("getIn");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        InputStream result = instance.getInput();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of setWriter method, of class ScriptingContainer.
     */
    @Test
    public void testSetWriter() {
        logger1.info("setWriter");
        Writer sw = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setWriter(writer);

        String filename = System.getProperty("user.dir") + "/src/test/ruby/quiet.rb";
        sw = new StringWriter();
        Writer esw = new StringWriter();
        instance.setWriter(sw);
        instance.setErrorWriter(esw);
        Object result = instance.runScriptlet(PathType.ABSOLUTE, filename);
        String expResult = "foo";
        // This never successes.
        //assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of resetWriter method, of class ScriptingContainer.
     */
    @Test
    public void testResetWriter() {
        logger1.info("resetWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.resetWriter();

        instance = null;
    }

    /**
     * Test of getWriter method, of class ScriptingContainer.
     */
    @Test
    public void testGetWriter() {
        logger1.info("getWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Writer result = instance.getWriter();
        assertTrue(result == writer);

        instance = null;
    }

    /**
     * Test of getOut method, of class ScriptingContainer.
     */
    @Test
    public void testGetOut() {
        logger1.info("getOut");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        PrintStream result = instance.getOutput();
        assertTrue(result == pstream);

        instance = null;
    }

    /**
     * Test of setErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testSetErrorWriter() {
        logger1.info("setErrorWriter");
        Writer esw = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setErrorWriter(esw);

        esw = new StringWriter();
        instance.setErrorWriter(esw);
        instance.runScriptlet("ABC=10;ABC=20");
        String expResult = "<script>:1: warning: already initialized constant ABC";
        assertEquals(expResult, esw.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of resetErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testResetErrorWriter() {
        logger1.info("resetErrorWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.resetErrorWriter();

        instance = null;
    }

    /**
     * Test of getErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testGetErrorWriter() {
        logger1.info("getErrorWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Writer result = instance.getErrorWriter();
        assertTrue(result == writer);

        instance = null;
    }

    /**
     * Test of getErr method, of class ScriptingContainer.
     */
    @Test
    public void testGetErr() {
        logger1.info("getErr");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        PrintStream result = instance.getError();
        assertTrue(result == pstream);

        instance = null;
    }

    /**
     * Test of methods Java object should have.
     *
     * Currently, __jtrap is missing and removed from expResult.
     */
    // This test is really sensitive to internal API changes and needs frequent update.
    // For the time being, this test will be eliminated.
    //@Test
    public void testMethods() {
        logger1.info("");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String[] expResult = {
            "==", "===", "=~", "[]", "[]=", "__id__", "__jcreate!", "__jsend!",
            "__send__", "all?", "any?", "class", "class__method",
            "clear", "clear__method", "clone", "clone__method", "collect", "com",
            "containsKey", "containsKey__method", "containsValue", "containsValue__method",
            "contains_key", "contains_key?", "contains_key__method", "contains_key__method?",
            "contains_value", "contains_value?", "contains_value__method",
            "contains_value__method?", "count", "cycle", "detect", "display", "drop",
            "drop_while", "dup", "each", "each_cons", "each_slice", "each_with_index",
            "empty", "empty?", "empty__method", "empty__method?", "entries", "entrySet",
            "entrySet__method", "entry_set", "entry_set__method", "enum_cons", "enum_for",
            "enum_slice", "enum_with_index", "eql?", "equal?", "equals", "equals?",
            "equals__method", "equals__method?", "extend", "finalize", "finalize__method",
            "find", "find_all", "find_index", "first", "freeze", "frozen?", "get",
            "getClass", "getClass__method", "get__method", "get_class", "get_class__method",
            "grep", "group_by", "hash", "hashCode",
            "hashCode__method", "hash_code", "hash_code__method", "id", "include?",
            "java_import", "initialize", "inject", "inspect", "instance_eval",
            "instance_exec", "instance_of?", "instance_variable_defined?",
            "instance_variable_get", "instance_variable_set", "instance_variables",
            "isEmpty", "isEmpty__method", "is_a?", "is_empty", "is_empty?",
            "is_empty__method", "is_empty__method?", "java", "java_class", "java_kind_of?",
            "java_method", "java_object", "java_object=", "java_send", "javax", "keySet",
            "keySet__method", "key_set", "key_set__method", "kind_of?", "map", "marshal_dump",
            "marshal_load", "max", "max_by", "member?", "method", "methods", "min",
            "min_by", "minmax", "minmax_by", "nil?", "none?", "notify", "notifyAll",
            "notifyAll__method", "notify__method", "notify_all", "notify_all__method",
            "object_id", "org", "partition", "private_methods", "protected_methods",
            "public_methods", "put", "putAll", "putAll__method", "put__method", "put_all",
            "put_all__method", "reduce", "reject", "remove", "remove__method", "respond_to?",
            "reverse_each", "select", "send", "singleton_methods", "size", "size__method",
            "sort", "sort_by", "synchronized", "taint", "tainted?", "take", "take_while",
            "tap", "toString", "toString__method", "to_a", "to_enum", "to_java", "to_java_object",
            "to_s", "to_string", "to_string__method", "type", "untaint", "values",
            "values__method", "wait", "wait__method", "zip"
        };
        String script = "require 'java'\njava.util.HashMap.new.methods.sort";
        List<String> ret = (List<String>)instance.runScriptlet(script);
        assertEquals(expResult.length, ret.size());

        String[] retMethods = ret.toArray(new String[ret.size()]);
        assertArrayEquals(expResult, retMethods);

        instance.clear();
        instance = null;
    }

    /**
     * Test of getLoadPaths method, of class ScriptingContainer.
     */
    @Test
    public void testGetLoadPaths() {
        logger1.info("getLoadPaths");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        List<String> result = instance.getLoadPaths();
        assertTrue(result != null);
        assertTrue(result.size() == 0);
        
        instance = null;
    }

    /**
     * Test of setloadPaths method, of class ScriptingContainer.
     */
    @Test
    public void testSetloadPaths() {
        logger1.info("setloadPaths");
        List<String> paths = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setLoadPaths(paths);
        List<String> expResult = null;
        assertEquals(expResult, instance.getLoadPaths());
        paths = Arrays.asList(new String[]{"abc", "def"});
        instance.setLoadPaths(paths);
        assertArrayEquals(paths.toArray(), instance.getLoadPaths().toArray());

        instance = null;
    }

    /**
     * Test of getInput method, of class ScriptingContainer.
     */
    @Test
    public void testGetInput() {
        logger1.info("getInput");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        InputStream expResult = System.in;
        InputStream result = instance.getInput();
        assertEquals(expResult, result);
    }

    /**
     * Test of setInput method, of class ScriptingContainer.
     */
    @Test
    public void testSetInput_InputStream() {
        logger1.info("setInput");
        InputStream istream = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setInput(istream);
        assertEquals(istream, instance.getInput());
        istream = System.in;
        instance.setInput(istream);
        assertTrue(instance.getInput() instanceof InputStream);

        instance = null;
    }

    /**
     * Test of setInput method, of class ScriptingContainer.
     */
    @Test
    public void testSetInput_Reader() {
        logger1.info("setInput");
        Reader reader = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setInput(reader);
        assertEquals(reader, instance.getInput());

        instance = null;
    }

    /**
     * Test of getOutput method, of class ScriptingContainer.
     */
    @Test
    public void testGetOutput() {
        logger1.info("getOutput");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        PrintStream expResult = System.out;
        PrintStream result = instance.getOutput();
        assertEquals(pstream, result);

        instance = null;
    }

    /**
     * Test of setOutput method, of class ScriptingContainer.
     */
    @Test
    public void testSetOutput_PrintStream() {
        logger1.info("setOutput");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        assertEquals(pstream, instance.getOutput());

        instance = null;
    }

    /**
     * Test of setOutput method, of class ScriptingContainer.
     */
    @Test
    public void testSetOutput_Writer() {
        logger1.info("setOutput");
        Writer ow = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setOutput(ow);
        assertEquals(ow, instance.getOutput());
        ow = new StringWriter();
        instance.setOutput(ow);
        assertTrue(instance.getOutput() instanceof PrintStream);

        instance = null;
    }

    /**
     * Test of getError method, of class ScriptingContainer.
     */
    @Test
    public void testGetError() {
        logger1.info("getError");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        PrintStream expResult = System.err;
        PrintStream result = instance.getError();
        assertEquals(pstream, result);

        instance = null;
    }

    /**
     * Test of setError method, of class ScriptingContainer.
     */
    @Test
    public void testSetError_PrintStream() {
        logger1.info("setError");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        assertEquals(pstream, instance.getError());

        instance = null;
    }

    /**
     * Test of setError method, of class ScriptingContainer.
     */
    @Test
    public void testSetError_Writer() {
        logger1.info("setError");
        Writer ew = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setError(ew);
        assertEquals(ew, instance.getError());
        ew = new StringWriter();
        instance.setError(ew);
        assertTrue(instance.getError() instanceof PrintStream);

        instance = null;
    }

    /**
     * Test of getCompileMode method, of class ScriptingContainer.
     */
    @Test
    public void testGetCompileMode() {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        // compare to default, as specified by properties etc
        assertEquals(Options.COMPILE_MODE.load(), instance.getCompileMode());
    }

    /**
     * Test of setCompileMode method, of class ScriptingContainer.
     */
    @Test
    public void testSetCompileMode() {
        logger1.info("setCompileMode");
        CompileMode mode = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setCompileMode(mode);
        assertEquals(mode, instance.getCompileMode());

        mode = CompileMode.FORCE;
        instance.setCompileMode(mode);
        assertEquals(mode, instance.getCompileMode());

        instance = null;
    }

    /**
     * Test of isRunRubyInProcess method, of class ScriptingContainer.
     */
    @Test
    public void testIsRunRubyInProcess() {
        logger1.info("isRunRubyInProcess");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        boolean expResult = false;
        boolean result = instance.isRunRubyInProcess();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setRunRubyInProcess method, of class ScriptingContainer.
     */
    @Test
    public void testSetRunRubyInProcess() {
        logger1.info("setRunRubyInProcess");
        boolean inprocess = false;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setRunRubyInProcess(inprocess);
        assertEquals(inprocess, instance.isRunRubyInProcess());

        inprocess = true;
        instance.setRunRubyInProcess(inprocess);
        assertEquals(inprocess, instance.isRunRubyInProcess());

        instance = null;
    }

    /**
     * Test of isObjectSpaceEnabled method, of class ScriptingContainer.
     */
    @Test
    public void testIsObjectSpaceEnabled() {
        logger1.info("isObjectSpaceEnabled");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        boolean expResult = false;
        boolean result = instance.isObjectSpaceEnabled();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setObjectSpaceEnabled method, of class ScriptingContainer.
     */
    @Test
    public void testSetObjectSpaceEnabled() {
        logger1.info("setObjectSpaceEnabled");
        boolean enable = false;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setObjectSpaceEnabled(enable);
        assertEquals(enable, instance.isObjectSpaceEnabled());

        instance = null;
    }

    /**
     * Test of getEnvironment method, of class ScriptingContainer.
     */
    @Test
    public void testGetEnvironment() {
        logger1.info("getEnvironment");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Map expResult = System.getenv();
        Map result = instance.getEnvironment();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setEnvironment method, of class ScriptingContainer.
     */
    @Test
    public void testSetEnvironment() {
        logger1.info("setEnvironment");
        Map environment = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setEnvironment(environment);
        assertEquals(new HashMap(), instance.getEnvironment());

        environment = new HashMap();
        environment.put("abc", "def");

        instance.setEnvironment(environment);
        assertEquals(environment, instance.getEnvironment());
        
        instance = null;
    }

    /**
     * Test of getCurrentDirectory method, of class ScriptingContainer.
     */
    @Test
    public void testGetCurrentDirectory() {
        logger1.info("getCurrentDirectory");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String expResult = System.getProperty("user.dir");
        String result = instance.getCurrentDirectory();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setCurrentDirectory method, of class ScriptingContainer.
     */
    @Test
    public void testSetCurrentDirectory() {
        logger1.info("setCurrentDirectory");
        String directory = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());

        directory = "abc";
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());

        directory = System.getProperty( "user.home" );
        instance = new ScriptingContainer();
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());
        
        instance = new ScriptingContainer(LocalContextScope.CONCURRENT);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());
        
        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setCurrentDirectory(directory);
        assertEquals(directory, instance.getCurrentDirectory());
    }

    /**
     * Test of getHomeDirectory method, of class ScriptingContainer.
     */
    @Test
    public void testGetHomeDirectory() {
        logger1.info("getHomeDirectory");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String expResult = System.getenv("JRUBY_HOME");
        if (expResult == null) {
            expResult = System.getProperty("jruby.home");
        }
        if (expResult == null) {
            expResult = System.getProperty("java.io.tmpdir");
        }
        String result = instance.getHomeDirectory();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setHomeDirectory method, of class ScriptingContainer.
     */
    @Test
    public void testSetHomeDirectory() {
        logger1.info("setHomeDirectory");
        String home = ".";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setHomeDirectory(home);
        assertEquals(System.getProperty("user.dir"), instance.getHomeDirectory());

        instance = null;
    }

    /**
     * Test of getClassLoader method, of class ScriptingContainer.
     */
    @Test
    public void testGetClassLoader() {
        logger1.info("getClassLoader");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        ClassLoader expResult = this.getClass().getClassLoader();
        ClassLoader result = instance.getClassLoader();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setClassLoader method, of class ScriptingContainer.
     */
    @Test
    public void testSetClassLoader() {
        logger1.info("setClassLoader");
        ClassLoader loader = ScriptingContainerTest.class.getClassLoader();
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setClassLoader(loader);
        assertEquals(loader, instance.getClassLoader());

        loader = instance.getProvider().getRuntime().getJRubyClassLoader();
        instance.setClassLoader(loader);
        assertEquals(loader, instance.getClassLoader());

        instance = null;
    }

    /**
     * Test of getProfile method, of class ScriptingContainer.
     */
    @Test
    public void testGetProfile() {
        logger1.info("getProfile");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Profile expResult = Profile.DEFAULT;
        Profile result = instance.getProfile();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setProfile method, of class ScriptingContainer.
     */
    @Test
    public void testSetProfile() {
        logger1.info("setProfile");
        Profile profile = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setProfile(profile);
        assertEquals(profile, instance.getProfile());

        profile = Profile.ALL;
        instance.setProfile(profile);
        assertEquals(profile, instance.getProfile());
        
        instance = null;
    }

    /**
     * Test of getLoadServiceCreator method, of class ScriptingContainer.
     */
    @Test
    public void testGetLoadServiceCreator() {
        logger1.info("getLoadServiceCreator");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        LoadServiceCreator expResult = LoadServiceCreator.DEFAULT;
        LoadServiceCreator result = instance.getLoadServiceCreator();
        assertEquals(expResult, result);


        instance = null;
    }

    /**
     * Test of setLoadServiceCreator method, of class ScriptingContainer.
     */
    @Test
    public void testSetLoadServiceCreator() {
        logger1.info("setLoadServiceCreator");
        LoadServiceCreator creator = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setLoadServiceCreator(creator);

        instance = null;
    }

    /**
     * Test of getArgv method, of class ScriptingContainer.
     */
    @Test
    public void testGetArgv() {
        logger1.info("getArgv");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String[] expResult = new String[]{};
        String[] result = instance.getArgv();
        assertArrayEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setArgv method, of class ScriptingContainer.
     */
    @Test
    public void testSetArgv() {
        logger1.info("setArgv");
        String[] argv = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setArgv(argv);
        assertArrayEquals(argv, instance.getArgv());

        instance = null;

        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        //instance.setError(pstream);
        //instance.setOutput(pstream);
        //instance.setWriter(writer);
        //instance.setErrorWriter(writer);
        argv = new String[] {"tree", "woods", "forest"};
        instance.setArgv(argv);
        String script = 
                "def print_argv\n" +
                  "all_of_them = \"\"\n" +
                  "ARGV.each { |item| all_of_them += item }\n" +
                  "return all_of_them\n" +
                "end\n" +
                "print_argv";
        String ret = (String)instance.runScriptlet(script);
        String expResult = "treewoodsforest";
        assertEquals(expResult, ret);

        List<String> list = (List<String>)instance.get("ARGV");
        //Object[] params = (Object[])instance.get("ARGV");
        //assertArrayEquals(argv, params);

        instance = null;
    }

    /**
     * Test of setArgv method, of class ScriptingContainer.
     */
    @Test
    public void testRubyArrayToJava() {
        logger1.info("RubyArray to Java");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String script =
        	"def get_array\n" +
              "return [\"snow\", \"sleet\", \"drizzle\", \"freezing rain\"]\n" +
            "end\n";
        Object receiver = instance.runScriptlet(script);
        String[] params = instance.callMethod(receiver, "get_array", String[].class);
        String[] expParams = {"snow", "sleet", "drizzle", "freezing rain"};
        assertArrayEquals(expParams, params);

        List<String> list = instance.callMethod(receiver, "get_array", List.class);
        List<String> expList = Arrays.asList(expParams);
        assertEquals(expList, list);
    }

    /**
     * Test of getScriptFilename method, of class ScriptingContainer.
     */
    @Test
    public void testGetScriptFilename() {
        logger1.info("getScriptFilename");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String expResult = "<script>";
        String result = instance.getScriptFilename();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setScriptFilename method, of class ScriptingContainer.
     */
    @Test
    public void testSetScriptFilename() {
        logger1.info("setScriptFilename");
        String filename = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setScriptFilename(filename);

        instance = null;

        filename = "["+this.getClass().getCanonicalName()+"]";
        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        instance.setScriptFilename(filename);
        StringWriter sw = new StringWriter();
        instance.setErrorWriter(sw);
        try {
            instance.runScriptlet("puts \"Hello");
        } catch (RuntimeException e) {
            assertTrue(sw.toString().contains(filename));
        }

        instance = null;
    }

    /**
     * Test of getRecordSeparator method, of class ScriptingContainer.
     */
    @Test
    public void testGetRecordSeparator() {
        logger1.info("getRecordSeparator");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String expResult = "\n";
        String result = instance.getRecordSeparator();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setRecordSeparator method, of class ScriptingContainer.
     */
    @Test
    public void testSetRecordSeparator() {
        logger1.info("setRecordSeparator");
        String separator = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setRecordSeparator(separator);

        instance = null;
    }

    /**
     * Test of getKCode method, of class ScriptingContainer.
     */
    @Test
    public void testGetKCode() {
        logger1.info("getKCode");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        KCode expResult = KCode.NONE;
        KCode result = instance.getKCode();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of getJitLogEvery method, of class ScriptingContainer.
     */
    @Test
    public void testGetJitLogEvery() {
        logger1.info("getJitLogEvery");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        int expResult = 0;
        int result = instance.getJitLogEvery();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setJitLogEvery method, of class ScriptingContainer.
     */
    @Test
    public void testSetJitLogEvery() {
        logger1.info("setJitLogEvery");
        int logEvery = 0;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setJitLogEvery(logEvery);

        instance = null;
    }

    /**
     * Test of getJitThreshold method, of class ScriptingContainer.
     */
    @Test
    public void testGetJitThreshold() {
        logger1.info("getJitThreshold");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        int expResult = 50;
        int result = instance.getJitThreshold();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setJitThreshold method, of class ScriptingContainer.
     */
    @Test
    public void testSetJitThreshold() {
        logger1.info("setJitThreshold");
        int threshold = 0;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setJitThreshold(threshold);

        instance = null;
    }

    /**
     * Test of getJitMax method, of class ScriptingContainer.
     */
    @Test
    public void testGetJitMax() {
        logger1.info("getJitMax");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        int expResult = 4096;
        int result = instance.getJitMax();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setJitMax method, of class ScriptingContainer.
     */
    @Test
    public void testSetJitMax() {
        logger1.info("setJitMax");
        int max = 0;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setJitMax(max);

        instance = null;
    }

    /**
     * Test of getJitMaxSize method, of class ScriptingContainer.
     */
    @Test
    public void testGetJitMaxSize() {
        logger1.info("getJitMaxSize");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        int expResult = 30000;
        int result = instance.getJitMaxSize();
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of setJitMaxSize method, of class ScriptingContainer.
     */
    @Test
    public void testSetJitMaxSize() {
        logger1.info("setJitMaxSize");
        int maxSize = 0;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.setJitMaxSize(maxSize);

        instance = null;
    }

    /**
     * Test of removeAttribute method, of class ScriptingContainer.
     */
    @Test
    public void testRemoveAttribute() {
        logger1.info("removeAttribute");
        Object key = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        Object expResult = null;
        Object result = instance.removeAttribute(key);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of remove method, of class ScriptingContainer.
     */
    @Test
    public void testRemove() {
        logger1.info("remove");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        String key = "abc";
        String value = "def";
        instance.put(key, value);
        Object expResult = "def";
        Object result = instance.remove(key);
        assertEquals(expResult, result);

        instance = null;
    }

    /**
     * Test of clear method, of class ScriptingContainer.
     */
    @Test
    public void testClear() {
        logger1.info("clear");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setWriter(writer);
        instance.setErrorWriter(writer);
        instance.clear();
        instance.put("abc", "local_def");
        instance.put("$abc", "global_def");
        instance.put("@abc", "instance_def");
        assertEquals(3, instance.getProvider().getVarMap().size());

        instance.clear();
        assertEquals(0, instance.getProvider().getVarMap().size());

        instance = null;
    }

    /**
     * Test of sharing local vars when JIT mode is set, of class ScriptingContainer.
     * Test for JRUBY-4695. JIT mode allows sharing variables, but FORCE mode doesn't so far.
     */
    //@Test
    public void testSharingVariableWithCompileMode() {
        logger1.info("sharing vars over JIT mode");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setError(pstream);
        instance.setOutput(pstream);
        instance.setErrorWriter(writer);
        
        instance.put("my_var", "Hullo!");

        StringWriter sw = new StringWriter();
        instance.setWriter(sw);
        instance.setCompileMode(CompileMode.OFF);
        instance.runScriptlet("puts my_var");
        assertEquals("Hullo!", sw.toString().trim());

        // need to put the lvar again since lvars vanish after eval on a transient setting
        instance.put("my_var", "Hullo!"); 
        sw = new StringWriter();
        instance.setWriter(sw);
        instance.setCompileMode(CompileMode.JIT);
        instance.runScriptlet("puts my_var");
        assertEquals("Hullo!", sw.toString().trim());

        instance.put("my_var", "Hullo!");
        sw = new StringWriter();
        instance.setWriter(sw);
        instance.setCompileMode(CompileMode.FORCE);
        instance.runScriptlet("puts my_var");
        assertEquals("Hullo!", sw.toString().trim());

        instance = null;
    }
    
    public void testEmbedEvalUnitCompileModes() {
        org.jruby.embed.ScriptingContainer container = new org.jruby.embed.ScriptingContainer();
        container.setCompileMode(CompileMode.OFF);
        EmbedEvalUnit evalUnit1 = container.parse("$one = \"script 1: success\"");
        EmbedEvalUnit evalUnit2 = container.parse("def script2() ; $two = \"script 2: success\"; end; script2()");
        evalUnit1.run();
        evalUnit2.run();
        assertEquals("script 1: success", container.get("$one").toString());
        assertEquals("script 2: success", container.get("$two").toString());

        container = new org.jruby.embed.ScriptingContainer();
        container.setCompileMode(CompileMode.JIT);
        evalUnit1 = container.parse("$one = \"script 1: success\"");
        evalUnit2 = container.parse("def script2() ; $two = \"script 2: success\"; end; script2()");
        evalUnit1.run();
        evalUnit2.run();
        assertEquals("script 1: success", container.get("$one").toString());
        assertEquals("script 2: success", container.get("$two").toString());

        container = new org.jruby.embed.ScriptingContainer();
        container.setCompileMode(CompileMode.FORCE);
        evalUnit1 = container.parse("$one = \"script 1: success\"");
        evalUnit2 = container.parse("def script2() ; $two = \"script 2: success\"; end; script2()");
        evalUnit1.run();
        evalUnit2.run();
        assertEquals("script 1: success", container.get("$one").toString());
        assertEquals("script 2: success", container.get("$two").toString());
    }


    /**
     * Test of Thread.currentThread().setContextClassLoader method
     */
    @Test
    public void testNullToContextClassLoader() {
        logger1.info("Thread.currentThread().setContextClassLoader(null)");
        ScriptingContainer instance = null;
        try {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        } catch (NullPointerException e) {
            fail(e.getMessage());
        } finally {
            instance = null;
        }
    }
    
    /**
     * Test of setClassLoader method, of SystemPropertyCatcher.
     * This method is only used in JSR223 but tested here. Since, JSR223
     * is not easy to test internal state.
     */
    @Test
    public void testSystemPropertyCatcherSetClassloader() {
        logger1.info("SystemPropertyCatcher.setClassloader");
        System.setProperty(PropertyName.CLASSLOADER.toString(), "container");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        org.jruby.embed.util.SystemPropertyCatcher.setClassLoader(instance);
        assertEquals(instance.getClass().getClassLoader(), instance.getClassLoader());
        
        System.setProperty(PropertyName.CLASSLOADER.toString(), "context");
        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        org.jruby.embed.util.SystemPropertyCatcher.setClassLoader(instance);
        assertEquals(Thread.currentThread().getContextClassLoader(), instance.getClassLoader());
    }
    
    /**
     * Test of setClassLoader method, of SystemPropertyCatcher.
     * This method is only used in JSR223 but tested here. Since, JSR223
     * is not easy to test internal state.
     */
    //@Test
    public void testScopeInCallMethod() {
        logger1.info("Scope in callMethod should not be null");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        Object someInstance = instance.runScriptlet("Object.new");
        Object result = instance.callMethod(someInstance, "instance_eval", "self", "<eval>", 1);
        assertNotNull(result);
    }

    @Test
    public void testExitTerminatesScript() {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        Object result = instance.runScriptlet("exit 1234");
        assertEquals(1234L, result);
    }

    @Test
    public void testLoadPathOfScriptingContainer() {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        // note that instance.getLoadPath is not the load-path of the runtime !!!
        String[] results = instance.runScriptlet("$LOAD_PATH").toString().split(", ");
        for(String result : results){
            assertTrue(result + " containt lib/ruby/", result.contains("lib/ruby/"));
        }
    }

    @Test
    public void testClasspathScriptletHasClasspathFile() {
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        Object result = instance.runScriptlet(PathType.CLASSPATH, "__FILE__.rb");
        assertEquals("classpath:/__FILE__.rb", result.toString());
    }
}
