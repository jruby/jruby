/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import org.jruby.truffle.core.rope.CodeRange;

public class BytesResult {

    private final byte[] output;
    private final int outputLength;
    private final int stringLength;
    private final CodeRange stringCodeRange;
    private final boolean tainted;
    private final FormatEncoding encoding;

    public BytesResult(byte[] output, int outputLength, int stringLength, CodeRange stringCodeRange, boolean tainted, FormatEncoding encoding) {
        this.output = output;
        this.outputLength = outputLength;
        this.stringLength = stringLength;
        this.stringCodeRange = stringCodeRange;
        this.tainted = tainted;
        this.encoding = encoding;
    }

    public byte[] getOutput() {
        return output;
    }

    public int getOutputLength() {
        return outputLength;
    }

    public int getStringLength() {
        return stringLength;
    }

    public CodeRange getStringCodeRange() {
        return stringCodeRange;
    }

    public boolean isTainted() {
        return tainted;
    }

    public FormatEncoding getEncoding() {
        return encoding;
    }
}
