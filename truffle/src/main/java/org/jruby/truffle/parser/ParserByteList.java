package org.jruby.truffle.parser;

import org.jcodings.Encoding;
import org.jruby.truffle.core.string.ByteList;

public class ParserByteList {

    private final ByteList byteList;

    public ParserByteList(ByteList byteList) {
        this.byteList = byteList;
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
        this(new ByteList(other.byteList));
    }

    public static ParserByteList create(String string) {
        return new ParserByteList(ByteList.create(string));
    }

    public int getStart() {
        return byteList.getBegin();
    }

    public void setStart(int start) {
        byteList.setBegin(start);
    }

    public int getLength() {
        return byteList.length();
    }

    public void setLength(int length) {
        byteList.setRealSize(length);
    }

    public Encoding getEncoding() {
        return byteList.getEncoding();
    }

    public void setEncoding(Encoding encoding) {
        byteList.setEncoding(encoding);
    }

    public void append(int b) {
        byteList.append(b);
    }

    public void append(byte[] bytes) {
        byteList.append(bytes);
    }

    public void append(ParserByteList other, int start, int length) {
        byteList.append(other.byteList, start, length);
    }

    public ParserByteList makeShared(int start, int length) {
        return new ParserByteList(byteList.makeShared(start, length));
    }

    public byte[] getUnsafeBytes() {
        return byteList.getUnsafeBytes();
    }

    public int caseInsensitiveCmp(ParserByteList other) {
        return byteList.caseInsensitiveCmp(other.byteList);
    }

    public ByteList toByteList() {
        return byteList;
    }

    public boolean equal(ParserByteList other) {
        return byteList.equals(other.byteList);
    }

    public void ensure(int length) {
        byteList.ensure(length);
    }

    public int charAt(int index) {
        return byteList.charAt(index);
    }

    public void append(ParserByteList other) {
        byteList.append(other.byteList);
    }

    public String toString() {
        return byteList.toString();
    }

}
