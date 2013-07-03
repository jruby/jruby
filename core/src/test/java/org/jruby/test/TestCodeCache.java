package org.jruby.test;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;  
import org.jruby.util.ClassCache;

import junit.framework.TestCase;

public class TestCodeCache extends TestCase {
    private Ruby runtime1;
    private Ruby runtime2;
    private RubyRuntimeAdapter evaler;
    
    private String savedMode = null;
    private String savedThreshold = null;

    @Override
    protected void setUp() throws Exception {
        savedMode = System.getProperty("jruby.compile.mode");
        savedThreshold = System.getProperty("jruby.jit.threshold");

        System.setProperty("jruby.compile.mode", "JIT");
        System.setProperty("jruby.jit.threshold", "0");

        // construct a new cache with thread's classloader and no limit
        ClassCache classCache = JavaEmbedUtils.createClassCache(Thread.currentThread().getContextClassLoader());
        runtime1 = JavaEmbedUtils.initialize(new ArrayList<Object>(), classCache);
        runtime2 = JavaEmbedUtils.initialize(new ArrayList<Object>(), classCache);
        evaler = JavaEmbedUtils.newRuntimeAdapter();
        
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        JavaEmbedUtils.terminate(runtime1);
        JavaEmbedUtils.terminate(runtime2);
        
        if (savedMode != null) System.setProperty("jruby.jit.threshold", savedMode);
        if (savedThreshold != null) System.setProperty("jruby.jit.threshold", savedThreshold);

        super.tearDown();
    }

    public void testTwoRuntimes() {
        evaler.eval(runtime1, "def foo; 1; end");
        evaler.eval(runtime2, "def foo; 1; end");
        
        for (int i = 0; i < 2; i++) {
            evaler.eval(runtime1, "foo");
        }
        
        for (int i = 0; i < 2; i++) {
            evaler.eval(runtime2, "foo");
        }
        
//        Class<?> script1 = ((DefaultMethod) runtime1.getObject().getMethods().get("foo")).getJITCompilerScript().getClass();
//        Class<?> script2 = ((DefaultMethod) runtime2.getObject().getMethods().get("foo")).getJITCompilerScript().getClass();
        
//        assertSame(script1, script2);
    }
}
