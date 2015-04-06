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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Catch-all class for Rubinius primitives that are invoked but haven't yet been defined.  Its only purpose is to
 * allow Truffle to parse Rubinius primitive calls without failing during the translation phase.  If any code ever
 * executes nodes here, things will break and you must implement the primitive in its respective parent.
 */
public abstract class UndefinedPrimitiveNodes {
    public final static String NAME = "undefined";


    @RubiniusPrimitive(name = NAME)
    public static abstract class UndefinedPrimitiveNode extends RubiniusPrimitiveNode {

        public UndefinedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UndefinedPrimitiveNode(UndefinedPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object undefined(Object... args) {
            throw new UnsupportedOperationException("Undefined Rubinius primitive.");
        }
    }

}
