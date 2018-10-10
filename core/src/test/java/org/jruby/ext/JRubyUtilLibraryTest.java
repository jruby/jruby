package org.jruby.ext;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.test.MockRubyObject;
import org.jruby.util.ClasspathLauncher;

import static org.jruby.ext.jruby.JRubyUtilLibrary.classpath_launcher;

public class JRubyUtilLibraryTest extends TestCase {

    /* Ugly hack to modify environmental variables for these tests only
       source: https://stackoverflow.com/a/496849
     */
    public static void setEnv(Map<String, String> newenv) throws Exception {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for(Class cl : classes) {
            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                Map<String, String> map = (Map<String, String>) obj;
                map.clear();
                map.putAll(newenv);
            }
        }
    }


    public void testClasspathLauncherWithEnvVar() throws Exception {
        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);
        String envRuby = ClasspathLauncher.jrubyCommand(ruby);
        // Set it to be an existing directory
        envRuby = (new File(envRuby)).getParent();
        Map<String, String> newEnv = new HashMap<>(System.getenv());
        newEnv.put("RUBY", envRuby);
        setEnv(newEnv);
        assertEquals(envRuby, System.getenv("RUBY"));
        RubyInstanceConfig updatedConfig = new RubyInstanceConfig();
        Ruby updatedRuby = Ruby.newInstance(updatedConfig);
        assertEquals(envRuby, updatedRuby.getInstanceConfig().getEnvironment().get("RUBY"));
        MockRubyObject obj = new MockRubyObject(updatedRuby);
        assertEquals(envRuby, classpath_launcher(updatedRuby.getCurrentContext(), obj).asJavaString());
    }

    public void testClasspathLauncherWithBogusEnvVar() throws Exception {
        // Set it to be a non-existing path
        String envRuby = "foo";
        Map<String, String> newEnv = new HashMap<>(System.getenv());
        newEnv.put("RUBY", envRuby);
        setEnv(newEnv);
        assertEquals(envRuby, System.getenv("RUBY"));
        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);
        assertEquals(envRuby, ruby.getInstanceConfig().getEnvironment().get("RUBY"));
        MockRubyObject obj = new MockRubyObject(ruby);
        assertEquals(ClasspathLauncher.jrubyCommand(ruby), classpath_launcher(ruby.getCurrentContext(), obj).asJavaString());
    }

    public void testClasspathLauncherWithoutEnvVar() throws Exception {
        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);
        Map<String, String> newEnv = new HashMap<>(System.getenv());
        newEnv.remove("RUBY");
        setEnv(newEnv);
        assertNull(System.getenv("RUBY"));
        MockRubyObject obj = new MockRubyObject(ruby);
        assertEquals(ClasspathLauncher.jrubyCommand(ruby), classpath_launcher(ruby.getCurrentContext(), obj).asJavaString());
    }
}
