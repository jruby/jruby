/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.pack;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.DescriptionTruncater;
import org.jruby.truffle.core.format.FormatErrorListener;
import org.jruby.truffle.core.format.FormatRootNode;
import org.jruby.truffle.core.format.LoopRecovery;
import org.jruby.truffle.language.RubyNode;

public class PackCompiler {

    private final RubyContext context;
    private final RubyNode currentNode;

    public PackCompiler(RubyContext context, RubyNode currentNode) {
        this.context = context;
        this.currentNode = currentNode;
    }

    public CallTarget compile(String format) {
        if (format.length() > context.getOptions().PACK_RECOVER_LOOP_MIN) {
            format = LoopRecovery.recoverLoop(format);
        }

        final FormatErrorListener errorListener = new FormatErrorListener(context, currentNode);

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
                new FormatRootNode(DescriptionTruncater.trunate(format), builder.getEncoding(), builder.getNode()));
    }

}
