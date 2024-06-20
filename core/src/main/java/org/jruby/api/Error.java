package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

public class Error {
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
