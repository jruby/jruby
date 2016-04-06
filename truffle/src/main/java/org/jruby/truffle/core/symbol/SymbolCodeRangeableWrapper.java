/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.symbol;

import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;

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
        return Layouts.SYMBOL.getRope(symbol).getCodeRange().toInt();
    }

    @Override
    public int scanForCodeRange() {
        return Layouts.SYMBOL.getRope(symbol).getCodeRange().toInt();
    }

    @Override
    public boolean isCodeRangeValid() {
        return Layouts.SYMBOL.getRope(symbol).getCodeRange() == CodeRange.CR_VALID;
    }

    @Override
    public void setCodeRange(int codeRange) {
        throw new UnsupportedOperationException("Can't set code range on a Symbol");
    }

    @Override
    public void clearCodeRange() {
        throw new UnsupportedOperationException("Can't clear code range on a Symbol");
    }

    @Override
    public void keepCodeRange() {
        throw new UnsupportedOperationException("Can't keep code range on a Symbol");
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
        return Layouts.SYMBOL.getRope(symbol).getEncoding();
    }

    @Override
    public ByteList getByteList() {
        return RopeOperations.getByteListReadOnly(Layouts.SYMBOL.getRope(symbol));
    }

}
