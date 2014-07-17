package org.example;

import static org.junit.Assert.assertTrue;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class JavaxTest {

    @Test
    public void test() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("jruby");
        String output = (String) engine.eval("require 'hello';Hello.new( :name => 'world' ).say");

        assertTrue( output, output.contains( "hello world" ) );
        assertTrue( output, output.contains( "zip file name: world" ) );
    }

}
