package org.jruby.ext.socket;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.load.Library;

/**
 *
 * @author nicksieger
 */
public class SocketLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        runtime.defineClass("SocketError", runtime.getStandardError(), runtime.getStandardError().getAllocator());
        RubyBasicSocket.createBasicSocket(runtime);
        RubySocket.createSocket(runtime);

        if (RubyInstanceConfig.nativeEnabled && RubyUNIXSocket.tryUnixDomainSocket()) {
            RubyUNIXSocket.createUNIXSocket(runtime);
            RubyUNIXServer.createUNIXServer(runtime);
        }

        RubyIPSocket.createIPSocket(runtime);
        RubyTCPSocket.createTCPSocket(runtime);
        RubyTCPServer.createTCPServer(runtime);
        RubyUDPSocket.createUDPSocket(runtime);
    }
}
