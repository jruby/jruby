/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.backtrace;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.ArrayList;
import java.util.List;

public class RubiniusBacktraceFormatter implements BacktraceFormatter {

    @Override
    public String[] format(RubyContext context, RubyException exception, Backtrace backtrace) {
        try {
            final List<Activation> activations = backtrace.getActivations();

            final ArrayList<String> lines = new ArrayList<>();

            if (backtrace != null) {
                final Activation firstActivation = activations.get(activations.size() - 1);

                lines.add(String.format("An exception occurred running %s:",
                        firstActivation.getCallNode().getEncapsulatingSourceSection().getSource().getName()));

                lines.add("");
                lines.add(String.format("    %s (%s)", exception.getMessage(), exception.getRubyClass().getName()));
                lines.add("");
                lines.add("Backtrace:");
                lines.add("");
            }

            for (int n = 1; n < activations.size(); n++) {
                lines.add(formatFromLine(activations, n));
            }

            final String[] linesArray = lines.toArray(new String[lines.size()]);

            BacktraceUtils.align(linesArray, 6, " at ");

            return linesArray;
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to format a Ruby call stack", e);
        }
    }

    private static String formatFromLine(List<Activation> activations, int n) {
        final StringBuilder builder = new StringBuilder();

        builder.append("  ");

        final SourceSection sourceSection = activations.get(n).getCallNode().getEncapsulatingSourceSection();
        final SourceSection reportedSourceSection;
        final String reportedName;
        final RubyMethod reportedMethod;

        final Activation activation = activations.get(n);

        if (sourceSection instanceof CoreSourceSection) {
            reportedSourceSection = activations.get(n + 1).getCallNode().getEncapsulatingSourceSection();
            reportedName = ((CoreSourceSection) sourceSection).getMethodName();
            reportedMethod = activations.get(n).getMethod();
        } else {
            reportedSourceSection = sourceSection;

            if (RubyMethod.hasBlockDecorator(sourceSection.getIdentifier())) {
                reportedName = "{ } in " + RubyMethod.removeBlockDecorator(sourceSection.getIdentifier());
            } else {
                reportedName = sourceSection.getIdentifier();
            }

            if (activations.get(n).getMethod() == null && n < activations.size() - 1) {
                reportedMethod = activations.get(n + 1).getMethod();
            } else {
                reportedMethod = activations.get(n).getMethod();
            }
        }

        if (n == activations.size() - 1) {
            builder.append("Object#__script__");
        } else {
            if (RubyMethod.hasBlockDecorator(sourceSection.getIdentifier())) {
                builder.append("{ } in ");
                builder.append(RubyMethod.removeBlockDecorator(sourceSection.getIdentifier()));
            } else {
                builder.append(reportedMethod.getDeclaringModule().getName());
                builder.append("#");
                builder.append(reportedMethod.getName());
            }
        }

        builder.append(" at ");

        if (n == 1) {
            builder.append(reportedSourceSection.getSource().getPath());
        } else {
            builder.append(reportedSourceSection.getSource().getName());
        }

        builder.append(":");
        builder.append(reportedSourceSection.getStartLine());

        return builder.toString();
    }

}
