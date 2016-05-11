/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.exception;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;

@CoreClass("Exception")
public abstract class ExceptionNodes {

    @CoreMethod(names = "initialize", optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject initialize(DynamicObject exception, NotProvided message) {
            Layouts.EXCEPTION.setMessage(exception, nil());
            return exception;
        }

        @Specialization(guards = "wasProvided(message)")
        public DynamicObject initialize(DynamicObject exception, Object message) {
            Layouts.EXCEPTION.setMessage(exception, message);
            return exception;
        }

    }

    @CoreMethod(names = "backtrace")
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        @Child private ReadObjectFieldNode readCustomBacktraceNode;

        @Specialization
        public Object backtrace(
                DynamicObject exception,
                @Cached("createBinaryProfile()") ConditionProfile hasCustomBacktraceProfile,
                @Cached("createBinaryProfile()") ConditionProfile hasBacktraceProfile) {
            final Object customBacktrace = getReadCustomBacktraceNode().execute(exception);

            if (hasCustomBacktraceProfile.profile(customBacktrace != null)) {
                return customBacktrace;
            } else if (hasBacktraceProfile.profile(Layouts.EXCEPTION.getBacktrace(exception) != null)) {
                return ExceptionOperations.backtraceAsRubyStringArray(
                        getContext(),
                        exception,
                        Layouts.EXCEPTION.getBacktrace(exception));
            } else {
                return nil();
            }
        }

        private ReadObjectFieldNode getReadCustomBacktraceNode() {
            if (readCustomBacktraceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readCustomBacktraceNode = insert(ReadObjectFieldNodeGen.create(
                        "@custom_backtrace", null));
            }

            return readCustomBacktraceNode;
        }

    }

    @NonStandard
    @CoreMethod(names = "capture_backtrace!", optional = 1)
    public abstract static class CaptureBacktraceNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject captureBacktrace(DynamicObject exception, NotProvided offset) {
            return captureBacktrace(exception, 1);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject captureBacktrace(DynamicObject exception, int offset) {
            final Backtrace backtrace = getContext().getCallStack().getBacktrace(this, offset, exception);
            Layouts.EXCEPTION.setBacktrace(exception, backtrace);
            return nil();
        }

    }

    @CoreMethod(names = "message")
    public abstract static class MessageNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object message(
                DynamicObject exception,
                @Cached("createBinaryProfile()") ConditionProfile messageProfile) {
            final Object message = Layouts.EXCEPTION.getMessage(exception);

            if (messageProfile.profile(message == null)) {
                final String className = Layouts.MODULE.getFields(
                        Layouts.BASIC_OBJECT.getLogicalClass(exception)).getName();
                return createString(StringOperations.encodeRope(className, UTF8Encoding.INSTANCE));
            } else {
                return message;
            }
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, null, null);
        }

    }

    @Primitive(name = "exception_errno_error", needsSelf = false)
    public static abstract class ExceptionErrnoErrorPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject exceptionErrnoError(DynamicObject message, int errno) {
            return coreExceptions().errnoError(errno, message.toString(), this);
        }

    }

}
