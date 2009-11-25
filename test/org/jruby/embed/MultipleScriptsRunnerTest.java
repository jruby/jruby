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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.jruby.CompatVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Yoko Harada <yokolet@gmial.com>
 */
public class MultipleScriptsRunnerTest {
    List<String> loadPaths;
    List<String> ruby19loadPaths;
    String basedir = System.getProperty("user.dir");

    public MultipleScriptsRunnerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        
    }

    @Before
    public void setUp() {
        String[] paths = {
            basedir + "/lib/ruby/1.8",
            basedir + "/lib/ruby/1.8/rdoc",
            basedir + "/lib/ruby/site_ruby/1.8",
            basedir + "/lib/ruby/site_ruby/shared",
            basedir + "/test",
            basedir + "/build/classes/test",
            basedir
        };
        loadPaths = Arrays.asList(paths);
        paths = new String[] {
            basedir + "/lib/ruby/1.9",
            basedir + "/lib/ruby/site_ruby/shared",
            basedir + "/lib/ruby/1.9/rdoc",
            basedir + "/test",
            basedir
        };
        ruby19loadPaths = Arrays.asList(paths);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testScripts() throws FileNotFoundException {
        System.out.println("[multiple scripts runner]");
        ScriptingContainer instance = null;
        List<String> scriptnames = getScriptNames(basedir + "/test");
        Iterator itr = scriptnames.iterator();
        while (itr.hasNext()) {
            try {
                String testname = (String) itr.next();
                System.out.println("\n[" + testname + "]");
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().setLoadPaths(loadPaths);
                instance.getProvider().getRubyInstanceConfig().setObjectSpaceEnabled(true);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.runScriptlet(PathType.CLASSPATH, "test/" + testname);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                instance.getVarMap().clear();
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
     * test_local_jump_error.rb: also fails on an interpreter, JRUBY-2760
     * test_missing_jruby_home.rb: bad test code. jruby.home system property doesn't
     *                             exists during this test. null can't be set to.
     * test_numeric.rb : also fails on an interprter.
     */
    private boolean isTestable(String filename) {
        String[] skipList = {         
            "test_local_jump_error.rb",
            "test_missing_jruby_home.rb",
            "test_numeric.rb"
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
        System.out.println("[ruby 1.9 script]");
        ScriptingContainer instance = null;
        List<String> ruby19names = getRuby19Names(basedir + "/test");
        Iterator itr = ruby19names.iterator();
        while (itr.hasNext()) {
            String testname = (String)itr.next();
            System.out.println("\n[" + testname + "]");
            try {
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().setLoadPaths(ruby19loadPaths);
                instance.getProvider().getRubyInstanceConfig().setCompatVersion(CompatVersion.RUBY1_9);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.runScriptlet(PathType.CLASSPATH, testname);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                instance = null;
            }
        }
    }

    /*
     * test_thread_backtrace.rb: fails when it is loaded from absolute pass but
     *                           succeeds when loaded from classpath.
     */
    @Test
    public void testByAbsolutePath() throws FileNotFoundException {
        String[] testnames = {
            "test_dir.rb",
            "test_file.rb",
            "test_io.rb",
            "test_kernel.rb",
            "test_load.rb",
            "test_load_class_before_rb.rb"
        };
        for (int i=0; i<testnames.length; i++) {
            System.out.println("[" + testnames[i] + "]");
            String testname = basedir + "/test/" + testnames[i];
            ScriptingContainer instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
            instance.getProvider().setLoadPaths(loadPaths);
            instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
            instance.runScriptlet(PathType.ABSOLUTE, testname);

            instance.getVarMap().clear();
            instance = null;
        }
    }

    @Test
    public void test19ByAbsolutePath() throws FileNotFoundException {
        String[] testnames = {
            "test_io_1_9.rb"
        };
        for (int i=0; i<testnames.length; i++) {
            System.out.println("[" + testnames[i] + "]");
            String testname = basedir + "/test/" + testnames[i];
            ScriptingContainer instance;
            try {
                instance = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
                instance.getProvider().setLoadPaths(ruby19loadPaths);
                instance.getProvider().getRubyInstanceConfig().setCompatVersion(CompatVersion.RUBY1_9);
                instance.getProvider().getRubyInstanceConfig().setJRubyHome(basedir);
                instance.runScriptlet(PathType.ABSOLUTE, testname);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                instance = null;
            }
        }
    }
}
