package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

public class Raise {
    /**
     * Throw a TypeError with the given message.
     *
     * @param context the current thread context
     * @param object which is the wrong type
     * @param expectedType the expected type(s) that object should have been
     */
    public static void typeError(ThreadContext context, IRubyObject object, String expectedType) {
        throw createTypeError(context, str(context.runtime, "wrong argument type ",
                typeFor(context.runtime, object), " (expected " + expectedType + ")"));
    }

    /**
     * Throw a TypeError with the given message.
     *
     * @param context the current thread context
     * @param object which is the wrong type
     * @param expectedType the expected type that object should have been
     */
    public static void typeError(ThreadContext context, IRubyObject object, RubyModule expectedType) {
        throw createTypeError(context, createTypeErrorMessage(context, object, expectedType));
    }

    /**
     * Throw a TypeError with the given message.
     *
     * @param context the current thread context
     * @param message to be message of the exception.  Note that this message should
     *                be properly formatted using RubyStringBuilder.str() or you
     *                absolutely know it is clean ASCII-7BIT
     */
    public static void typeError(ThreadContext context, String message) {
        throw createTypeError(context, message);
    }

    /**
     * Create an instance of TypeError with the given message.
     *
     * @param context the current thread context
     * @param message to be message of the exception.  Note that this message should
     *                be properly formatted using RubyStringBuilder.str() or you
     *                absolutely know it is clean ASCII-7BIT
     * @return the created exception
     */
    public static RaiseException createTypeError(ThreadContext context, String message) {
        throw context.runtime.newTypeError(message);
    }

    /**
     * Create an properly formatted error message for a typical TypeError.
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


    private static IRubyObject typeFor(Ruby runtime, IRubyObject object) {
        return object instanceof RubyModule ? types(runtime, (RubyModule) object) : object.getMetaClass();
    }
}
