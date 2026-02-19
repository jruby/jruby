package org.jruby.ext.socket;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * RubyTCPSocket - Ruby wrapper for TCP sockets
 * Provides MRI-compatible TCPSocket API with enhanced features
 */
@JRubyClass(name="TCPSocket", parent="IPSocket")
public class RubyTCPSocket extends RubyBasicSocket {

    private RubyTCPSocketChannel channel;
    private String remoteHost;
    private int remotePort;

    public RubyTCPSocket(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    static RubyClass createTCPSocket(ThreadContext context, RubyClass IPSocket) {
        return org.jruby.api.Define.defineClass(context, "TCPSocket", IPSocket, RubyTCPSocket::new).
                defineMethods(context, RubyTCPSocket.class);
    }

    /**
     * TCPSocket.new(host, port, local_host=nil, local_port=nil, **opts)
     * Create and connect to a TCP socket
     *
     * Options:
     *   :connect_timeout - Connection timeout in seconds (default: no timeout)
     *   :read_timeout    - Read timeout in seconds (default: no timeout)
     *   :write_timeout   - Write timeout in seconds (default: no timeout)
     *   :keepalive       - Enable TCP keepalive (default: false)
     *   :nodelay         - Disable Nagle's algorithm (default: false)
     */
    @JRubyMethod(name = "initialize", required = 2, optional = 3)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        // Parse arguments
        this.remoteHost = args[0].convertToString().asJavaString();
        this.remotePort = RubyNumeric.fix2int(args[1]);

        // Validate inputs
        if (remoteHost == null || remoteHost.trim().isEmpty()) {
            throw context.runtime.newArgumentError("invalid host");
        }
        if (remotePort < 1 || remotePort > 65535) {
            throw context.runtime.newArgumentError("invalid port: " + remotePort);
        }

        // Parse options (if provided as last argument as hash)
        RubyHash options = null;
        if (args.length > 2 && args[args.length - 1] instanceof RubyHash) {
            options = (RubyHash) args[args.length - 1];
        }

        try {
            // Create channel
            channel = new PlainTCPSocketChannel();

            // Apply options
            if (options != null) {
                applyOptions(context, options);
            }

            // Extract connect timeout
            int connectTimeout = 0;
            if (options != null) {
                IRubyObject timeoutObj = options.op_aref(context,
                    context.runtime.newSymbol("connect_timeout"));
                if (timeoutObj != null && !timeoutObj.isNil()) {
                    connectTimeout = (int)(RubyNumeric.num2dbl(timeoutObj) * 1000);
                }
            }

            // Connect
            channel.connect(remoteHost, remotePort, connectTimeout);

        } catch (IOException e) {
            throw context.runtime.newErrnoECONNREFUSEDError(
                "Connection refused - " + remoteHost + ":" + remotePort + ": " + e.getMessage()
            );
        } catch (Exception e) {
            throw context.runtime.newIOError(e.getMessage());
        }

        return this;
    }

    /**
     * Apply socket options from hash
     */
    private void applyOptions(ThreadContext context, RubyHash options) throws IOException {
        Ruby runtime = context.runtime;

        // Read timeout
        IRubyObject readTimeout = options.op_aref(context, runtime.newSymbol("read_timeout"));
        if (readTimeout != null && !readTimeout.isNil()) {
            int timeout = (int)(RubyNumeric.num2dbl(readTimeout) * 1000);
            channel.setReadTimeout(timeout);
        }

        // Write timeout
        IRubyObject writeTimeout = options.op_aref(context, runtime.newSymbol("write_timeout"));
        if (writeTimeout != null && !writeTimeout.isNil()) {
            int timeout = (int)(RubyNumeric.num2dbl(writeTimeout) * 1000);
            channel.setWriteTimeout(timeout);
        }

        // Keepalive
        IRubyObject keepalive = options.op_aref(context, runtime.newSymbol("keepalive"));
        if (keepalive != null && keepalive.isTrue()) {
            channel.setKeepAlive(true);
        }

        // TCP No Delay
        IRubyObject nodelay = options.op_aref(context, runtime.newSymbol("nodelay"));
        if (nodelay != null && nodelay.isTrue()) {
            channel.setTcpNoDelay(true);
        }
    }

    /**
     * recv(maxlen, flags=0)
     * Receive up to maxlen bytes from the socket
     */
    @JRubyMethod(name = "recv", required = 1, optional = 1)
    public IRubyObject recv(ThreadContext context, IRubyObject[] args) {
        int maxlen = RubyNumeric.fix2int(args[0]);

        // Validate maxlen
        if (maxlen < 0) {
            throw context.runtime.newArgumentError("negative length");
        }
        if (maxlen == 0) {
            return RubyString.newEmptyString(context.runtime);
        }

        if (channel == null || !channel.isOpen()) {
            throw context.runtime.newIOError("closed stream");
        }

        try {
            ByteBuffer buffer = ByteBuffer.allocate(maxlen);
            int bytesRead = channel.read(buffer);

            if (bytesRead == -1) {
                return context.nil;
            }
            if (bytesRead == 0) {
                return RubyString.newEmptyString(context.runtime);
            }

            buffer.flip();
            byte[] data = new byte[bytesRead];
            buffer.get(data);

            return RubyString.newString(context.runtime, new ByteList(data, false));

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * send(mesg, flags=0)
     * Send a message over the socket
     */
    @JRubyMethod(name = "send", required = 1, optional = 1)
    public IRubyObject send(ThreadContext context, IRubyObject[] args) {
        if (channel == null || !channel.isOpen()) {
            throw context.runtime.newIOError("closed stream");
        }

        RubyString message = args[0].convertToString();
        byte[] data = message.getBytes();

        if (data.length == 0) {
            return context.runtime.newFixnum(0);
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int totalWritten = 0;

            // Handle partial writes
            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    break;
                }
                totalWritten += written;
            }

            return context.runtime.newFixnum(totalWritten);

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * read(length=nil)
     * Read length bytes, or all available if length is nil
     */
    @JRubyMethod(name = "read", optional = 1)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        if (args.length == 0) {
            // Read all available (up to 64KB)
            return recv(context, new IRubyObject[]{context.runtime.newFixnum(65536)});
        }
        return recv(context, args);
    }

    /**
     * write(string)
     * Write string to socket
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject string) {
        return send(context, new IRubyObject[]{string});
    }

    /**
     * close()
     * Close the socket connection
     */
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                throw context.runtime.newIOError(e.getMessage());
            }
        }
        return context.nil;
    }

    /**
     * closed?()
     * Returns true if the socket is closed
     */
    @JRubyMethod(name = "closed?")
    public RubyBoolean closed_p(ThreadContext context) {
        if (channel == null) {
            return context.runtime.getTrue();
        }
        return RubyBoolean.newBoolean(context.runtime, !channel.isOpen());
    }

    /**
     * peeraddr()
     * Returns remote address information [family, port, hostname, ip]
     */
    @JRubyMethod(name = "peeraddr")
    public IRubyObject peeraddr(ThreadContext context) {
        try {
            InetSocketAddress addr = channel.getRemoteAddress();
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
     * addr()
     * Returns local address information [family, port, hostname, ip]
     */
    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        try {
            InetSocketAddress addr = channel.getLocalAddress();
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
     * Internal method to set the channel (used by TCPServer.accept)
     */
    public void setChannel(RubyTCPSocketChannel channel) {
        this.channel = channel;
    }
}
