package org.jruby.ext.socket;

import java.net.*;
import java.io.*;

import org.jruby.exceptions.NotImplementedError;

/**
 * Implements TCPServer from the 'socket' library.
 *
 * @author Anders
 */

public class TCPServer extends Socket {

    private ServerSocket socket;

    public TCPServer(int port) throws IOException {
        try {
            socket = new ServerSocket(port);
        } catch (UnknownHostException e) {
            //throw new SocketError(ruby, "Unknown host");
            throw new NotImplementedError("FIXME"); // FIXME
        }
    }

    public TCPSocket accept() throws IOException {
        return new TCPSocket(socket.accept());
    }

    public void close() throws IOException {
        socket.close();
    }
}
