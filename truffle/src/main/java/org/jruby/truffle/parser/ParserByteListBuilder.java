/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import org.jcodings.Encoding;
import org.jruby.truffle.core.string.ByteList;

import java.util.Arrays;

public class ParserByteListBuilder {

    private byte[] bytes;
    private int start;
    private int length;
    private Encoding encoding;

    public ParserByteListBuilder(byte[] bytes, Encoding encoding) {
        this.bytes = bytes;
        start = 0;
        length = bytes.length;
        this.encoding = encoding;
    }

    public ParserByteListBuilder(ByteList byteList) {
        fromByteList(byteList);
    }

    public ParserByteListBuilder(int capacity) {
        this(new ByteList(capacity));
    }

    public ParserByteListBuilder() {
        this(new ByteList());
    }

    public ParserByteListBuilder(ParserByteListBuilder other) {
        this(other.toByteList());
    }

    public static ParserByteListBuilder create(String string) {
        return new ParserByteListBuilder(ByteList.create(string));
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public void append(int b) {
        fromByteList(toByteList().append(b));
    }

    public void append(byte[] bytes) {
        fromByteList(toByteList().append(bytes));
    }

    public void append(ParserByteListBuilder other, int start, int length) {
        fromByteList(toByteList().append(other.toByteList(), start, length));
    }

    public void append(ParserByteListBuilder other) {
        fromByteList(toByteList().append(other.toByteList()));
    }

    public void ensure(int length) {
        bytes = Arrays.copyOf(bytes, Math.max(bytes.length, length));
    }

    public byte[] getUnsafeBytes() {
        return bytes;
    }

    public ByteList toByteList() {
        return new ByteList(bytes, start, length, encoding, true);
    }

    private void fromByteList(ByteList byteList) {
        bytes = byteList.bytes();
        start = 0;
        length = byteList.length();
        encoding = byteList.getEncoding();
    }

    public boolean equal(ParserByteListBuilder other) {
        return toByteList().equals(other.toByteList());
    }

    public int charAt(int index) {
        return toByteList().charAt(index);
    }

    public String toString() {
        return toByteList().toString();
    }

    public ParserByteList toParserByteList() {
        return new ParserByteList(Arrays.copyOfRange(bytes, start, length), 0, length, encoding);
    }
}
