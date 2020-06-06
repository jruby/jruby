package org.jruby.embed.util;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PropertyName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author yoko
 */
public class SystemPropertyCatcherTest {

    private static String LOCALCONTEXT_PROPERTY;

    @BeforeClass
    public static void setUpClass() {
        LOCALCONTEXT_PROPERTY = System.getProperty(PropertyName.LOCALCONTEXT_SCOPE.toString());
    }

    @AfterClass
    public static void tearDownClass() {
        if ( LOCALCONTEXT_PROPERTY == null ) {
            System.clearProperty(PropertyName.LOCALCONTEXT_SCOPE.toString());
        }
        else {
            System.setProperty(PropertyName.LOCALCONTEXT_SCOPE.toString(), LOCALCONTEXT_PROPERTY);
        }
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
