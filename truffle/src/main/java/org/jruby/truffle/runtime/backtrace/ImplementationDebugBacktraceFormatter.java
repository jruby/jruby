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
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.MethodLike;

import java.util.ArrayList;
import java.util.List;

public class ImplementationDebugBacktraceFormatter implements BacktraceFormatter {

    @Override
    public String[] format(RubyContext context, RubyException exception, Backtrace backtrace) {
        try {
            final List<Activation> activations = backtrace.getActivations();

            final List<String> lines = new ArrayList<>();

            if (exception != null) {
                lines.add(String.format("%s (%s)", exception.getMessage(), exception.getLogicalClass().getName()));
            }

            for (Activation activation : activations) {
                formatActivation(context, activation, lines);
            }

            return lines.toArray(new String[lines.size()]);
        } catch (Exception e) {
            throw new TruffleFatalException("Exception while trying to format a Ruby call stack", e);
        }
    }

    private static void formatActivation(RubyContext context, Activation activation, List<String> lines) {
        lines.add(DebugBacktraceFormatter.formatBasicLine(activation));

        final MaterializedFrame frame = activation.getMaterializedFrame();

        final Object[] arguments = frame.getArguments();

        final MethodLike method = RubyArguments.getMethod(arguments);
        lines.add(String.format("      method = %s", method));

        if (method instanceof InternalMethod) {
            final InternalMethod internalMethod = (InternalMethod) method;

            if (internalMethod.getDeclaringModule() == null) {
                lines.add(String.format("        declaring module = null"));
            } else {
                lines.add(String.format("        declaring module = %s", internalMethod.getDeclaringModule().getName()));
            }
        }

        lines.add("      declaration frame:");
        formatDeclarationFrame(context, RubyArguments.getDeclarationFrame(arguments), lines);

        lines.add(String.format("      self = %s", DebugBacktraceFormatter.debugString(context, RubyArguments.getSelf(arguments))));
        lines.add(String.format("      block = %s", RubyArguments.getBlock(arguments)));

        lines.add("      arguments:");

        for (int n = 0; n < RubyArguments.getUserArgumentsCount(arguments); n++) {
            lines.add(String.format("        [%d] = %s", n, DebugBacktraceFormatter.debugString(context, RubyArguments.getUserArgument(arguments, n))));
        }

        lines.add("      frame:");
        formatFrame(context, frame, lines);
    }

    private static void formatDeclarationFrame(RubyContext context, MaterializedFrame frame, List<String> lines) {
        if (frame != null) {
            formatDeclarationFrame(context, RubyArguments.getDeclarationFrame(frame.getArguments()), lines);
            formatFrame(context, frame, lines);
        }
    }

    private static void formatFrame(RubyContext context, MaterializedFrame frame, List<String> lines) {
        final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();

        for (Object identifier : frameDescriptor.getIdentifiers()) {
            lines.add(String.format("        %s = %s", identifier, DebugBacktraceFormatter.debugString(context, frame.getValue(frameDescriptor.findFrameSlot(identifier)))));
        }
    }

}
