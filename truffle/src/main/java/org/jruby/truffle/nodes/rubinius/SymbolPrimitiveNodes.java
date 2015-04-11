/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * Rubinius primitives associated with the Ruby {@code Symbol} class.
 */
public abstract class SymbolPrimitiveNodes {

    @RubiniusPrimitive(name = "symbol_is_constant")
    public static abstract class SymbolIsConstantPrimitiveNode extends RubiniusPrimitiveNode {

        public SymbolIsConstantPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean symbolIsConstant(RubySymbol symbol) {
            notDesignedForCompilation();
            final String string = symbol.toString();
            return string.length() > 0 && Character.isUpperCase(string.charAt(0));
        }

    }

}
