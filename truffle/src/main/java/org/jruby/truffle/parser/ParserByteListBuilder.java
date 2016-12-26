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
import org.jcodings.specific.ASCIIEncoding;

import java.util.Arrays;

public class ParserByteListBuilder {

    private byte[] bytes;
    private int length;
    private Encoding encoding;

    public ParserByteListBuilder() {
        bytes = new byte[16];
        length = 0;
        encoding = ASCIIEncoding.INSTANCE;
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
        append((byte) b);
    }

    public void append(byte b) {
        grow(1);
        bytes[length] = b;
        length++;
    }

    public void append(byte[] bytes) {
        append(bytes, 0, bytes.length);
    }

    public void append(ParserByteList other) {
        append(other.getUnsafeBytes(), other.getStart(), other.getLength());
    }

    public void append(byte[] appendBytes, int appendStart, int appendLength) {
        grow(appendLength);
        System.arraycopy(appendBytes, appendStart, bytes, length, appendLength);
        length += appendLength;
    }

    public void grow(int extra) {
        if (length + extra > bytes.length) {
            bytes = Arrays.copyOf(bytes, (length + extra) * 2);
        }
    }

    public byte[] getUnsafeBytes() {
        return bytes;
    }

    public ParserByteList toParserByteList() {
        return new ParserByteList(Arrays.copyOf(bytes, length), 0, length, encoding);
    }

}
