/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tools.simpleshell;

import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.backtrace.Activation;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.parser.ParserContext;

import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;

public class SimpleShell {

    private final RubyContext context;

    private final ShellInterface shellInterface;

    private int currentFrameIndex;
    private MaterializedFrame currentFrame;

    public SimpleShell(RubyContext context) {
        this.context = context;

        if (TruffleOptions.AOT || System.console() == null) {
            shellInterface = new StandardShellInterface();
        } else {
            shellInterface = new ConsoleShellInterface();
        }
    }

    public void run(MaterializedFrame frame, Node currentNode) {
        currentFrameIndex = 0;
        currentFrame = frame;

        while (true) {
            final String shellLine;

            try {
                shellLine = shellInterface.readLine("> ");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            final StringTokenizer tokenizer = new StringTokenizer(shellLine);

            if (!tokenizer.hasMoreElements()) {
                continue;
            }

            switch (tokenizer.nextToken()) {
                case "backtrace": {
                    final BacktraceFormatter formatter = BacktraceFormatter.createDefaultFormatter(context);

                    int n = 0;

                    for (Activation activation : context.getCallStack().getBacktrace(currentNode).getActivations()) {
                        if (n == currentFrameIndex) {
                            shellInterface.getWriter().print("  ▶ ");
                        } else {
                            shellInterface.getWriter().printf("%3d ", n);
                        }

                        shellInterface.getWriter().println(
                                formatter.formatLine(Collections.singletonList(activation), 0, null));

                        n++;
                    }
                } break;

                case "continue":
                    return;

                case "exit": {
                    // We're in the debugger, not normal Ruby, so just hard exit here
                    System.exit(0);
                } break;

                case "frame": {
                    // TODO CS 4-Mar-2015
                    throw new UnsupportedOperationException();
                }

                default: {
                    try {
                        final RubyRootNode rootNode = context.getCodeLoader().parse(
                                context.getSourceLoader().loadFragment(shellLine, "(shell)"),
                                UTF8Encoding.INSTANCE,
                                ParserContext.EVAL,
                                currentFrame,
                                false,
                                currentNode);

                        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(
                                ParserContext.EVAL,
                                RubyArguments.getDeclarationContext(currentFrame),
                                rootNode, currentFrame,
                                RubyArguments.getSelf(currentFrame));

                        final Object result = deferredCall.callWithoutCallNode();

                        String inspected;

                        try {
                            inspected = context.send(result, "inspect", null).toString();
                        } catch (Exception e) {
                            inspected = StringUtils.format("(error inspecting %s@%x %s)",
                                    result.getClass().getSimpleName(),
                                    result.hashCode(),
                                    e.toString());
                        }

                        shellInterface.getWriter().println(inspected);
                    } catch (RaiseException e) {
                        final DynamicObject rubyException = e.getException();

                        final BacktraceFormatter formatter = BacktraceFormatter.createDefaultFormatter(context);
                        formatter.printBacktrace(context, rubyException, Layouts.EXCEPTION.getBacktrace(rubyException), shellInterface.getWriter());
                    }
                } break;
            }

            shellInterface.getWriter().flush();
        }
    }


}
