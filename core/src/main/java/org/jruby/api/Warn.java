package org.jruby.api;

import org.jruby.runtime.ThreadContext;

public class Warn {
    public static void warn(ThreadContext context, String message) {
        context.runtime.getWarnings().warn(message);
    }

    /**
     * Produce a warning if deprecated is set (e.g. Warning[:deprecated] = true)
     * @param context the current context
     * @param message to be displayed as a warning
     */
    public static void warnDeprecated(ThreadContext context, String message) {
        context.runtime.getWarnings().warnDeprecated(message);
    }

    /**
     * Produce a warning if deprecated is set (e.g. Warning[:deprecated] = true)
     * @param context the current context
     * @param message to be displayed as a warning
     * @param version the version at which this deprecated feature will be removed
     */
    public static void warnDeprecatedForRemoval(ThreadContext context, String message, String version) {
        context.runtime.getWarnings().warnDeprecatedForRemoval(message, version);
    }

    /**
     * Produce a warning if deprecated is set (e.g. Warning[:deprecated] = true)
     * @param context the current context
     * @param message to be displayed as a warning
     * @param version the version at which this deprecated feature will be removed
     * @param alternate the alternate feature that should be used instead
     */
    public static void warnDeprecatedForRemovalAlternate(ThreadContext context, String message, String version, String alternate) {
        context.runtime.getWarnings().warnDeprecatedForRemovalAlternate(message, version, alternate);
    }

    public static void warning(ThreadContext context, String message) {
        context.runtime.getWarnings().warning(message);
    }

    /**
     * Produce a warning if deprecated is set (e.g. Warning[:deprecated] = true) and in verbose mode.
     *
     * @param context the current context
     * @param message to be displayed as a warning
     */
    public static void warningDeprecated(ThreadContext context, String message) {
        context.runtime.getWarnings().warningDeprecated(message);
    }

    /**
     * Produce a warning id deprecated is set (e.g. Warning[:deprecated] = true)
     * @param context the current context
     * @param message to be displayed as a warning
     */
    public static void warnExperimental(ThreadContext context, String message) {
        context.runtime.getWarnings().warnExperimental(message);
    }

}
