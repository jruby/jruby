package org.jruby.ext.socket;

import java.io.IOException;

/**
 * Factory that tries JDK first, falls back to JNR
 */
public class UnixSocketChannelFactory {
    private static final boolean JDK_AVAILABLE;
    private static final String IMPLEMENTATION;

    static {
        boolean jdkAvailable = false;
        try {
            // Try to load JDK 16+ classes
            Class.forName("java.net.UnixDomainSocketAddress");
            jdkAvailable = true;
        } catch (ClassNotFoundException e) {
            // JDK < 16, will use JNR
        }
        JDK_AVAILABLE = jdkAvailable;
        IMPLEMENTATION = jdkAvailable ? "JDK (JEP-380)" : "JNR (Legacy)";
    }

    public static String getImplementation() {
        return IMPLEMENTATION;
    }

    public static boolean isJDKAvailable() {
        return JDK_AVAILABLE;
    }

    public static RubyUNIXSocketChannel connect(String path) throws IOException {
        if (JDK_AVAILABLE) {
            try {
                return new JDKUnixSocketChannel(path);
            } catch (Exception e) {
                // JDK failed, try JNR
                System.err.println("JDK connection failed, falling back to JNR: " + e.getMessage());
            }
        }
        return new JNRUnixSocketChannel(path);
    }

    public static RubyUNIXServerChannel bind(String path) throws IOException {
        if (JDK_AVAILABLE) {
            try {
                return new JDKUnixServerChannel(path);
            } catch (Exception e) {
                // JDK failed, try JNR
                System.err.println("JDK bind failed, falling back to JNR: " + e.getMessage());
            }
        }
        return new JNRUnixServerChannel(path);
    }
}
