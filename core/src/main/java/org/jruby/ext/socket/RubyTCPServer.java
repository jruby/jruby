package org.jruby.ext.socket;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * RubyTCPServer - Ruby wrapper for TCP server sockets
 * Provides MRI-compatible TCPServer API with enhanced features
 */
@JRubyClass(name="TCPServer", parent="TCPSocket")
public class RubyTCPServer extends RubyObject {

    private RubyTCPServerChannel serverChannel;
    private String host;
    private int port;

    public RubyTCPServer(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    static void createTCPServer(ThreadContext context, RubyClass TCPSocket) {
        org.jruby.api.Define.defineClass(context, "TCPServer", TCPSocket, RubyTCPServer::new).
                defineMethods(context, RubyTCPServer.class);
    }

    /**
     * TCPServer.new(host=nil, port, **opts)
     * Create and bind a TCP server socket
     *
     * Options:
     *   :backlog      - Maximum queue length (default: 128)
     *   :reuseaddr    - Enable SO_REUSEADDR (default: true)
     */
    @JRubyMethod(name = "initialize", required = 1, optional = 2)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        // Parse arguments
        // Forms:
        //   TCPServer.new(port)
        //   TCPServer.new(host, port)
        //   TCPServer.new(host, port, options)

        if (args.length == 1) {
            // TCPServer.new(port)
            this.host = null;
            this.port = RubyNumeric.fix2int(args[0]);
        } else {
            // TCPServer.new(host, port) or TCPServer.new(host, port, options)
            if (args[0].isNil()) {
                this.host = null;
            } else {
                this.host = args[0].convertToString().asJavaString();
            }
            this.port = RubyNumeric.fix2int(args[1]);
        }

        // Validate port
        if (port < 1 || port > 65535) {
            throw runtime.newArgumentError("invalid port: " + port);
        }

        // Parse options (if provided as last argument as hash)
        RubyHash options = null;
        if (args.length > 2 && args[args.length - 1] instanceof RubyHash) {
            options = (RubyHash) args[args.length - 1];
        }

        // Extract backlog (default: 128)
        int backlog = 128;
        if (options != null) {
            IRubyObject backlogObj = options.op_aref(context, runtime.newSymbol("backlog"));
            if (backlogObj != null && !backlogObj.isNil()) {
                backlog = RubyNumeric.fix2int(backlogObj);
            }
        }

        // Extract reuseaddr (default: true)
        boolean reuseaddr = true;
        if (options != null) {
            IRubyObject reuseaddrObj = options.op_aref(context, runtime.newSymbol("reuseaddr"));
            if (reuseaddrObj != null && !reuseaddrObj.isNil()) {
                reuseaddr = reuseaddrObj.isTrue();
            }
        }

        try {
            // Create server channel
            serverChannel = new PlainTCPServerChannel();

            // Set options
            serverChannel.setReuseAddress(reuseaddr);

            // Bind to address
            serverChannel.bind(host, port, backlog);

        } catch (IOException e) {
            throw runtime.newErrnoEADDRINUSEError(
                "Address already in use - " + (host != null ? host : "0.0.0.0") + ":" + port + ": " + e.getMessage()
            );
        } catch (Exception e) {
            throw runtime.newIOError(e.getMessage());
        }

        return this;
    }

    /**
     * accept()
     * Accept an incoming connection and return a TCPSocket for it
     */
    @JRubyMethod(name = "accept")
    public IRubyObject accept(ThreadContext context) {
        if (serverChannel == null || !serverChannel.isOpen()) {
            throw context.runtime.newIOError("closed server socket");
        }

        try {
            RubyTCPSocketChannel clientChannel = serverChannel.accept();

            // Create a new RubyTCPSocket for the accepted connection
            RubyClass socketClass = context.runtime.getClass("TCPSocket");
            RubyTCPSocket clientSocket = (RubyTCPSocket) socketClass.allocate();
            clientSocket.setChannel(clientChannel);

            return clientSocket;

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * accept_nonblock(exception: true)
     * Non-blocking accept with optional timeout
     */
    @JRubyMethod(name = "accept_nonblock", optional = 1)
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject[] args) {
        if (serverChannel == null || !serverChannel.isOpen()) {
            throw context.runtime.newIOError("closed server socket");
        }

        try {
            // Try accept with minimal timeout (0 = immediate return)
            RubyTCPSocketChannel clientChannel = serverChannel.acceptTimeout(0);

            if (clientChannel == null) {
                // No connection available
                throw context.runtime.newErrnoEAGAINError("Resource temporarily unavailable");
            }

            // Create a new RubyTCPSocket for the accepted connection
            RubyClass socketClass = context.runtime.getClass("TCPSocket");
            RubyTCPSocket clientSocket = (RubyTCPSocket) socketClass.allocate();
            clientSocket.setChannel(clientChannel);

            return clientSocket;

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * close()
     * Close the server socket
     */
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                throw context.runtime.newIOError(e.getMessage());
            }
        }
        return context.nil;
    }

    /**
     * closed?()
     * Returns true if the server socket is closed
     */
    @JRubyMethod(name = "closed?")
    public RubyBoolean closed_p(ThreadContext context) {
        if (serverChannel == null) {
            return context.runtime.getTrue();
        }
        return RubyBoolean.newBoolean(context.runtime, !serverChannel.isOpen());
    }

    /**
     * addr()
     * Returns local address information [family, port, hostname, ip]
     */
    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        try {
            InetSocketAddress addr = serverChannel.getLocalAddress();
            if (addr == null) {
                return context.nil;
            }

            RubyArray result = context.runtime.newArray();
            result.append(context.runtime.newString("AF_INET"));
            result.append(context.runtime.newFixnum(addr.getPort()));
            result.append(context.runtime.newString(addr.getHostName()));
            result.append(context.runtime.newString(addr.getAddress().getHostAddress()));
            return result;

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * listen(backlog)
     * Set the listen backlog (currently no-op for compatibility)
     * Backlog is set during bind in Java
     */
    @JRubyMethod(name = "listen", required = 1)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        // Backlog is set during bind in Java, so this is a no-op
        return context.nil;
    }
}
