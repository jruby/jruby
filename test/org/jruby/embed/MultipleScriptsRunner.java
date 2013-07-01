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
package org.jruby.embed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.jruby.CompatVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Yoko Harada <yokolet@gmial.com>
 */
public class MultipleScriptsRunner {
    List<String> loadPaths;
    List<String> ruby19loadPaths;
    String basedir = System.getProperty("user.dir");

    static Logger logger0 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static Logger logger1 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static OutputStream outStream = null;

    public MultipleScriptsRunner() {
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
        String[] paths = {
            basedir + "/lib/ruby/1.8",
            basedir + "/lib/ruby/1.8/rdoc",
            basedir + "/lib/ruby/shared",
            basedir + "/test",
            basedir + "/target/test-classes",
            basedir
        };
        loadPaths = Arrays.asList(paths);
        paths = new String[] {
            basedir + "/lib/ruby/1.9",
            basedir + "/lib/ruby/shared",
            basedir + "/lib/ruby/1.9/rdoc",
            basedir + "/test",
            basedir
        };
        ruby19loadPaths = Arrays.asList(paths);
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

    @Test
    public void testScripts() throws FileNotFoundException {
        logger0.info("[multiple scripts runner]");
        ScriptingContainer instance = null;
        List<String> scriptnames = getScriptNames(basedir + "/test");
        Iterator itr = scriptnames.iterator();
        while (itr.hasNext()) {
            try {
                String testname = (String) itr.next();
                logger1.info("\n[" + testname + "]");
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(loadPaths);
                instance.getProvider().getRubyInstanceConfig().setObjectSpaceEnabled(true);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.setError(new PrintStream(outStream, true));
                instance.setOutput(new PrintStream(outStream, true));
                instance.setWriter(new FileWriter(basedir + "/target/run-junit-embed.txt", true));
                // test_backquote.rb fails when the current directory is set.
                //instance.getProvider().getRubyInstanceConfig().setCurrentDirectory(basedir + "/test");
                instance.runScriptlet(PathType.CLASSPATH, testname);
            } catch (Throwable t) {
                t.printStackTrace(new PrintStream(outStream));
            } finally {
                if (instance != null) {
                    instance.getVarMap().clear();
                    instance.terminate();
                }
                instance = null;
            }
        }
    }

    private List<String> getScriptNames(String testDir) {
        List<String> list = new ArrayList();
        File[] files = new File(testDir).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName();
                if (!filename.endsWith("rb")) {
                    continue;
                }
                if (!isTestable(filename)) {
                    continue;
                }
                if (filename.contains("1_9")) {
                    continue;
                }
                list.add(filename);
            } else if (file.isDirectory()) {
                //String nextDir = testDir + "/" + file.getName();
                //getRubyFileNames(nextDir, filenames);
            }
        }
        return list;
    }

    /*
     * foo_for_test_require_once.rb is a file to be used by test_require_once.rb
     * junit_testrunner.rb can't be evaluated itself.
     * test_backtraces.rb needs java library to be loaded before the evaluation.
     * test_loading_behavior.rb is a file to be used in test_load.rb.
     * test_local_jump_error.rb: also fails on an interpreter, JRUBY-2760
     * test_missing_jruby_home.rb: bad test code. jruby.home system property doesn't
     *                             exists during this test. null can't be set to.
     * test_numeric.rb : also fails on an interprter.
     * test_thread_backtrace.rb fails loaded from both classpath and absolute paths.
     * testLine_*.rbs are used in testLine.rb
     */
    private boolean isTestable(String filename) {
        String[] skipList = {
            "foo_for_test_require_once.rb",
            "load_error.rb",
            "parse_error.rb",
            "junit_testrunner.rb",
            "test_auto_load.rb",
            "test_backtraces.rb",
            "test_command_line_switches.rb",
            "test_dir.rb",
            "test_file.rb",
            "test_io.rb",
            "test_kernel.rb",
            "test_load.rb",
            "test_load_class_before_rb.rb",
            "test_loading_behavior.rb",
            "test_local_jump_error.rb",
            "test_missing_jruby_home.rb",
            "test_numeric.rb",
            "test_thread_backtrace.rb",
            "testLine_block_comment.rb",
            "testLine_block_comment_start.rb",
            "testLine_code.rb",
            "testLine_comment.rb",
            "testLine_line_comment_start.rb",
            "testLine_mixed_comment.rb"
        };
        for (int i = 0; i < skipList.length; i++) {
            if (filename.equals(skipList[i])) {
                return false;
            }
        }
        return true;
    }

    private List<String> getRuby19Names(String testDir) {
        List<String> list = new ArrayList();
        File[] files = new File(testDir).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName();
                if (!filename.endsWith("rb")) {
                    continue;
                }
                if (!is19Testable(filename)) {
                    continue;
                }
                if (filename.contains("1_9")) {
                    list.add(filename);
                }
            } else if (file.isDirectory()) {
                //String nextDir = testDir + "/" + file.getName();
                //getRubyFileNames(nextDir, filenames);
            }
        }
        return list;
    }

    private boolean is19Testable(String filename) {
        String[] skipList = {
            "test_io_1_9.rb",
        };
        for (int i = 0; i < skipList.length; i++) {
            if (filename.equals(skipList[i])) {
                return false;
            }
        }
        return true;
    }

    
    @Test
    public void testRuby19Script() throws FileNotFoundException {
        logger0.info("[ruby 1.9 script]");
        ScriptingContainer instance = null;
        List<String> ruby19names = getRuby19Names(basedir + "/test");
        Iterator itr = ruby19names.iterator();
        while (itr.hasNext()) {
            String testname = (String)itr.next();
            logger1.info("\n[" + testname + "]");
            try {
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(ruby19loadPaths);
                instance.getProvider().getRubyInstanceConfig().setCompatVersion(CompatVersion.RUBY1_9);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.setError(new PrintStream(outStream, true));
                instance.setOutput(new PrintStream(outStream, true));
                instance.setWriter(new FileWriter(basedir + "/target/run-junit-embed.txt", true));
                instance.runScriptlet(PathType.CLASSPATH, testname);
            } catch (Throwable t) {
                t.printStackTrace(new PrintStream(outStream));
            } finally {
                if (instance != null) {
                    instance.terminate();
                }
                instance = null;
            }
        }
    }

    /*
     * test_thread_backtrace.rb: fails when it is loaded from absolute pass but
     *                           succeeds when loaded from classpath.
     */
    @Test
    public void testByAbsolutePath() throws FileNotFoundException, IOException {
        logger0.info("[absolute path]");
        String[] testnames = {
            "test_dir.rb",
            "test_file.rb",
            "test_io.rb",
            "test_kernel.rb",
            "test_load.rb",
            "test_load_class_before_rb.rb"
        };
        for (int i = 0; i < testnames.length; i++) {
            logger1.info("[" + testnames[i] + "]");
            String testname = basedir + "/test/" + testnames[i];
            ScriptingContainer instance = null;
            try {
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(loadPaths);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.setError(new PrintStream(outStream, true));
                instance.setOutput(new PrintStream(outStream, true));
                instance.setWriter(new FileWriter(basedir + "/target/run-junit-embed.txt", true));
                instance.runScriptlet(PathType.ABSOLUTE, testname);
                instance.getVarMap().clear();
            } catch (Throwable t) {
                t.printStackTrace(new PrintStream(outStream));
            } finally {
                instance.terminate();
                instance = null;
            }
        }
    }

    @Test
    public void test19ByAbsolutePath() throws FileNotFoundException {
        logger0.info("[ruby 1.9 absolute path]");
        String[] testnames = {
            "test_io_1_9.rb"
        };
        for (int i=0; i<testnames.length; i++) {
            logger1.info("[" + testnames[i] + "]");
            String testname = basedir + "/test/" + testnames[i];
            ScriptingContainer instance = null;
            try {
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(ruby19loadPaths);
                instance.getProvider().getRubyInstanceConfig().setCompatVersion(CompatVersion.RUBY1_9);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.setError(new PrintStream(outStream, true));
                instance.setOutput(new PrintStream(outStream, true));
                instance.setWriter(new FileWriter(basedir + "/target/run-junit-embed.txt", true));
                instance.runScriptlet(PathType.ABSOLUTE, testname);
            } catch (Throwable t) {
                t.printStackTrace(new PrintStream(outStream));
            } finally {
                instance.terminate();
                instance = null;
            }
        }
    }

    /*
     * test_backtraces.rb fails test_exception_from_thread_with_abort_on_exception_true(TestBacktraces)
     *                    [test_backtraces.rb:288]:
     *                    <SystemExit> expected but was
     *                    <Thread>.
     */
    //@Test
    public void testWithJavaLibrary() throws FileNotFoundException, IOException {
        String[] testnames = {
            //"test_backtraces.rb"
        };
        for (int i=0; i<testnames.length; i++) {
            logger1.info("[" + testnames[i] + "]");
            String testname = testnames[i];
            ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
            instance.getProvider().getRubyInstanceConfig().setLoadPaths(loadPaths);
            instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
            instance.setError(new PrintStream(outStream, true));
            instance.setOutput(new PrintStream(outStream, true));
            instance.setWriter(new FileWriter(basedir + "/target/run-junit-embed.txt", true));
            instance.runScriptlet("require 'java'");
            instance.runScriptlet(PathType.CLASSPATH, testname);

            instance.getVarMap().clear();
            instance.terminate();
            instance = null;
        }
    }
}
