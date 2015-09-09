package org.jruby.runtime;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.management.Runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by Stewart on 02/09/2015.
 */
public final class TestThreadTearDown extends TestCase {

    public void testTearDownDisposesOfThreads() {

        RubyInstanceConfig config = new RubyInstanceConfig();
        PrintStream out = new PrintStream(System.out, true);
        config.setOutput(out);
        config.setError(out);

        Ruby ruby = Ruby.newInstance( config );

        org.jruby.management.Runtime runtime = new Runtime(ruby);

        String rubyScriptThatCreatesAThread = "" +
                "class Test\r\n" +
                "   def initialize\r\n" +
                "       @thread = Thread.new { puts 'thread running'; sleep 5000000 }\r\n" +
                "       puts 'started'\r\n" +
                "   end\r\n" +
                "end\r\n" +
                "" +
                "t = Test.new\r\n";

        runtime.executeRuby(rubyScriptThatCreatesAThread);
        assertEquals(2, ruby.getThreadService().getActiveRubyThreads().length);
        ruby.tearDown();
        assertEquals(0, ruby.getThreadService().getActiveRubyThreads().length);
    }
}
