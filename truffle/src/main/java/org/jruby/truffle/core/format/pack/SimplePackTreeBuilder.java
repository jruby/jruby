/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
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

public class SimplePackTreeBuilder implements SimplePackListener {

    private final RubyContext context;
    private final Node currentNode;

    private final SharedTreeBuilder sharedTreeBuilder;

    private FormatEncoding encoding = FormatEncoding.DEFAULT;
    private final Deque<List<FormatNode>> sequenceStack = new ArrayDeque<>();

    public SimplePackTreeBuilder(RubyContext context, Node currentNode) {
        this.context = context;
        this.currentNode = currentNode;
        sharedTreeBuilder = new SharedTreeBuilder(context);
        pushSequence();
    }

    public void enterSequence() {
        pushSequence();
    }

    public void exitSequence() {
        final List<FormatNode> sequence = sequenceStack.pop();
        appendNode(new SequenceNode(sequence.toArray(new FormatNode[sequence.size()])));
    }

    @Override
    public void integer(int size, boolean signed, ByteOrder byteOrder, int count) {
        appendIntegerNode(size, byteOrder, count);
    }

    @Override
    public void floatingPoint(int size, ByteOrder byteOrder, int count) {
        appendFloatNode(size, byteOrder, count);
    }

    @Override
    public void utf8Character(int count) {
        unify(FormatEncoding.UTF_8);

        appendNode(sharedTreeBuilder.applyCount(count,
                WriteUTF8CharacterNodeGen.create(
                        ToLongNodeGen.create(false,
                                ReadValueNodeGen.create(new SourceNode())))));
    }

    @Override
    public void berInteger(int count) {
        appendNode(sharedTreeBuilder.applyCount(count,
                WriteBERNodeGen.create(
                        ReadLongOrBigIntegerNodeGen.create(new SourceNode()))));
    }

    @Override
    public void binaryStringSpacePadded(int count) {
        binaryString((byte) ' ', true, false, count);
    }

    @Override
    public void binaryStringNullPadded(int count) {
        binaryString((byte) 0, true, false, count);
    }

    @Override
    public void binaryStringNullStar(int count) {
        binaryString((byte) 0, true, count == SimplePackParser.COUNT_STAR, count);
    }

    @Override
    public void bitStringMSBFirst(int count) {
        bitString(ByteOrder.BIG_ENDIAN, count);
    }

    @Override
    public void bitStringMSBLast(int count) {
        bitString(ByteOrder.LITTLE_ENDIAN, count);
    }

    @Override
    public void hexStringHighFirst(int count) {
        hexString(ByteOrder.BIG_ENDIAN, count);
    }

    @Override
    public void hexStringLowFirst(int count) {
        hexString(ByteOrder.LITTLE_ENDIAN, count);
    }

    @Override
    public void uuString(int count) {
        unify(FormatEncoding.US_ASCII);

        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(count);

        appendNode(WriteUUStringNodeGen.create(starLength.getLength(), starLength.isStar(),
                ReadStringNodeGen.create(false, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    @Override
    public void mimeString(int count) {
        unify(FormatEncoding.US_ASCII);

        int length;

        if (count == SimplePackParser.COUNT_STAR) {
            length = 72;
        } else {
            length = count;

            if (length <= 1) {
                length = 72;
            }
        }

        appendNode(WriteMIMEStringNodeGen.create(length,
                ReadStringNodeGen.create(true, "to_s",
                        true, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    @Override
    public void base64String(int count) {
        unify(FormatEncoding.US_ASCII);

        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(count);

        appendNode(WriteBase64StringNodeGen.create(starLength.getLength(), starLength.isStar(),
                ReadStringNodeGen.create(false, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    @Override
    public void pointer() {
        /*
         * P and p print the address of a string. Obviously that doesn't work
         * well in Java. We'll print 0x0000000000000000 with the hope that at
         * least it should page fault if anyone tries to read it.
         */

        appendNode(new SequenceNode(new FormatNode[]{
                new AdvanceSourcePositionNode(false),
                writeInteger(64, ByteOrder.nativeOrder(),
                        new LiteralFormatNode((long) 0))
        }));
    }

    @Override
    public void at(int position) {
        if (position == SimplePackParser.COUNT_NONE) {
            position = 1;
        } else if (position == SimplePackParser.COUNT_STAR) {
            throw new UnsupportedOperationException();
        }

        appendNode(new SetOutputPositionNode(position));
    }

    @Override
    public void back(int count) {
        if (count == SimplePackParser.COUNT_NONE || count >= 0) {
            appendNode(sharedTreeBuilder.applyCount(count, new ReverseOutputPositionNode()));
        }
    }

    @Override
    public void nullByte(int count) {
        appendNode((sharedTreeBuilder.applyCount(count,
                WriteByteNodeGen.create(
                        new LiteralFormatNode((byte) 0)))));
    }

    @Override
    public void startSubSequence() {
        pushSequence();
    }

    @Override
    public void finishSubSequence(int count) {
        appendNode(sharedTreeBuilder.finishSubSequence(sequenceStack, count));
    }

    @Override
    public void error(String message) {
        // TODO CS 29-Oct-16 make this a node so that side effects from previous directives happen
        throw new RaiseException(context.getCoreExceptions().argumentError(message, currentNode));
    }

    public FormatNode getNode() {
        return sequenceStack.peek().get(0);
    }

    public FormatEncoding getEncoding() {
        return encoding;
    }

    private void pushSequence() {
        sequenceStack.push(new ArrayList<>());
    }

    private void appendNode(FormatNode node) {
        sequenceStack.peek().add(node);
    }

    private void appendIntegerNode(int size, ByteOrder byteOrder, int count) {
        appendNode(sharedTreeBuilder.applyCount(count, writeInteger(size, byteOrder)));
    }

    private void appendFloatNode(int size, ByteOrder byteOrder, int count) {
        final FormatNode readNode = ReadDoubleNodeGen.create(new SourceNode());

        final FormatNode typeNode;

        switch (size) {
            case 32:
                typeNode = ToFloatNodeGen.create(readNode);
                break;
            case 64:
                typeNode = readNode;
                break;
            default:
                throw new IllegalArgumentException();
        }

        appendNode(sharedTreeBuilder.applyCount(count,
                writeInteger(size, byteOrder,
                        ReinterpretAsLongNodeGen.create(
                                typeNode))));
    }

    private FormatNode writeInteger(int size, ByteOrder byteOrder) {
        final FormatNode readNode = ToLongNodeGen.create(false,
                ReadValueNodeGen.create(new SourceNode()));

        return writeInteger(size, byteOrder, readNode);
    }

    private FormatNode writeInteger(int size, ByteOrder byteOrder, FormatNode readNode) {
        final FormatNode convertNode;

        switch (size) {
            case 8:
                return WriteByteNodeGen.create(readNode);
            case 16:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer16LittleToBytesNodeGen.create(readNode);
                } else {
                    convertNode = Integer16BigToBytesNodeGen.create(readNode);
                }
                break;
            case 32:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer32LittleToBytesNodeGen.create(readNode);
                } else {
                    convertNode = Integer32BigToBytesNodeGen.create(readNode);
                }
                break;
            case 64:
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    convertNode = Integer64LittleToBytesNodeGen.create(readNode);
                } else {
                    convertNode = Integer64BigToBytesNodeGen.create(readNode);
                }
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(size));
        }

        return WriteBytesNodeGen.create(convertNode);
    }

    private void binaryString(byte padding, boolean padOnNull, boolean appendNull, int count) {
        unify(FormatEncoding.ASCII_8BIT);

        final boolean pad;
        final int width;

        if (count >= 0) {
            pad = true;
            width = count;
        } else {
            pad = false;

            if (count == SimplePackParser.COUNT_STAR) {
                padOnNull = false;
            }

            width = 1;
        }

        final boolean takeAll;

        if (count == SimplePackParser.COUNT_STAR) {
            takeAll = true;
        } else {
            takeAll = false;
        }

        appendNode(WriteBinaryStringNodeGen.create(pad, padOnNull,
                width, padding, takeAll, appendNull,
                ReadStringNodeGen.create(true, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));

    }

    private void bitString(ByteOrder byteOrder, int count) {
        final SharedTreeBuilder.StarLength starLength = sharedTreeBuilder.parseCountContext(count);

        appendNode(WriteBitStringNodeGen.create(byteOrder, starLength.isStar(), starLength.getLength(),
                ReadStringNodeGen.create(true, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));
    }

    private void hexString(ByteOrder byteOrder, int count) {
        final int length;

        if (count == SimplePackParser.COUNT_NONE) {
            length = 1;
        } else if (count == SimplePackParser.COUNT_STAR) {
            length = -1;
        } else {
            length = count;
        }

        appendNode(WriteHexStringNodeGen.create(byteOrder, length,
                ReadStringNodeGen.create(true, "to_str",
                        false, context.getCoreLibrary().getNilObject(),
                        new SourceNode())));

    }

    private void unify(FormatEncoding other) {
        encoding = encoding.unifyWith(other);
    }

}
