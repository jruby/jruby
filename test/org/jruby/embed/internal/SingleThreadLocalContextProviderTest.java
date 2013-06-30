/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.embed.internal;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Handler;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
public class SingleThreadLocalContextProviderTest {
    static Logger logger0 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static Logger logger1 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static OutputStream outStream = null;
    
    public SingleThreadLocalContextProviderTest() {
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
     * Test of getRuntime method, of class SingleThreadLocalContextProvider.
     */
    @Test
    public void testGetRuntime() {
        logger1.info("getRuntime");
        SingleThreadLocalContextProvider instance = null;
        Ruby expResult = null;
        //Ruby result = instance.getRuntime();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of getVarMap method, of class SingleThreadLocalContextProvider.
     */
    @Test
    public void testGetVarMap() {
        logger1.info("getVarMap");
        SingleThreadLocalContextProvider instance = null;
        BiVariableMap expResult = null;
        //BiVariableMap result = instance.getVarMap();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of getAttributeMap method, of class SingleThreadLocalContextProvider.
     */
    @Test
    public void testGetAttributeMap() {
        logger1.info("getAttributeMap");
        SingleThreadLocalContextProvider instance = null;
        Map expResult = null;
        //Map result = instance.getAttributeMap();
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of isRuntimeInitialized method, of class SingleThreadLocalContextProvider.
     */
    @Test
    public void testIsRuntimeInitialized() {
        logger1.info("isRuntimeInitialized");
        SingleThreadLocalContextProvider instance = 
                new SingleThreadLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        assertFalse(instance.isRuntimeInitialized());
        instance.getRuntime();
        assertTrue(instance.isRuntimeInitialized());
        
    }

    /**
     * Test of terminate method, of class SingleThreadLocalContextProvider.
     */
    @Test
    public void testTerminate() {
        logger1.info("terminate");
        SingleThreadLocalContextProvider instance = 
                new SingleThreadLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        try {
            instance.terminate();
        } catch (Exception e) {
            fail();
        }
    }
}
