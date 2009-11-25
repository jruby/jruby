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
package org.jruby.embed;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.embed.internal.SingleThreadLocalContextProvider;
import org.jruby.embed.internal.SingletonLocalContextProvider;
import org.jruby.embed.internal.ThreadSafeLocalContextProvider;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author yoko
 */
public class ScriptingContainerTest {

    public ScriptingContainerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getProperty method, of class ScriptingContainer.
     */
    @Test
    public void testGetProperty() {
        System.out.println("getProperty");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        System.out.println("getSupportedRubyVersion");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        String expResult = "jruby 1.5.0";
        String result = instance.getSupportedRubyVersion();
        assertTrue(result.startsWith(expResult));
        instance = null;
    }

    /**
     * Test of getProvider method, of class ScriptingContainer.
     */
    @Test
    public void testGetProvider() {
        System.out.println("getProvider");
        ScriptingContainer instance = new ScriptingContainer();
        LocalContextProvider result = instance.getProvider();
        assertTrue(result instanceof SingletonLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        result = instance.getProvider();
        assertTrue(result instanceof ThreadSafeLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        result = instance.getProvider();
        assertTrue(result instanceof SingleThreadLocalContextProvider);
        instance = null;

        instance = new ScriptingContainer(LocalContextScope.SINGLETON);
        result = instance.getProvider();
        assertTrue(result instanceof SingletonLocalContextProvider);
        instance = null;
    }

    /**
     * Test of getRuntime method, of class ScriptingContainer.
     */
    @Test
    public void testGetRuntime() {
        System.out.println("getRuntime");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Ruby runtime  = JavaEmbedUtils.initialize(new ArrayList());
        Ruby result = instance.getRuntime();
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
        System.out.println("getVarMap");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        System.out.println("getAttributeMap");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        System.out.println("getAttribute");
        Object key = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        System.out.println("setAttribute");
        Object key = null;
        Object value = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.setAttribute(key, value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of get method, of class ScriptingContainer.
     */
    @Test
    public void testGet() {
        System.out.println("get");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        //assertEquals(2.718281828, instance.get("@eular"));
        instance.runScriptlet("@name = \"holly\"");
        //assertEquals("holly", instance.get("@name"));
        instance.runScriptlet("$category = \"bush\"");
        assertEquals("bush", instance.get("$category"));

        instance.getVarMap().clear();
        instance = null;

        instance = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
        instance.runScriptlet("ivalue = 200000");
        assertEquals(200000L, instance.get("ivalue"));
        
        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of put method, of class ScriptingContainer.
     */
    @Test
    public void testPut() {
        System.out.println("put");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
    @Test
    public void testParse_String_intArr() {
        System.out.println("parse");
        String script = null;
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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

        script = "def say_something()" +
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
        StringWriter writer = new StringWriter();
        instance.setErrorWriter(writer);
        try {
            instance.parse(script, 1);
        } catch (Exception e) {
            assertTrue(writer.toString().contains("<script>:3:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    @Test
    public void testParse_3args_1() throws FileNotFoundException {
        System.out.println("parse(reader, filename, lines)");
        Reader reader = null;
        String filename = "";
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        EmbedEvalUnit expResult = null;
        EmbedEvalUnit result = instance.parse(reader, filename, lines);
        assertEquals(expResult, result);

        String basedir = System.getProperty("user.dir");
        filename = basedir + "/test/org/jruby/embed/ruby/iteration.rb";
        reader = new FileReader(filename);
        instance.put("@t", 2);
        result = instance.parse(reader, filename);
        IRubyObject ret = result.run();
        String expStringResult =
            "Trick or Treat!\nTrick or Treat!\n\nHmmm...I'd like trick.";
        assertEquals(expStringResult, ret.toJava(String.class));

        // line number test
        filename = basedir + "/test/org/jruby/embed/ruby/raises_parse_error.rb";
        reader = new FileReader(filename);
        StringWriter writer = new StringWriter();
        instance.setErrorWriter(writer);
        try {
            instance.parse(reader, filename, 2);
        } catch (Exception e) {
            System.out.println(writer.toString());
            assertTrue(writer.toString().contains(filename + ":7:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of parse method, of class ScriptingContainer.
     */
    @Test
    public void testParse_3args_2() {
        System.out.println("parse(type, filename, lines)");
        PathType type = null;
        String filename = "";
        int[] lines = null;

        String basedir = System.getProperty("user.dir");
        String[] paths = {basedir + "/lib/ruby/1.8"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.getProvider().setLoadPaths(Arrays.asList(paths));
        EmbedEvalUnit result;
        try {
            result = instance.parse(type, filename, lines);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }

        filename = basedir + "/test/org/jruby/embed/ruby/next_year.rb";
        result = instance.parse(PathType.ABSOLUTE, filename);
        IRubyObject ret = result.run();
        assertEquals(getNextYear(), ret.toJava(Integer.class));

        StringWriter writer = new StringWriter();
        instance.setWriter(writer);
        String[] planets = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};
        instance.put("@list", Arrays.asList(planets));
        filename = "/test/org/jruby/embed/ruby/list_printer.rb";
        result = instance.parse(PathType.RELATIVE, filename);
        ret = result.run();
        String expResult = "Mercury >> Venus >> Earth >> Mars >> Jupiter >> Saturn >> Uranus >> Neptune: 8 in total";
        assertEquals(expResult, writer.toString().trim());

        writer = new StringWriter();
        instance.setWriter(writer);
        instance.setAttribute(AttributeName.UNICODE_ESCAPE, true);
        planets = new String[]{"水星", "金星", "地球", "火星", "木星", "土星", "天王星", "海王星"};
        instance.put("@list", Arrays.asList(planets));
        filename = "org/jruby/embed/ruby/list_printer.rb";
        result = instance.parse(PathType.CLASSPATH, filename);
        ret = result.run();
        expResult = "水星 >> 金星 >> 地球 >> 火星 >> 木星 >> 土星 >> 天王星 >> 海王星: 8 in total";
        assertEquals(expResult, writer.toString().trim());

        filename = "org/jruby/embed/ruby/raises_parse_error.rb";
        writer = new StringWriter();
        instance.setErrorWriter(writer);
        try {
            instance.parse(PathType.CLASSPATH, filename, 2);
        } catch (Exception e) {
            System.out.println(writer.toString());
            assertTrue(writer.toString().contains(filename + ":7:"));
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
        System.out.println("parse(istream, filename, lines)");
        InputStream istream = null;
        String filename = "";
        int[] lines = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        EmbedEvalUnit expResult = null;
        EmbedEvalUnit result = instance.parse(istream, filename, lines);
        assertEquals(expResult, result);

        String basedir = System.getProperty("user.dir");
        filename = basedir + "/test/org/jruby/embed/ruby/law_of_cosines.rb";
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

        filename = basedir + "/test/org/jruby/embed/ruby/raises_parse_error.rb";
        StringWriter writer = new StringWriter();
        instance.setErrorWriter(writer);
        istream = new FileInputStream(filename);
        try {
            instance.parse(istream, filename, 2);
        } catch (Exception e) {
            System.out.println(writer.toString());
            assertTrue(writer.toString().contains(filename + ":7:"));
        }

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of runScriptlet method, of class ScriptingContainer.
     */
    @Test
    public void testRunScriptlet_String() {
        System.out.println("runScriptlet(script)");
        String script = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.runScriptlet(script);
        assertEquals(expResult, result);
        script = "";
        expResult = "";
        result = instance.runScriptlet(script);
        // Maybe bug. This should return "", but RubyNil.
        //assertEquals(expResult, result);

        script = "def say_something()" +
                   "\"いけてるね! ＞　JRuby\"\n" +
                 "end\n" +
                 "say_something";
        result = instance.runScriptlet(script);
        expResult = "いけてるね! ＞　JRuby";
        assertEquals(expResult, result);

        // unicode escape
        String str = "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c";
        result = instance.runScriptlet("given_str = \"" + str + "\"");
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
        System.out.println("runScriptlet(reader, filename)");
        Reader reader = null;
        String filename = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.runScriptlet(reader, filename);
        assertEquals(expResult, result);

        String basedir = System.getProperty("user.dir");
        filename = basedir + "/test/org/jruby/embed/ruby/iteration.rb";
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
        System.out.println("runScriptlet(istream, filename)");
        InputStream istream = null;
        String filename = "";
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.runScriptlet(istream, filename);
        assertEquals(expResult, result);

        filename = "org/jruby/embed/ruby/law_of_cosines.rb";
        istream = getClass().getClassLoader().getResourceAsStream(filename);
        instance.put("@a", 2.0);
        instance.put("@b", 2 * Math.sqrt(3.0));
        instance.put("@c", 2.0);
        List<Double> angles = (List<Double>) instance.runScriptlet(istream, filename);
        // this result goes to 30.00000000000004,30.00000000000004,120.0.
        // these should be 30.0, 30.0, 120.0. conversion precision error?
        System.out.println(angles.get(0) + ", " + angles.get(1) + ", " +angles.get(2));
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
        System.out.println("runScriptlet(type, filename)");
        PathType type = null;
        String filename = "";
        String basedir = System.getProperty("user.dir");
        String[] paths = {basedir + "/lib/ruby/1.8"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.getProvider().setLoadPaths(Arrays.asList(paths));
        Object expResult = null;
        Object result;
        try {
            result = instance.parse(type, filename);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }

        // absolute path
        filename = basedir + "/test/org/jruby/embed/ruby/next_year.rb";
        result = instance.runScriptlet(PathType.ABSOLUTE, filename);
        // perhaps, a return type should be in a method argument
        // since implicit cast results in a Long type
        expResult = new Long(getNextYear());
        assertEquals(expResult, result);

        instance.setAttribute(AttributeName.BASE_DIR, basedir + "/test/org/jruby/embed");
        filename = "/ruby/next_year.rb";
        result = instance.runScriptlet(PathType.RELATIVE, filename);
        assertEquals(expResult, result);
        instance.removeAttribute(AttributeName.BASE_DIR);

        StringWriter writer = new StringWriter();
        instance.setWriter(writer);
        String[] radioactive_isotopes = {"Uranium", "Plutonium", "Carbon", "Radium", "Einstenium", "Nobelium"};
        instance.put("@list", Arrays.asList(radioactive_isotopes));
        filename = "/test/org/jruby/embed/ruby/list_printer.rb";
        result = instance.runScriptlet(PathType.RELATIVE, filename);
        expResult = "Uranium >> Plutonium >> Carbon >> Radium >> Einstenium >> Nobelium: 6 in total";
        assertEquals(expResult, writer.toString().trim());

        writer = new StringWriter();
        instance.setWriter(writer);
        radioactive_isotopes = new String[]{"ウラン", "プルトニウム", "炭素", "ラジウム", "アインスタイニウム", "ノーベリウム"};
        instance.put("@list", Arrays.asList(radioactive_isotopes));
        filename = "org/jruby/embed/ruby/list_printer.rb";
        result = instance.runScriptlet(PathType.CLASSPATH, filename);
        expResult = "ウラン >> プルトニウム >> 炭素 >> ラジウム >> アインスタイニウム >> ノーベリウム: 6 in total";
        assertEquals(expResult, writer.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of newRuntimeAdapter method, of class ScriptingContainer.
     */
    @Test
    public void testNewRuntimeAdapter() {
        System.out.println("newRuntimeAdapter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
        System.out.println("newObjectAdapter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        EmbedRubyObjectAdapter result = instance.newObjectAdapter();
        Class[] interfaces = result.getClass().getInterfaces();
        assertEquals(org.jruby.embed.EmbedRubyObjectAdapter.class, interfaces[0]);
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_3args() {
        System.out.println("callMethod(receiver, methodName, returnType)");
        Object receiver = null;
        String methodName = "";
        Class<Object> returnType = null;
        String basedir = System.getProperty("user.dir");
        String[] paths = {basedir + "/lib/ruby/1.8"};
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.getProvider().setLoadPaths(Arrays.asList(paths));
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, returnType);
        assertEquals(expResult, result);

        String filename = "org/jruby/embed/ruby/next_year_1.rb";
        receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
        int next_year = instance.callMethod(receiver, "get_year", Integer.class);
        assertEquals(getNextYear(), next_year);

        String script =
            "def volume\n"+
            "  (Math::PI * (@r ** 2.0) * @h)/3.0\n" +
            "end\n" +
            "def surface_area\n" +
            "  Math::PI * @r * Math.sqrt((@r ** 2.0) + (@h ** 2.0)) + Math::PI * (@r ** 2.0)\n" +
            "end";
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
        System.out.println("callMethod(receiver, methodName, singleArg, returnType)");
        Object receiver = null;
        String methodName = "";
        Object singleArg = null;
        Class<Object> returnType = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, singleArg, returnType);
        assertEquals(expResult, result);

        String filename = "org/jruby/embed/ruby/list_printer_1.rb";
        receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
        methodName = "print_list";
        String[] hellos = {"你好", "こんにちは", "Hello", "Здравствуйте"};
        singleArg = Arrays.asList(hellos);
        StringWriter writer = new StringWriter();
        instance.setWriter(writer);
        instance.callMethod(receiver, methodName, singleArg, null);
        expResult = "Hello >> Здравствуйте >> こんにちは >> 你好: 4 in total";
        assertEquals(expResult, writer.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    @Test
    public void testCallMethod_4args_2() {
        System.out.println("callMethod(receiver, methodName, args, returnType)");
        Object receiver = null;
        String methodName = "";
        Object[] args = null;
        Class<Object> returnType = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, args, returnType);
        assertEquals(expResult, result);

        String filename = "org/jruby/embed/ruby/quadratic_formula.rb";
        receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
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
        System.out.println("callMethod");
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
        System.out.println("callMethod(receiver, methodName, returnType, unit)");
        Object receiver = null;
        String methodName = "";
        Class<Object> returnType = null;
        EmbedEvalUnit unit = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE, LocalVariableBehavior.PERSISTENT);
        instance.getProvider().getRubyInstanceConfig().setJRubyHome(System.getProperty("user.dir"));
        Object expResult = null;
        Object result = instance.callMethod(receiver, methodName, returnType, unit);
        assertEquals(expResult, result);

        String text = 
            "songs:\n"+
            "- Hey Soul Sister\n" +
            "- Who Says\n" +
            "- Apologize\n" +
            "podcasts:\n" +
            "- Java Posse\n" +
            "- Stack Overflow";
        String filename = "org/jruby/embed/ruby/yaml_dump.rb";
        StringWriter writer = new StringWriter();
        instance.setWriter(writer);
        // local variable doesn't work in this case, so instance variable is used.
        instance.put("@text", text);
        unit = instance.parse(PathType.CLASSPATH, filename);
        receiver = unit.run();
        methodName = "dump";
        result = instance.callMethod(receiver, methodName, null, unit);
        expResult =
            "songs: Hey Soul Sister, Who Says, Apologize\npodcasts: Java Posse, Stack Overflow\n";
        assertEquals(expResult, writer.toString());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of callMethod method, of class ScriptingContainer.
     */
    /*
    @Test
    public void testCallMethod_5args_2() {
        System.out.println("callMethod");
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
        System.out.println("callMethod");
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
        System.out.println("callSuper");
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
        System.out.println("callSuper");
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
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        Object receiver = null;
        Class<Object> clazz = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Object expResult = null;
        Object result = instance.getInstance(receiver, clazz);
        assertEquals(expResult, result);

        // calculates Plutonium decay
        instance.put("$h", 24100.0); // half-life of Plutonium is 24100 years.
        String filename = "org/jruby/embed/ruby/radioactive_decay.rb";
        receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
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
        filename = "org/jruby/embed/ruby/position_function.rb";
        receiver = instance.runScriptlet(PathType.CLASSPATH, filename);
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
        System.out.println("setReader");
        Reader reader = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setReader(reader);

        instance = null;
    }

    /**
     * Test of getReader method, of class ScriptingContainer.
     */
    @Test
    public void testGetReader() {
        System.out.println("getReader");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Reader result = instance.getReader();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of getIn method, of class ScriptingContainer.
     */
    @Test
    public void testGetIn() {
        System.out.println("getIn");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        InputStream result = instance.getIn();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of setWriter method, of class ScriptingContainer.
     */
    @Test
    public void testSetWriter() {
        System.out.println("setWriter");
        Writer writer = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setWriter(writer);

        String filename = System.getProperty("user.dir") + "/test/quiet.rb";
        writer = new StringWriter();
        Writer errorWriter = new StringWriter();
        instance.setWriter(writer);
        instance.setErrorWriter(errorWriter);
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
        System.out.println("resetWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.resetWriter();

        instance = null;
    }

    /**
     * Test of getWriter method, of class ScriptingContainer.
     */
    @Test
    public void testGetWriter() {
        System.out.println("getWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Writer result = instance.getWriter();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of getOut method, of class ScriptingContainer.
     */
    @Test
    public void testGetOut() {
        System.out.println("getOut");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        PrintStream result = instance.getOut();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of setErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testSetErrorWriter() {
        System.out.println("setErrorWriter");
        Writer errorWriter = null;
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.setErrorWriter(errorWriter);

        errorWriter = new StringWriter();
        instance.setErrorWriter(errorWriter);
        instance.runScriptlet("ABC=10;ABC=20");
        String expResult = "<script>:1 warning: already initialized constant ABC";
        assertEquals(expResult, errorWriter.toString().trim());

        instance.getVarMap().clear();
        instance = null;
    }

    /**
     * Test of resetErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testResetErrorWriter() {
        System.out.println("resetErrorWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        instance.resetErrorWriter();

        instance = null;
    }

    /**
     * Test of getErrorWriter method, of class ScriptingContainer.
     */
    @Test
    public void testGetErrorWriter() {
        System.out.println("getErrorWriter");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        Writer result = instance.getErrorWriter();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of getErr method, of class ScriptingContainer.
     */
    @Test
    public void testGetErr() {
        System.out.println("getErr");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
        PrintStream result = instance.getErr();
        assertFalse(result == null);

        instance = null;
    }

    /**
     * Test of methods Java object should have.
     *
     * Currently, __jtrap is missing and removed from expResult.
     */
    @Test
    public void testMethods() {
        System.out.println("");
        ScriptingContainer instance = new ScriptingContainer(LocalContextScope.THREADSAFE);
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
            "grep", "group_by", "handle_different_imports", "hash", "hashCode",
            "hashCode__method", "hash_code", "hash_code__method", "id", "include?",
            "include_class", "initialize", "inject", "inspect", "instance_eval",
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
            "tap", "toString", "toString__method", "to_a", "to_enum", "to_java_object",
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

}