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

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.SourceNode;
import org.jruby.truffle.format.nodes.control.*;
import org.jruby.truffle.format.nodes.decode.*;
import org.jruby.truffle.format.nodes.read.*;
import org.jruby.truffle.format.nodes.type.AsSinglePrecisionNodeGen;
import org.jruby.truffle.format.nodes.type.ReinterpretLongNodeGen;
import org.jruby.truffle.format.nodes.type.ToLongNodeGen;
import org.jruby.truffle.format.nodes.write.*;
import org.jruby.truffle.format.parser.PackBaseListener;
import org.jruby.truffle.format.parser.PackParser;
import org.jruby.truffle.format.runtime.PackEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class UnpackTreeBuilder extends PackBaseListener {

    private final RubyContext context;
    private final Node currentNode;

    private PackEncoding encoding = PackEncoding.DEFAULT;
    private final Deque<List<PackNode>> sequenceStack = new ArrayDeque<>();

    public UnpackTreeBuilder(RubyContext context, Node currentNode) {
        this.context = context;
        this.currentNode = currentNode;
        pushSequence();
    }

    @Override
    public void enterSequence(PackParser.SequenceContext ctx) {
        pushSequence();
    }

    @Override
    public void exitSequence(PackParser.SequenceContext ctx) {
        final List<PackNode> sequence = sequenceStack.pop();
        appendNode(new SequenceNode(context, sequence.toArray(new PackNode[sequence.size()])));
    }

    @Override
    public void exitCharacter(PackParser.CharacterContext ctx) {
        appendNode(applyCount(ctx.count(), WriteValueNodeGen.create(context,
                ReadByteNodeGen.create(context,
                        new SourceNode()))));
    }

    @Override
    public void exitShortLittle(PackParser.ShortLittleContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(16, ByteOrder.LITTLE_ENDIAN)));
    }

    @Override
    public void exitShortBig(PackParser.ShortBigContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(16, ByteOrder.BIG_ENDIAN)));
    }

    @Override
    public void exitShortNative(PackParser.ShortNativeContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(16, ByteOrder.nativeOrder())));
    }

    @Override
    public void exitIntLittle(PackParser.IntLittleContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.LITTLE_ENDIAN)));
    }

    @Override
    public void exitIntBig(PackParser.IntBigContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.BIG_ENDIAN)));
    }

    @Override
    public void exitIntNative(PackParser.IntNativeContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.nativeOrder())));
    }

    @Override
    public void exitLongLittle(PackParser.LongLittleContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.LITTLE_ENDIAN)));
    }

    @Override
    public void exitLongBig(PackParser.LongBigContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.BIG_ENDIAN)));
    }

    @Override
    public void exitLongNative(PackParser.LongNativeContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(32, ByteOrder.nativeOrder())));
    }

    @Override
    public void exitQuadLittle(PackParser.QuadLittleContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(64, ByteOrder.LITTLE_ENDIAN)));
    }

    @Override
    public void exitQuadBig(PackParser.QuadBigContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(64, ByteOrder.BIG_ENDIAN)));
    }

    @Override
    public void exitQuadNative(PackParser.QuadNativeContext ctx) {
        appendNode(applyCount(ctx.count(), readInteger(64, ByteOrder.nativeOrder())));
    }

    @Override
    public void exitUtf8Character(PackParser.Utf8CharacterContext ctx) {
        throw new UnsupportedOperationException();
        //unify(PackEncoding.UTF_8);

        //appendNode(applyCount(ctx.count(), WriteUTF8CharacterNodeGen.create(context,
        //        ToLongNodeGen.create(context,
        //                ReadValueNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitBerInteger(PackParser.BerIntegerContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        WriteBERNodeGen.create(context, ReadLongOrBigIntegerNodeGen.create(context, new SourceNode()))));
    }

    @Override
    public void exitDoubleNative(PackParser.DoubleNativeContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(64, ByteOrder.nativeOrder(),
        //                ReinterpretLongNodeGen.create(context,
        //                        ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitFloatNative(PackParser.FloatNativeContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(32, ByteOrder.nativeOrder(),
        //                ReinterpretLongNodeGen.create(context,
        //                        AsSinglePrecisionNodeGen.create(context,
        //                                ReadDoubleNodeGen.create(context, new SourceNode()))))));
    }

    @Override
    public void exitDoubleLittle(PackParser.DoubleLittleContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(64, ByteOrder.LITTLE_ENDIAN,
        //                ReinterpretLongNodeGen.create(context,
        //                        ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitFloatLittle(PackParser.FloatLittleContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(32, ByteOrder.LITTLE_ENDIAN,
        //                ReinterpretLongNodeGen.create(context,
        //                        AsSinglePrecisionNodeGen.create(context,
        //                            ReadDoubleNodeGen.create(context, new SourceNode()))))));
    }

    @Override
    public void exitDoubleBig(PackParser.DoubleBigContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(64, ByteOrder.BIG_ENDIAN,
        //                ReinterpretLongNodeGen.create(context,
        //                        ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitFloatBig(PackParser.FloatBigContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(applyCount(ctx.count(),
        //        writeInteger(32, ByteOrder.BIG_ENDIAN,
        //                ReinterpretLongNodeGen.create(context,
        //                        AsSinglePrecisionNodeGen.create(context,
        //                            ReadDoubleNodeGen.create(context, new SourceNode()))))));
    }

    @Override
    public void exitBinaryStringSpacePadded(PackParser.BinaryStringSpacePaddedContext ctx) {
        //binaryString((byte) ' ', true, false, ctx.count());
    }

    @Override
    public void exitBinaryStringNullPadded(PackParser.BinaryStringNullPaddedContext ctx) {
        //binaryString((byte) 0, true, false, ctx.count());
    }

    @Override
    public void exitBinaryStringNullStar(PackParser.BinaryStringNullStarContext ctx) {
        //binaryString((byte) 0, true, ctx.count() != null && ctx.count().INT() == null, ctx.count());
    }

    @Override
    public void exitBitStringMSBFirst(PackParser.BitStringMSBFirstContext ctx) {
        //bitString(ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitBitStringMSBLast(PackParser.BitStringMSBLastContext ctx) {
        //bitString(ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitHexStringHighFirst(PackParser.HexStringHighFirstContext ctx) {
        //hexString(ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitHexStringLowFirst(PackParser.HexStringLowFirstContext ctx) {
        //hexString(ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUuString(PackParser.UuStringContext ctx) {
        throw new UnsupportedOperationException();
        /*unify(PackEncoding.US_ASCII);

        final int length;
        final boolean ignoreStar;

        if (ctx.count() == null) {
            length = 1;
            ignoreStar = false;
        } else if (ctx.count().INT() == null) {
            length = 0;
            ignoreStar = true;
        } else {
            length = Integer.parseInt(ctx.count().INT().getText());
            ignoreStar = false;
        }

        appendNode(WriteUUStringNodeGen.create(context, length, ignoreStar,
                ReadStringNodeGen.create(context, false, "to_str",
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));*/
    }

    @Override
    public void exitMimeString(PackParser.MimeStringContext ctx) {
        throw new UnsupportedOperationException();
        /*unify(PackEncoding.US_ASCII);

        int length;

        if (ctx.INT() == null) {
            length = 72;
        } else {
            length = Integer.parseInt(ctx.INT().getText());

            if (length <= 1) {
                length = 72;
            }
        }

        appendNode(WriteMIMEStringNodeGen.create(context, length,
                ReadStringNodeGen.create(context, true, "to_s",
                        true, context.getCoreLibrary().getNilObject(), new SourceNode())));*/

    }

    @Override
    public void exitBase64String(PackParser.Base64StringContext ctx) {
        throw new UnsupportedOperationException();
        /*unify(PackEncoding.US_ASCII);

        final int length;
        final boolean ignoreStar;

        if (ctx.count() == null) {
            length = 1;
            ignoreStar = false;
        } else if (ctx.count().INT() == null) {
            length = 0;
            ignoreStar = true;
        } else {
            length = Integer.parseInt(ctx.count().INT().getText());
            ignoreStar = false;
        }

        appendNode(WriteBase64StringNodeGen.create(context, length, ignoreStar,
                ReadStringNodeGen.create(context, false, "to_str",
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));*/
    }

    @Override
    public void exitPointer(PackParser.PointerContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode(writeInteger(64, ByteOrder.nativeOrder(),
        //        new PNode(context)));
    }

    @Override
    public void exitAt(PackParser.AtContext ctx) {
        throw new UnsupportedOperationException();
        /*final int position;

        if (ctx.INT() == null) {
            position = 1;
        } else {
            position = Integer.parseInt(ctx.INT().getText());
        }

        appendNode(new AtNode(context, position));*/
    }

    @Override
    public void exitBack(PackParser.BackContext ctx) {
        if (ctx.count() == null || ctx.count().INT() != null) {
            appendNode(applyCount(ctx.count(), new BackNode(context)));
        }
    }

    @Override
    public void exitNullByte(PackParser.NullByteContext ctx) {
        throw new UnsupportedOperationException();
        //appendNode((applyCount(ctx.count(), new WriteByteNode(context, (byte) 0))));
    }

    @Override
    public void enterSubSequence(PackParser.SubSequenceContext ctx) {
        pushSequence();
    }

    @Override
    public void exitSubSequence(PackParser.SubSequenceContext ctx) {
        final List<PackNode> sequence = sequenceStack.pop();
        final SequenceNode sequenceNode = new SequenceNode(context, sequence.toArray(new PackNode[sequence.size()]));

        final PackNode resultingNode;

        if (ctx.INT() == null) {
            resultingNode = sequenceNode;
        } else {
            resultingNode = new NNode(context, Integer.parseInt(ctx.INT().getText()), sequenceNode);
        }

        appendNode(resultingNode);
    }

    @Override
    public void exitErrorDisallowedNative(PackParser.ErrorDisallowedNativeContext ctx) {
        throw new RaiseException(context.getCoreLibrary().argumentError("'" + ctx.NATIVE().getText() + "' allowed only after types sSiIlLqQ", currentNode));
    }

    public PackNode getNode() {
        return sequenceStack.peek().get(0);
    }

    public PackEncoding getEncoding() {
        return encoding;
    }

    private void pushSequence() {
        sequenceStack.push(new ArrayList<PackNode>());
    }

    private void appendNode(PackNode node) {
        sequenceStack.peek().add(node);
    }

    private PackNode readInteger(int size, ByteOrder byteOrder) {
        final PackNode readNode = ReadBytesNodeGen.create(context, size, new SourceNode());
        return readInteger(size, byteOrder, readNode);
    }

    private PackNode readInteger(int size, ByteOrder byteOrder, PackNode readNode) {
        final PackNode convert;

        switch (size) {
            case 16:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convert = DecodeInteger16LittleNodeGen.create(context, readNode);
                } else {
                    convert = DecodeInteger16BigNodeGen.create(context, readNode);
                }
                break;
            case 32:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convert = DecodeInteger32LittleNodeGen.create(context, readNode);
                } else {
                    convert = DecodeInteger32BigNodeGen.create(context, readNode);
                }
                break;
            case 64:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convert = DecodeInteger64LittleNodeGen.create(context, readNode);
                } else {
                    convert = DecodeInteger64BigNodeGen.create(context, readNode);
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return WriteValueNodeGen.create(context, convert);
    }

    private void binaryString(byte padding, boolean padOnNull, boolean appendNull, PackParser.CountContext count) {
        unify(PackEncoding.ASCII_8BIT);

        final boolean pad;
        final int width;

        if (count != null && count.INT() != null) {
            pad = true;
            width = Integer.parseInt(count.INT().getText());
        } else {
            pad = false;

            if (count != null && count.INT() == null) {
                padOnNull = false;
            }

            width = 1;
        }

        final boolean takeAll;

        if (count != null && count.INT() == null) {
            takeAll = true;
        } else {
            takeAll = false;
        }

        appendNode(WriteBinaryStringNodeGen.create(context, pad, padOnNull,
                width, padding, takeAll, appendNull,
                ReadStringNodeGen.create(context, true, "to_str",
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));

    }

    private void bitString(ByteOrder byteOrder, PackParser.CountContext ctx) {
        final boolean star;
        final int length;

        if (ctx == null) {
            star = false;
            length = 1;
        } else if (ctx.INT() == null) {
            star = true;
            length = 0;
        } else {
            star = false;
            length = Integer.parseInt(ctx.INT().getText());
        }

        appendNode(WriteBitStringNodeGen.create(context, byteOrder, star, length,
                ReadStringNodeGen.create(context, true, "to_str",
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));
    }

    private void hexString(ByteOrder byteOrder, PackParser.CountContext ctx) {
        final int length;

        if (ctx == null) {
            length = 1;
        } else if (ctx.INT() == null) {
            length = -1;
        } else {
            length = Integer.parseInt(ctx.INT().getText());
        }

        appendNode(WriteHexStringNodeGen.create(context, byteOrder, length,
                ReadStringNodeGen.create(context, true, "to_str",
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));

    }

    private PackNode applyCount(PackParser.CountContext count, PackNode node) {
        if (count == null) {
            return node;
        } else if (count.INT() != null) {
            return new NNode(context, Integer.parseInt(count.INT().getText()), node);
        } else {
            return new StarNode(context, node);
        }
    }
    
    private void unify(PackEncoding other) {
        encoding = encoding.unifyWith(other);
    }

}
