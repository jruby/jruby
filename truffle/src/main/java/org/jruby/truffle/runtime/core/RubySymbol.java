/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

public class RubySymbol extends RubyBasicObject {

    public final String symbol;
    public final ByteList bytes;
    public final int hashCode;
    public int codeRange = StringSupport.CR_UNKNOWN;
    public SymbolCodeRangeableWrapper codeRangeableWrapper;

    public RubySymbol(RubyClass symbolClass, String symbol, ByteList bytes, int hashCode) {
        super(symbolClass);
        this.symbol = symbol;
        this.bytes = bytes;
        this.hashCode = hashCode;
    }

}
