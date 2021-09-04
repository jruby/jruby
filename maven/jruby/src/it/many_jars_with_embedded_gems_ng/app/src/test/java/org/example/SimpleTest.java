package org.example;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;

import org.jruby.embed.IsolatedScriptingContainer;

import org.junit.Test;

public class SimpleTest {

    @Test
    public void test() throws Exception {
        IsolatedScriptingContainer container = new IsolatedScriptingContainer();
	String output = (String) container.runScriptlet("require 'hello';Hello.new( :name => 'world' ).say");

        assertTrue( output, output.contains( "hello world" ) );
        assertTrue( output, output.contains( "zip file name: world" ) );
    }

    @Test
    public void testDifferentClassloader() throws Exception {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	try {
            // make sure we have classloader which does not find jruby
            ClassLoader c = new URLClassLoader( new URL[] {}, null );
	    Thread.currentThread().setContextClassLoader(c);

	    IsolatedScriptingContainer container = new IsolatedScriptingContainer();
	    String output = (String) container.runScriptlet("require 'hello';Hello.new( :name => 'world' ).say");

	    assertTrue( output, output.contains( "hello world" ) );
	    assertTrue( output, output.contains( "zip file name: world" ) );
	}
	finally {
	    Thread.currentThread().setContextClassLoader(cl);
	}
    }

}
