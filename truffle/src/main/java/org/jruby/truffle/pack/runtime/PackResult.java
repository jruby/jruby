/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.runtime;

public class PackResult {

    private final byte[] output;
    private final int outputLength;
    private final boolean tainted;
    private final PackEncoding encoding;

    public PackResult(byte[] output, int outputLength, boolean tainted, PackEncoding encoding) {
        this.output = output;
        this.outputLength = outputLength;
        this.tainted = tainted;
        this.encoding = encoding;
    }

    public byte[] getOutput() {
        return output;
    }

    public int getOutputLength() {
        return outputLength;
    }

    public boolean isTainted() {
        return tainted;
    }

    public PackEncoding getEncoding() {
        return encoding;
    }
}
