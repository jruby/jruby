/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

public class CoverageManager {

    public static final String LINE_TAG = "org.jruby.truffle.coverage.line";

    private final Instrumenter instrumenter;

    private boolean enabled = false;

    private final Map<Source, AtomicLongArray> counters = new ConcurrentHashMap<>();

    public CoverageManager(RubyContext context, Instrumenter instrumenter) {
        this.instrumenter = instrumenter;

        if (context.getOptions().COVERAGE_GLOBAL) {
            enable();
        }
    }

    public void enable() {
        if (enabled) {
            throw new UnsupportedOperationException();
        }

        instrumenter.attachFactory(SourceSectionFilter.newBuilder().tagIs(LINE_TAG).build(), new ExecutionEventNodeFactory() {

            @Override
            public ExecutionEventNode create(EventContext eventContext) {
                return new ExecutionEventNode() {

                    @CompilationFinal private AtomicLongArray counters;
                    @CompilationFinal private int lineNumber;

                    @Override
                    protected void onEnter(VirtualFrame frame) {
                        if (counters == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            final SourceSection sourceSection = getEncapsulatingSourceSection();
                            counters = getCounters(sourceSection.getSource());
                            lineNumber = sourceSection.getStartLine() - 1;
                        }

                        counters.incrementAndGet(lineNumber);
                    }

                };
            }

        });

        enabled = true;
    }

    private synchronized AtomicLongArray getCounters(Source source) {
        AtomicLongArray c = counters.get(source);

        if (c == null) {
            c = new AtomicLongArray(source.getLineCount());
            counters.put(source, c);
        }

        return c;
    }

    public Map<Source, long[]> getCounts() {
        final Map<Source, long[]> counts = new HashMap<>();

        for (Map.Entry<Source, AtomicLongArray> entry : counters.entrySet()) {
            final long[] array = new long[entry.getValue().length()];

            for (int n = 0; n < array.length; n++) {
                array[n] = entry.getValue().get(n);
            }

            counts.put(entry.getKey(), array);
        }

        return counts;
    }

    public void print(PrintStream out) {
        for (Map.Entry<Source, AtomicLongArray> entry : counters.entrySet()) {
            out.print(entry.getKey().getName());

            for (int n = 0; n < entry.getValue().length(); n++) {
                String line = entry.getKey().getCode(n + 1);

                if (line.length() > 60) {
                    line = line.substring(0, 60);
                }

                out.printf("  % 12d  %s%n", entry.getValue().get(n), line);
            }
        }
    }

}
