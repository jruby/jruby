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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import jnr.constants.platform.Errno;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyString;

/**
 * Rubinius primitives associated with the Ruby {@code Exception} class.
 */
public abstract class ExceptionPrimitiveNodes {

    @RubiniusPrimitive(name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends RubiniusPrimitiveNode {

        // If you add a constant here, add it below in isExceptionSupported() too.
        protected final static int ENOENT = Errno.ENOENT.intValue();
        protected final static int EBADF = Errno.EBADF.intValue();
        protected final static int EEXIST = Errno.EEXIST.intValue();
        protected final static int EACCES = Errno.EACCES.intValue();
        protected final static int EFAULT = Errno.EFAULT.intValue();
        protected final static int ENOTDIR = Errno.ENOTDIR.intValue();

        public static boolean isExceptionSupported(int errno) {
            return errno == ENOENT || errno == EBADF || errno == EEXIST || errno == EACCES || errno == EFAULT || errno == ENOTDIR;
        }

        public ExceptionErrnoErrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "errno == ENOENT")
        public RubyException enoent(RubyString message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError(message.toString(), this);
        }

        @Specialization(guards = { "errno == ENOENT", "isNil(message)" })
        public RubyException enoent(Object message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError("nil", this);
        }

        @Specialization(guards = { "errno == EBADF", "isNil(message)" })
        public RubyException ebadf(Object message, int errno) {
            return getContext().getCoreLibrary().badFileDescriptor(this);
        }

        @Specialization(guards = "errno == EEXIST")
        public RubyException eexist(RubyString message, int errno) {
            return getContext().getCoreLibrary().fileExistsError(message.toString(), this);
        }

        @Specialization(guards = { "errno == EEXIST", "isNil(message)" })
        public RubyException eexist(Object message, int errno) {
            return getContext().getCoreLibrary().fileExistsError("nil", this);
        }

        @Specialization(guards = "errno == EACCES")
        public RubyException eacces(RubyString message, int errno) {
            return getContext().getCoreLibrary().permissionDeniedError(message.toString(), this);
        }

        @Specialization(guards = {"errno == EACCES", "isNil(message)"})
        public RubyException eacces(Object message, int errno) {
            return getContext().getCoreLibrary().permissionDeniedError("nil", this);
        }

        @Specialization(guards = "errno == EFAULT")
        public RubyException efault(RubyString message, int errno) {
            return getContext().getCoreLibrary().badAddressError(this);
        }

        @Specialization(guards = {"errno == EFAULT", "isNil(message)"})
        public RubyException efault(Object message, int errno) {
            return getContext().getCoreLibrary().badAddressError(this);
        }

        @Specialization(guards = "errno == ENOTDIR")
        public RubyException enotdir(RubyString message, int errno) {
            return getContext().getCoreLibrary().notDirectoryError(message.toString(), this);
        }

        @TruffleBoundary
        @Specialization(guards = "!isExceptionSupported(errno)")
        public RubyException unsupported(Object message, int errno) {
            final Errno errnoObject = Errno.valueOf(errno);

            final String messageString;
            if (message instanceof RubyString) {
                messageString = message.toString();
            } else if (message == nil()) {
                messageString = "nil";
            } else {
                messageString = "unsupported message type";
            }

            if (errnoObject == null) {
                throw new UnsupportedOperationException("errno: " + errno + " " + messageString);
            } else {
                throw new UnsupportedOperationException("errno: " + errnoObject.name());
            }
        }

    }

}
