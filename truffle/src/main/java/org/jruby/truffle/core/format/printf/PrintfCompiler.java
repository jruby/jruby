/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.printf;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.DescriptionTruncater;
import org.jruby.truffle.core.format.FormatEncoding;
import org.jruby.truffle.core.format.FormatErrorListener;
import org.jruby.truffle.core.format.FormatRootNode;
import org.jruby.truffle.language.RubyNode;

public class PrintfCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PrintfCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(String formatString, byte[] format) {
        final FormatErrorListener errorListener = new FormatErrorListener(context, currentNode);

        final ANTLRInputStream input = new ANTLRInputStream(bytesToChars(format), format.length);

        final PrintfLexer lexer = new PrintfLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        final CommonTokenStream tokens = new CommonTokenStream(lexer);

        final PrintfParser parser = new PrintfParser(tokens);

        final PrintfTreeBuilder builder = new PrintfTreeBuilder(context, format);
        parser.addParseListener(builder);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        parser.sequence();

        return Truffle.getRuntime().createCallTarget(
                new FormatRootNode(DescriptionTruncater.trunate(formatString),
                        FormatEncoding.DEFAULT, builder.getNode()));
    }

    private static char[] bytesToChars(byte[] bytes) {
        final char[] chars = new char[bytes.length];

        for (int n = 0; n < bytes.length; n++) {
            chars[n] = (char) bytes[n];
        }

        return chars;
    }

}
