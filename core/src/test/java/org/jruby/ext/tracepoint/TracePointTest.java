package org.jruby.ext.tracepoint;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.cli.Options;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author Andre Kullmann
 */
public class TracePointTest extends TestCase {

    public void testEvents() throws IOException {

        boolean fullTraceEnabled = RubyInstanceConfig.FULL_TRACE_ENABLED;
        boolean debugFulltrace = Options.DEBUG_FULLTRACE.load();
        try {
            RubyInstanceConfig config = new RubyInstanceConfig();

            Options.DEBUG_FULLTRACE.force("true");
            RubyInstanceConfig.FULL_TRACE_ENABLED = true;
            config.setDebuggingFrozenStringLiteral(true);

            Ruby ruby = Ruby.newInstance(config);

            String fileName = "tracepoint_tests/event_test.rb";

            InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
            assertNotNull("File '" + fileName + "' not found.", in);
            try {
                try {
                    ruby.compileAndLoadFile(fileName, in, false);
                } catch (RaiseException e) {
                    if ("trace_point.event is nil".equals(e.getMessage())) {
                        fail("The TracePoint event property is nil.");
                    } else {
                        throw e;
                    }
                }
            } finally {
                in.close();
            }

            RubyArray events = (RubyArray) ruby.evalScriptlet("Events.events");
            assertNotNull("Events.events array is nil.", events);
            assertFalse("Events.events array is empty", events.isEmpty());

        } finally {
            Options.DEBUG_FULLTRACE.force( String.valueOf( debugFulltrace ) );
            RubyInstanceConfig.FULL_TRACE_ENABLED = fullTraceEnabled;
        }
    }
}
