package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.load.Library;

import java.io.IOException;

import static org.jruby.api.Access.*;
import static org.jruby.api.Define.defineClass;

/**
 *
 * @author nicksieger
 */
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
            var UNIXSocket = RubyUNIXSocket.createUNIXSocket(context, BasicSocket, Object);
            RubyUNIXServer.createUNIXServer(context, UNIXSocket, Object);
        }

        var IPSocket = RubyIPSocket.createIPSocket(context, BasicSocket);
        var TCPSocket = RubyTCPSocket.createTCPSocket(context, IPSocket, Object);
        RubyTCPServer.createTCPServer(context, TCPSocket, Object);
        RubyUDPSocket.createUDPSocket(context, IPSocket, Socket, Object);

        Addrinfo.createAddrinfo(context, Object);
        Option.createOption(context, Object, Socket);
        Ifaddr.createIfaddr(context, Object, Socket);
    }
}
