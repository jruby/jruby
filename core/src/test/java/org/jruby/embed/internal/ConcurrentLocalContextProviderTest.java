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
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
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
package org.jruby.embed.internal;

import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Handler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.Map;
import org.jruby.Ruby;
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
public class ConcurrentLocalContextProviderTest {
    static Logger logger0 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static Logger logger1 = Logger.getLogger(ConcurrentLocalContextProviderTest.class.getName());
    static OutputStream outStream = null;

    public ConcurrentLocalContextProviderTest() {
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
        outStream = new FileOutputStream(System.getProperty("user.dir") + "/target/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);
    }

    private class Starter implements Runnable {
        private ConcurrentLocalContextProvider provider;
        private Ruby runtime;
        private RubyInstanceConfig config;
        private BiVariableMap map;
        private Map attributes;

        Starter(ConcurrentLocalContextProvider provider) {
            this.provider = provider;
        }

        Ruby getRuntime() {
            while(runtime == null) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException e) {
                    // no-op
                }
            }
            return runtime;
        }

        RubyInstanceConfig getRubyInstanceConfig() {
            while(config == null) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException e) {
                    // no-op
                }
            }
            return config;
        }

        BiVariableMap getVarMap() {
            while(map == null) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException e) {
                    // no-op
                }
            }
            return map;
        }

        Map getAttributeMap() {
            while(attributes == null) {
                try {
                    Thread.currentThread().sleep(1000L);
                } catch (InterruptedException e) {
                    // no-op
                }
            }
            return attributes;
        }

        public void run() {
            runtime = provider.getRuntime();
            config = provider.getRubyInstanceConfig();
            map = provider.getVarMap();
            attributes = provider.getAttributeMap();
            Thread.currentThread().yield();
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getRuntime method, of class ConcurrentLocalContextProvider.
     */
    //@Test
    public void testGetRuntime() {
        logger1.info("getRuntime");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        Ruby tiramisu = cook.getRuntime();
        assertEquals(true, Ruby.isGlobalRuntimeReady());
        assertEquals(Ruby.getGlobalRuntime(), tiramisu); // only one tiramisu in the world?

        Starter calamari = new Starter(cook); new Thread(calamari).start();
        Starter dumplings = new Starter(cook); new Thread(dumplings).start();

        assertFalse(calamari.getRuntime() == null);
        assertFalse(dumplings.getRuntime() == null);
        assertTrue(calamari.getRuntime() == dumplings.getRuntime());
    }

    /**
     * Test of getRubyInstanceConfig method, of class ConcurrentLocalContextProvider.
     */
    //@Test
    public void testGetRubyInstanceConfig() {
        logger1.info("getRubyInstanceConfig");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        if (Ruby.isGlobalRuntimeReady()) {
            Starter calamari = new Starter(cook); new Thread(calamari).start();
            Starter dumplings = new Starter(cook); new Thread(dumplings).start();

            assertTrue(Ruby.getGlobalRuntime().getInstanceConfig() == cook.getRubyInstanceConfig());
            assertFalse(calamari.getRubyInstanceConfig() == null);
            assertFalse(dumplings.getRubyInstanceConfig() == null);
            assertTrue(calamari.getRubyInstanceConfig() == dumplings.getRubyInstanceConfig());
        } else {
            // no need to test
        }

        ConcurrentLocalContextProviderStub provider = new ConcurrentLocalContextProviderStub(LocalVariableBehavior.TRANSIENT);
        provider.isGlobalRuntimeReady = false;
        assertNotNull( provider.getRubyInstanceConfig() );
        assertFalse( provider.runtimeInitialized ); // make sure we do not call getRuntime()


    }

    /**
     * Test of getVarMap method, of class ConcurrentLocalContextProvider.
     */
    //@Test
    public void testGetVarMap() {
        logger1.info("getVarMap");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        BiVariableMap pizza = cook.getVarMap();

        assertFalse(pizza == null);
        assertEquals(0, pizza.size()); // Orz, hungry...

        Starter calamari = new Starter(cook); new Thread(calamari).start();
        Starter dumplings = new Starter(cook); new Thread(dumplings).start();

        assertFalse(calamari.getVarMap() == null);
        assertFalse(dumplings.getVarMap() == null);
        // BiVariableMap should be thread local
        assertFalse(calamari.getVarMap() == dumplings.getVarMap());
    }

    /**
     * Test of getAttributeMap method, of class ConcurrentLocalContextProvider.
     */
    //@Test
    public void testGetAttributeMap() {
        logger1.info("getAttributeMap");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        Map lasagna = cook.getAttributeMap();
        assertFalse(lasagna == null);
        assertEquals(3, lasagna.size()); // Thank god! I've stored some.

        Starter calamari = new Starter(cook); new Thread(calamari).start();
        Starter dumplings = new Starter(cook); new Thread(dumplings).start();

        assertFalse(calamari.getAttributeMap() == null);
        assertFalse(dumplings.getAttributeMap() == null);
        // AttributeMap should be thread local
        assertFalse(calamari.getAttributeMap() == dumplings.getAttributeMap());
    }

    /**
     * Test of isRuntimeInitialized method, of class ConcurrentLocalContextProvider.
     */
    //@Test
    public void testIsRuntimeInitialized() {
        logger1.info("isRuntimeInitialized");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        boolean result = cook.isRuntimeInitialized();
        if (Ruby.isGlobalRuntimeReady()) assertTrue(result);
        else assertFalse(result);
    }

    //@Test
    public void testTerminate() {
        logger1.info("isTerminate");
        ConcurrentLocalContextProvider cook =
                new ConcurrentLocalContextProvider(LocalVariableBehavior.TRANSIENT, true);
        try {
            cook.terminate();
        } catch (Exception e) {
            fail();
        }
    }

    private static class ConcurrentLocalContextProviderStub extends ConcurrentLocalContextProvider {

        Boolean isGlobalRuntimeReady = null;

        ConcurrentLocalContextProviderStub(LocalVariableBehavior behavior) {
            super( behavior );
        }

        ConcurrentLocalContextProviderStub(LocalVariableBehavior behavior, boolean lazy) {
            super( behavior, lazy );
        }

        boolean runtimeInitialized = false;

        @Override
        public Ruby getRuntime() {
            runtimeInitialized = true;
            return super.getRuntime();
        }

        @Override
        boolean isGlobalRuntimeReady() {
            if ( isGlobalRuntimeReady != null ) {
                return isGlobalRuntimeReady.booleanValue();
            }
            return super.isGlobalRuntimeReady();
        }

    }

}