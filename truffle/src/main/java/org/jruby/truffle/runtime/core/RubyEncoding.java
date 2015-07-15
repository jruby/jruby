/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jcodings.Encoding;
import org.jruby.util.ByteList;

/**
 * This is a bridge between JRuby encoding and Truffle encoding
 */
public class RubyEncoding extends RubyBasicObject {

    public final Encoding encoding;
    public final ByteList name;
    public final boolean dummy;

    public RubyEncoding(RubyClass encodingClass, Encoding encoding, ByteList name, boolean dummy) {
        super(encodingClass);
        this.encoding = encoding;
        this.name = name;
        this.dummy = dummy;
    }

}
