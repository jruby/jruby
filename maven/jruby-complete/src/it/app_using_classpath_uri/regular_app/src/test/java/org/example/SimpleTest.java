package org.example;

import static org.junit.Assert.assertEquals;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.osgi.bundle.Bundle;
import org.junit.Test;

public class SimpleTest {

    @Test
    public void test() throws Exception {
        IsolatedScriptingContainer container = new IsolatedScriptingContainer();
	// this is not needed outside of OSGi
	container.addLoadPath( Bundle.class.getClassLoader() );
	String output = (String) container.runScriptlet("require 'hello';Hello.say");

        assertEquals( output, "world" );
    }

}
