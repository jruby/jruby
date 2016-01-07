/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
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

public class RopeOperations {

    public static LeafRope create(byte[] bytes, Encoding encoding, int codeRange) {
        switch(codeRange) {
            case StringSupport.CR_7BIT: return new LeafRope(bytes, encoding);
            case StringSupport.CR_VALID: return new LeafRope(bytes, encoding);
            case StringSupport.CR_UNKNOWN: return new LeafRope(bytes, encoding);
            case StringSupport.CR_BROKEN: return new LeafRope(bytes, encoding);
            default: throw new RuntimeException(String.format("Unknown code range type: %d", codeRange));
        }
    }

}
