
package org.jruby.ext.socket;

import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

import org.jruby.exceptions.NotImplementedError;

/**
 * Implements TCPSocket from the 'socket' library.
 *
 * @author Anders
 */

public class TCPSocket extends Socket {

    private java.net.Socket socket;
    private OutputStream output;
    private InputStream input;

    protected TCPSocket(java.net.Socket socket) throws IOException {
        this.socket = socket;
        this.output = socket.getOutputStream();
        this.input = socket.getInputStream();
    }

    public TCPSocket(String name, int port) throws IOException {
        try {
            // Can't call another constructor within try-catch, that's why
            // we repeat ourselves.
            this.socket = new java.net.Socket(name, port);
            this.output = socket.getOutputStream();
            this.input = socket.getInputStream();
        } catch (UnknownHostException e) {
            //throw new SocketError(ruby, "Unknown host");
            throw new NotImplementedError("FIXME"); // FIXME
        }
    }

    public static String getaddress(String hostName) {
        InetAddress address;
        try {
            address = InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            //throw new SocketError(ruby, "getaddrinfo: No address associated with hostname");
            return null; // FIXME: remove me
        }
        return address.getHostAddress();
    }

    public static List gethostbyname(String hostName) {
        List result = new ArrayList(4);
        result.add(null);
        result.add(null);
        result.add(new Integer(AF_INET));
        result.add(getaddress(hostName));
        return result;
    }

    public int send(String data, int flags) throws IOException {
        byte[] byteData = data.getBytes(); // FIXME: locale dependency
        output.write(byteData);
        return byteData.length;
    }

    public String recv(int size) throws IOException {
        int actualSize = Math.min(input.available(), size);
        byte[] result = new byte[actualSize];
        int bytesRead = input.read(result);
        if (bytesRead != actualSize) {
            throw new IOException("unexpected size read");
        }
        return new String(result);
    }

    public void close() throws IOException {
        socket.close();
    }
}
