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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;

/**
 * Catch-all class for Rubinius primitives that are invoked but haven't yet been defined.  Its only purpose is to
 * allow Truffle to parse Rubinius primitive calls without failing during the translation phase.  If any code ever
 * executes nodes here, things will break and you must implement the primitive in its respective parent.
 */
public abstract class UndefinedPrimitiveNodes {

    public final static String NAME = "undefined";

    @Primitive(name = NAME)
    public static abstract class UndefinedPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object undefined(Object args) {
            final SourceSection sourceSection = getEncapsulatingSourceSection();
            throw new UnsupportedOperationException(
                    "Undefined Rubinius primitive: \"" + sourceSection.toString().trim() + '"');
        }

    }

}
