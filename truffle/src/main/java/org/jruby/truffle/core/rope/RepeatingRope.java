/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives;
import org.jcodings.Encoding;
import org.jruby.truffle.core.CoreLibrary;

public class RepeatingRope extends Rope {

    private final Rope child;
    private final long times;

    public RepeatingRope(Rope child, long times) {
        super(child.getEncoding(), child.getCodeRange(), child.isSingleByteOptimizable(), child.byteLength() * times, child.characterLength() * times, child.depth() + 1, null);
        this.child = child;
        this.times = times;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        return new RepeatingRope(child.withEncoding(newEncoding, newCodeRange), times);
    }

    @Override
    protected byte getByteSlow(long index) {
        return child.getByteSlow(index % child.byteLength());
    }

    public Rope getChild() {
        return child;
    }

    public long getTimes() {
        return times;
    }

    @Override
    public String toString() {
        if (!CoreLibrary.fitsIntoInteger(byteLength())) {
            CompilerDirectives.transferToInterpreter();
            throw new RopeTooLongException("Can't convert larger than int range to a Java String");
        }

        final String childString = child.toString();
        final StringBuilder builder = new StringBuilder((int) (childString.length() * times));

        for (int i = 0; i < times; i++) {
            builder.append(childString);
        }

        return builder.toString();
    }
}
