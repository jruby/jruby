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
import org.jruby.truffle.format.runtime.PackEncoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

public class PrintfCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PrintfCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(ByteList format) {
        final PackErrorListener errorListener = new PackErrorListener(context, currentNode);

        final ANTLRInputStream input = new ANTLRInputStream(bytesToChars(format.bytes()), format.realSize());

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
                new PackRootNode(PackCompiler.describe(format.toString()), PackEncoding.DEFAULT, builder.getNode()));
    }

    public static char[] bytesToChars(byte[] bytes) {
        final char[] chars = new char[bytes.length];

        for (int n = 0; n < bytes.length; n++) {
            chars[n] = (char) bytes[n];
        }

        return chars;
    }

}
