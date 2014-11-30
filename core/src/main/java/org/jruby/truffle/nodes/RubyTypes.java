/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.truffle.runtime.rubinius.RubiniusChannel;
import org.jruby.truffle.runtime.LexicalScope;

import java.math.BigInteger;

/**
 * The list of types and type conversions that the AST interpreter knows about and can specialise
 * using. Used by the DSL.
 */
@TypeSystem({ //
                Dispatch.DispatchAction.class, //
                LexicalScope.class, //
                UndefinedPlaceholder.class, //
                boolean.class, //
                int.class, //
                long.class, //
                double.class, //
                String.class, // for SymbolCastNode
                BigInteger.class, //
                RubyRange.IntegerFixnumRange.class, //
                RubyRange.LongFixnumRange.class, //
                RubyRange.ObjectRange.class, //
                RubyArray.class, //
                RubyBinding.class, //
                RubyClass.class, //
                RubyException.class, //
                RubyFiber.class, //
                RubyFile.class, //
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
                RubiniusChannel.class, //
                RubiniusByteArray.class, //
                RubyEncodingConverter.class, //
                RubyBasicObject.class, //
                Object[].class})

public class RubyTypes {

}
