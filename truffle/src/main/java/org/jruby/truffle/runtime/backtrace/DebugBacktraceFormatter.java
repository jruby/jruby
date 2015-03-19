/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.backtrace;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.util.cli.Options;

import java.util.ArrayList;
import java.util.List;

public class DebugBacktraceFormatter implements BacktraceFormatter {

    private static final int BACKTRACE_MAX_VALUE_LENGTH = Options.TRUFFLE_BACKTRACE_MAX_VALUE_LENGTH.load();

    @Override
    public String[] format(RubyContext context, RubyException exception, Backtrace backtrace) {
        try {
            final List<Activation> activations = backtrace.getActivations();

            final List<String> lines = new ArrayList<>();

            if (exception != null) {
                lines.add(String.format("%s (%s)", exception.getMessage(), exception.getLogicalClass().getName()));
            }

            for (Activation activation : activations) {
                lines.add(formatLine(context, activation));
            }

            return lines.toArray(new String[lines.size()]);
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to format a Ruby call stack", e);
        }
    }

    private static String formatLine(RubyContext context, Activation activation) {
        final StringBuilder builder = new StringBuilder();
        builder.append(formatBasicLine(activation));

        final MaterializedFrame frame = activation.getMaterializedFrame();
        final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();

        builder.append(" self=");
        builder.append(debugString(context, RubyArguments.getSelf(frame.getArguments())));

        for (Object identifier : frameDescriptor.getIdentifiers()) {
            if (identifier instanceof String) {
                builder.append(" ");
                builder.append(identifier);
                builder.append("=");
                builder.append(debugString(context, frame.getValue(frameDescriptor.findFrameSlot(identifier))));
            }
        }
        return builder.toString();
    }

    public static String formatBasicLine(Activation activation) {
        final StringBuilder builder = new StringBuilder();
        builder.append("    at ");

        final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

        if (sourceSection instanceof CoreSourceSection) {
            final InternalMethod method = RubyArguments.getMethod(activation.getMaterializedFrame().getArguments());
            builder.append(method.getDeclaringModule().getName());
            builder.append("#");
            builder.append(method.getName());
        } else {
            builder.append(sourceSection.getSource().getName());
            builder.append(":");
            builder.append(sourceSection.getStartLine());
            builder.append(":in '");
            builder.append(sourceSection.getIdentifier());
            builder.append("'");
        }

        return builder.toString();
    }

    public static String debugString(RubyContext context, Object value) {
        if (value == null) {
            return "*null*";
        }

        try {
            final String string = DebugOperations.inspect(context, value);

            if (string.length() <= BACKTRACE_MAX_VALUE_LENGTH) {
                return string;
            } else {
                return string.substring(0, BACKTRACE_MAX_VALUE_LENGTH) + "…";
            }
        } catch (Throwable t) {
            return "*error*";
        }
    }

}
