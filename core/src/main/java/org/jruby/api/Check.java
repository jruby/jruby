package org.jruby.api;

import org.jruby.RubyBoolean;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warning;
import static org.jruby.util.RubyStringBuilder.str;
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
                "path name contains null char" : "path name contains null byte");
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

    /**
     * Check that the given value is a Boolean and return its boolean value.
     * <p>
     * If the value is not a Boolean, this function will either raise (if the 'raise' parameter is true) or warn and
     * return true only if the object is non-nil.
     * <p>
     * C API: rb_bool_expected
     *
     * @param context the current context
     * @param maybeBool the value that may be a Boolean
     * @param id the keyword that referred to the value
     * @param raise whether to raise if the value is not a Boolean
     * @return the boolean value of the object, after checking if it is a Boolean and raising or warning if not
     */
    public static boolean checkBoolean(ThreadContext context, IRubyObject maybeBool, String id, boolean raise) {
        return switch (maybeBool) {
            case RubyBoolean.True ignored -> true;
            case RubyBoolean.False ignored -> false;
            default -> {
                String message = "expected true or false as " + id + ": ";

                if (raise) {
                    throw argumentError(context, str(context.runtime, message, maybeBool));
                }

                warning(context, message);

                yield !maybeBool.isNil();
            }
        };
    }
}
