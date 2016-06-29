/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.unpack;

public class ArrayResult {

    private final Object output;
    private final int outputLength;
    private final boolean tainted;

    public ArrayResult(Object output, int outputLength, boolean tainted) {
        this.output = output;
        this.outputLength = outputLength;
        this.tainted = tainted;
    }

    public Object getOutput() {
        return output;
    }

    public int getOutputLength() {
        return outputLength;
    }

    public boolean isTainted() {
        return tainted;
    }

}
