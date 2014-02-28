/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.core.hash.RubyHash;
import org.jruby.truffle.runtime.core.range.FixnumRange;
import org.jruby.truffle.runtime.core.range.ObjectRange;
import org.jruby.truffle.runtime.core.range.RubyRange;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.math.BigInteger;

/**
 * The list of types and type conversions that the AST interpreter knows about and can specialise
 * using. Used by the DSL.
 */
@TypeSystem({UndefinedPlaceholder.class, //
                NilPlaceholder.class, //
                boolean.class, //
                int.class, //
                double.class, //
                BigInteger.class, //
                FixnumRange.class, //
                ObjectRange.class, //
                RubyArray.class, //
                RubyBignum.class, //
                RubyBinding.class, //
                RubyClass.class, //
                RubyContinuation.class, //
                RubyException.class, //
                RubyFiber.class, //
                RubyFile.class, //
                RubyFixnum.class, //
                RubyFloat.class, //
                RubyHash.class, //
                RubyMatchData.class, //
                RubyMethod.class, //
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
                RubyObject.class, //
                RubyBasicObject.class, //
                Node.class, //
                Object[].class})
public class RubyTypes {

    /*
     * The implicit casts allow the DSL to convert from an object of one type to another to satisfy
     * specializations.
     */

    @ImplicitCast
    public NilPlaceholder unboxNil(@SuppressWarnings("unused") RubyNilClass value) {
        return NilPlaceholder.INSTANCE;
    }

    @ImplicitCast
    public int unboxFixnum(RubyFixnum value) {
        return value.getValue();
    }

    @ImplicitCast
    public BigInteger unboxBignum(RubyBignum value) {
        return value.getValue();
    }

    @ImplicitCast
    public double unboxFloat(RubyFloat value) {
        return value.getValue();
    }

}
