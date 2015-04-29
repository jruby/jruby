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
import jnr.constants.platform.Errno;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

/**
 * Rubinius primitives associated with the Ruby {@code Exception} class.
 */
public abstract class ExceptionPrimitiveNodes {

    @RubiniusPrimitive(name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends RubiniusPrimitiveNode {

        protected final int ENOENT = Errno.ENOENT.intValue();

        public ExceptionErrnoErrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "errno == ENOENT")
        public RubyException enoent(RubyString message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError(message.toString(), this);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "errno != ENOENT")
        public RubyException unsupported(RubyString message, int errno) {
            final Errno errnoObject = Errno.valueOf(errno);

            if (errnoObject == null) {
                throw new UnsupportedOperationException("errno: " + errno + " " + message);
            } else {
                throw new UnsupportedOperationException("errno: " + errnoObject.name());
            }
        }

    }

}
