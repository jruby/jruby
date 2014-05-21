/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.RubyFloat;

import java.math.BigInteger;

public class RawBoxingNode extends Node {

    private RubyContext context;

    @CompilerDirectives.CompilationFinal private boolean seenBoxed = false;
    @CompilerDirectives.CompilationFinal private boolean seenNil = false;
    @CompilerDirectives.CompilationFinal private boolean seenBoolean = false;
    @CompilerDirectives.CompilationFinal private boolean seenInteger = false;
    @CompilerDirectives.CompilationFinal private boolean seenLong = false;
    @CompilerDirectives.CompilationFinal private boolean seenDouble = false;
    @CompilerDirectives.CompilationFinal private boolean seenBigInteger = false;

    public RawBoxingNode(RubyContext context) {
        this.context = context;
    }

    public RubyBasicObject box(Object value) {
        if (seenBoxed && value instanceof RubyBasicObject) {
            return (RubyBasicObject) value;
        }

        if (seenNil && value instanceof NilPlaceholder) {
            return context.getCoreLibrary().getNilObject();
        }

        if (seenBoolean && value instanceof Boolean) {
            if ((boolean) value) {
                return context.getCoreLibrary().getTrueObject();
            } else {
                return context.getCoreLibrary().getFalseObject();
            }
        }

        if (seenInteger && value instanceof Integer) {
            return new RubyFixnum.IntegerFixnum(context.getCoreLibrary().getFixnumClass(), (int) value);
        }

        if (seenLong && value instanceof Long) {
            return new RubyFixnum.LongFixnum(context.getCoreLibrary().getFixnumClass(), (long) value);
        }

        if (seenDouble && value instanceof Double) {
            return new RubyFloat(context.getCoreLibrary().getFloatClass(), (double) value);
        }

        if (seenBigInteger && value instanceof BigInteger) {
            return new RubyBignum(context.getCoreLibrary().getFixnumClass(), (BigInteger) value);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (value instanceof RubyBasicObject) {
            seenBoxed = true;
        } else if (value instanceof NilPlaceholder) {
            seenNil = true;
        } else if (value instanceof Boolean) {
            seenBoolean = true;
        } else if (value instanceof Integer) {
            seenInteger = true;
        } else if (value instanceof Long) {
            seenLong = true;
        } else if (value instanceof Double) {
            seenDouble = true;
        } else if (value instanceof BigInteger) {
            seenBigInteger = true;
        } else {
            throw new UnsupportedOperationException(value.getClass().getName());
        }

        return box(value);
    }

}
