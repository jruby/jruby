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

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatEncoding;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.LiteralFormatNode;
import org.jruby.truffle.core.format.SharedTreeBuilder;
import org.jruby.truffle.core.format.control.AdvanceSourcePositionNode;
import org.jruby.truffle.core.format.control.ReverseOutputPositionNode;
import org.jruby.truffle.core.format.control.SequenceNode;
import org.jruby.truffle.core.format.control.SetOutputPositionNode;
import org.jruby.truffle.core.format.convert.Integer16BigToBytesNodeGen;
import org.jruby.truffle.core.format.convert.Integer16LittleToBytesNodeGen;
import org.jruby.truffle.core.format.convert.Integer32BigToBytesNodeGen;
import org.jruby.truffle.core.format.convert.Integer32LittleToBytesNodeGen;
import org.jruby.truffle.core.format.convert.Integer64BigToBytesNodeGen;
import org.jruby.truffle.core.format.convert.Integer64LittleToBytesNodeGen;
import org.jruby.truffle.core.format.convert.ReinterpretAsLongNodeGen;
import org.jruby.truffle.core.format.convert.ToFloatNodeGen;
import org.jruby.truffle.core.format.convert.ToLongNodeGen;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.format.read.array.ReadDoubleNodeGen;
import org.jruby.truffle.core.format.read.array.ReadLongOrBigIntegerNodeGen;
import org.jruby.truffle.core.format.read.array.ReadStringNodeGen;
import org.jruby.truffle.core.format.read.array.ReadValueNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBERNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBase64StringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBinaryStringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBitStringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteByteNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteBytesNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteHexStringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteMIMEStringNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteUTF8CharacterNodeGen;
import org.jruby.truffle.core.format.write.bytes.WriteUUStringNodeGen;
import org.jruby.truffle.language.control.RaiseException;

import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jruby.truffle.core.format.pack.PackParser;
import org.jruby.truffle.core.format.pack.PackBaseListener;

public class PackTreeBuilder extends PackBaseListener {

    private final RubyContext context;
    private final Node currentNode;

    private final SharedTreeBuilder sharedTreeBuilder;

    private FormatEncoding encoding = FormatEncoding.DEFAULT;
    private final Deque<List<FormatNode>> sequenceStack = new ArrayDeque<>();

    public PackTreeBuilder(RubyContext context, Node currentNode) {
        this.context = context;
        this.currentNode = currentNode;
        sharedTreeBuilder = new SharedTreeBuilder(context);
        pushSequence();
    }

    @Override
    public void enterSequence(PackParser.SequenceContext ctx) {
        pushSequence();
    }

    @Override
    public void exitSequence(PackParser.SequenceContext ctx) {
        final List<FormatNode> sequence = sequenceStack.pop();
        appendNode(new SequenceNode(context, sequence.toArray(new FormatNode[sequence.size()])));
    }

    @Override
    public void exitInt8(PackParser.Int8Context ctx) {
        appendIntegerNode(8, null, ctx.count());
    }

    @Override
    public void exitUint8(PackParser.Uint8Context ctx) {
        appendIntegerNode(8, null, ctx.count());
    }

    @Override
    public void exitInt16Little(PackParser.Int16LittleContext ctx) {
        appendIntegerNode(16, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt16Big(PackParser.Int16BigContext ctx) {
        appendIntegerNode(16, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt16Native(PackParser.Int16NativeContext ctx) {
        appendIntegerNode(16, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint16Little(PackParser.Uint16LittleContext ctx) {
        appendIntegerNode(16, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint16Big(PackParser.Uint16BigContext ctx) {
        appendIntegerNode(16, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint16Native(PackParser.Uint16NativeContext ctx) {
        appendIntegerNode(16, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitInt32Little(PackParser.Int32LittleContext ctx) {
        appendIntegerNode(32, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt32Big(PackParser.Int32BigContext ctx) {
        appendIntegerNode(32, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt32Native(PackParser.Int32NativeContext ctx) {
        appendIntegerNode(32, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint32Little(PackParser.Uint32LittleContext ctx) {
        appendIntegerNode(32, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint32Big(PackParser.Uint32BigContext ctx) {
        appendIntegerNode(32, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint32Native(PackParser.Uint32NativeContext ctx) {
        appendIntegerNode(32, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitInt64Little(PackParser.Int64LittleContext ctx) {
        appendIntegerNode(64, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt64Big(PackParser.Int64BigContext ctx) {
        appendIntegerNode(64, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt64Native(PackParser.Int64NativeContext ctx) {
        appendIntegerNode(64, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint64Little(PackParser.Uint64LittleContext ctx) {
        appendIntegerNode(64, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint64Big(PackParser.Uint64BigContext ctx) {
        appendIntegerNode(64, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint64Native(PackParser.Uint64NativeContext ctx) {
        appendIntegerNode(64, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUtf8Character(PackParser.Utf8CharacterContext ctx) {
        unify(FormatEncoding.UTF_8);

        appendNode(sharedTreeBuilder.applyCount(ctx.count(),
                WriteUTF8CharacterNodeGen.create(context,
                        ToLongNodeGen.create(context, false,
                                ReadValueNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitBerInteger(PackParser.BerIntegerContext ctx) {
        appendNode(sharedTreeBuilder.applyCount(ctx.count(),
                WriteBERNodeGen.create(context,
                        ReadLongOrBigIntegerNodeGen.create(context, new SourceNode()))));

    }

    @Override
    public void exitF64Native(PackParser.F64NativeContext ctx) {
        appendFloatNode(64, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitF32Native(PackParser.F32NativeContext ctx) {
        appendFloatNode(32, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitF64Little(PackParser.F64LittleContext ctx) {
        appendFloatNode(64, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitF32Little(PackParser.F32LittleContext ctx) {
        appendFloatNode(32, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitF64Big(PackParser.F64BigContext ctx) {
        appendFloatNode(64, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitF32Big(PackParser.F32BigContext ctx) {
        appendFloatNode(32, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitBinaryStringSpacePadded(PackParser.BinaryStringSpacePaddedContext ctx) {
        binaryString((byte) ' ', true, false, ctx.count());
    }

    @Override
    public void exitBinaryStringNullPadded(PackParser.BinaryStringNullPaddedContext ctx) {
        binaryString((byte) 0, true, false, ctx.count());
    }

    @Override
    public void exitBinaryStringNullStar(PackParser.BinaryStringNullStarContext ctx) {
        binaryString((byte) 0, true, ctx.count() != null && ctx.count().INT() == null, ctx.count());
    }

    @Override
    public void exitBitStringMSBFirst(PackParser.BitStringMSBFirstContext ctx) {
        bitString(ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitBitStringMSBLast(PackParser.BitStringMSBLastContext ctx) {
        bitString(ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitHexStringHighFirst(PackParser.HexStringHighFirstContext ctx) {
        hexString(ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitHexStringLowFirst(PackParser.HexStringLowFirstContext ctx) {
        hexString(ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUuString(PackParser.UuStringContext ctx) {
        unify(FormatEncoding.US_ASCII);

        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(ctx.count());

        appendNode(WriteUUStringNodeGen.create(context, starLength.getLength(), starLength.isStar(),
                ReadStringNodeGen.create(context, false, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    @Override
    public void exitMimeString(PackParser.MimeStringContext ctx) {
        unify(FormatEncoding.US_ASCII);

        int length;

        if (ctx.count() == null || ctx.count().INT() == null) {
            length = 72;
        } else {
            length = Integer.parseInt(ctx.count().INT().getText());

            if (length <= 1) {
                length = 72;
            }
        }

        appendNode(WriteMIMEStringNodeGen.create(context, length,
                ReadStringNodeGen.create(context, true, "to_s",
                        true, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));

    }

    @Override
    public void exitBase64String(PackParser.Base64StringContext ctx) {
        unify(FormatEncoding.US_ASCII);

        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(ctx.count());

        appendNode(WriteBase64StringNodeGen.create(context, starLength.getLength(), starLength.isStar(),
                ReadStringNodeGen.create(context, false, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    @Override
    public void exitPointer(PackParser.PointerContext ctx) {
        /*
         * P and p print the address of a string. Obviously that doesn't work
         * well in Java. We'll print 0x0000000000000000 with the hope that at
         * least it should page fault if anyone tries to read it.
         */

        appendNode(new SequenceNode(context, new FormatNode[]{
                new AdvanceSourcePositionNode(context, false),
                writeInteger(64, ByteOrder.nativeOrder(),
                    new LiteralFormatNode(context, (long) 0))
        }));

    }

    @Override
    public void exitAt(PackParser.AtContext ctx) {
        final int position;

        if (ctx.count() == null) {
            position = 1;
        } else if (ctx.count() != null && ctx.count().INT() == null) {
            throw new UnsupportedOperationException();
        } else {
            position = Integer.parseInt(ctx.count().INT().getText());
        }

        appendNode(new SetOutputPositionNode(context, position));
    }

    @Override
    public void exitBack(PackParser.BackContext ctx) {
        if (ctx.count() == null || ctx.count().INT() != null) {
            appendNode(sharedTreeBuilder.applyCount(ctx.count(), new ReverseOutputPositionNode(context)));
        }
    }

    @Override
    public void exitNullByte(PackParser.NullByteContext ctx) {
        appendNode((sharedTreeBuilder.applyCount(ctx.count(),
                WriteByteNodeGen.create(context,
                        new LiteralFormatNode(context, (byte) 0)))));
    }

    @Override
    public void enterSubSequence(PackParser.SubSequenceContext ctx) {
        pushSequence();
    }

    @Override
    public void exitSubSequence(PackParser.SubSequenceContext ctx) {
        appendNode(sharedTreeBuilder.finishSubSequence(sequenceStack, ctx));
    }

    @Override
    public void exitErrorDisallowedNative(PackParser.ErrorDisallowedNativeContext ctx) {
        throw new RaiseException(context.getCoreExceptions().argumentError(
                "'" + ctx.NATIVE().getText() + "' allowed only after types sSiIlLqQ", currentNode));
    }

    public FormatNode getNode() {
        return sequenceStack.peek().get(0);
    }

    public FormatEncoding getEncoding() {
        return encoding;
    }

    private void pushSequence() {
        sequenceStack.push(new ArrayList<FormatNode>());
    }

    private void appendNode(FormatNode node) {
        sequenceStack.peek().add(node);
    }

    private void appendIntegerNode(int size, ByteOrder byteOrder, PackParser.CountContext count) {
        appendNode(sharedTreeBuilder.applyCount(count, writeInteger(size, byteOrder)));
    }

    private void appendFloatNode(int size, ByteOrder byteOrder, PackParser.CountContext count) {
        final FormatNode readNode = ReadDoubleNodeGen.create(context, new SourceNode());

        final FormatNode typeNode;

        switch (size) {
            case 32:
                typeNode = ToFloatNodeGen.create(context, readNode);
                break;
            case 64:
                typeNode = readNode;
                break;
            default:
                throw new IllegalArgumentException();
        }

        appendNode(sharedTreeBuilder.applyCount(count,
                writeInteger(size, byteOrder,
                        ReinterpretAsLongNodeGen.create(context,
                                typeNode))));
    }

    private FormatNode writeInteger(int size, ByteOrder byteOrder) {
        final FormatNode readNode = ToLongNodeGen.create(context, false,
                ReadValueNodeGen.create(context, new SourceNode()));

        return writeInteger(size, byteOrder, readNode);
    }

    private FormatNode writeInteger(int size, ByteOrder byteOrder, FormatNode readNode) {
        final FormatNode convertNode;

        switch (size) {
            case 8:
                return WriteByteNodeGen.create(context, readNode);
            case 16:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer16LittleToBytesNodeGen.create(context, readNode);
                } else {
                    convertNode = Integer16BigToBytesNodeGen.create(context, readNode);
                }
                break;
            case 32:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer32LittleToBytesNodeGen.create(context, readNode);
                } else {
                    convertNode = Integer32BigToBytesNodeGen.create(context, readNode);
                }
                break;
            case 64:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer64LittleToBytesNodeGen.create(context, readNode);
                } else {
                    convertNode = Integer64BigToBytesNodeGen.create(context, readNode);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

        return WriteBytesNodeGen.create(context, convertNode);
    }

    private void binaryString(byte padding, boolean padOnNull, boolean appendNull, PackParser.CountContext count) {
        unify(FormatEncoding.ASCII_8BIT);

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
                        false, context.getCoreLibrary().getNilObject(),
                                new SourceNode())));

    }

    private void bitString(ByteOrder byteOrder, PackParser.CountContext ctx) {
        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(ctx);

        appendNode(WriteBitStringNodeGen.create(context, byteOrder, starLength.isStar(), starLength.getLength(),
                ReadStringNodeGen.create(context, true, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                                new SourceNode())));
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
                        false, context.getCoreLibrary().getNilObject(),
                                new SourceNode())));

    }

    private void unify(FormatEncoding other) {
        encoding = encoding.unifyWith(other);
    }

}
