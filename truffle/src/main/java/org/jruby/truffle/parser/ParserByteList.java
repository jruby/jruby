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

public class ParserByteList {

    private byte[] bytes;
    private int start;
    private int length;
    private Encoding encoding;

    public ParserByteList(ByteList byteList) {
        fromByteList(byteList);
    }

    public ParserByteList(int capacity) {
        this(new ByteList(capacity));
    }

    public ParserByteList() {
        this(new ByteList());
    }

    public ParserByteList(byte[] bytes, int start, int length, Encoding encoding, boolean copy) {
        this(new ByteList(bytes, start, length, encoding, copy));
    }

    public ParserByteList(byte[] bytes, int start, int length, Encoding encoding) {
        this(new ByteList(bytes, start, length, encoding, false));
    }

    public ParserByteList(byte[] bytes) {
        this(new ByteList(bytes));
    }

    public ParserByteList(ParserByteList other) {
        this(other.toByteList());
    }

    public static ParserByteList create(String string) {
        return new ParserByteList(ByteList.create(string));
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

    public void append(ParserByteList other, int start, int length) {
        fromByteList(toByteList().append(other.toByteList(), start, length));
    }

    public void append(ParserByteList other) {
        fromByteList(toByteList().append(other.toByteList()));
    }

    public void ensure(int length) {
        bytes = Arrays.copyOf(bytes, Math.max(bytes.length, length));
    }

    public ParserByteList makeShared(int sharedStart, int sharedLength) {
        return new ParserByteList(bytes, start + sharedStart, sharedLength, encoding);
    }

    public byte[] getUnsafeBytes() {
        return bytes;
    }

    public int caseInsensitiveCmp(ParserByteList other) {
        return toByteList().caseInsensitiveCmp(other.toByteList());
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

    public boolean equal(ParserByteList other) {
        return toByteList().equals(other.toByteList());
    }

    public int charAt(int index) {
        return toByteList().charAt(index);
    }

    public String toString() {
        return toByteList().toString();
    }

}
