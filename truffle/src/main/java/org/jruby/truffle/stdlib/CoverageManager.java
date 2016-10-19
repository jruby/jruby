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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

public class CoverageManager {

    public class LineTag {
    }

    public static final long NO_CODE = -1;

    private final Instrumenter instrumenter;
    private EventBinding<?> binding;
    private final Map<Source, AtomicLongArray> counters = new ConcurrentHashMap<>();
    private final Map<Source, BitSet> linesHaveCode = new HashMap<>();

    private boolean enabled;

    public CoverageManager(RubyContext context, Instrumenter instrumenter) {
        this.instrumenter = instrumenter;

        if (context.getOptions().COVERAGE_GLOBAL) {
            enable();
        }
    }

    public synchronized void setLineHasCode(Source source, int line) {
        if (!enabled) {
            return;
        }

        BitSet bitmap = linesHaveCode.get(source);

        if (bitmap == null) {
            bitmap = new BitSet(source.getLineCount());
            linesHaveCode.put(source, bitmap);
        }

        bitmap.set(line - 1);
    }

    private boolean getLineHasCode(Source source, int line) {
        final BitSet bitmap = linesHaveCode.get(source);

        if (bitmap == null) {
            return false;
        }

        return bitmap.get(line - 1);
    }

    @TruffleBoundary
    public synchronized void enable() {
        if (enabled) {
            return;
        }

        binding = instrumenter.attachFactory(SourceSectionFilter.newBuilder()
                .tagIs(LineTag.class)
                .build(), eventContext -> new ExecutionEventNode() {

                    @CompilationFinal private boolean configured;
                    @CompilationFinal private int lineNumber;
                    @CompilationFinal private AtomicLongArray counters;

                    @Override
                    protected void onEnter(VirtualFrame frame) {
                        if (!configured) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            final SourceSection sourceSection = eventContext.getInstrumentedSourceSection();

                            if (getLineHasCode(sourceSection.getSource(), sourceSection.getStartLine())) {
                                lineNumber = sourceSection.getStartLine() - 1;
                                counters = getCounters(sourceSection.getSource());
                            }

                            configured = true;
                        }

                        if (counters != null) {
                            counters.incrementAndGet(lineNumber);
                        }
                    }

                });

        enabled = true;
    }

    @TruffleBoundary
    public synchronized void disable() {
        if (!enabled) {
            return;
        }

        binding.dispose();
        linesHaveCode.clear();
        counters.clear();

        enabled = false;
    }
    
    private synchronized AtomicLongArray getCounters(Source source) {
        if (source.getName() == null) {
            return null;
        }

        AtomicLongArray c = counters.get(source);

        if (c == null) {
            c = new AtomicLongArray(source.getLineCount());
            counters.put(source, c);
        }

        return c;
    }

    public synchronized Map<Source, long[]> getCounts() {
        if (!enabled) {
            return null;
        }

        final Map<Source, long[]> counts = new HashMap<>();

        for (Map.Entry<Source, AtomicLongArray> entry : counters.entrySet()) {
            final BitSet hasCode = linesHaveCode.get(entry.getKey());

            final long[] array = new long[entry.getValue().length()];

            for (int n = 0; n < array.length; n++) {
                if (hasCode != null && hasCode.get(n)) {
                    array[n] = entry.getValue().get(n);
                } else {
                    array[n] = NO_CODE;
                }
            }

            counts.put(entry.getKey(), array);
        }

        return counts;
    }

    public void print(PrintStream out) {
        final int maxCountDigits = Long.toString(getMaxCount()).length();

        final String countFormat = "%" + maxCountDigits + "d";

        final char[] noCodeChars = new char[maxCountDigits];
        Arrays.fill(noCodeChars, ' ');
        noCodeChars[maxCountDigits - 1] = '-';
        final String noCodeString = new String(noCodeChars);

        for (Map.Entry<Source, AtomicLongArray> entry : counters.entrySet()) {
            final BitSet hasCode = linesHaveCode.get(entry.getKey());

            out.println(entry.getKey().getName());

            for (int n = 0; n < entry.getValue().length(); n++) {
                String line = entry.getKey().getCode(n + 1);

                if (line.length() > 60) {
                    line = line.substring(0, 60);
                }

                out.print("  ");

                if (hasCode != null && hasCode.get(n)) {
                    out.printf(countFormat, entry.getValue().get(n));
                } else {
                    out.print(noCodeString);
                }

                out.printf("  %s%n", line);
            }
        }
    }

    private long getMaxCount() {
        long max = 0;

        for (Map.Entry<Source, AtomicLongArray> entry : counters.entrySet()) {
            for (int n = 0; n < entry.getValue().length(); n++) {
                max = Math.max(max, entry.getValue().get(n));
            }
        }

        return max;
    }

}
