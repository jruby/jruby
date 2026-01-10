package org.jruby.ext.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

/**
 * SSL/TLS TCP socket implementation
 * Uses Java SSLEngine for encrypted communication
 */
public class SSLTCPSocketChannel implements RubyTCPSocketChannel {
    private SocketChannel channel;
    private SSLEngine sslEngine;
    private ByteBuffer netInBuffer;
    private ByteBuffer netOutBuffer;
    private ByteBuffer appInBuffer;
    private ByteBuffer appOutBuffer;
    private int readTimeout = 0;
    private int writeTimeout = 0;
    private boolean handshakeComplete = false;

    public SSLTCPSocketChannel(SSLContext sslContext) throws IOException {
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(true);

        // Create SSL engine
        this.sslEngine = sslContext.createSSLEngine();
        this.sslEngine.setUseClientMode(true);

        // Initialize buffers
        initializeBuffers();
    }

    /**
     * Internal constructor for server-side accepted connections
     */
    protected SSLTCPSocketChannel(SocketChannel channel, SSLContext sslContext, boolean clientMode) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(true);

        // Create SSL engine
        this.sslEngine = sslContext.createSSLEngine();
        this.sslEngine.setUseClientMode(clientMode);

        // Initialize buffers
        initializeBuffers();
    }

    private void initializeBuffers() {
        int appBufferSize = sslEngine.getSession().getApplicationBufferSize();
        int netBufferSize = sslEngine.getSession().getPacketBufferSize();

        appInBuffer = ByteBuffer.allocate(appBufferSize);
        appOutBuffer = ByteBuffer.allocate(appBufferSize);
        netInBuffer = ByteBuffer.allocate(netBufferSize);
        netOutBuffer = ByteBuffer.allocate(netBufferSize);
    }

    @Override
    public void connect(String host, int port, int timeout) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(host, port);

        // Connect TCP
        if (timeout <= 0) {
            channel.connect(addr);
        } else {
            // Connect with timeout
            channel.configureBlocking(false);
            channel.connect(addr);
            // Wait for connection with timeout
            long startTime = System.currentTimeMillis();
            while (!channel.finishConnect()) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new IOException("Connection timeout");
                }
                Thread.yield();
            }
            channel.configureBlocking(true);
        }

        // Perform SSL handshake
        doHandshake();
    }

    private void doHandshake() throws IOException {
        sslEngine.beginHandshake();
        HandshakeStatus status = sslEngine.getHandshakeStatus();

        while (status != HandshakeStatus.FINISHED && status != HandshakeStatus.NOT_HANDSHAKING) {
            switch (status) {
                case NEED_WRAP:
                    netOutBuffer.clear();
                    SSLEngineResult wrapResult = sslEngine.wrap(appOutBuffer, netOutBuffer);
                    status = wrapResult.getHandshakeStatus();
                    netOutBuffer.flip();
                    while (netOutBuffer.hasRemaining()) {
                        channel.write(netOutBuffer);
                    }
                    break;

                case NEED_UNWRAP:
                    if (channel.read(netInBuffer) < 0) {
                        throw new SSLException("Connection closed during handshake");
                    }
                    netInBuffer.flip();
                    SSLEngineResult unwrapResult = sslEngine.unwrap(netInBuffer, appInBuffer);
                    status = unwrapResult.getHandshakeStatus();
                    netInBuffer.compact();
                    break;

                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    status = sslEngine.getHandshakeStatus();
                    break;

                default:
                    throw new IllegalStateException("Invalid SSL handshake status: " + status);
            }
        }

        handshakeComplete = true;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!handshakeComplete) {
            throw new IOException("SSL handshake not complete");
        }

        // Check if we have unwrapped data
        if (appInBuffer.position() > 0) {
            appInBuffer.flip();
            int bytesToRead = Math.min(dst.remaining(), appInBuffer.remaining());
            int oldLimit = appInBuffer.limit();
            appInBuffer.limit(appInBuffer.position() + bytesToRead);
            dst.put(appInBuffer);
            appInBuffer.limit(oldLimit);
            appInBuffer.compact();
            return bytesToRead;
        }

        // Read and unwrap
        netInBuffer.clear();
        int bytesRead = channel.read(netInBuffer);
        if (bytesRead < 0) {
            return -1;
        }

        netInBuffer.flip();
        appInBuffer.clear();
        SSLEngineResult result = sslEngine.unwrap(netInBuffer, appInBuffer);
        netInBuffer.compact();

        appInBuffer.flip();
        int bytes = Math.min(dst.remaining(), appInBuffer.remaining());
        int oldLimit = appInBuffer.limit();
        appInBuffer.limit(appInBuffer.position() + bytes);
        dst.put(appInBuffer);
        appInBuffer.limit(oldLimit);
        appInBuffer.compact();

        return bytes;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!handshakeComplete) {
            throw new IOException("SSL handshake not complete");
        }

        int totalWritten = 0;

        while (src.hasRemaining()) {
            // Wrap application data
            netOutBuffer.clear();
            SSLEngineResult result = sslEngine.wrap(src, netOutBuffer);
            totalWritten += result.bytesConsumed();

            // Write encrypted data
            netOutBuffer.flip();
            while (netOutBuffer.hasRemaining()) {
                channel.write(netOutBuffer);
            }

            if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                break;
            }
        }

        return totalWritten;
    }

    @Override
    public void close() throws IOException {
        if (sslEngine != null && handshakeComplete) {
            try {
                sslEngine.closeOutbound();
                // Send close_notify
                netOutBuffer.clear();
                sslEngine.wrap(appOutBuffer, netOutBuffer);
                netOutBuffer.flip();
                channel.write(netOutBuffer);
            } catch (Exception e) {
                // Ignore errors during close
            }
        }

        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isConnected() && handshakeComplete;
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) channel.getLocalAddress();
    }

    @Override
    public void setReadTimeout(int timeout) throws IOException {
        this.readTimeout = timeout;
        // TODO: Implement timeout for SSL
    }

    @Override
    public void setWriteTimeout(int timeout) throws IOException {
        this.writeTimeout = timeout;
        // TODO: Implement timeout for SSL
    }

    @Override
    public void setKeepAlive(boolean enabled) throws IOException {
        channel.socket().setKeepAlive(enabled);
    }

    @Override
    public void setTcpNoDelay(boolean enabled) throws IOException {
        channel.socket().setTcpNoDelay(enabled);
    }

    public SocketChannel getChannel() {
        return channel;
    }
}
