/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.symbol;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;

public class SymbolEquality {
    private DynamicObject symbol;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final DynamicObject otherSymbol = ((SymbolEquality) other).symbol;
        return Layouts.SYMBOL.getRope(symbol).equals(Layouts.SYMBOL.getRope(otherSymbol));
    }

    @Override
    public int hashCode() {
        return Layouts.SYMBOL.getRope(symbol).hashCode();
    }

    void setSymbol(DynamicObject symbol) {
        assert RubyGuards.isRubySymbol(symbol);
        this.symbol = symbol;
    }
}
