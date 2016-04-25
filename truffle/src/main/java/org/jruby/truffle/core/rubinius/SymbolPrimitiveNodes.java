/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * Rubinius primitives associated with the Ruby {@code Symbol} class.
 */
public abstract class SymbolPrimitiveNodes {

    @RubiniusPrimitive(name = "symbol_is_constant")
    public static abstract class SymbolIsConstantPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(symbol)")
        public boolean symbolIsConstant(DynamicObject symbol) {
            final String string = symbol.toString();
            return string.length() > 0 && Character.isUpperCase(string.charAt(0));
        }

    }

}
