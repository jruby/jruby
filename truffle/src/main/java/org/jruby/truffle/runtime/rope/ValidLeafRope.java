/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.runtime.rope;

import org.jcodings.Encoding;
import org.jruby.util.StringSupport;

public class ValidLeafRope extends LeafRope {

    private final int characterLength;

    public ValidLeafRope(byte[] bytes, Encoding encoding, int characterLength) {
        super(bytes, encoding);
        this.characterLength = characterLength;
    }

    @Override
    public int getCodeRange() {
        return StringSupport.CR_VALID;
    }

    @Override
    public boolean isSingleByteOptimizable() {
        return getEncoding().isSingleByte();
    }

    @Override
    public int characterLength() {
        return characterLength;
    }

}
