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

import org.jcodings.Encoding;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;

public class SymbolCodeRangeableWrapper implements CodeRangeable {

    private final RubySymbol symbol;

    public SymbolCodeRangeableWrapper(RubySymbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol.toString();
    }

    @Override
    public int getCodeRange() {
        return symbol.getCodeRange();
    }

    @Override
    public int scanForCodeRange() {
        return symbol.scanForCodeRange();
    }

    @Override
    public boolean isCodeRangeValid() {
        return symbol.isCodeRangeValid();
    }

    @Override
    public void setCodeRange(int codeRange) {
        symbol.setCodeRange(codeRange);
    }

    @Override
    public void clearCodeRange() {
        symbol.clearCodeRange();
    }

    @Override
    public void keepCodeRange() {
        symbol.keepCodeRange();
    }

    @Override
    public void modify() {
        symbol.modify();
    }

    @Override
    public void modify(int length) {
        symbol.modify(length);
    }

    @Override
    public void modifyAndKeepCodeRange() {
        symbol.modifyAndKeepCodeRange();
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        return symbol.checkEncoding(other);
    }

    @Override
    public ByteList getByteList() {
        return symbol.getByteList();
    }

}
