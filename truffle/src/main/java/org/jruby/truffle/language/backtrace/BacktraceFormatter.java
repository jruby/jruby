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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.SourceLoader;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BacktraceFormatter {

    public enum FormattingFlags {
        OMIT_EXCEPTION,
        OMIT_FROM_PREFIX,
        INCLUDE_CORE_FILES,
        INTERLEAVE_JAVA
    }

    private final RubyContext context;
    private final EnumSet<FormattingFlags> flags;

    public static BacktraceFormatter createDefaultFormatter(RubyContext context) {
        final EnumSet<FormattingFlags> flags = EnumSet.noneOf(FormattingFlags.class);

        if (!context.getOptions().BACKTRACES_HIDE_CORE_FILES) {
            flags.add(FormattingFlags.INCLUDE_CORE_FILES);
        }

        if (context.getOptions().BACKTRACES_INTERLEAVE_JAVA) {
            flags.add(FormattingFlags.INTERLEAVE_JAVA);
        }

        return new BacktraceFormatter(context, flags);
    }

    private static List<String> rubyBacktrace(RubyContext context, Node node) {
        return new BacktraceFormatter(context, EnumSet.of(FormattingFlags.INCLUDE_CORE_FILES)).
                        formatBacktrace(context, null, context.getCallStack().getBacktrace(node));
    }

    // For debugging:
    // org.jruby.truffle.language.backtrace.BacktraceFormatter.printableRubyBacktrace(getContext(), this)
    public static String printableRubyBacktrace(RubyContext context, Node node) {
        final StringBuilder builder = new StringBuilder();
        for (String line : rubyBacktrace(context, node)) {
            builder.append("\n");
            builder.append(line);
        }
        return builder.toString().substring(1);
    }

    public BacktraceFormatter(RubyContext context, EnumSet<FormattingFlags> flags) {
        this.context = context;
        this.flags = flags;
    }

    @TruffleBoundary
    public void printBacktrace(RubyContext context, DynamicObject exception, Backtrace backtrace) {
        printBacktrace(context, exception, backtrace, new PrintWriter(context.getEnv().err(), true));
    }

    @TruffleBoundary
    public void printBacktrace(RubyContext context, DynamicObject exception, Backtrace backtrace, PrintWriter writer) {
        for (String line : formatBacktrace(context, exception, backtrace)) {
            writer.println(line);
        }
    }

    public List<String> formatBacktrace(RubyContext context, DynamicObject exception, Backtrace backtrace) {
        if (backtrace == null) {
            backtrace = context.getCallStack().getBacktrace(null);
        }

        final List<Activation> activations = backtrace.getActivations();
        final ArrayList<String> lines = new ArrayList<>();

        for (int n = 0; n < activations.size(); n++) {
            try {
                lines.add(formatLine(activations, n, exception));
            } catch (Exception e) {
                if (context.getOptions().EXCEPTIONS_PRINT_JAVA) {
                    e.printStackTrace();
                }

                lines.add(StringUtils.format("(exception %s %s", e.getMessage(), e.getStackTrace()[0].toString()));
            }
        }

        if (backtrace.getJavaThrowable() != null && flags.contains(FormattingFlags.INTERLEAVE_JAVA)) {
            return BacktraceInterleaver.interleave(lines, backtrace.getJavaThrowable().getStackTrace());
        }

        return lines;
    }

    public String formatLine(List<Activation> activations, int n, DynamicObject exception) {
        final Activation activation = activations.get(n);

        if (activation == Activation.OMITTED_LIMIT) {
            return context.getCoreStrings().BACKTRACE_OMITTED_LIMIT.toString();
        }

        if (activation == Activation.OMITTED_UNUSED) {
            return context.getCoreStrings().BACKTRACE_OMITTED_UNUSED.toString();
        }

        final StringBuilder builder = new StringBuilder();

        if (!flags.contains(FormattingFlags.OMIT_FROM_PREFIX) && n > 0) {
            builder.append("\tfrom ");
        }

        final RootNode rootNode = activation.getCallNode().getRootNode().getRootNode();

        if (rootNode instanceof RubyRootNode) {
            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();
            final SourceSection reportedSourceSection;
            String reportedName;

            if (isJavaCore(sourceSection) ||
                    (isCore(context, sourceSection) && !flags.contains(FormattingFlags.INCLUDE_CORE_FILES))) {
                final SourceSection nextUserSourceSection = nextUserSourceSection(activations, n);
                // if there is no next source section use a core one to avoid ???
                reportedSourceSection = nextUserSourceSection != null ? nextUserSourceSection : sourceSection;
                reportedName = getMethodNameFromActivation(activation);
            } else {
                reportedSourceSection = sourceSection;
                reportedName = rootNode.getName();
            }

            if (reportedSourceSection == null) {
                builder.append("???");
            } else {
                builder.append(reportedSourceSection.getSource().getName());
                builder.append(":");
                builder.append(reportedSourceSection.getStartLine());
            }

            builder.append(":in `");
            builder.append(reportedName);
            builder.append("'");
        } else {
            builder.append(formatForeign(activation.getCallNode()));
        }

        if (!flags.contains(FormattingFlags.OMIT_EXCEPTION) && exception != null && n == 0) {
            String message;
            try {
                Object messageObject = context.send(exception, "message", null);
                if (RubyGuards.isRubyString(messageObject)) {
                    message = messageObject.toString();
                } else {
                    message = Layouts.EXCEPTION.getMessage(exception).toString();
                }
            } catch (RaiseException e) {
                message = Layouts.EXCEPTION.getMessage(exception).toString();
            }

            builder.append(": ");
            builder.append(message);
            builder.append(" (");
            builder.append(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(exception)).getName());
            builder.append(")");
        }

        return builder.toString();
    }

    private String getMethodNameFromActivation(Activation activation) {
        try {
            return activation.getMethod().getName();
        } catch (Exception e) {
            return "???";
        }
    }

    private SourceSection nextUserSourceSection(List<Activation> activations, int n) {
        while (n < activations.size()) {
            final Node callNode = activations.get(n).getCallNode();

            if (callNode != null) {
                final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();

                if (!isCore(context, sourceSection)) {
                    return sourceSection;
                }
            }

            n++;
        }
        return null;
    }

    public boolean isJavaCore(SourceSection sourceSection) {
        return sourceSection == context.getCoreLibrary().getSourceSection();
    }

    public static boolean isCore(RubyContext context, SourceSection sourceSection) {
        if (sourceSection == null) {
            return true;
        }

        if (sourceSection.getSource() == context.getCoreLibrary().getSource()) {
            return true;
        }

        final Source source = sourceSection.getSource();
        if (source == null) {
            return true;
        }

        final String path = source.getPath();
        if (path != null) {
            return path.startsWith(SourceLoader.TRUFFLE_SCHEME);
        }

        final String name = source.getName();
        if (name != null) {
            return name.startsWith(SourceLoader.TRUFFLE_SCHEME);
        }

        return true;
    }

    /** For debug purposes. */
    public static boolean isUserSourceSection(RubyContext context, SourceSection sourceSection) {
        if (!BacktraceFormatter.isCore(context, sourceSection)) {
            return false;
        }

        final String path = sourceSection.getSource().getPath();
        if (path.startsWith(context.getCoreLibrary().getCoreLoadPath())) {
            return false;
        }

        return path.indexOf("/lib/ruby/stdlib/rubygems") == -1;
    }

    private String formatForeign(Node callNode) {
        final StringBuilder builder = new StringBuilder();

        final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();

        if (sourceSection != null) {
            final String shortDescription = RubyLanguage.fileLine(sourceSection);


            builder.append(shortDescription);

            final RootNode rootNode = callNode.getRootNode();
            final String identifier = rootNode.getName();

            if (identifier != null && !identifier.isEmpty()) {
                builder.append(":in `");
                builder.append(identifier);
                builder.append("'");
            }
        } else {
            builder.append(getRootOrTopmostNode(callNode).getClass().getSimpleName());
        }

        return builder.toString();
    }

    private Node getRootOrTopmostNode(Node node) {
        while (node.getParent() != null) {
            node = node.getParent();
        }

        return node;
    }

}
