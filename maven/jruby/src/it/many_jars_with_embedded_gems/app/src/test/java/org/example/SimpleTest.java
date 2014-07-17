package org.example;

import static org.junit.Assert.assertTrue;

import org.jruby.embed.ScriptingContainer;

import org.junit.Test;

public class SimpleTest {

    @Test
    public void test() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
	String output = (String) container.runScriptlet("require 'hello';Hello.new( :name => 'world' ).say");

        assertTrue( output, output.contains( "hello world" ) );
        assertTrue( output, output.contains( "zip file name: world" ) );
    }

}
