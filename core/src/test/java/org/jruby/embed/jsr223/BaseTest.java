package org.jruby.embed.jsr223;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseTest {

    static final String basedir = new File(System.getProperty("user.dir")).getParent();

    final Map<String, String> systemPropertiesMemo = new LinkedHashMap<>();

    @Before
    public void setUp() throws Exception {
        String name;
        systemPropertiesMemo.clear();
        name = "org.jruby.embed.localcontext.scope";
        systemPropertiesMemo.put(name, System.getProperty(name));
        name = "org.jruby.embed.localvariable.behavior";
        systemPropertiesMemo.put(name, System.getProperty(name));
    }

    @After
    public void tearDown() throws Exception {
        systemPropertiesMemo.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

}
