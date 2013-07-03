/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.embed.internal;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.logging.Handler;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.embed.LocalVariableBehavior;
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
public class ThreadSafeLocalContextProviderTest {
    static Logger logger0 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static Logger logger1 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static OutputStream outStream = null;
    
    public ThreadSafeLocalContextProviderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        outStream = new FileOutputStream(System.getProperty("user.dir") + "/target/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        outStream.close();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getRuntime method, of class ThreadSafeLocalContextProvider.
     */
    @Test
    public void testGetRuntime() {
        logger1.info("getRuntime");
        ThreadSafeLocalContextProvider instance = 
                new ThreadSafeLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        Ruby result = instance.getRuntime();
        assertNotNull(result);
    }

    /**
     * Test of getVarMap method, of class ThreadSafeLocalContextProvider.
     */
    @Test
    public void testGetVarMap() {
        logger1.info("getVarMap");
        ThreadSafeLocalContextProvider instance = null;
        BiVariableMap expResult = null;
        //BiVariableMap result = instance.getVarMap();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of getAttributeMap method, of class ThreadSafeLocalContextProvider.
     */
    @Test
    public void testGetAttributeMap() {
        logger1.info("getAttributeMap");
        ThreadSafeLocalContextProvider instance = null;
        Map expResult = null;
        //Map result = instance.getAttributeMap();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of isRuntimeInitialized method, of class ThreadSafeLocalContextProvider.
     */
    @Test
    public void testIsRuntimeInitialized() {
        logger1.info("isRuntimeInitialized");
        ThreadSafeLocalContextProvider instance = 
                new ThreadSafeLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        assertFalse(instance.isRuntimeInitialized());
        instance.getRuntime();
        assertTrue(instance.isRuntimeInitialized());
    }

    /**
     * Test of terminate method, of class ThreadSafeLocalContextProvider.
     */
    @Test
    public void testTerminate() {
        logger1.info("terminate");
        ThreadSafeLocalContextProvider instance =
                new ThreadSafeLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        try {
            instance.terminate();
        } catch (Exception e) {
            fail();
        }
    }
    
    @Test
    public void testTerminate2() throws NullPointerException {
        logger1.info("terminate");
        ThreadSafeLocalContextProvider instance =
                new ThreadSafeLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        instance.getRuntime();
        try {
            instance.terminate();
        } catch (Exception e) {
            fail();
        }
    }
    
    @Test
    public void testTerminate3() throws NullPointerException {
        logger1.info("terminate");
        ThreadSafeLocalContextProvider instance =
                new ThreadSafeLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        Puppet puppet1 = new Puppet(instance);
        Puppet puppet2 = new Puppet(instance);
        try {
            new Thread(puppet1, "puppet#1").start();
            new Thread(puppet2, "puppet#2").start();
        } catch (Exception e) {
            fail();
        }
    }
    
    private class Puppet implements Runnable {
        private ThreadSafeLocalContextProvider provider;
        private Ruby runtime = null;
        
        private Puppet(ThreadSafeLocalContextProvider provider) {
            this.provider = provider;
        }
        
        private void getRuntime() {
            while(runtime == null) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException e) {
                    // no-op
                }
            }
            provider.getRuntime();
        }

        public void run() {
            getRuntime();
            provider.terminate();
            Thread.currentThread().yield();
        }
        
    }
}
