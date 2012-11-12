package org.jruby.embed.util;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PropertyName;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author yoko
 */
public class SystemPropertyCatcherTest {
    
    public SystemPropertyCatcherTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        
    }
    
    @After
    public void tearDown() {
        
    }

    /**
     * Test of getScope method, of class SystemPropertyCatcher.
     */
    @Test
    public void testGetScope() {
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "");
        
        LocalContextScope defaultScope = LocalContextScope.SINGLETON;
        
        // no setting
        LocalContextScope result = SystemPropertyCatcher.getScope(defaultScope);
        assertEquals(defaultScope, result);
        
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "singleton");
        result = SystemPropertyCatcher.getScope(defaultScope);
        assertEquals(LocalContextScope.SINGLETON, result);
        
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "singlethread");
        result = SystemPropertyCatcher.getScope(defaultScope);
        assertEquals(LocalContextScope.SINGLETHREAD, result);
        
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "threadsafe");
        result = SystemPropertyCatcher.getScope(defaultScope);
        assertEquals(LocalContextScope.THREADSAFE, result);
        
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "concurrent");
        result = SystemPropertyCatcher.getScope(defaultScope);
        assertEquals(LocalContextScope.CONCURRENT, result);
        
        System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), "");
    }
}
