/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.parser;

import com.oracle.truffle.api.object.DynamicObject;
import org.antlr.v4.runtime.Token;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.nodes.PackNode;
import org.jruby.truffle.core.format.nodes.SourceNode;
import org.jruby.truffle.core.format.nodes.control.SequenceNode;
import org.jruby.truffle.core.format.nodes.format.FormatFloatNodeGen;
import org.jruby.truffle.core.format.nodes.format.FormatIntegerNodeGen;
import org.jruby.truffle.core.format.nodes.read.LiteralBytesNode;
import org.jruby.truffle.core.format.nodes.read.LiteralIntegerNode;
import org.jruby.truffle.core.format.nodes.read.ReadHashValueNodeGen;
import org.jruby.truffle.core.format.nodes.read.ReadIntegerNodeGen;
import org.jruby.truffle.core.format.nodes.read.ReadStringNodeGen;
import org.jruby.truffle.core.format.nodes.read.ReadValueNodeGen;
import org.jruby.truffle.core.format.nodes.type.ToDoubleWithCoercionNodeGen;
import org.jruby.truffle.core.format.nodes.type.ToIntegerNodeGen;
import org.jruby.truffle.core.format.nodes.type.ToStringNodeGen;
import org.jruby.truffle.core.format.nodes.write.WriteByteNode;
import org.jruby.truffle.core.format.nodes.write.WriteBytesNodeGen;
import org.jruby.truffle.core.format.nodes.write.WritePaddedBytesNodeGen;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

public class PrintfTreeBuilder extends org.jruby.truffle.core.format.parser.PrintfParserBaseListener {

    public static final int PADDING_FROM_ARGUMENT = -2;

    public static final int DEFAULT = -1;

    private final RubyContext context;
    private final ByteList source;

    private final List<PackNode> sequence = new ArrayList<>();

    public PrintfTreeBuilder(RubyContext context, ByteList source) {
        this.context = context;
        this.source = source;
    }

    @Override
    public void exitEscaped(org.jruby.truffle.core.format.parser.PrintfParser.EscapedContext ctx) {
        sequence.add(new WriteByteNode(context, (byte) '%'));
    }

    @Override
    public void exitString(org.jruby.truffle.core.format.parser.PrintfParser.StringContext ctx) {
        final ByteList keyBytes = tokenAsBytes(ctx.CURLY_KEY().getSymbol(), 1);
        final DynamicObject key = context.getSymbol(keyBytes);

        sequence.add(
                WriteBytesNodeGen.create(context,
                    ToStringNodeGen.create(context, true, "to_s", false, new ByteList(),
                        ReadHashValueNodeGen.create(context, key, new SourceNode()))));
    }

    @Override
    public void exitFormat(org.jruby.truffle.core.format.parser.PrintfParser.FormatContext ctx) {
        final int width;

        if (ctx.width != null) {
            width = Integer.parseInt(ctx.width.getText());
        } else {
            width = DEFAULT;
        }

        boolean leftJustified = false;
        int spacePadding = DEFAULT;
        int zeroPadding = DEFAULT;


        for (int n = 0; n < ctx.flag().size(); n++) {
            final org.jruby.truffle.core.format.parser.PrintfParser.FlagContext flag = ctx.flag(n);

            if (flag.MINUS() != null) {
                leftJustified = true;
            } else if (flag.SPACE() != null) {
                if (n + 1 < ctx.flag().size() && ctx.flag(n + 1).STAR() != null) {
                    spacePadding = PADDING_FROM_ARGUMENT;
                } else {
                    spacePadding = width;
                }
            } else if (flag.ZERO() != null) {
                if (n + 1 < ctx.flag().size() && ctx.flag(n + 1).STAR() != null) {
                    zeroPadding = PADDING_FROM_ARGUMENT;
                } else {
                    zeroPadding = width;
                }
            } else if (flag.STAR() != null) {
                // Handled in space and zero, above
            } else {
                throw new UnsupportedOperationException();
            }
        }

        if (spacePadding == DEFAULT && zeroPadding == DEFAULT) {
            spacePadding = width;
        }

        final char type = ctx.TYPE().getSymbol().getText().charAt(0);

        final PackNode valueNode;

        if (ctx.ANGLE_KEY() == null) {
            valueNode = ReadValueNodeGen.create(context, new SourceNode());
        } else {
            final ByteList keyBytes = tokenAsBytes(ctx.ANGLE_KEY().getSymbol(), 1);
            final DynamicObject key = context.getSymbol(keyBytes);
            valueNode = ReadHashValueNodeGen.create(context, key, new SourceNode());
        }

        final int precision;

        if (ctx.precision != null) {
            precision = Integer.parseInt(ctx.precision.getText());
        } else {
            precision = DEFAULT;
        }

        final PackNode node;

        switch (type) {
            case 's':
                if (ctx.ANGLE_KEY() == null) {
                    if (spacePadding == DEFAULT) {
                        node = WriteBytesNodeGen.create(context, ReadStringNodeGen.create(
                                context, true, "to_s", false, new ByteList(), new SourceNode()));
                    } else {
                        node = WritePaddedBytesNodeGen.create(context, spacePadding, leftJustified,
                                ReadStringNodeGen.create(context, true, "to_s", false, new ByteList(), new SourceNode()));
                    }
                } else {
                    if (spacePadding == DEFAULT) {
                        node = WriteBytesNodeGen.create(context, ToStringNodeGen.create(
                                context, true, "to_s", false, new ByteList(), valueNode));
                    } else {
                        node = WritePaddedBytesNodeGen.create(context, spacePadding, leftJustified,
                                ToStringNodeGen.create(context, true, "to_s", false, new ByteList(), valueNode));
                    }
                }
                break;
            case 'd':
            case 'i':
            case 'o':
            case 'u':
            case 'x':
            case 'X':
                final PackNode spacePaddingNode;
                if (spacePadding == PADDING_FROM_ARGUMENT) {
                    spacePaddingNode = ReadIntegerNodeGen.create(context, new SourceNode());
                } else {
                    spacePaddingNode = new LiteralIntegerNode(context, spacePadding);
                }

                final PackNode zeroPaddingNode;

                        /*
                         * Precision and zero padding both set zero padding -
                         * but precision has priority and explicit zero padding
                         * is actually ignored if it's set.
                         */

                if (zeroPadding == PADDING_FROM_ARGUMENT) {
                    zeroPaddingNode = ReadIntegerNodeGen.create(context, new SourceNode());
                } else if (ctx.precision != null) {
                    zeroPaddingNode = new LiteralIntegerNode(context, Integer.parseInt(ctx.precision.getText()));
                } else {
                    zeroPaddingNode = new LiteralIntegerNode(context, zeroPadding);
                }

                final char format;

                switch (type) {
                    case 'd':
                    case 'i':
                    case 'u':
                        format = 'd';
                        break;
                    case 'o':
                        format = 'o';
                        break;
                    case 'x':
                    case 'X':
                        format = type;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                node = WriteBytesNodeGen.create(context,
                        FormatIntegerNodeGen.create(context, format,
                                spacePaddingNode,
                                zeroPaddingNode,
                                ToIntegerNodeGen.create(context, valueNode)));
                break;
            case 'f':
            case 'g':
            case 'G':
            case 'e':
            case 'E':
                node = WriteBytesNodeGen.create(context,
                        FormatFloatNodeGen.create(context, spacePadding,
                                zeroPadding, precision,
                                type,
                                ToDoubleWithCoercionNodeGen.create(context,
                                        valueNode)));
                break;
            default:
                throw new UnsupportedOperationException();
        }

        sequence.add(node);
    }

    @Override
    public void exitLiteral(org.jruby.truffle.core.format.parser.PrintfParser.LiteralContext ctx) {
        final ByteList text = tokenAsBytes(ctx.LITERAL().getSymbol());

        final PackNode node;

        if (text.length() == 1) {
            node = new WriteByteNode(context, (byte) text.get(0));
        } else {
            node = WriteBytesNodeGen.create(context, new LiteralBytesNode(context, text));
        }

        sequence.add(node);
    }

    public PackNode getNode() {
        return new SequenceNode(context, sequence.toArray(new PackNode[sequence.size()]));
    }

    private ByteList tokenAsBytes(Token token) {
        return tokenAsBytes(token, 0);
    }

    private ByteList tokenAsBytes(Token token, int trim) {
        return new ByteList(source, token.getStartIndex() + trim, token.getStopIndex() - token.getStartIndex() + 1 - 2 * trim);
    }

}
