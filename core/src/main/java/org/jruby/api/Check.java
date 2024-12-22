package org.jruby.api;

import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.StringSupport.strNullCheck;
import static org.jruby.util.TypeConverter.convertToTypeWithCheck;

public class Check {
    /**
     * Check to see if the supplied object (which is convertable to a string) contains any
     * null (\0) bytes.  It will throw an ArgumentError if so and a TypeError is obj is not
     * a string{able}.
     *
     * @param context the current thread context
     * @param obj object to be made into a string and checked for NULLs
     * @return the converted str (or original if no conversion happened).
     */
    public static RubyString checkEmbeddedNulls(ThreadContext context, IRubyObject obj) {
        // FIXME: make into a record
        Object[] checked = strNullCheck(obj);

        if (checked[0] == null) {
            throw argumentError(context, (boolean)checked[1] ?
                "string contains null char" : "string contains null byte");
        }

        return (RubyString) checked[0];
    }

    /**
     * Convert the supplied object into an internal identifier String.  Basically, symbols
     * are stored internally as raw bytes from whatever encoding they were originally sourced from.
     * When methods are stored they must also get stored in this same raw fashion so that if we
     * use symbols to look up methods or make symbols from these method names they will match up.
     *
     * For 2.2 compatibility, we also force all incoming identifiers to get anchored as hard-referenced symbols.
     */
    public static RubySymbol checkID(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubySymbol || obj instanceof RubyString) {
            return RubySymbol.newHardSymbol(context.runtime, obj);
        }

        final IRubyObject str = convertToTypeWithCheck(obj, stringClass(context), "to_str");
        if (!str.isNil()) return RubySymbol.newHardSymbol(context.runtime, str);

        throw typeError(context, obj.callMethod(context, "inspect") + " is not a symbol nor a string");
    }
}
