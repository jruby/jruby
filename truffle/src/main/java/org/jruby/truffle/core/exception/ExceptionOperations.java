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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.backtrace.BacktraceFormatter.FormattingFlags;

import java.util.EnumSet;
import java.util.List;

public abstract class ExceptionOperations {

    private static final EnumSet<BacktraceFormatter.FormattingFlags> FORMAT_FLAGS = EnumSet.of(
            FormattingFlags.OMIT_FROM_PREFIX,
            FormattingFlags.OMIT_EXCEPTION);

    @TruffleBoundary
    public static DynamicObject backtraceAsRubyStringArray(RubyContext context, DynamicObject exception, Backtrace backtrace) {
        final BacktraceFormatter formatter = new BacktraceFormatter(context, FORMAT_FLAGS);
        final List<String> lines = formatter.formatBacktrace(context, exception, backtrace);

        final Object[] array = new Object[lines.size()];

        for (int n = 0; n < lines.size(); n++) {
            array[n] = StringOperations.createString(context,
                    StringOperations.encodeRope(lines.get(n), UTF8Encoding.INSTANCE));
        }

        return ArrayHelpers.createArray(context, array, array.length);
    }

    // because the factory is not constant
    @TruffleBoundary
    public static DynamicObject createRubyException(DynamicObject rubyClass, Object message, Backtrace backtrace) {
        return Layouts.EXCEPTION.createException(Layouts.CLASS.getInstanceFactory(rubyClass), message, backtrace);
    }

    // because the factory is not constant
    @TruffleBoundary
    public static DynamicObject createSystemCallError(DynamicObject rubyClass, Object message, Backtrace backtrace, int errno) {
        return Layouts.SYSTEM_CALL_ERROR.createSystemCallError(Layouts.CLASS.getInstanceFactory(rubyClass), message, backtrace, errno);
    }

}
