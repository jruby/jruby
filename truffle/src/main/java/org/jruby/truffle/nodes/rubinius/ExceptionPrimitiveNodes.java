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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Rubinius primitives associated with the Ruby {@code Exception} class.
 */
public abstract class ExceptionPrimitiveNodes {

    @RubiniusPrimitive(name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends RubiniusPrimitiveNode {

        // If you add a constant here, add it below in isExceptionSupported() too.
        protected final static int EPERM = Errno.EPERM.intValue();
        protected final static int ENOENT = Errno.ENOENT.intValue();
        protected final static int EBADF = Errno.EBADF.intValue();
        protected final static int EEXIST = Errno.EEXIST.intValue();
        protected final static int EACCES = Errno.EACCES.intValue();
        protected final static int EFAULT = Errno.EFAULT.intValue();
        protected final static int ENOTDIR = Errno.ENOTDIR.intValue();
        protected final static int EINVAL = Errno.EINVAL.intValue();
        protected final static int EINPROGRESS = Errno.EINPROGRESS.intValue();

        public static boolean isExceptionSupported(int errno) {
            return errno == EPERM || errno == ENOENT || errno == EBADF || errno == EEXIST || errno == EACCES || errno == EFAULT || errno == ENOTDIR || errno == EINVAL || errno == EINPROGRESS;
        }

        public ExceptionErrnoErrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EPERM"})
        public DynamicObject eperm(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().operationNotPermittedError(message.toString(), this);
        }

        @Specialization(guards = {"errno == EPERM", "isNil(message)"})
        public DynamicObject eperm(Object message, int errno) {
            return getContext().getCoreLibrary().operationNotPermittedError("nil", this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == ENOENT"})
        public DynamicObject enoent(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError(message.toString(), this);
        }

        @Specialization(guards = { "errno == ENOENT", "isNil(message)" })
        public DynamicObject enoent(Object message, int errno) {
            return getContext().getCoreLibrary().fileNotFoundError("nil", this);
        }

        @Specialization(guards = { "errno == EBADF", "isNil(message)" })
        public DynamicObject ebadf(Object message, int errno) {
            return getContext().getCoreLibrary().badFileDescriptor(this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EEXIST"})
        public DynamicObject eexist(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().fileExistsError(message.toString(), this);
        }

        @Specialization(guards = { "errno == EEXIST", "isNil(message)" })
        public DynamicObject eexist(Object message, int errno) {
            return getContext().getCoreLibrary().fileExistsError("nil", this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EACCES"})
        public DynamicObject eacces(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().permissionDeniedError(message.toString(), this);
        }

        @Specialization(guards = {"errno == EACCES", "isNil(message)"})
        public DynamicObject eacces(Object message, int errno) {
            return getContext().getCoreLibrary().permissionDeniedError("nil", this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EFAULT"})
        public DynamicObject efault(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().badAddressError(this);
        }

        @Specialization(guards = {"errno == EFAULT", "isNil(message)"})
        public DynamicObject efault(Object message, int errno) {
            return getContext().getCoreLibrary().badAddressError(this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == ENOTDIR"})
        public DynamicObject enotdir(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().notDirectoryError(message.toString(), this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EINVAL"})
        public DynamicObject einval(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().errnoError(errno, this);
        }

        @Specialization(guards = {"isRubyString(message)", "errno == EINPROGRESS"})
        public DynamicObject einprogress(DynamicObject message, int errno) {
            return getContext().getCoreLibrary().errnoError(errno, this);
        }

        @TruffleBoundary
        @Specialization(guards = "!isExceptionSupported(errno)")
        public DynamicObject unsupported(Object message, int errno) {
            final Errno errnoObject = Errno.valueOf(errno);

            final String messageString;
            if (RubyGuards.isRubyString(message)) {
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
