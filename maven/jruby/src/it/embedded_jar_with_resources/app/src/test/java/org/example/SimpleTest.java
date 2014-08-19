package org.example;

import static org.junit.Assert.assertTrue;

import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.IsolatedScriptingContainer;

import org.junit.Test;

public class SimpleTest {

    @Test
    public void test() throws Exception {
	//	System.setProperty( "jruby.debug.loadService", "true" );
        ScriptingContainer container = new IsolatedScriptingContainer();
	String output = (String) container.runScriptlet("require 'hello';Hello.new.say");
        assertTrue( output, output.contains( "hello world" ) );
	output = (String) container.runScriptlet("File.read('uri:' + Java::App.new.bannerURL())");
        assertTrue( output, output.contains( "hello world" ) );
    }

}
