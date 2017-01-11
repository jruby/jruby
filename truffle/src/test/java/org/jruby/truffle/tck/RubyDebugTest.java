/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tck;

import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import org.jruby.truffle.RubyLanguage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RubyDebugTest {

    private static final int BREAKPOINT_LINE = 13;

    private Debugger debugger;
    private DebuggerSession debuggerSession;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    private PolyglotEngine engine;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private static Source getSource(String path) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(path);
        Reader reader = new InputStreamReader(stream);
        try {
            return Source.newBuilder(reader).name(new File(path).getName()).mimeType(RubyLanguage.MIME_TYPE).build();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Before
    public void before() {
        suspendedEvent = null;

        engine = PolyglotEngine.newBuilder().setOut(out).setErr(err).build();
        debugger = Debugger.find(engine);
        debuggerSession = debugger.startSession(event -> {
            suspendedEvent = event;
            performWork();
            suspendedEvent = null;
        });

        engine.eval(getSource("src/test/ruby/init.rb"));

        run.clear();
    }

    @After
    public void dispose() {
        debuggerSession.close();
        if (engine != null) {
            engine.dispose();
        }
    }

    @Test
    public void testBreakpoint() throws Throwable {
        final Source factorial = createFactorial();

        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            Breakpoint breakpoint = Breakpoint.newBuilder(factorial).lineIs(BREAKPOINT_LINE).build();
            debuggerSession.install(breakpoint);
        });

        // Init before eval:
        performWork();
        engine.eval(factorial);
        engine.eval(Source.newBuilder("Truffle::Debug.resolve_lazy_nodes").mimeType(RubyLanguage.MIME_TYPE).name("resolve_lazy_nodes").build());

        assertExecutedOK("Algorithm loaded");

        assertLocation(13, "1",
                        "n", "1",
                        "nMinusOne", "nil",
                        "nMOFact", "nil",
                        "res", "nil");

        continueExecution();

        final Value main = engine.findGlobalSymbol("main");
        assertNotNull( "main method found", main);
        Value value = main.execute();
        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
        assertExecutedOK("Algorithm computed OK: " + n + "; Checking if it stopped at the breakpoint");
    }

    @Test
    public void stepInStepOver() throws Throwable {
        final Source factorial = createFactorial();
        engine.eval(factorial);
        engine.eval(Source.newBuilder("Truffle::Debug.resolve_lazy_nodes").mimeType(RubyLanguage.MIME_TYPE).name("resolve_lazy_nodes").build());
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        assertLocation(23, "res = fac(2)", "res", "nil");
        stepInto(1);
        assertLocation(12, "if n <= 1",
                        "n", "2",
                        "nMinusOne", "nil",
                        "nMOFact", "nil",
                        "res", "nil");
        stepOver(1);
        assertLocation(15, "nMinusOne = n - 1",
                        "n", "2",
                        "nMinusOne", "nil",
                        "nMOFact", "nil",
                        "res", "nil");
        stepOver(1);
        assertLocation(16, "nMOFact = fac(nMinusOne)",
                        "n", "2",
                        "nMinusOne", "1",
                        "nMOFact", "nil",
                        "res", "nil");
        stepOver(1);
        assertLocation(17, "res = n * nMOFact",
                        "n", "2", "nMinusOne", "1",
                        "nMOFact", "1",
                        "res", "nil");
        continueExecution();

        // Init before eval:
        performWork();
        Value value = engine.findGlobalSymbol("main").execute();

        Number n = value.as(Number.class);

        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
        assertExecutedOK("Stepping went OK");
    }

    private void performWork() {
        try {
            if (ex == null && !run.isEmpty()) {
                Runnable c = run.removeFirst();
                c.run();
            }
        } catch (Throwable e) {
            ex = e;
        }
    }

    private void stepOver(final int size) {
        run.addLast(() -> suspendedEvent.prepareStepOver(size));
    }

    private void continueExecution() {
        run.addLast(() -> suspendedEvent.prepareContinue());
    }

    private void stepInto(final int size) {
        run.addLast(() -> suspendedEvent.prepareStepInto(size));
    }

    private void assertLocation(final int line, final String code, final Object... expectedFrame) {
        run.addLast(() -> {
            assertNotNull(suspendedEvent);
            final int currentLine = suspendedEvent.getSourceSection().getStartLine();
            assertEquals(line, currentLine);
            final String currentCode = suspendedEvent.getSourceSection().getCode().trim();
            assertEquals(code, currentCode);
            final DebugStackFrame frame = suspendedEvent.getTopStackFrame();

            final AtomicInteger numFrameVars = new AtomicInteger(0);
            frame.forEach(var -> { numFrameVars.incrementAndGet(); });
            // There is (self) among the variables, hence substract 1:
            assertEquals(expectedFrame.length / 2, numFrameVars.get() - 1);

            for (int i = 0; i < expectedFrame.length; i = i + 2) {
                String expectedIdentifier = (String) expectedFrame[i];
                Object expectedValue = expectedFrame[i + 1];
                DebugValue value = frame.getValue(expectedIdentifier);
                assertNotNull(value);
                String valueStr = value.as(String.class);

                assertEquals(expectedValue, valueStr);
            }

            run.removeFirst().run();
        });
    }

    private void assertExecutedOK(String msg) throws Throwable {
        assertTrue(getErr(), getErr().isEmpty());

        if (ex != null) {
            if (ex instanceof AssertionError) {
                throw ex;
            } else {
                throw new AssertionError(msg + ". Error during execution ", ex);
            }
        }

        assertTrue(msg + ". Assuming all requests processed: " + run, run.isEmpty());
    }

    private static Source createFactorial() {
        return getSource("src/test/ruby/factorial.rb");
    }

    private final String getErr() {
        try {
            err.flush();
        } catch (IOException e) {
        }
        return new String(err.toByteArray());
    }

}
