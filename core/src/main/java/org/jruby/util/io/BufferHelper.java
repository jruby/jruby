package org.jruby.util.io;

import java.nio.Buffer;

/**
 * Utility functions to help avoid binary incompatibility due to variadic return types on Java 9.
 *
 * Java 9 introduced new overloads of several Buffer methods on its subclasses. Because those methods
 * return the actual type of the subclass, they are incompatible with Java 8 where no such overloads
 * exist. This utility casts all buffers to Buffer and makes the call from there, avoiding binding
 * directly to the overloads that don't exist on Java 8.
 *
 * See https://github.com/jruby/jruby/pull/5451
 */
public class BufferHelper {
    /**
     * Invoke Buffer.clear always using Buffer as the target, to avoid binary incompatibility on Java 8.
     *
     * @param buf the buffer
     * @param <T> any java.nio.Buffer type
     * @return the buffer
     */
    public static <T extends Buffer> T clearBuffer(T buf) {
        return (T) buf.clear();
    }

    /**
     * Invoke Buffer.flip always using Buffer as the target, to avoid binary incompatibility on Java 8.
     *
     * @param buf the buffer
     * @param <T> any java.nio.Buffer type
     * @return the buffer
     */
    public static <T extends Buffer> T flipBuffer(T buf) {
        return (T) buf.flip();
    }

    /**
     * Invoke Buffer.limit always using Buffer as the target, to avoid binary incompatibility on Java 8.
     *
     * @param buf the buffer
     * @param <T> any java.nio.Buffer type
     * @return the buffer
     */
    public static <T extends Buffer> T limitBuffer(T buf, int limit) {
        return (T) buf.limit(limit);
    }

    /**
     * Invoke Buffer.position always using Buffer as the target, to avoid binary incompatibility on Java 8.
     *
     * @param buf the buffer
     * @param <T> any java.nio.Buffer type
     * @return the buffer
     */
    public static <T extends Buffer> T positionBuffer(T buf, int limit) {
        return (T) buf.position(limit);
    }

    /**
     * Invoke Buffer.mark always using Buffer as the target, to avoid binary incompatibility on Java 8.
     *
     * @param buf the buffer
     * @param <T> any java.nio.Buffer type
     * @return the buffer
     */
    public static <T extends Buffer> T markBuffer(T buf) {
        return (T) buf.mark();
    }
}
