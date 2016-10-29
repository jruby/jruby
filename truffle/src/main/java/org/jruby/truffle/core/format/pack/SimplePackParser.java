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

import java.nio.ByteOrder;

public class SimplePackParser {

    public static final int COUNT_NONE = -1;
    public static final int COUNT_STAR = -2;
    public static final int SIZE_NATIVE = -1;

    private final SimplePackListener listener;
    private final byte[] bytes;
    private int n;

    public SimplePackParser(SimplePackListener listener, byte[] bytes) {
        this.listener = listener;
        this.bytes = bytes;
    }

    public void parse() {
        while (n < bytes.length) {
            final byte b = bytes[n];

            switch (b) {
                case 'c':
                case 'C':
                case 's':
                case 'S':
                case 'v':
                case 'n':
                case 'i':
                case 'j':
                case 'J':
                case 'l':
                case 'I':
                case 'L':
                case 'V':
                case 'N':
                case 'q':
                case 'Q': {
                    n++;

                    int size;

                    switch (b) {
                        case 'c':
                        case 'C':
                            size = 8;
                            break;

                        case 's':
                        case 'S':
                        case 'v':
                        case 'n':
                            size = 16;
                            break;

                        case 'i':
                        case 'I':
                        case 'V':
                        case 'N':
                            size = 32;
                            break;

                        case 'q':
                        case 'Q':
                        case 'j':
                        case 'J':
                            size = 64;
                            break;

                        case 'l':
                        case 'L':
                            size = SIZE_NATIVE;
                            break;

                        default:
                            throw new UnsupportedOperationException(Character.toString((char) b));
                    }

                    final boolean signed;

                    switch (b) {
                        case 'c':
                        case 's':
                        case 'i':
                        case 'j':
                        case 'l':
                        case 'q':
                            signed = true;
                            break;

                        case 'C':
                        case 'S':
                        case 'I':
                        case 'J':
                        case 'v':
                        case 'n':
                        case 'V':
                        case 'N':
                        case 'L':
                        case 'Q':
                            signed = false;
                            break;

                        default:
                            throw new UnsupportedOperationException(Character.toString((char) b));
                    }

                    ByteOrder byteOrder = null;

                    switch (b) {
                        case 'v':
                        case 'V':
                            byteOrder = ByteOrder.LITTLE_ENDIAN;
                            break;

                        case 'n':
                        case 'N':
                            byteOrder = ByteOrder.BIG_ENDIAN;
                            break;

                        case 'j':
                        case 'J':
                            byteOrder = ByteOrder.nativeOrder();
                            break;
                    }

                    modifierLoop:
                    while (n < bytes.length) {
                        final byte m = bytes[n];

                        switch (m) {
                            case '<':
                            case '>':
                                n++;
                                switch (m) {
                                    case '<':
                                        byteOrder = ByteOrder.LITTLE_ENDIAN;
                                        break;
                                    case '>':
                                        byteOrder = ByteOrder.BIG_ENDIAN;
                                        break;
                                    default:
                                        throw new UnsupportedOperationException(Character.toString((char) m));
                                }
                                break;

                            case '!':
                            case '_':
                                n++;

                                switch (b) {
                                    case 's':
                                    case 'S':
                                    case 'i':
                                    case 'I':
                                    case 'l':
                                    case 'L':
                                    case 'q':
                                    case 'Q':
                                    case 'j':
                                    case 'J':
                                        if (byteOrder == null) {
                                            byteOrder = ByteOrder.nativeOrder();
                                        }
                                        break;

                                    default:
                                        listener.error(String.format("'%c' allowed only after types sSiIlLqQjJ", (char) m));
                                }

                                if (size == SIZE_NATIVE) {
                                    size = 64;
                                }

                                break;

                            case ' ':
                            case '\t':
                            case '\n':
                            case '\u000b':
                            case '\f':
                            case '\r':
                            case '\u0000':
                                n++;
                                break;

                            default:
                                break modifierLoop;
                        }
                    }

                    if (size == SIZE_NATIVE) {
                        size = 32;
                    }

                    if (byteOrder == null) {
                        byteOrder = ByteOrder.nativeOrder();
                    }

                    final int count = count();

                    listener.integer(size, signed, byteOrder, count);
                } break;

                case 'U':
                    n++;
                    disallowNative(b);
                    listener.utf8Character(count());
                    break;

                case 'w':
                    n++;
                    disallowNative(b);
                    listener.berInteger(count());
                    break;

                case 'D':
                case 'd':
                case 'F':
                case 'f':
                case 'E':
                case 'e':
                case 'G':
                case 'g': {
                    n++;

                    final int size;

                    switch (b) {
                        case 'f':
                        case 'F':
                        case 'e':
                        case 'g':
                            size = 32;
                            break;

                        case 'd':
                        case 'D':
                        case 'E':
                        case 'G':
                            size = 64;
                            break;

                        default:
                            throw new UnsupportedOperationException(Character.toString((char) b));
                    }

                    ByteOrder byteOrder;

                    switch (b) {
                        case 'd':
                        case 'D':
                        case 'f':
                        case 'F':
                            byteOrder = ByteOrder.nativeOrder();
                            break;

                        case 'e':
                        case 'E':
                            byteOrder = ByteOrder.LITTLE_ENDIAN;
                            break;

                        case 'g':
                        case 'G':
                            byteOrder = ByteOrder.BIG_ENDIAN;
                            break;

                        default:
                            throw new UnsupportedOperationException(Character.toString((char) b));
                    }

                    disallowNative(b);

                    final int count = count();

                    listener.floatingPoint(size, byteOrder, count);
                } break;

                case 'A':
                    n++;
                    disallowNative(b);
                    listener.binaryStringSpacePadded(count());
                    break;

                case 'a':
                    n++;
                    disallowNative(b);
                    listener.binaryStringNullPadded(count());
                    break;

                case 'Z':
                    n++;
                    disallowNative(b);
                    listener.binaryStringNullStar(count());
                    break;

                case 'b':
                    n++;
                    disallowNative(b);
                    listener.bitStringMSBLast(count());
                    break;

                case 'B':
                    n++;
                    disallowNative(b);
                    listener.bitStringMSBFirst(count());
                    break;

                case 'H':
                    n++;
                    disallowNative(b);
                    listener.hexStringHighFirst(count());
                    break;

                case 'h':
                    n++;
                    disallowNative(b);
                    listener.hexStringLowFirst(count());
                    break;

                case 'u':
                    n++;
                    disallowNative(b);
                    listener.uuString(count());
                    break;

                case 'M':
                    n++;
                    disallowNative(b);
                    listener.mimeString(count());
                    break;

                case 'm':
                    n++;
                    disallowNative(b);
                    listener.base64String(count());
                    break;

                case 'p':
                case 'P':
                    n++;
                    disallowNative(b);
                    listener.pointer();
                    break;

                case '@':
                    n++;
                    disallowNative(b);
                    listener.at(count());
                    break;

                case 'X':
                    n++;
                    disallowNative(b);
                    listener.back(count());
                    break;

                case 'x':
                    n++;
                    disallowNative(b);
                    listener.nullByte(count());
                    break;

                case '%':
                    n++;
                    listener.error("% is not supported");
                    break;

                case '(':
                    n++;
                    listener.startSubSequence();
                    break;

                case ')':
                    n++;
                    listener.finishSubSequence(count());
                    break;

                case ' ':
                case '\t':
                case '\n':
                case '\u000b':
                case '\f':
                case '\r':
                case '\u0000':
                    n++;
                    break;

                case '#':
                    n++;

                    commentLoop: while (n < bytes.length) {
                        switch (bytes[n]) {
                            case '\r':
                            case '\n':
                                n++;
                                break commentLoop;

                            default:
                                n++;
                                break;
                        }
                    }
                    break;

                default:
                    throw new UnsupportedOperationException(Character.toString((char) b));
            }
        }
    }

    private int count() {
        int count = COUNT_NONE;

        countLoop: while (n < bytes.length) {
            final byte b = bytes[n];

            switch (b) {
                case '*':
                    n++;
                    return COUNT_STAR;

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (count == COUNT_NONE) {
                        count = 0;
                    }

                    count *= 10;
                    count += bytes[n] - '0';
                    n++;
                    break;

                default:
                    break countLoop;
            }
        }

        return count;
    }

    private void disallowNative(byte b) {
        if (n < bytes.length) {
            final byte m = bytes[n];

            if (m == '!' || m == '_') {
                n++;
                listener.error(String.format("'%c' allowed only after types sSiIlLqQjJ", (char) b));
            }
        }
    }

}
