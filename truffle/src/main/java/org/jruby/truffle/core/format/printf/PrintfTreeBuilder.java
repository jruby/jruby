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

import com.oracle.truffle.api.object.DynamicObject;
import org.antlr.v4.runtime.Token;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.format.control.SequenceNode;
import org.jruby.truffle.core.format.format.FormatFloatNodeGen;
import org.jruby.truffle.core.format.format.FormatIntegerNodeGen;
import org.jruby.truffle.core.format.LiteralFormatNode;
import org.jruby.truffle.core.format.read.array.ReadHashValueNodeGen;
import org.jruby.truffle.core.format.read.array.ReadIntegerNodeGen;
import org.jruby.truffle.core.format.read.array.ReadStringNodeGen;
import org.jruby.truffle.core.format.read.array.ReadValueNodeGen;
import org.jruby.truffle.core.format.convert.ToDoubleWithCoercionNodeGen;
import org.jruby.truffle.core.format.convert.ToIntegerNodeGen;
import org.jruby.truffle.core.format.convert.ToStringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteByteNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBytesNodeGen;
import org.jruby.truffle.core.format.write.bytes.WritePaddedBytesNodeGen;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

import org.jruby.truffle.core.format.printf.PrintfParserBaseListener;
import org.jruby.truffle.core.format.printf.PrintfParser;

public class PrintfTreeBuilder extends PrintfParserBaseListener {

    public static final int PADDING_FROM_ARGUMENT = -2;

    public static final int DEFAULT = -1;

    private final RubyContext context;
    private final byte[] source;

    private final List<FormatNode> sequence = new ArrayList<>();

    public PrintfTreeBuilder(RubyContext context, byte[] source) {
        this.context = context;
        this.source = source;
    }

    @Override
    public void exitEscaped(PrintfParser.EscapedContext ctx) {
        sequence.add(WriteByteNodeGen.create(context, new LiteralFormatNode(context, (byte) '%')));
    }

    @Override
    public void exitString(PrintfParser.StringContext ctx) {
        final ByteList keyBytes = tokenAsBytes(ctx.CURLY_KEY().getSymbol(), 1);
        final DynamicObject key = context.getSymbolTable().getSymbol(keyBytes);

        sequence.add(
                WriteBytesNodeGen.create(context,
                        ToStringNodeGen.create(context, true, "to_s", false, new ByteList(),
                                ReadHashValueNodeGen.create(context, key,
                                        new SourceNode()))));
    }

    @Override
    public void exitFormat(PrintfParser.FormatContext ctx) {
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
            final PrintfParser.FlagContext flag = ctx.flag(n);

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

        final FormatNode valueNode;

        if (ctx.ANGLE_KEY() == null) {
            valueNode = ReadValueNodeGen.create(context, new SourceNode());
        } else {
            final ByteList keyBytes = tokenAsBytes(ctx.ANGLE_KEY().getSymbol(), 1);
            final DynamicObject key = context.getSymbolTable().getSymbol(keyBytes);
            valueNode = ReadHashValueNodeGen.create(context, key, new SourceNode());
        }

        final int precision;

        if (ctx.precision != null) {
            precision = Integer.parseInt(ctx.precision.getText());
        } else {
            precision = DEFAULT;
        }

        final FormatNode node;

        switch (type) {
            case 's':
                if (ctx.ANGLE_KEY() == null) {
                    if (spacePadding == DEFAULT) {
                        node = WriteBytesNodeGen.create(context, ReadStringNodeGen.create(
                                context, true, "to_s", false, new ByteList(),
                                        new SourceNode()));
                    } else {
                        node = WritePaddedBytesNodeGen.create(context, spacePadding, leftJustified,
                                ReadStringNodeGen.create(context, true, "to_s", false, new ByteList(),
                                        new SourceNode()));
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
                final FormatNode spacePaddingNode;
                if (spacePadding == PADDING_FROM_ARGUMENT) {
                    spacePaddingNode = ReadIntegerNodeGen.create(context, new SourceNode());
                } else {
                    spacePaddingNode = new LiteralFormatNode(context, spacePadding);
                }

                final FormatNode zeroPaddingNode;

                /*
                 * Precision and zero padding both set zero padding -
                 * but precision has priority and explicit zero padding
                 * is actually ignored if it's set.
                 */

                if (zeroPadding == PADDING_FROM_ARGUMENT) {
                    zeroPaddingNode = ReadIntegerNodeGen.create(context, new SourceNode());
                } else if (ctx.precision != null) {
                    zeroPaddingNode = new LiteralFormatNode(context, Integer.parseInt(ctx.precision.getText()));
                } else {
                    zeroPaddingNode = new LiteralFormatNode(context, zeroPadding);
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
    public void exitLiteral(PrintfParser.LiteralContext ctx) {
        final ByteList text = tokenAsBytes(ctx.LITERAL().getSymbol());

        final FormatNode node;

        if (text.length() == 1) {
            node = WriteByteNodeGen.create(context, new LiteralFormatNode(context, (byte) text.get(0)));
        } else {
            node = WriteBytesNodeGen.create(context, new LiteralFormatNode(context, text));
        }

        sequence.add(node);
    }

    public FormatNode getNode() {
        return new SequenceNode(context, sequence.toArray(new FormatNode[sequence.size()]));
    }

    private ByteList tokenAsBytes(Token token) {
        return tokenAsBytes(token, 0);
    }

    private ByteList tokenAsBytes(Token token, int trim) {
        return new ByteList(source, token.getStartIndex() + trim, token.getStopIndex() - token.getStartIndex() + 1 - 2 * trim);
    }

}
