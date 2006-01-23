package org.jruby.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.bsf.BSFManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAdoptedThreading extends TestCase {
    private static Logger LOGGER = Logger.getLogger(TestAdoptedThreading.class
            .getName());

    private static final String SCRIPT = "require 'java'\n"
            + "include_class 'org.jruby.test.ITest'\n"
            + "class TestImpl < ITest\n" + "    def exec(_value)\n"
            + "        puts \"start executing!\"\n"
            + "        1000.times do | item |\n"
            + "           value = \"#{item}\"\n" + "        end\n"
            + "        puts \"end executing1!\"\n" + "        exec2(_value)\n"
            + "    end\n" + "    def exec2(_value)\n"
            + "        puts \"start executing2!\"\n"
            + "        5000.times do | item |\n"
            + "           value = \"#{item}\"\n" + "        end\n"
            + "        puts \"end executing2!\"\n"
            + "        \"VALUE: #{_value}\"\n" + "    end\n" + "end";

    public TestAdoptedThreading(String _name) {
        super(_name);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(TestAdoptedThreading.class);

        return suite;
    }

    private BSFManager manager_;

    protected void setUp() throws Exception {
        try {
            LOGGER.log(Level.INFO, SCRIPT);
            BSFManager.registerScriptingEngine("ruby",
                    "org.jruby.javasupport.bsf.JRubyEngine",
                    new String[] { "rb" });
            manager_ = new BSFManager();
            manager_.exec("ruby", "(java)", 1, 1, SCRIPT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testThreading() {
        Runner r00 = new Runner("R1", 10);
        Runner r01 = new Runner("R1", 10);
        Runner r02 = new Runner("R1", 10);
        Runner r03 = new Runner("R1", 10);
        Runner r04 = new Runner("R1", 10);
        Runner r05 = new Runner("R1", 10);
        Runner r06 = new Runner("R1", 10);
        Runner r07 = new Runner("R1", 10);
        Runner r08 = new Runner("R1", 10);
        Runner r09 = new Runner("R1", 10);

        r00.start();
        r01.start();
        r02.start();
        r03.start();
        r04.start();
        r05.start();
        r06.start();
        r07.start();
        r08.start();
        r09.start();

        try {
            r00.join();
            r01.join();
            r02.join();
            r03.join();
            r04.join();
            r05.join();
            r06.join();
            r07.join();
            r08.join();
            r09.join();
        } catch (InterruptedException ie) {
            // XXX: do something?
        }
    }

    class Runner extends Thread {
        private int count_;

        public Runner(String _name, int _count) {
            count_ = _count;
        }

        public void run() {
            for (int i = 0; i < count_; i++) {
                ITest test = getTest();
                if (test != null) {
                    LOGGER.log(Level.INFO, "[NAME:" + getName() + "VALUE: "
                            + test.exec("KABOOM!") + ", COUNT: " + i + "]");
                }
            }
        }

        private ITest getTest() {
            ITest result = null;
            synchronized (manager_) {
                try {
                    result = (ITest) manager_.eval("ruby", "(java)", 1, 1,
                            "TestImpl.new");
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
