package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.RubyFrozenError;
import org.jruby.RubyModule;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Create.newString;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

public class Error {
    /**
     * Return an instance of ArgumentError with the given message.
     *
     * @param context the current thread context
     * @param message to be the message of the exception.  Note that this message should
     *                be properly formatted using RubyStringBuilder.str() or you
     *                absolutely know it is clean ASCII-7BIT
     * @return the created exception
     */
    public static ArgumentError argumentError(ThreadContext context, String message) {
        return (ArgumentError) context.runtime.newRaiseException(context.runtime.getArgumentError(), message);
    }

    /**
     * Return an instance of ArgumentError for the given argument list length, min, and max.
     *
     * @param context the current thread context
     * @param got how many arguments we received
     * @param expected how many arguments we expect
     * @return the created exception
     */
    public static ArgumentError argumentError(ThreadContext context, int got, int expected) {
        return argumentError(context, got, expected, expected);
    }

    /**
     * Return an instance of ArgumentError for the given argument list length, min, and max.
     *
     * @param context the current thread context
     * @param length the length of the given argument array
     * @param min the minimum length required
     * @param max the maximum length required
     * @return the created exception
     */
    public static ArgumentError argumentError(ThreadContext context, int length, int min, int max) {
        return (ArgumentError) context.runtime.newArgumentError(length, min, max);
    }

    /**
     * Create a frozen error with a simple ASCII String.
     *
     * @param context the current thread context
     * @param object which is the frozen object
     * @param message to be displayed in the error
     * @return the error
     */
    public static RaiseException frozenError(ThreadContext context, IRubyObject object, String message) {
        return RubyFrozenError.newFrozenError(context, newString(context, message), object).toThrowable();
    }


    /**
     * Create an index error with a simple ASCII String.
     *
     * @param context the current thread context
     * @param message to be displayed in the error
     * @return the error
     */
    public static RaiseException indexError(ThreadContext context, String message) {
        return context.runtime.newRaiseException(context.runtime.getIndexError(), message);
    }

    /**
     * Create a runtime error with a simple ASCII String.
     *
     * @param context the current thread context
     * @param message to be displayed in the error
     * @return the error
     */
    public static RaiseException rangeError(ThreadContext context, String message) {
        return context.runtime.newRaiseException(context.runtime.getRangeError(), message);
    }

    /**
     * Create a runtime error with a simple ASCII String.
     *
     * @param context the current thread context
     * @param message to be displayed in the error
     * @return the error
     */
    public static RaiseException runtimeError(ThreadContext context, String message) {
        return context.runtime.newRaiseException(context.runtime.getRuntimeError(), message);
    }

    /**
     * Create a TypeError with the given message.
     *
     * @param context the current thread context
     * @param object which is the wrong type
     * @param expectedType the expected type(s) that object should have been
     */
    public static TypeError typeError(ThreadContext context, IRubyObject object, String expectedType) {
        return createTypeError(context, str(context.runtime, "wrong argument type ",
                typeFor(context.runtime, object), " (expected " + expectedType + ")"));
    }

    /**
     * Create a TypeError with the given message.
     *
     * @param context the current thread context
     * @param startOfMessage the start of the message
     * @param object which is the wrong type
     * @param restOfMessage the rest of the message
     */
    public static TypeError typeError(ThreadContext context, String startOfMessage, IRubyObject object, String restOfMessage) {
        return createTypeError(context, str(context.runtime, startOfMessage, typeFor(context.runtime, object), restOfMessage));
    }

    /**
     * Create a TypeError with the given message.
     *
     * @param context the current thread context
     * @param object which is the wrong type
     * @param expectedType the expected type that object should have been
     */
    public static TypeError typeError(ThreadContext context, IRubyObject object, RubyModule expectedType) {
        return createTypeError(context, createTypeErrorMessage(context, object, expectedType));
    }

    /**
     * Create a TypeError with the given message.
     *
     * @param context the current thread context
     * @param message to be the message of the exception.  Note that this message should
     *                be properly formatted using RubyStringBuilder.str() or you
     *                absolutely know it is clean ASCII-7BIT
     */
    public static TypeError typeError(ThreadContext context, String message) {
        return createTypeError(context, message);
    }

    /**
     * Create an instance of TypeError with the given message.
     *
     * @param context the current thread context
     * @param message to be the message of the exception.  Note that this message should
     *                be properly formatted using RubyStringBuilder.str() or you
     *                absolutely know it is clean ASCII-7BIT
     * @return the created exception
     */
    public static TypeError createTypeError(ThreadContext context, String message) {
        return (TypeError) context.runtime.newRaiseException(context.runtime.getTypeError(), message);
    }

    /**
     * Create a properly formatted error message for a typical TypeError.
     *
     * @param context the current thread context
     * @param object which is the wrong type
     * @param expectedType the expected type that object should have been
     * @return the created exception
     */
    public static String createTypeErrorMessage(ThreadContext context, IRubyObject object, RubyModule expectedType) {
        return str(context.runtime, "wrong argument type ",
                typeFor(context.runtime, object), " (expected ", types(context.runtime, expectedType), ")");
    }

    /**
     * Attach an exception to an error.R
     * @param error the error to attach the exception to
     * @param exception the exception to attach
     * @return the error with the exception attached
     */
    public static RaiseException withException(RaiseException error, Exception exception) {
        error.initCause(exception);
        return error;
    }

    private static IRubyObject typeFor(Ruby runtime, IRubyObject object) {
        return object instanceof RubyModule ? types(runtime, (RubyModule) object) : object.getMetaClass().getRealClass();
    }
}
