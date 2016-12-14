/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.backtrace;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.backtrace.BacktraceFormatter.FormattingFlags;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class Backtrace {

    private final Activation[] activations;
    private final Throwable javaThrowable;

    public Backtrace(Activation[] activations, Throwable javaThrowable) {
        this.activations = activations;
        this.javaThrowable = javaThrowable;
    }

    public List<Activation> getActivations() {
        return Collections.unmodifiableList(Arrays.asList(activations));
    }

    public Throwable getJavaThrowable() {
        return javaThrowable;
    }

    @Override
    public String toString() {
        RubyContext context = null;
        if (activations.length > 0) {
            Activation activation = activations[0];
            Node node = activation.getCallNode();
            if (node != null && node instanceof RubyBaseNode) {
                context = ((RubyBaseNode) node).getContext();
            }
        }

        if (context != null) {
            final BacktraceFormatter backtraceFormatter = new BacktraceFormatter(context, EnumSet.of(FormattingFlags.INCLUDE_CORE_FILES));
            final StringBuilder builder = new StringBuilder();
            for (String line : backtraceFormatter.formatBacktrace(context, null, this)) {
                builder.append("\n");
                builder.append(line);
            }
            return builder.toString().substring(1);
        } else {
            return "";
        }
    }

}
