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
import org.jruby.truffle.nodes.core.SymbolNodes;
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
        return SymbolNodes.getCodeRange(symbol);
    }

    @Override
    public int scanForCodeRange() {
        return SymbolNodes.scanForCodeRange(symbol);
    }

    @Override
    public boolean isCodeRangeValid() {
        return SymbolNodes.isCodeRangeValid(symbol);
    }

    @Override
    public void setCodeRange(int codeRange) {
        SymbolNodes.setCodeRange(symbol, codeRange);
    }

    @Override
    public void clearCodeRange() {
        SymbolNodes.clearCodeRange(symbol);
    }

    @Override
    public void keepCodeRange() {
        SymbolNodes.keepCodeRange(symbol);
    }

    @Override
    public void modify() {
        SymbolNodes.modify();
    }

    @Override
    public void modify(int length) {
        SymbolNodes.modify(length);
    }

    @Override
    public void modifyAndKeepCodeRange() {
        SymbolNodes.modifyAndKeepCodeRange(symbol);
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        return SymbolNodes.checkEncoding(symbol, other);
    }

    @Override
    public ByteList getByteList() {
        return SymbolNodes.getByteList(symbol);
    }

}
