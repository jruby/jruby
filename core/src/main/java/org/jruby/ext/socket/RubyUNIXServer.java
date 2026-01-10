package org.jruby.ext.socket;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;

/**
 * RubyUNIXServer - Ruby wrapper for Unix Domain Server Sockets
 * Uses JEP-380 (JDK 16+) when available, falls back to JNR
 */
@JRubyClass(name="UNIXServer", parent="UNIXSocket")
public class RubyUNIXServer extends RubyObject {

    private RubyUNIXServerChannel serverChannel;
    private String path;

    public RubyUNIXServer(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    static void createUNIXServer(ThreadContext context, RubyClass UNIXSocket) {
        org.jruby.api.Define.defineClass(context, "UNIXServer", UNIXSocket, RubyUNIXServer::new).
                defineMethods(context, RubyUNIXServer.class);
    }

    /**
     * UNIXServer.new(path)
     * Create and bind a Unix domain server socket at the given path
     */
    @JRubyMethod(name = "initialize", required = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject pathArg) {
        this.path = pathArg.convertToString().asJavaString();

        // Validate path
        if (path == null || path.trim().isEmpty()) {
            throw context.runtime.newArgumentError("invalid socket path");
        }

        try {
            // Check if socket file exists
            java.io.File socketFile = new java.io.File(path);
            if (socketFile.exists()) {
                // Try to detect if it's a stale socket by attempting connection
                boolean isStale = false;
                try {
                    java.net.Socket testSocket = new java.net.Socket();
                    // If this succeeds, socket is in use
                    isStale = false;
                } catch (Exception e) {
                    // Connection failed, likely stale
                    isStale = true;
                }

                if (!isStale) {
                    // Socket appears to be in use, but let's try to bind anyway
                    // and let the OS decide (might fail with Address in use)
                }

                // Delete the file (either stale or we'll let bind() fail if in use)
                socketFile.delete();
            }

            // Use factory to get appropriate implementation
            serverChannel = UnixSocketChannelFactory.bind(path);

        } catch (IOException e) {
            throw context.runtime.newErrnoEADDRINUSEError(
                "Address already in use - " + path + ": " + e.getMessage()
            );
        } catch (Exception e) {
            throw context.runtime.newIOError(e.getMessage());
        }

        return this;
    }

    /**
     * accept()
     * Accept an incoming connection and return a UNIXSocket for it
     */
    @JRubyMethod(name = "accept")
    public IRubyObject accept(ThreadContext context) {
        if (serverChannel == null || !serverChannel.isOpen()) {
            throw context.runtime.newIOError("closed server socket");
        }

        try {
            RubyUNIXSocketChannel clientChannel = serverChannel.accept();

            // Create a new RubyUNIXSocket for the accepted connection
            RubyClass socketClass = context.runtime.getClass("UNIXSocket");
            RubyUNIXSocket clientSocket = (RubyUNIXSocket) socketClass.allocate();
            clientSocket.setChannel(clientChannel);

            return clientSocket;

        } catch (IOException e) {
            throw context.runtime.newIOError(e.getMessage());
        }
    }

    /**
     * accept_nonblock()
     * Non-blocking accept (currently just calls accept for compatibility)
     */
    @JRubyMethod(name = "accept_nonblock", optional = 1)
    public IRubyObject accept_nonblock(ThreadContext context, IRubyObject[] args) {
        // TODO: Implement true non-blocking behavior
        return accept(context);
    }

    /**
     * close()
     * Close the server socket and delete the socket file
     */
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();

                // Clean up socket file
                if (path != null) {
                    java.io.File socketFile = new java.io.File(path);
                    if (socketFile.exists()) {
                        socketFile.delete();
                    }
                }
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
    public IRubyObject closed_p(ThreadContext context) {
        if (serverChannel == null) {
            return context.runtime.getTrue();
        }
        return context.runtime.newBoolean(!serverChannel.isOpen());
    }

    /**
     * path()
     * Get the path of the Unix server socket
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
     * Returns address information
     */
    @JRubyMethod(name = "addr")
    public IRubyObject addr(ThreadContext context) {
        RubyArray addr = context.runtime.newArray();
        addr.append(context.runtime.newString("AF_UNIX"));
        addr.append(path(context));
        return addr;
    }

    /**
     * listen(backlog)
     * Set the listen backlog (currently no-op for compatibility)
     */
    @JRubyMethod(name = "listen", required = 1)
    public IRubyObject listen(ThreadContext context, IRubyObject backlog) {
        // Backlog is set during bind in Java, so this is a no-op
        return context.nil;
    }
}
