package org.jruby.api;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

public class Create {
    /**
     * Creates a new RubyString from the provided int.
     *
     * @param context the current thread context
     * @param value the bytes to become a fixnum
     * @return the new RubyFixnum
     */
    public static RubyFixnum newFixnum(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Creates a new RubyString from the provided long.
     *
     * @param context the current thread context
     * @param value the bytes to become a fixnum
     * @return the new RubyFixnum
     */
    public static RubyFixnum newFixnum(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Creates a new RubyString from the provided bytelist.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, ByteList bytes) {
        return RubyString.newString(context.runtime, bytes);
    }

    /**
     * Creates a new RubyString from the provided bytelist but use the supplied
     * encoding if possible.
     *
     * @param context the current thread context
     * @param bytes the bytes to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, ByteList bytes, Encoding encoding) {
        return RubyString.newString(context.runtime, bytes, encoding);
    }

    /**
     * Creates a new RubyString from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, String string, Encoding encoding) {
        return RubyString.newString(context.runtime, string, encoding);
    }

    /**
     * Creates a new RubyString from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubyString newString(ThreadContext context, String string) {
        return RubyString.newString(context.runtime, string);
    }

    /**
     * Creates a new RubySymbol from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a string
     * @return the new RubyString
     */
    public static RubySymbol newSymbol(ThreadContext context, String string) {
        return context.runtime.newSymbol(string);
    }
}
