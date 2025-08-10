package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.load.Library;

import java.io.IOException;

import static org.jruby.api.Access.*;
import static org.jruby.api.Define.defineClass;

public class SocketLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        var context = runtime.getCurrentContext();
        var Object = objectClass(context);
        var StandardError = standardErrorClass(context);
        var IO = ioClass(context);

        defineClass(context, "SocketError", StandardError, StandardError.getAllocator());

        var BasicSocket = RubyBasicSocket.createBasicSocket(context, IO);
        var Socket = RubySocket.createSocket(context, BasicSocket);
        RubyServerSocket.createServerSocket(context, Socket);

        if (instanceConfig(context).isNativeEnabled() && !Platform.IS_WINDOWS) {
            var UNIXSocket = RubyUNIXSocket.createUNIXSocket(context, BasicSocket);
            RubyUNIXServer.createUNIXServer(context, UNIXSocket);
        }

        var IPSocket = RubyIPSocket.createIPSocket(context, BasicSocket);
        var TCPSocket = RubyTCPSocket.createTCPSocket(context, IPSocket);
        RubyTCPServer.createTCPServer(context, TCPSocket);
        RubyUDPSocket.createUDPSocket(context, IPSocket, Socket);

        Addrinfo.createAddrinfo(context, Object);
        Option.createOption(context, Object, Socket);
        Ifaddr.createIfaddr(context, Object, Socket);
    }
}
