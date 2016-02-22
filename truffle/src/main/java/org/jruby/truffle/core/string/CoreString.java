/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;

import java.nio.charset.StandardCharsets;

public class CoreString {

    private final RubyContext context;
    private final String literal;

    @CompilationFinal private volatile Rope rope;

    public CoreString(RubyContext context, String literal) {
        assert is7Bit(literal);
        this.context = context;
        this.literal = literal;
    }

    public Rope getRope() {
        if (rope == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rope = createRope();
        }

        return rope;
    }

    public DynamicObject createInstance() {
        return StringOperations.createString(context, getRope());
    }

    private Rope createRope() {
        // getRope is fully synchronised and returns identically the same object for the same parameters

        return context.getRopeTable().getRope(
                literal.getBytes(StandardCharsets.US_ASCII),
                ASCIIEncoding.INSTANCE,
                CodeRange.CR_7BIT);
    }

    private static boolean is7Bit(String literal) {
        for (int n = 0; n < literal.length(); n++) {
            if (literal.charAt(n) > 0b1111111) {
                return false;
            }
        }

        return true;
    }

}
