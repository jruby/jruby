package org.jruby.embed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IsolatedScriptingContainerTest {

    static ClassLoader cl;

    @BeforeClass
    public static void setupClassLoader() {
        cl = Thread.currentThread().getContextClassLoader();
        // make sure we have classloader which does not find jruby
        ClassLoader c = new URLClassLoader( new URL[] {}, null );
        try {
          c.loadClass( "org.jruby.embed.ScriptingContainer" );
          fail( "this classloader shall not find jruby" );
        }
        catch( ClassNotFoundException expected){}
        // set it as context classloader
        Thread.currentThread().setContextClassLoader( c );
    }

    @AfterClass
    public static void restClassLoader() {
        Thread.currentThread().setContextClassLoader( cl );
    }

    @Test
    public void testIsolatedScriptingContainer() throws Exception {
        // we do have an instance of "jruby" loaded via some other classloader
    	//System.setProperty("jruby.debug.loadService", "true");
        ScriptingContainer instance = new IsolatedScriptingContainer();
        instance.runScriptlet("require 'jruby.rb'");
        String result = instance.runScriptlet( "$LOAD_PATH" ).toString();
        assertNotNull(result);

        assertEquals(instance.runScriptlet("JRuby.runtime.jruby_class_loader.parent" ), cl );
        assertEquals(ScriptingContainer.class.getClassLoader(), cl);
    }
}