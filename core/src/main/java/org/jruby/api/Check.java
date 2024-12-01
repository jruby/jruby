package org.jruby.api;

import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Error.argumentError;
import static org.jruby.util.StringSupport.strNullCheck;

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
}
