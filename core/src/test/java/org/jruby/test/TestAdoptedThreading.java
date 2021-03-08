/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
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

package org.jruby.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jruby.RubyArray;
import org.jruby.RubyThreadGroup;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.Block;

/**
 * A simple "adopted thread" concurrency test case.
 */
public class TestAdoptedThreading extends TestCase {
    private static Logger LOGGER = Logger.getLogger(TestAdoptedThreading.class
            .getName());

    // Uncomment the "puts" lines if you want to see more detail
    private static final String SCRIPT = "require 'java'\n"
            + "java_import 'org.jruby.test.ITest'\n"
            + "if ITest.instance_of?(Module)\n"
            + "  class TestImpl; include ITest; end\n"
            + "else\n"
            + "  class TestImpl < ITest; end\n"
            + "end\n"
            + "class TestImpl\n"
            + "    def exec(_value)\n"
            + "        #puts \"start executing!\"\n"
            + "        100.times do | item |\n"
            + "           value = \"#{item}\"\n"
            + "        end\n"
            + "        #puts \"end executing1!\"\n"
            + "        exec2(_value)\n"
            + "    end\n" + "    def exec2(_value)\n"
            + "        #puts \"start executing2!\"\n"
            + "        500.times do | item |\n"
            + "           value = \"#{item}\"\n"
            + "        end\n"
            + "        #puts \"end executing2!\"\n"
            + "        \"VALUE: #{_value}\"\n"
            + "    end\n"
            + "end";

    public TestAdoptedThreading(String _name) {
        super(_name);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(TestAdoptedThreading.class);

        return suite;
    }

    private ScriptingContainer container;

    protected void setUp() throws Exception {
        LOGGER.log(Level.FINEST, SCRIPT);
        container = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        container.runScriptlet(SCRIPT);
    }
    
    private static final int RUNNER_COUNT = 10;
    private static final int RUNNER_LOOP_COUNT = 10;

    public void testThreading() {
        Runner[] runners = new Runner[RUNNER_COUNT];
        
        for (int i = 0; i < RUNNER_COUNT; i++) runners[i] = new Runner("R" + i, RUNNER_LOOP_COUNT);
        for (int i = 0; i < RUNNER_COUNT; i++) runners[i].start();

        try {
            for (int i = 0; i < RUNNER_COUNT; i++) runners[i].join();
        } catch (InterruptedException ie) {
            // XXX: do something?
        }
        
        for (int i = 0; i < RUNNER_COUNT; i++) {
            if (runners[i].isFailed()) {
                if (runners[i].getFailureException() != null) {
                    throw runners[i].getFailureException();
                }
            }
        }
    }
    
    public void testThreadsStayAdopted() throws Exception {
        final Object start = new Object();
        final Exception[] fail = {null};
        
        RubyThreadGroup rtg = (RubyThreadGroup)container.runScriptlet("ThreadGroup::Default");
        
        int initialCount = ((RubyArray)rtg.list(Block.NULL_BLOCK)).getLength();

        Thread pausyThread = new Thread() {
            public void run() {
                synchronized (this) {
                    // Notify the calling thread that we're about to go to sleep the first time
                    synchronized(start) {
                        start.notify();
                    }

                    // wait for the go signal
                    try {
                        this.wait();
                    } catch (InterruptedException ie) {
                        fail[0] = ie;
                        return;
                    }
                }

                // run ten separate calls into Ruby, with delay and explicit GC
                for (int i = 0; i < 10; i++) {
                    container.runScriptlet("a = 0; while a < 1000; a += 1; end");
                    System.gc();

                    try {
                        sleep(1000);
                    } catch (InterruptedException ie) {
                        fail[0] = ie;
                        break;
                    }
                }

                synchronized (start) {
                    start.notify();
                }
            }
        };

        synchronized (start) {
            pausyThread.start();
            
            // wait until thread has initialized
            start.wait();
            
            // notify thread to proceed
            synchronized(pausyThread) {
                pausyThread.notify();
            }
            
            // wait until thread has completed
            start.wait();
        }

        // Give thread time to finish
        while (pausyThread.isAlive()) Thread.yield();

        long startTime = System.currentTimeMillis();
        // wait until thread count is as expected or 5s have passed, GCing all the while
        while (rtg.size() != initialCount + 1
                && (System.currentTimeMillis() - startTime) < 5000) {
            System.gc();
        }
        
        // if any exceptions were raised, we fail
        assertNull(fail[0]);
        
        // there should only be one more thread in thread group than before we started
        assertEquals(initialCount + 1, rtg.size());
    }

    class Runner extends Thread {
        private int count_;
        private boolean failed;
        private RuntimeException failureException;

        public Runner(String _name, int _count) {
            count_ = _count;
        }
        
        public boolean isFailed() {
            return failed;
        }
        
        public RuntimeException getFailureException() {
            return failureException;
        }

        public void run() {
            for (int i = 0; i < count_; i++) {
                ITest test = getTest();
                if (test != null) {
                    try {
                        Object result = test.exec("foo");
                        if (!result.toString().equals("VALUE: 5000")) {
                            failed = true;
                        }
                    } catch (RuntimeException re) {
                        failed = true;
                        failureException = re;
                        break;
                    }
                }
            }
        }

        private ITest getTest() {
            ITest result = null;
            synchronized (container) {
                try {
                    result = (ITest) container.runScriptlet("TestImpl.new");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return result;
        }
    }

    public static void main(String[] _args) {
        junit.textui.TestRunner.run(suite());
    }

}
