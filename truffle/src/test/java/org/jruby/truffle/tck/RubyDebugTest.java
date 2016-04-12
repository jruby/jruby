/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.ExecutionEvent;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.EventConsumer;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import org.junit.After;

public class RubyDebugTest {
    private Debugger debugger;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    private ExecutionEvent executionEvent;
    protected PolyglotEngine engine;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        suspendedEvent = null;
        executionEvent = null;
        engine = PolyglotEngine.newBuilder().setOut(out).setErr(err).onEvent(new EventConsumer<ExecutionEvent>(ExecutionEvent.class) {
            @Override
            protected void on(ExecutionEvent event) {
                executionEvent = event;
                debugger = executionEvent.getDebugger();
                performWork();
                executionEvent = null;
            }
        }).onEvent(new EventConsumer<SuspendedEvent>(SuspendedEvent.class) {
            @Override
            protected void on(SuspendedEvent event) {
                suspendedEvent = event;
                performWork();
                suspendedEvent = null;
            }
        }).build();
        run.clear();
    }

    @After
    public void dispose() {
        if (engine != null) {
            engine.dispose();
        }
    }

    private static Source createFactorial() {
        return Source.fromText(
            "def fac(n)\n" +
            "  if n <= 1\n" +
            "    1\n" +
            "  else\n" +
            "    nMinusOne = n - 1\n" +
            "    nMOFact = fac(nMinusOne)\n" +
            "    res = n * nMOFact\n" +
            "    res\n" +
            "  end\n" +
            "end\n" +
            "\n" +
            "def main\n" +
            "  res = fac(2)\n" +
            "  puts res\n" +
            "  res\n" +
            "end\n" +
            "Truffle::Interop.export_method(:main)\n" +
            "\n",
            "factorial.rb").withMimeType(
        "application/x-ruby");
    }

    protected final String getOut() {
        return new String(out.toByteArray());
    }

    protected final String getErr() {
        try {
            err.flush();
        } catch (IOException e) {
        }
        return new String(err.toByteArray());
    }

    @Test
    public void testBreakpoint() throws Throwable {
        final Source factorial = createFactorial();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                try {
                    assertNull(suspendedEvent);
                    assertNotNull(executionEvent);
                    LineLocation nMinusOne = factorial.createLineLocation(2);
                    debugger.setLineBreakpoint(0, nMinusOne, false);
                    executionEvent.prepareContinue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        engine.eval(factorial);
        assertExecutedOK();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                // the breakpoint should hit instead
            }
        });
        assertLocation(2, "1",
                        "n", 1L,
                        "nMinusOne", null,
                        "nMOFact", null,
                        "res", null);
        continueExecution();

        Value value = engine.findGlobalSymbol("main").execute();
        assertExecutedOK();
        Assert.assertEquals("2\n", getOut());
        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void stepInStepOver() throws Throwable {
        final Source factorial = createFactorial();
        engine.eval(factorial);

        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
                executionEvent.prepareStepInto();
            }
        });

        assertLocation(2, "res = fac(2)", "res", null);
        stepInto(1);
        assertLocation(7, "n <= 1",
                        "n", 2L,
                        "nMinusOne", null,
                        "nMOFact", null,
                        "res", null);
        stepOver(1);
        assertLocation(10, "nMinusOne = n - 1",
                        "n", 2L,
                        "nMinusOne", null,
                        "nMOFact", null,
                        "res", null);
        stepOver(1);
        assertLocation(11, "nMOFact = fac(nMinusOne)",
                        "n", 2L,
                        "nMinusOne", 1L,
                        "nMOFact", null,
                        "res", null);
        stepOver(1);
        assertLocation(12, "res = n * nMOFact",
                        "n", 2L, "nMinusOne", 1L,
                        "nMOFact", 1L,
                        "res", null);
        stepOver(1);
        assertLocation(13, "return res",
                        "n", 2L,
                        "nMinusOne", 1L,
                        "nMOFact", 1L,
                        "res", 2L);
        stepOver(1);
        assertLocation(2, "fac(2)", "res", null);
        stepOver(1);
        assertLocation(3, "println(res)", "res", 2L);
        stepOut();

        Value value = engine.findGlobalSymbol("main").execute();
        assertExecutedOK();

        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
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
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepOver(size);
            }
        });
    }

    private void stepOut() {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepOut();
            }
        });
    }

    private void continueExecution() {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareContinue();
            }
        });
    }

    private void stepInto(final int size) {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepInto(size);
            }
        });
    }

    private void assertLocation(final int line, final String code, final Object... expectedFrame) {
        run.addLast(new Runnable() {
            public void run() {
                assertNotNull(suspendedEvent);
                Assert.assertEquals(line, suspendedEvent.getNode().getSourceSection().getLineLocation().getLineNumber());
                Assert.assertEquals(code, suspendedEvent.getNode().getSourceSection().getCode());
                final MaterializedFrame frame = suspendedEvent.getFrame();

                Assert.assertEquals(expectedFrame.length / 2, frame.getFrameDescriptor().getSize());

                for (int i = 0; i < expectedFrame.length; i = i + 2) {
                    String expectedIdentifier = (String) expectedFrame[i];
                    Object expectedValue = expectedFrame[i + 1];
                    FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(expectedIdentifier);
                    Assert.assertNotNull(slot);
                    Assert.assertEquals(expectedValue, frame.getValue(slot));
                }
                run.removeFirst().run();
            }
        });
    }

    private void assertExecutedOK() throws Throwable {
        Assert.assertTrue(getErr(), getErr().isEmpty());
        if (ex != null) {
            if (ex instanceof AssertionError) {
                throw ex;
            } else {
                throw new AssertionError("Error during execution", ex);
            }
        }
        assertTrue("Assuming all requests processed: " + run, run.isEmpty());
    }
}
