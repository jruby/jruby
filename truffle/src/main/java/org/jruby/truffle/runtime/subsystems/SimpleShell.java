/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.backtrace.DebugBacktraceFormatter;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.truffle.translator.TranslatorDriver;

import java.util.StringTokenizer;

public class SimpleShell {

    private int currentFrameIndex;
    private MaterializedFrame currentFrame;

    private final RubyContext context;

    public SimpleShell(RubyContext context) {
        this.context = context;
    }

    public void run(MaterializedFrame frame, Node currentNode) {
        currentFrameIndex = 0;
        currentFrame = frame;

        while (true) {
            final String shellLine = System.console().readLine("> ");

            final StringTokenizer tokenizer = new StringTokenizer(shellLine);

            if (!tokenizer.hasMoreElements()) {
                continue;
            }

            switch (tokenizer.nextToken()) {
                case "backtrace":
                    backtrace(currentNode);
                    break;

                case "continue":
                    return;

                case "exit":
                    System.exit(0);
                    break;

                case "frame":
                    currentFrameIndex = Integer.parseInt(tokenizer.nextToken());
                    currentFrame = RubyCallStack.getBacktrace(currentNode).getActivations().get(currentFrameIndex).getMaterializedFrame();
                    break;

                default:
                    try {
                        final Object result = context.execute(
                                Source.fromText(shellLine, "shell"), UTF8Encoding.INSTANCE,
                                TranslatorDriver.ParserContext.EVAL,
                                RubyArguments.getSelf(currentFrame.getArguments()), currentFrame,
                                false, currentNode, NodeWrapper.IDENTITY);

                        System.console().writer().println(DebugOperations.inspect(context, result));
                    } catch (RaiseException e) {
                        final RubyException rubyException = e.getRubyException();

                        for (String line : Backtrace.DISPLAY_FORMATTER.format(e.getRubyException().getContext(), rubyException, rubyException.getBacktrace())) {
                            System.console().writer().println(line);
                        }
                    }
            }
        }
    }

    private void backtrace(Node currentNode) {
        int n = 0;

        for (Activation activation : RubyCallStack.getBacktrace(currentNode).getActivations()) {
            if (n == currentFrameIndex) {
                System.console().writer().print("  ▶");
            } else {
                System.console().writer().printf("%3d", n);
            }

            System.console().writer().println(DebugBacktraceFormatter.formatBasicLine(activation));
            n++;
        }
    }

}
