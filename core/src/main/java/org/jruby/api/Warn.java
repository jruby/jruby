package org.jruby.api;

import org.jruby.runtime.ThreadContext;

public class Warn {
    public static void warn(ThreadContext context, String message) {
        context.runtime.getWarnings().warn(message);
    }

    public static void warningDeprecated(ThreadContext context, String message) {
        context.runtime.getWarnings().warningDeprecated(message);
    }
}
