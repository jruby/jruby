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
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.SourceNode;
import org.jruby.truffle.core.format.control.AtNode;
import org.jruby.truffle.core.format.control.BackNode;
import org.jruby.truffle.core.format.control.NNode;
import org.jruby.truffle.core.format.control.SequenceNode;
import org.jruby.truffle.core.format.control.StarNode;
import org.jruby.truffle.core.format.read.ReadDoubleNodeGen;
import org.jruby.truffle.core.format.read.ReadLongOrBigIntegerNodeGen;
import org.jruby.truffle.core.format.read.ReadStringNodeGen;
import org.jruby.truffle.core.format.read.ReadValueNodeGen;
import org.jruby.truffle.core.format.type.AsSinglePrecisionNodeGen;
import org.jruby.truffle.core.format.type.ReinterpretLongNodeGen;
import org.jruby.truffle.core.format.type.ToLongNodeGen;
import org.jruby.truffle.core.format.write.PNode;
import org.jruby.truffle.core.format.write.Write16BigNodeGen;
import org.jruby.truffle.core.format.write.Write16LittleNodeGen;
import org.jruby.truffle.core.format.write.Write32BigNodeGen;
import org.jruby.truffle.core.format.write.Write32LittleNodeGen;
import org.jruby.truffle.core.format.write.Write64BigNodeGen;
import org.jruby.truffle.core.format.write.Write64LittleNodeGen;
import org.jruby.truffle.core.format.write.Write8NodeGen;
import org.jruby.truffle.core.format.write.WriteBERNodeGen;
import org.jruby.truffle.core.format.write.WriteBase64StringNodeGen;
import org.jruby.truffle.core.format.write.WriteBinaryStringNodeGen;
import org.jruby.truffle.core.format.write.WriteBitStringNodeGen;
import org.jruby.truffle.core.format.write.WriteByteNode;
import org.jruby.truffle.core.format.write.WriteHexStringNodeGen;
import org.jruby.truffle.core.format.write.WriteMIMEStringNodeGen;
import org.jruby.truffle.core.format.write.WriteUTF8CharacterNodeGen;
import org.jruby.truffle.core.format.write.WriteUUStringNodeGen;
import org.jruby.truffle.core.format.FormatEncoding;
import org.jruby.truffle.language.control.RaiseException;

import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jruby.truffle.core.format.pack.PackBaseListener;
import org.jruby.truffle.core.format.pack.PackParser;

public class PackTreeBuilder extends PackBaseListener {

    private final RubyContext context;
    private final Node currentNode;

    private FormatEncoding encoding = FormatEncoding.DEFAULT;
    private final Deque<List<FormatNode>> sequenceStack = new ArrayDeque<>();

    public PackTreeBuilder(RubyContext context, Node currentNode) {
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
        final List<FormatNode> sequence = sequenceStack.pop();
        appendNode(new SequenceNode(context, sequence.toArray(new FormatNode[sequence.size()])));
    }

    @Override
    public void exitInt8(PackParser.Int8Context ctx) {
        integer(8, null, ctx.count());
    }

    @Override
    public void exitUint8(PackParser.Uint8Context ctx) {
        integer(8, null, ctx.count());
    }

    @Override
    public void exitInt16Little(PackParser.Int16LittleContext ctx) {
        integer(16, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt16Big(PackParser.Int16BigContext ctx) {
        integer(16, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt16Native(PackParser.Int16NativeContext ctx) {
        integer(16, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint16Little(PackParser.Uint16LittleContext ctx) {
        integer(16, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint16Big(PackParser.Uint16BigContext ctx) {
        integer(16, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint16Native(PackParser.Uint16NativeContext ctx) {
        integer(16, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitInt32Little(PackParser.Int32LittleContext ctx) {
        integer(32, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt32Big(PackParser.Int32BigContext ctx) {
        integer(32, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt32Native(PackParser.Int32NativeContext ctx) {
        integer(32, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint32Little(PackParser.Uint32LittleContext ctx) {
        integer(32, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint32Big(PackParser.Uint32BigContext ctx) {
        integer(32, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint32Native(PackParser.Uint32NativeContext ctx) {
        integer(32, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitInt64Little(PackParser.Int64LittleContext ctx) {
        integer(64, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt64Big(PackParser.Int64BigContext ctx) {
        integer(64, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitInt64Native(PackParser.Int64NativeContext ctx) {
        integer(64, ByteOrder.nativeOrder(), ctx.count());
    }

    @Override
    public void exitUint64Little(PackParser.Uint64LittleContext ctx) {
        integer(64, ByteOrder.LITTLE_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint64Big(PackParser.Uint64BigContext ctx) {
        integer(64, ByteOrder.BIG_ENDIAN, ctx.count());
    }

    @Override
    public void exitUint64Native(PackParser.Uint64NativeContext ctx) {
        integer(64, ByteOrder.nativeOrder(), ctx.count());
    }

    public void integer(int size, ByteOrder byteOrder, PackParser.CountContext count) {
        appendNode(applyCount(count, writeInteger(size, byteOrder)));
    }

    @Override
    public void exitUtf8Character(PackParser.Utf8CharacterContext ctx) {
        unify(FormatEncoding.UTF_8);

        appendNode(applyCount(ctx.count(), WriteUTF8CharacterNodeGen.create(context,
                ToLongNodeGen.create(context,
                        ReadValueNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitBerInteger(PackParser.BerIntegerContext ctx) {
        appendNode(applyCount(ctx.count(),
                WriteBERNodeGen.create(context, ReadLongOrBigIntegerNodeGen.create(context, new SourceNode()))));

    }

    @Override
    public void exitF64Native(PackParser.F64NativeContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(64, ByteOrder.nativeOrder(),
                        ReinterpretLongNodeGen.create(context,
                                ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitF32Native(PackParser.F32NativeContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(32, ByteOrder.nativeOrder(),
                        ReinterpretLongNodeGen.create(context,
                                AsSinglePrecisionNodeGen.create(context,
                                        ReadDoubleNodeGen.create(context, new SourceNode()))))));
    }

    @Override
    public void exitF64Little(PackParser.F64LittleContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(64, ByteOrder.LITTLE_ENDIAN,
                        ReinterpretLongNodeGen.create(context,
                                ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitF32Little(PackParser.F32LittleContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(32, ByteOrder.LITTLE_ENDIAN,
                        ReinterpretLongNodeGen.create(context,
                                AsSinglePrecisionNodeGen.create(context,
                                    ReadDoubleNodeGen.create(context, new SourceNode()))))));
    }

    @Override
    public void exitF64Big(PackParser.F64BigContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(64, ByteOrder.BIG_ENDIAN,
                        ReinterpretLongNodeGen.create(context,
                                ReadDoubleNodeGen.create(context, new SourceNode())))));
    }

    @Override
    public void exitF32Big(PackParser.F32BigContext ctx) {
        appendNode(applyCount(ctx.count(),
                writeInteger(32, ByteOrder.BIG_ENDIAN,
                        ReinterpretLongNodeGen.create(context,
                                AsSinglePrecisionNodeGen.create(context,
                                    ReadDoubleNodeGen.create(context, new SourceNode()))))));
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
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));
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
                        true, context.getCoreLibrary().getNilObject(), new SourceNode())));

    }

    @Override
    public void exitBase64String(PackParser.Base64StringContext ctx) {
        unify(FormatEncoding.US_ASCII);

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
                        false, context.getCoreLibrary().getNilObject(), new SourceNode())));
    }

    @Override
    public void exitPointer(PackParser.PointerContext ctx) {
        appendNode(writeInteger(64, ByteOrder.nativeOrder(),
                new PNode(context)));

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

        appendNode(new AtNode(context, position));
    }

    @Override
    public void exitBack(PackParser.BackContext ctx) {
        if (ctx.count() == null || ctx.count().INT() != null) {
            appendNode(applyCount(ctx.count(), new BackNode(context)));
        }
    }

    @Override
    public void exitNullByte(PackParser.NullByteContext ctx) {
        appendNode((applyCount(ctx.count(), new WriteByteNode(context, (byte) 0))));
    }

    @Override
    public void enterSubSequence(PackParser.SubSequenceContext ctx) {
        pushSequence();
    }

    @Override
    public void exitSubSequence(PackParser.SubSequenceContext ctx) {
        final List<FormatNode> sequence = sequenceStack.pop();
        final SequenceNode sequenceNode = new SequenceNode(context, sequence.toArray(new FormatNode[sequence.size()]));

        final FormatNode resultingNode;

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

    private FormatNode writeInteger(int size, ByteOrder byteOrder) {
        final FormatNode readNode = ToLongNodeGen.create(context,
                ReadValueNodeGen.create(context, new SourceNode()));
        return writeInteger(size, byteOrder, readNode);
    }

    private FormatNode writeInteger(int size, ByteOrder byteOrder, FormatNode readNode) {
        switch (size) {
            case 8:
                return Write8NodeGen.create(context, readNode);
            case 16:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    return Write16LittleNodeGen.create(context, readNode);
                } else {
                    return Write16BigNodeGen.create(context, readNode);
                }
            case 32:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    return Write32LittleNodeGen.create(context, readNode);
                } else {
                    return Write32BigNodeGen.create(context, readNode);
                }
            case 64:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    return Write64LittleNodeGen.create(context, readNode);
                } else {
                    return Write64BigNodeGen.create(context, readNode);
                }
            default:
                throw new UnsupportedOperationException();
        }
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

    private FormatNode applyCount(PackParser.CountContext count, FormatNode node) {
        if (count == null) {
            return node;
        } else if (count.INT() != null) {
            return new NNode(context, Integer.parseInt(count.INT().getText()), node);
        } else {
            return new StarNode(context, node);
        }
    }
    
    private void unify(FormatEncoding other) {
        encoding = encoding.unifyWith(other);
    }

}
