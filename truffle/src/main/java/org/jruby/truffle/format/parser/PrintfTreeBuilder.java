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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.control.SequenceNode;
import org.jruby.truffle.format.nodes.read.LiteralBytesNode;
import org.jruby.truffle.format.nodes.write.WriteByteNode;
import org.jruby.truffle.format.nodes.write.WriteBytesNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

public class PrintfTreeBuilder extends org.jruby.truffle.format.parser.PrintfBaseListener {

    private final RubyContext context;
    private final ByteList source;

    private final List<PackNode> sequence = new ArrayList<>();

    public PrintfTreeBuilder(RubyContext context, ByteList source) {
        this.context = context;
        this.source = source;
    }

    @Override
    public void exitEscape(org.jruby.truffle.format.parser.PrintfParser.EscapeContext ctx) {
        sequence.add(new WriteByteNode(context, (byte) '%'));
    }

    @Override
    public void exitString(org.jruby.truffle.format.parser.PrintfParser.StringContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exitFormat(org.jruby.truffle.format.parser.PrintfParser.FormatContext ctx) {
        if (ctx.ANGLE_KEY() != null) {
            throw new UnsupportedOperationException();
        }

        for (org.jruby.truffle.format.parser.PrintfParser.FlagContext flag : ctx.flag()) {
            throw new UnsupportedOperationException();
        }

        if (ctx.width != null) {
            throw new UnsupportedOperationException();
        }

        if (ctx.precision != null) {
            throw new UnsupportedOperationException();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public void exitText(org.jruby.truffle.format.parser.PrintfParser.TextContext ctx) {
        tokenAsText(ctx.TEXT().getSymbol());
    }

    @Override
    public void exitNumber(org.jruby.truffle.format.parser.PrintfParser.NumberContext ctx) {
        tokenAsText(ctx.NUMBER().getSymbol());
    }

    public PackNode getNode() {
        return new SequenceNode(context, sequence.toArray(new PackNode[sequence.size()]));
    }

    private void tokenAsText(Token token) {
        final ByteList text = new ByteList(source, token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1);

        final PackNode node;

        if (text.length() == 1) {
            node = new WriteByteNode(context, (byte) text.get(0));
        } else {
            node = WriteBytesNodeGen.create(context, new LiteralBytesNode(context, text));
        }

        sequence.add(node);
    }

}
