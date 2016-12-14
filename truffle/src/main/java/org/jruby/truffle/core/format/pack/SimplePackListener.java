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

public interface SimplePackListener {

    void integer(int size, boolean signed, ByteOrder byteOrder, int count);

    void floatingPoint(int size, ByteOrder byteOrder, int count);

    void utf8Character(int count);

    void berInteger(int count);

    void binaryStringSpacePadded(int count);

    void binaryStringNullPadded(int count);

    void binaryStringNullStar(int count);

    void bitStringMSBFirst(int count);

    void bitStringMSBLast(int count);

    void hexStringHighFirst(int count);

    void hexStringLowFirst(int count);

    void uuString(int count);

    void mimeString(int count);

    void base64String(int count);

    void pointer();

    void at(int position);

    void back(int count);

    void nullByte(int count);

    void startSubSequence();

    void finishSubSequence(int count);

    void error(String message);

}
