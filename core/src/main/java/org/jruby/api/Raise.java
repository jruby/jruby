package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.RubyModule;
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
        throw context.runtime.newTypeError(str(context.runtime, "wrong argument type ",
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
        throw context.runtime.newTypeError(str(context.runtime, "wrong argument type ",
                typeFor(context.runtime, object), " (expected ", types(context.runtime, expectedType), ")"));
    }


    private static IRubyObject typeFor(Ruby runtime, IRubyObject object) {
        return object instanceof RubyModule ? types(runtime, (RubyModule) object) : object.getMetaClass();
    }
}
