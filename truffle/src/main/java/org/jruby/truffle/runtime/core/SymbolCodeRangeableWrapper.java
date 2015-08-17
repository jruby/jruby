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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

public class SymbolCodeRangeableWrapper implements CodeRangeable {

    private final DynamicObject symbol;

    public SymbolCodeRangeableWrapper(DynamicObject symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return SymbolNodes.SYMBOL_LAYOUT.getString(symbol);
    }

    @Override
    public int getCodeRange() {
        return SymbolNodes.SYMBOL_LAYOUT.getCodeRange(symbol);
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public int scanForCodeRange() {
        final ByteList byteList = SymbolNodes.SYMBOL_LAYOUT.getByteList(symbol);

        int cr = SymbolNodes.SYMBOL_LAYOUT.getCodeRange(symbol);

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = StringSupport.codeRangeScan(byteList.getEncoding(), byteList);
            SymbolNodes.SYMBOL_LAYOUT.setCodeRange(symbol, cr);
        }

        return cr;
    }

    @Override
    public boolean isCodeRangeValid() {
        return SymbolNodes.SYMBOL_LAYOUT.getCodeRange(symbol) == StringSupport.CR_VALID;
    }

    @Override
    public void setCodeRange(int codeRange) {
        SymbolNodes.SYMBOL_LAYOUT.setCodeRange(symbol, codeRange);
    }

    @Override
    public void clearCodeRange() {
        SymbolNodes.SYMBOL_LAYOUT.setCodeRange(symbol, StringSupport.CR_UNKNOWN);
    }

    @Override
    public void keepCodeRange() {
        if (SymbolNodes.SYMBOL_LAYOUT.getCodeRange(symbol) == StringSupport.CR_BROKEN) {
            SymbolNodes.SYMBOL_LAYOUT.setCodeRange(symbol, StringSupport.CR_UNKNOWN);
        }
    }

    @Override
    public void modify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modify(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAndKeepCodeRange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Encoding checkEncoding(CodeRangeable other) {
        // TODO (nirvdrum Jan. 13, 2015): This should check if the encodings are compatible rather than just always succeeding.
        return SymbolNodes.SYMBOL_LAYOUT.getByteList(symbol).getEncoding();
    }

    @Override
    public ByteList getByteList() {
        return SymbolNodes.SYMBOL_LAYOUT.getByteList(symbol);
    }

}
