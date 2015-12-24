/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jruby.truffle.format.nodes.PackRootNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public class PackCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PackCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(String format) {
        if (format.length() > 32) {
            format = recoverLoop(format);
        }

        final PackErrorListener errorListener = new PackErrorListener(context, currentNode);

        final ANTLRInputStream input = new ANTLRInputStream(format);

        final PackLexer lexer = new PackLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        final PackParser parser = new PackParser(tokens);

        final PackTreeBuilder builder = new PackTreeBuilder(context, currentNode);
        parser.addParseListener(builder);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        parser.sequence();

        return Truffle.getRuntime().createCallTarget(
                new PackRootNode(describe(format), builder.getEncoding(), builder.getNode()));
    }

    /**
     * Format strings can sometimes be dynamically generated with code such as:
     * <p>
     *   <code>'x' + ('NX' * size)</code>
     * <p>
     * This is problematic for us as it expands to:
     * <p>
     *   <code>xNXNXNXNXNXNXNXNXNXNX...</code>
     * <p>
     * We will struggle to compile that with a large value for size because it
     * will generate a huge number of nodes. Even if we could compile it, it's
     * not great for memory consumption or the instruction cache. Instead, we'd
     * like to recover the loop in there and convert it to:
     * <p>
     *   <code>x(NX)1000</code>
     * <p>
     * We could try and do something really sophisticated here, with nested
     * loops and finding the optimal pattern, but for the moment we just look
     * for one simple loop.
     * <p>
     * To do that, for each character we look 1..n characters behind and see if
     * that pattern is repeated. If it is we have the loop. Nothing more
     * complicated than that.
     */
    public static String recoverLoop(String format) {
        int break_point = 0;

        while (break_point < format.length()) {
            if ("0123456789*".indexOf(format.charAt(break_point)) != -1) {
                break_point++;
                continue;
            }

            int repeated_length = 1;
            int max_repeated_length = -1;

            while (repeated_length <= break_point && break_point + repeated_length <= format.length()) {
                if (format.substring(break_point - repeated_length, break_point)
                        .equals(format.substring(break_point, break_point + repeated_length))) {
                    max_repeated_length = repeated_length;
                }

                repeated_length++;
            }

            if (max_repeated_length == -1) {
                break_point++;
            } else {
                final String repeated = format.substring(break_point, break_point + max_repeated_length);

                int count = 2;
                int rep_point = break_point + max_repeated_length;

                while (rep_point + max_repeated_length <= format.length()) {
                    if (!format.substring(rep_point, rep_point + max_repeated_length).equals(repeated)) {
                        break;
                    }

                    count++;
                    rep_point += max_repeated_length;
                }

                final StringBuilder builder = new StringBuilder();
                builder.append(format.substring(0, break_point - max_repeated_length));
                builder.append('(');
                builder.append(repeated);
                builder.append(')');
                builder.append(count);
                builder.append(format.substring(rep_point));
                format = builder.toString();
            }

        }

        return format;
    }

    /**
     * Provide a simple string describing the format expression that is short
     * enough to be used in Truffle and Graal diagnostics.
     */
    public static String describe(String format) {
        format = format.replace("\\s+", "");

        if (format.length() > 10) {
            format = format.substring(0, 10) + "…";
        }

        return format;
    }


}
