package org.jruby.javasupport;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newEmptyArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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

    private static final List<String> EMPTY = Collections.emptyList();
    
    @Test
    public void testAddClassloaderToLoadPathOnTCCL() throws Exception {
        Thread.currentThread().setContextClassLoader( cl );
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setLoader(RubyInstanceConfig.class.getClassLoader());
        URL url = new File("src/test/resources/java_embed_utils").toURI().toURL();
        config.addLoader(new URLClassLoader(new URL[]{url}));
        Ruby runtime = JavaEmbedUtils.initialize(EMPTY, config);
        String result = runtime.evalScriptlet("require 'test_me';$result").toString();
        String expected = "uri:" + url; // File#toURI() adds a trailing '/' for directories
        if (expected.endsWith("/")) expected = expected.substring(0, expected.length() - 1);
        assertEquals(expected, result);
    }

    @Test
    public void testAddClassloaderToLoadPathOnNoneTCCL() throws Exception {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setLoader(RubyInstanceConfig.class.getClassLoader());
        URL url = new File("src/test/resources/java_embed_utils").toURI().toURL();
        config.addLoader(new URLClassLoader(new URL[]{url}));
        Ruby runtime = JavaEmbedUtils.initialize(EMPTY, config);
        String result = runtime.evalScriptlet("require 'test_me';$result").toString();
        String expected = "uri:" + url; // File#toURI() adds a trailing '/' for directories
        if (expected.endsWith("/")) expected = expected.substring(0, expected.length() - 1);
        assertEquals(expected, result);
    }

    @Test
    public void testAPIUsageTheNonGenericWay() { // before <T> generic signatures were introduced (JRuby <= 9.4.0)
        final Ruby runtime = Ruby.newInstance();
        var context = runtime.getCurrentContext();
        IRubyObject str = runtime.evalScriptlet("'foo'");
        Object javaStr = JavaEmbedUtils.rubyToJava(runtime, str, String.class);
        assertEquals("foo", javaStr);

        str = runtime.evalScriptlet("'bar' * 3");
        javaStr = JavaEmbedUtils.rubyToJava(runtime, str, Object.class);
        assertEquals("barbarbar", javaStr);

        Object val = JavaEmbedUtils.rubyToJava(newEmptyArray(context));
        assertEquals("org.jruby.RubyArray", val.getClass().getName());
    }

    @Test
    public void testJavaToRubyPrimitive() {
        final Ruby runtime = Ruby.newInstance();
        ThreadContext context = runtime.getCurrentContext();

        IRubyObject v;
        v = JavaEmbedUtils.javaToRuby(runtime, -100L);
        assertEquals(asFixnum(context, -100), v);

        v = JavaEmbedUtils.javaToRuby(runtime, 200);
        assertEquals(asFixnum(context, 200), v);

        v = JavaEmbedUtils.javaToRuby(runtime, (short) 200);
        assertEquals(asFixnum(context, 200), v);

        v = JavaEmbedUtils.javaToRuby(runtime, (byte) 100);
        assertEquals(asFixnum(context, 100), v);

        v = JavaEmbedUtils.javaToRuby(runtime, 10.0f);
        assertEquals(runtime.newFloat(10.0), v);

        v = JavaEmbedUtils.javaToRuby(runtime, 10.0d);
        assertEquals(runtime.newFloat(10.0), v);

        v = JavaEmbedUtils.javaToRuby(runtime, true);
        assertSame(context.tru, v);

        v = JavaEmbedUtils.javaToRuby(runtime, false);
        assertSame(context.fals, v);
    }

    @Test
    public void testJavaToRuby() {
        final Ruby runtime = Ruby.newInstance();
        ThreadContext context = runtime.getCurrentContext();

        IRubyObject v;
        v = JavaEmbedUtils.javaToRuby(runtime, "");
        assertEquals(runtime.newString(), v);

        v = JavaEmbedUtils.javaToRuby(runtime, Long.valueOf(42L));
        assertEquals(asFixnum(context, 42), v);

        v = JavaEmbedUtils.javaToRuby(runtime, Boolean.TRUE);
        assertSame(context.tru, v);

        v = JavaEmbedUtils.javaToRuby(runtime, new StringBuilder());
        assertEquals(ConcreteJavaProxy.class, v.getClass()); // no more JavaObject wrapping!
    }

    @Test
    public void testRubyToJava() {
        final Ruby runtime = Ruby.newInstance();

        CharSequence sym = JavaEmbedUtils.rubyToJava(runtime.newSymbol("foo"), CharSequence.class);
        assertEquals("foo", sym);
    }

    @Test
    public void testJavaToRubyAndRubyToJava() {
        final Ruby runtime = Ruby.newInstance();

        IRubyObject v = JavaEmbedUtils.javaToRuby(runtime, new ArrayList<>(Arrays.asList("1", '2', 3)));
        assertTrue(v instanceof JavaProxy);
        List<?> val = JavaEmbedUtils.rubyToJava(v);
        assertEquals(3, val.size());

        assertEquals(java.util.ArrayList.class, val.getClass());

        Collection<Object> coll = JavaEmbedUtils.rubyToJava(v, Collection.class);
        assertSame(val, coll);
    }
}