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

    public byte[] unsafeBytes() {
        return byteList.unsafeBytes();
    }

    public int begin() {
        return byteList.begin();
    }

    public int getBegin() {
        return byteList.getBegin();
    }

    public void setBegin(int start) {
        byteList.setBegin(start);
    }

    public int realSize() {
        return byteList.realSize();
    }

    public int length() {
        return byteList.length();
    }

    public void setRealSize(int length) {
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

    public int get(int index) {
        return byteList.get(index);
    }

    public CharSequence subSequence(int start, int length) {
        return byteList.subSequence(start, length);
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

    public int getRealSize() {
        return byteList.getRealSize();
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
