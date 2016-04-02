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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;

/**
 * Rubinius primitives associated with the Ruby {@code Exception} class.
 */
public abstract class ExceptionPrimitiveNodes {

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        public ExceptionErrnoErrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject exceptionErrnoError(DynamicObject message, int errno) {
            return coreLibrary().errnoError(errno, message.toString(), this);
        }

    }

}
