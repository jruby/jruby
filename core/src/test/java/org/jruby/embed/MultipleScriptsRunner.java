/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2017 Yoko Harada <yokolet@gmail.com>
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
 ***** END LICENSE BLOCK *****/
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
        String[] paths = new String[] {
            basedir + "/lib/ruby/stdlib",
            basedir + "/lib/ruby/stdlib/rdoc",
            basedir + "/test",
            basedir
        };
        loadPaths = Arrays.asList(paths);
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
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(loadPaths);
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
                instance.getProvider().getRubyInstanceConfig().setLoadPaths(loadPaths);
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
}
