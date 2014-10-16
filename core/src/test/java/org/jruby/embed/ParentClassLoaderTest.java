package org.jruby.embed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.net.URLClassLoader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING) // we need the last test method to run last
public class ParentClassLoaderTest {

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
    public void test1ScriptingContainer() throws Exception {
        try {
            // we do have an instance of "jruby" loaded via some other classloader
            ScriptingContainer instance = new ScriptingContainer(LocalContextScope.CONCURRENT);
            instance.runScriptlet( "$LOAD_PATH" ).toString();
            fail("should not come here");
        }
        catch(Exception e){
            //expected
        }
    }

    @Test
    public void test2JRubyEngineFactoryWithWrongPropertyNameCanFail() throws Exception {
        try {
            System.setProperty(PropertyName.CLASSLOADER.toString(), "something");
            // we do have an instance of "jruby" loaded via some other classloader
            ScriptEngineManager m = new ScriptEngineManager();
            m.registerEngineName( "jruby", new JRubyEngineFactory() );
            ScriptEngine jruby = m.getEngineByName("jruby");
            jruby.eval("$LOAD_PATH" ).toString();
            fail("should not come here");
        }
        catch(Exception e){
            //expected
        }
    }

    @Test
    public void test3JRubyEngineFactoryWithNoneClassloaderPropertyNameCanFail() throws Exception {
        try {
            System.setProperty(PropertyName.CLASSLOADER.toString(), "none");
            // we do have an instance of "jruby" loaded via some other classloader
            ScriptEngineManager m = new ScriptEngineManager();
            m.registerEngineName( "jruby", new JRubyEngineFactory() );
            ScriptEngine jruby = m.getEngineByName("jruby");
            jruby.eval("$LOAD_PATH" ).toString();
            fail("should not come here");
        }
        catch(Exception e){
            //expected
        }
    }

    @Test
    // this test needs to be the last since JRubyEngineFactory uses ScriptingContainer with singleton
    // context scope, i.e. once the classloader is setup correctly it remains as it is
    public void test4JRubyEngineFactory() throws Exception {
        System.getProperties().remove(PropertyName.CLASSLOADER.toString());
        // we do have an instance of "jruby" loaded via some other classloader
        ScriptEngineManager m = new ScriptEngineManager();
        m.registerEngineName( "jruby", new JRubyEngineFactory() );
        ScriptEngine jruby = m.getEngineByName("jruby");
        String result = jruby.eval("$LOAD_PATH" ).toString();
        assertNotNull(result);

        assertEquals(jruby.eval("JRuby.runtime.jruby_class_loader.parent" ), cl );
    }

}