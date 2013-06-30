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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.embed.bsf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.jruby.embed.PathType;
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
public class JRubyEngineTest {
    private String basedir;

    static Logger logger0 = Logger.getLogger(JRubyEngineTest.class.getName());
    static Logger logger1 = Logger.getLogger(JRubyEngineTest.class.getName());
    static OutputStream outStream = null;

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
    public void setUp() throws FileNotFoundException {
        basedir = System.getProperty("user.dir");
        System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
        System.setProperty("org.jruby.embed.class.path", basedir + "/test");

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
     * Test of apply method, of class JRubyEngine.
     */
    @Test
    public void testApply() throws BSFException {
        logger1.info("apply");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        String file = "";
        int line = 0;
        int col = 0;
        Object funcBody = null;
        Vector paramNames = new Vector();
        Vector args = new Vector();
        Object expResult = null;
        Object result = instance.apply(file, line, col, funcBody, paramNames, args);
        assertEquals(expResult, result);

        expResult = new Long(144);
        result = instance.apply("<script>", 0, 0, "x=144", null, null);
        assertEquals(expResult, result);
        expResult = new Double(12.0);
        result = instance.apply("<script>", 0, 0, "Math.sqrt x", null, null);
        assertEquals(expResult, result);

        paramNames.add("message");
        args.add("red small beans and often used in a form of paste.");
        result = instance.apply("<script>", 0, 0, "ret=\"Azuki beans are #{message}\"", paramNames, args);
        expResult = "Azuki beans are red small beans and often used in a form of paste.";
        assertEquals(expResult, result);
        paramNames.clear();
        args.clear();
        paramNames.add("correction");
        args.add("usually");
        result = instance.apply("<script>", 0, 0, "ret = ret.gsub(/often/, correction)", paramNames, args);
        expResult = "Azuki beans are red small beans and usually used in a form of paste.";
        assertEquals(expResult, result);
    }

    /**
     * Test of eval method, of class JRubyEngine.
     */
    @Test
    public void testEval() throws Exception {
        logger1.info("eval");
        BSFManager.registerScriptingEngine("jruby", "org.jruby.embed.bsf.JRubyEngine", new String[] {"rb"});
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        String file = "";
        int line = 0;
        int col = 0;
        Object expr = null;
        Object expResult = null;
        Object result = instance.eval(file, line, col, expr);
        assertEquals(expResult, result);

        expResult = "HELLO WORLD!";
        result = instance.eval("<script>", 0, 0, "message=\"Hello \" + \"World!\"\nmessage.upcase");
        assertEquals(expResult, result);

        manager.declareBean("greet", "Heeey", String.class);
        result = manager.eval("jruby", "<script>", 0, 0, "message=$greet + \" World!\"");
        expResult = "Heeey World!";
        assertEquals(expResult, result);
    }

    /**
     * Test of exec method, of class JRubyEngine.
     */
    @Test
    public void testExec() throws Exception {
        logger1.info("exec");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        String file = "";
        int line = 0;
        int col = 0;
        Object expr = null;
        instance.exec(file, line, col, expr);

        String partone =
                "def partone\n" +
                  "impression = \"Sooooo Gooood!\"\n" +
                  "f = File.new(\"" + basedir + "/target/bsfeval.txt\", \"w\")\n" +
                  "begin\n" +
                    "f.puts impression\n" +
                  "ensure\n" +
                    "f.close\n" +
                  "end\n" +
                "end\n" +
                "partone";
        String parttwo =
                "def parttwo\n" +
                  "f = File.open \"" + basedir + "/target/bsfeval.txt\"\n" +
                  "begin\n" +
                    "comment = f.gets\n" +
                    "return comment\n" +
                  "ensure\n" +
                    "f.close\n" +
                  "end\n" +
                "end\n" +
                "parttwo";
        instance.exec("<script>", 0, 0, partone);

        Object expResult = "Sooooo Gooood!\n";
        Object result = instance.eval("<script>", 0, 0, parttwo);
        assertEquals(expResult, result);
    }

    /**
     * Test of call method, of class JRubyEngine.
     */
    @Test
    public void testCall() throws Exception {
        logger1.info("call");
        BSFManager.registerScriptingEngine("jruby", "org.jruby.embed.bsf.JRubyEngine", new String[] {"rb"});
        BSFManager manager = new BSFManager();
        JRubyEngine instance = (JRubyEngine) manager.loadScriptingEngine("jruby");
        instance.initialize(manager, "jruby", null);
        Object recv = null;
        String method = "";
        Object[] args = null;
        Object expResult = null;
        Object result = instance.call(recv, method, args);
        assertEquals(expResult, result);

        String script =
                "# Radioactive decay\n" +
                "def amount_after_years(q0, t)\n" +
                  "q0 * Math.exp(1.0 / $h * Math.log(1.0/2.0) * t)\n" +
                "end\n" +
                "def years_to_amount(q0, q)\n" +
                  "$h * (Math.log(q) - Math.log(q0)) / Math.log(1.0/2.0)\n" +
                "end";
        recv = manager.eval("jruby", "radioactive_decay", 0, 0, script);
        method = "amount_after_years";
        args = new Object[2];
        args[0] = 10.0; args[1] = 1000;
        
        // Radium
        manager.declareBean("h", 1599, Long.class);
        result = instance.call(recv, method, args);
        assertEquals(6.482, (Double)result, 0.001);

        method = "years_to_amount";
        args[0] = 10.0; args[1] = 1.0;
        result = instance.call(recv, method, args);
        assertEquals(5311.8, (Double)result, 0.1);
    }

    /**
     * Test of initialize method, of class JRubyEngine.
     */
    @Test
    public void testInitialize() throws Exception {
        logger1.info("initialize");
        BSFManager manager = new BSFManager();;
        String language = "";
        Vector someDeclaredBeans = null;
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, language, someDeclaredBeans);
    }

    /**
     * Test of declareBean method, of class JRubyEngine.
     */
    @Test
    public void testDeclareBean() throws Exception {
        logger1.info("declareBean");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        manager.declareBean("abc", "aaabbbccc", String.class);
        BSFDeclaredBean bean = (BSFDeclaredBean) manager.getObjectRegistry().lookup("abc");
        instance.declareBean(bean);
    }

    /**
     * Test of undeclareBean method, of class JRubyEngine.
     */
    @Test
    public void testUndeclareBean() throws Exception {
        logger1.info("undeclareBean");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        manager.declareBean("abc", "aaabbbccc", String.class);
        BSFDeclaredBean bean = (BSFDeclaredBean) manager.getObjectRegistry().lookup("abc");
        instance.undeclareBean(bean);
    }

    /**
     * Test of handleException method, of class JRubyEngine.
     */
    @Test
    public void testHandleException() throws BSFException {
        logger1.info("handleException");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        BSFException bsfExcptn = new BSFException(BSFException.REASON_EXECUTION_ERROR, "test", new NullPointerException());
        instance.handleException(bsfExcptn);
    }

    /**
     * Test of terminate method, of class JRubyEngine.
     */
    @Test
    public void testTerminate() throws BSFException {
        logger1.info("terminate");
        BSFManager manager = new BSFManager();
        JRubyEngine instance = new JRubyEngine();
        instance.initialize(manager, "jruby", null);
        instance.terminate();
    }

    @Test
    public void testPathTyp() throws BSFException {
        logger1.info("PathType");
        BSFManager.registerScriptingEngine("jruby", "org.jruby.embed.bsf.JRubyEngine", new String[] {"rb"});
        BSFManager manager = new BSFManager();
        JRubyEngine instance = (JRubyEngine) manager.loadScriptingEngine("jruby");
        Object receiver = instance.eval("org/jruby/embed/ruby/radioactive_decay.rb", 0, 0, PathType.CLASSPATH);
        String method = "amount_after_years";
        Object[] args = new Object[2];
        args[0] = 10.0; args[1] = 1000;

        // Plutonium
        manager.declareBean("h", 24100, Long.class);
        Object result = instance.call(receiver, method, args);
        assertEquals(9.716, (Double)result, 0.001);
    }

    @Test
    public void testRubyVersion() throws BSFException {
        logger1.info("RubyVersion");
        BSFManager.registerScriptingEngine("jruby", "org.jruby.embed.bsf.JRubyEngine", new String[] {"rb"});
        BSFManager manager = new BSFManager();
        JRubyEngine instance = (JRubyEngine) manager.loadScriptingEngine("jruby");
        Object result = instance.eval("org/jruby/embed/ruby/block-param-scope.rb", 0, 0, PathType.CLASSPATH);
        String expResult = "cat";
        assertEquals(expResult, ((String)result).trim());

        // Ruby 1.9 mode is somehow broken in 1.5.0dev
        BSFManager.registerScriptingEngine("jruby19", "org.jruby.embed.bsf.JRubyEngine", new String[] {"rb"});
        instance = (JRubyEngine) manager.loadScriptingEngine("jruby19");
        result = instance.eval("org/jruby/embed/ruby/block-param-scope.rb", 0, 0, PathType.CLASSPATH);
        expResult = "bear";
        assertEquals(expResult, ((String)result).trim());
    }

    @Test
    public void testLVar() throws BSFException {
        BSFManager.registerScriptingEngine("jruby",
                "org.jruby.embed.bsf.JRubyEngine", new String[]{"rb"});
        BSFManager bsf = new BSFManager();
        bsf.eval("jruby", "(java)", 1, 1, "$x='GVar'");
        bsf.eval("jruby", "(java)", 1, 1, "@gvar = \"$x = #{$x}\"");

        bsf.eval("jruby", "(java)", 1, 1, "x='LVar'");
        bsf.eval("jruby", "(java)", 1, 1, "at_exit { @result =  \"#{x} and #{$x} in an at_exit block\" }");
        Object ret = bsf.eval("jruby", "(java)", 1, 1, "@lvar = \"x = #{x}\";return @gvar, @lvar");
        List<String> expResult = Arrays.asList("$x = GVar", "x = LVar");
        assertEquals(expResult, ret);
        logger1.info(ret.toString());
        bsf.terminate();
    }

}