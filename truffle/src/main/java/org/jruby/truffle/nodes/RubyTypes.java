/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.internal.DSLOptions;
import com.oracle.truffle.api.interop.TruffleObject;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;

/**
 * The list of types and type conversions that the AST interpreter knows about and can specialise
 * using. Used by the DSL.
 */
@DSLOptions(
        useNewLayout = true,
        monomorphicTypeBoxingOptimization = DSLOptions.TypeBoxingOptimization.NONE,
        polymorphicTypeBoxingElimination = DSLOptions.TypeBoxingOptimization.NONE,
        implicitTypeBoxingOptimization = DSLOptions.TypeBoxingOptimization.NONE,
        voidBoxingOptimization = DSLOptions.TypeBoxingOptimization.NONE)
@TypeSystem({ //
                UndefinedPlaceholder.class, //
                boolean.class, //
                byte.class, //
                int.class, //
                char.class, //
                short.class, //
                long.class, //
                float.class, //
                double.class, //
                String.class, // for SymbolCastNode
                RubyBignum.class, //
                RubyRange.IntegerFixnumRange.class, //
                RubyRange.LongFixnumRange.class, //
                RubyRange.ObjectRange.class, //
                RubyArray.class, //
                RubyBinding.class, //
                RubyClass.class, //
                RubyException.class, //
                RubyFiber.class, //
                RubyHash.class, //
                RubyMatchData.class, //
                RubyModule.class, //
                RubyNilClass.class, //
                RubyProc.class, //
                RubyRange.class, //
                RubyRegexp.class, //
                RubyString.class, //
                RubyEncoding.class, //
                RubySymbol.class, //
                RubyThread.class, //
                RubyTime.class, //
                RubyEncodingConverter.class, //
                RubyMethod.class, //
                RubyUnboundMethod.class, //
                RubiniusByteArray.class, //
                RubyBasicObject.class, //
                TruffleObject.class, //
                ThreadLocal.class, //
                Object[].class})

public class RubyTypes {

}
