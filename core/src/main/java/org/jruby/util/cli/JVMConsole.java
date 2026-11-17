package org.jruby.util.cli;

import org.jruby.util.SafePropertyAccessor;

import java.io.Console;
import java.lang.reflect.InvocationTargetException;

/**
 * Determines whether the JVM process's stdio is connected to a TTY.
 * </p>
 * The logic here is based on the {@link System#console()} method with additional checks:
 * </p>
 * <li>On Java 21 and lower, we assume a non-null Console indicates a controlling TTY.</li>
 * <li>On Java 22 and later, we additionally check {@link java.io.Console#isTerminal()}.</li>
 * <li>If the specification version cannot be determined, or we are on 22+ and cannot call isTerminal, we return false.</li>
 * </p>
 * See https://github.com/jruby/jruby/issues/9590 for some discussion on the JRuby side.
 * </p>
 * See https://bugs.openjdk.org/browse/JDK-8361911 for an OpenJDK issue describing the changing behavior of System.console().
 */
public class JVMConsole {
    public static final Console console = System.console();
    public static final boolean isTerminal = isTerminal(console);

    static boolean isTerminal(Console console) {
        if (console == null) {
            return false;
        } else {
            // There's a bug in JDK 22 where it always returns non-null System.console(), so we have further checks.
            String specVersion = SafePropertyAccessor.getProperty("java.specification.version");

            if (specVersion == null) {
                // cannot determine spec, cannot trust console
                return false;
            } else {
                try {
                    int specVersionInt = Integer.parseInt(specVersion);
                    if (specVersionInt < 22) {
                        // Assume JDK 21 and lower return null for non-terminal
                        return true;
                    } else {
                        // double check isTerminal on JDK 22+
                        return checkConsoleTerminal(console);
                    }
                } catch (NumberFormatException e) {
                    // can't parse spec version, cannot trust console
                    return false;
                }
            }
        }
    }

    /**
     * Reflectively call Console.isTerminal as an additional verification that the Console is a TTY.
     *
     * @param console the Console returned by {@link System#console()}
     * @return the result of isTerminal() if it could be called, false otherwise
     */
    private static boolean checkConsoleTerminal(Console console) {
        try {
            return (Boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException e) {
            // can't invoke isTerminal method, return false
            return false;
        }
    }
}
