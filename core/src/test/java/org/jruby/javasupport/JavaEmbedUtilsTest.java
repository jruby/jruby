package org.jruby.javasupport;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public class JavaEmbedUtilsTest {

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

    private static List<String> EMPTY = Collections.emptyList();
    
    @Test
    public void testAddClassloaderToLoadPathOnTCCL() throws Exception {
        Thread.currentThread().setContextClassLoader( cl );
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setLoader(RubyInstanceConfig.class.getClassLoader());
        URL url = new File("src/test/resources/java_embed_utils").toURI().toURL();
        config.addLoader(new URLClassLoader(new URL[]{url}));
        Ruby runtime = JavaEmbedUtils.initialize(EMPTY, config);
        String result = runtime.evalScriptlet("require 'test_me'; $result").toString();
        assertEquals(result, "uri:" + url);
    }

    @Test
    public void testAddClassloaderToLoadPathOnNoneTCCL() throws Exception {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setLoader(RubyInstanceConfig.class.getClassLoader());
        URL url = new File("src/test/resources/java_embed_utils").toURI().toURL();
        config.addLoader(new URLClassLoader(new URL[]{url}));
        Ruby runtime = JavaEmbedUtils.initialize(EMPTY, config);
        String result = runtime.evalScriptlet("require 'test_me';$result").toString();
        assertEquals(result, "uri:" + url);
    }
}