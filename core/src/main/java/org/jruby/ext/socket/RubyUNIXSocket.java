package org.jruby.ext.socket;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * RubyUNIXSocket - Ruby wrapper for Unix Domain Sockets
 * Uses JEP-380 (JDK 16+) when available, falls back to JNR
 */
@JRubyClass(name="UNIXSocket", parent="BasicSocket")
public class RubyUNIXSocket extends RubyBasicSocket {

    private RubyUNIXSocketChannel channel;
    private String path;

    public RubyUNIXSocket(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    static RubyClass createUNIXSocket(ThreadContext context, RubyClass BasicSocket) {
        return org.jruby.api.Define.defineClass(context, "UNIXSocket", BasicSocket, RubyUNIXSocket::new).
                defineMethods(context, RubyUNIXSocket.class);
    }

    /**
     * UNIXSocket.new(path)
     * Connect to Unix domain socket at the given path
     */
    @JRubyMethod(name = "initialize", required = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        String socketPath = path.convertToString().asJavaString();

        // Validate path
        if (socketPath == null || socketPath.trim().isEmpty()) {
            throw context.runtime.newArgumentError("invalid socket path");
        }

        this.path = socketPath;

        try {
            // Use factory to get appropriate implementation
            channel = UnixSocketChannelFactory.connect(socketPath);

        } catch (IOException e) {
            throw context.runtime.newErrnoECONNREFUSEDError(
                "Connection refused - " + socketPath + ": " + e.getMessage()
            );
        } catch (Exception e) {
            throw context.runtime.newIOError(e.getMessage());
        }

        return this;
    }

    /**
     * recv(maxlen)
     * Receive up to maxlen bytes from the socket
     */
    @JRubyMethod(name = "recv", required = 1)
    public IRubyObject recv(ThreadContext context, IRubyObject length) {
        int maxlen = RubyNumeric.fix2int(length);

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
     * send(mesg, flags)
     * Send a message over the socket
     * flags parameter is accepted for compatibility but currently ignored
     */
    @JRubyMethod(name = "send", required = 2)
    public IRubyObject send(ThreadContext context, IRubyObject mesg, IRubyObject flags) {
        if (channel == null || !channel.isOpen()) {
            throw context.runtime.newIOError("closed stream");
        }

        RubyString message = mesg.convertToString();
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
                    // No progress, might be blocking issue
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
     * read(length)
     * Read exactly length bytes (alias for recv)
     */
    @JRubyMethod(name = "read", optional = 1)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        if (args.length == 0) {
            // Read all available
            return recv(context, context.runtime.newFixnum(8192));
        }
        return recv(context, args[0]);
    }

    /**
     * write(string)
     * Write string to socket (alias for send with flags=0)
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject string) {
        return send(context, string, context.runtime.newFixnum(0));
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
     * path()
     * Get the path of the Unix socket
     */
    @JRubyMethod(name = "path")
    public IRubyObject path(ThreadContext context) {
        if (path == null) {
            return context.nil;
        }
        return context.runtime.newString(path);
    }

    /**
     * addr()
     * Returns address information (for compatibility with Socket API)
     */
    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        RubyArray addr = context.runtime.newArray();
        addr.append(context.runtime.newString("AF_UNIX"));
        addr.append(path(context));
        return addr;
    }

    /**
     * peeraddr()
     * Returns peer address information
     */
    @JRubyMethod(name = "peeraddr")
    public IRubyObject peeraddr(ThreadContext context) {
        return addr(context);
    }

    /**
     * Internal method to set the channel (used by UNIXServer.accept)
     */
    public void setChannel(RubyUNIXSocketChannel channel) {
        this.channel = channel;
        // Note: path remains null for accepted connections (peer socket)
    }
}
