/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.cli.Options;

public final class ExceptionTranslator {

    /**
     * Translate a Java exception into a Ruby exception.
     */
    public static RubyBasicObject translateException(RubyContext context, Throwable exception) {
        assert context != null;
        assert exception != null;

        CompilerAsserts.neverPartOfCompilation();

        // RaiseException already includes the Ruby exception

        if (exception instanceof RaiseException) {
            return ((RaiseException) exception).getRubyException();
        }

        // Translate divide by zero into ZeroDivisionError

        // TODO(CS): both of these error strings seem to be used in different places, but can't remember where...

        if (exception instanceof ArithmeticException && (exception.getMessage().endsWith("divide by zero") || exception.getMessage().endsWith("/ by zero"))) {
            return new RubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), "divided by 0", RubyCallStack.getRubyStacktrace());
        }

        // Translate the UnsupportedSpecializationException

        if (exception instanceof UnsupportedSpecializationException) {
            final UnsupportedSpecializationException specialization = (UnsupportedSpecializationException) exception;

            final StringBuilder builder = new StringBuilder();
            builder.append("Truffle doesn't have a case for the ");
            builder.append(specialization.getNode().getClass().getSimpleName());
            builder.append(" node with values of class");

            for (Object value : specialization.getSuppliedValues()) {
                builder.append(" ");

                if (value instanceof RubyBasicObject) {
                    builder.append(((RubyBasicObject) value).getRubyClass().getName());
                } else {
                    builder.append(value.getClass().getSimpleName());
                }
            }

            builder.append(" - this is either a feature we haven't implemented for Truffle yet, or it might be disallowed in Ruby anyway");

            return new RubyException(context.getCoreLibrary().getRubyTruffleErrorClass(), builder.toString());
        }

        /*
         * If we can't translate the exception into a Ruby exception, then the error is ours and we
         * report it as as RubyTruffleError. If a programmer sees this then it's a bug in our
         * implementation.
         */

        if (Options.TRUFFLE_PRINT_JAVA_EXCEPTIONS.load()) {
            exception.printStackTrace();
        }

        String message;

        if (exception.getMessage() == null) {
            message = exception.getClass().getSimpleName();
        } else {
            message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }

        return new RubyException(context.getCoreLibrary().getRubyTruffleErrorClass(), message, RubyCallStack.getRubyStacktrace());
    }

}
