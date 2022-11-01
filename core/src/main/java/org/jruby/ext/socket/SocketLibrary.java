package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.load.Library;

import java.io.IOException;

/**
 *
 * @author nicksieger
 */
public class SocketLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        runtime.defineClass("SocketError", runtime.getStandardError(), runtime.getStandardError().getAllocator());
        RubyBasicSocket.createBasicSocket(runtime);
        RubySocket.createSocket(runtime);
        RubyServerSocket.createServerSocket(runtime);

        if (runtime.getInstanceConfig().isNativeEnabled() && !Platform.IS_WINDOWS) {
            RubyUNIXSocket.createUNIXSocket(runtime);
            RubyUNIXServer.createUNIXServer(runtime);
        }

        RubyIPSocket.createIPSocket(runtime);
        RubyTCPSocket.createTCPSocket(runtime);
        RubyTCPServer.createTCPServer(runtime);
        RubyUDPSocket.createUDPSocket(runtime);

        Addrinfo.createAddrinfo(runtime);
        Option.createOption(runtime);
        Ifaddr.createIfaddr(runtime);
    }
}
