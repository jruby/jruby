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

import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyString;

import java.util.ArrayList;
import java.util.List;

public class MRIBacktraceFormatter implements BacktraceFormatter {

    @Override
    public String[] format(RubyContext context, RubyException exception, Backtrace backtrace) {
        try {
            final List<Activation> activations = backtrace.getActivations();

            final ArrayList<String> lines = new ArrayList<>();

            if (activations.isEmpty()) {
                if (exception != null) {
                    lines.add(String.format("%s (%s)", exception.getMessage(), exception.getLogicalClass().getName()));
                }
            } else {
                lines.add(formatInLine(context, activations, exception));

                for (int n = 1; n < activations.size(); n++) {
                    lines.add(formatFromLine(activations, n));
                }
            }

            return lines.toArray(new String[lines.size()]);
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to format a Ruby call stack", e);
        }
    }

    private static String formatInLine(RubyContext context, List<Activation> activations, RubyException exception) {
        final StringBuilder builder = new StringBuilder();

        final Activation activation = activations.get(0);
        final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();
        final SourceSection reportedSourceSection;
        final String reportedName;

        if (sourceSection instanceof CoreSourceSection) {
            reportedSourceSection = nextUserSourceSection(activations, 1);
            reportedName = RubyArguments.getMethod(activation.getMaterializedFrame().getArguments()).getName();
        } else {
            reportedSourceSection = sourceSection;
            reportedName = reportedSourceSection.getIdentifier();
        }

        if (reportedSourceSection == null || reportedSourceSection.getSource() == null) {
            builder.append("???");
        } else {
            builder.append(reportedSourceSection.getSource().getName());
            builder.append(":");
            builder.append(reportedSourceSection.getStartLine());
            builder.append(":in `");
            builder.append(reportedName);
            builder.append("'");
        }

        if (exception != null) {
            String message;
            try {
                Object messageObject = DebugOperations.send(context, exception, "message", null);
                if (messageObject instanceof RubyString) {
                    message = messageObject.toString();
                } else {
                    message = exception.getMessage().toString();
                }
            } catch (RaiseException e) {
                message = exception.getMessage().toString();
            }

            builder.append(": ");
            builder.append(message);
            builder.append(" (");
            builder.append(exception.getLogicalClass().getName());
            builder.append(")");
        }

        return builder.toString();
    }

    private static String formatFromLine(List<Activation> activations, int n) {
        return "\tfrom " + formatCallerLine(activations, n);
    }

    public static String formatCallerLine(List<Activation> activations, int n) {
        final Activation activation = activations.get(n);
        final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();
        final SourceSection reportedSourceSection;
        final String reportedName;

        if (sourceSection instanceof CoreSourceSection) {
            reportedSourceSection = activations.get(n + 1).getCallNode().getEncapsulatingSourceSection();
            reportedName = RubyArguments.getMethod(activation.getMaterializedFrame().getArguments()).getName();
        } else {
            reportedSourceSection = sourceSection;
            reportedName = sourceSection.getIdentifier();
        }

        final StringBuilder builder = new StringBuilder();
        if (reportedSourceSection instanceof NullSourceSection) {
            builder.append("???");
        } else {
            builder.append(reportedSourceSection.getSource().getName());
            builder.append(":");
            builder.append(reportedSourceSection.getStartLine());
        }
        builder.append(":in `");
        builder.append(reportedName);
        builder.append("'");

        return builder.toString();
    }

    private static SourceSection nextUserSourceSection(List<Activation> activations, int n) {
        while (n < activations.size()) {
            SourceSection sourceSection = activations.get(n).getCallNode().getEncapsulatingSourceSection();

            if (!(sourceSection instanceof CoreSourceSection)) {
                return sourceSection;
            }

            n++;
        }
        return null;
    }

}
