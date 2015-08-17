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
import org.jruby.truffle.runtime.layouts.Layouts;
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
        return Layouts.SYMBOL.getString(symbol);
    }

    @Override
    public int getCodeRange() {
        return Layouts.SYMBOL.getCodeRange(symbol);
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public int scanForCodeRange() {
        final ByteList byteList = Layouts.SYMBOL.getByteList(symbol);

        int cr = Layouts.SYMBOL.getCodeRange(symbol);

        if (cr == StringSupport.CR_UNKNOWN) {
            cr = StringSupport.codeRangeScan(byteList.getEncoding(), byteList);
            Layouts.SYMBOL.setCodeRange(symbol, cr);
        }

        return cr;
    }

    @Override
    public boolean isCodeRangeValid() {
        return Layouts.SYMBOL.getCodeRange(symbol) == StringSupport.CR_VALID;
    }

    @Override
    public void setCodeRange(int codeRange) {
        Layouts.SYMBOL.setCodeRange(symbol, codeRange);
    }

    @Override
    public void clearCodeRange() {
        Layouts.SYMBOL.setCodeRange(symbol, StringSupport.CR_UNKNOWN);
    }

    @Override
    public void keepCodeRange() {
        if (Layouts.SYMBOL.getCodeRange(symbol) == StringSupport.CR_BROKEN) {
            Layouts.SYMBOL.setCodeRange(symbol, StringSupport.CR_UNKNOWN);
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
        return Layouts.SYMBOL.getByteList(symbol).getEncoding();
    }

    @Override
    public ByteList getByteList() {
        return Layouts.SYMBOL.getByteList(symbol);
    }

}
