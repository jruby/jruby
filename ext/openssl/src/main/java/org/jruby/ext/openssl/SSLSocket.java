/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.X509Utils;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.SSL._SSL;
import static org.jruby.ext.openssl.SSL.newSSLErrorWaitReadable;
import static org.jruby.ext.openssl.SSL.newSSLErrorWaitWritable;
import static org.jruby.ext.openssl.OpenSSLReal.debug;
import static org.jruby.ext.openssl.OpenSSLReal.debugStackTrace;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSLSocket extends RubyObject {

    private static final long serialVersionUID = 5170831655279559707L;

    private static ObjectAllocator SSLSOCKET_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new SSLSocket(runtime, klass);
        }
    };

    public static void createSSLSocket(final Ruby runtime, final RubyModule _SSL) { // OpenSSL::SSL
        final ThreadContext context = runtime.getCurrentContext();
        RubyClass _SSLSocket = _SSL.defineClassUnder("SSLSocket", runtime.getObject(), SSLSOCKET_ALLOCATOR);
        _SSLSocket.addReadWriteAttribute(context, "io");
        _SSLSocket.addReadWriteAttribute(context, "context");
        _SSLSocket.addReadWriteAttribute(context, "sync_close");
        _SSLSocket.addReadWriteAttribute(context, "hostname");
        _SSLSocket.defineAlias("to_io", "io");
        _SSLSocket.defineAnnotatedMethods(SSLSocket.class);
    }

    public SSLSocket(Ruby runtime, RubyClass type) {
        super(runtime,type);
        verifyResult = X509Utils.V_OK;
    }

    private static RaiseException newSSLError(Ruby runtime, Exception exception) {
        return SSL.newSSLError(runtime, exception);
    }

    private static RaiseException newSSLError(Ruby runtime, String message) {
        return SSL.newSSLError(runtime, message);
    }

    private org.jruby.ext.openssl.SSLContext sslContext;
    private SSLEngine engine;
    private RubyIO io;

    private ByteBuffer peerAppData;
    private ByteBuffer peerNetData;
    private ByteBuffer netData;
    private ByteBuffer dummy;

    private boolean initialHandshake = false;

    private SSLEngineResult.HandshakeStatus hsStatus;
    private SSLEngineResult.Status status = null;

    int verifyResult;

    @JRubyMethod(name = "initialize", rest = true, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context,
        final IRubyObject[] args, final Block unused) {
        final Ruby runtime = context.runtime;

        if ( Arity.checkArgumentCount(runtime, args, 1, 2) == 1 ) {
            RubyModule _SSLContext = _SSL(runtime).getClass("SSLContext");
            sslContext = (SSLContext) _SSLContext.callMethod(context, "new");
        } else {
            sslContext = (SSLContext) args[1];
        }

        if ( ! ( args[0] instanceof RubyIO ) ) {
            throw runtime.newTypeError("IO expected but got " + args[0].getMetaClass().getName());
        }
        io = (RubyIO) args[0];
        this.callMethod(context, "io=", io);
        this.callMethod(context, "hostname=", runtime.newString(""));
        // This is a bit of a hack: SSLSocket should share code with RubyBasicSocket, which always sets sync to true.
        // Instead we set it here for now.
        io.callMethod(context, "sync=", runtime.getTrue());
        this.callMethod(context, "context=", sslContext);
        this.callMethod(context, "sync_close=", runtime.getFalse());
        sslContext.setup(context);
        return Utils.invokeSuper(context, this, args, unused); // super()
    }

    private void ossl_ssl_setup(final ThreadContext context)
        throws NoSuchAlgorithmException, KeyManagementException, IOException {
        if ( engine == null ) {
            final Socket socket = getSocketChannel().socket();
            // Server Name Indication (SNI) RFC 3546
            // SNI support will not be attempted unless hostname is explicitly set by the caller
            String peerHost = this.callMethod(context, "hostname").toString();
            int peerPort = socket.getPort();
            engine = sslContext.createSSLEngine(peerHost, peerPort);
            final SSLSession session = engine.getSession();
            peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
            peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
            netData = ByteBuffer.allocate(session.getPacketBufferSize());
            peerNetData.limit(0);
            peerAppData.limit(0);
            netData.limit(0);
            dummy = ByteBuffer.allocate(0);
        }
    }

    @JRubyMethod
    public IRubyObject connect(ThreadContext context) {
        return connectCommon(context, true);
    }

    @JRubyMethod
    public IRubyObject connect_nonblock(ThreadContext context) {
        return connectCommon(context, false);
    }

    private IRubyObject connectCommon(final ThreadContext context, boolean blocking) {
        final Ruby runtime = context.runtime;

        if ( ! sslContext.isProtocolForClient() ) {
            throw newSSLError(runtime, "called a function you should not call");
        }

        try {
            if ( ! initialHandshake ) {
                ossl_ssl_setup(context);
                engine.setUseClientMode(true);
                engine.beginHandshake();
                hsStatus = engine.getHandshakeStatus();
                initialHandshake = true;
            }
            doHandshake(blocking);
        }
        catch (SSLHandshakeException e) {
            // unlike server side, client should close outbound channel even if
            // we have remaining data to be sent.
            forceClose();
            Exception cause = e;
            while (cause.getCause() != null && (cause instanceof SSLHandshakeException)) {
                cause = (Exception) cause.getCause();
            }
            throw newSSLError(runtime, cause);
        }
        catch (NoSuchAlgorithmException e) {
            forceClose();
            throw newSSLError(runtime, e);
        }
        catch (KeyManagementException e) {
            forceClose();
            throw newSSLError(runtime, e);
        }
        catch (IOException e) {
            forceClose();
            throw newSSLError(runtime, e);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject accept(ThreadContext context) {
        return acceptCommon(context, true);
    }

    @JRubyMethod
    public IRubyObject accept_nonblock(ThreadContext context) {
        return acceptCommon(context, false);
    }

    public IRubyObject acceptCommon(final ThreadContext context, boolean blocking) {
        final Ruby runtime = context.runtime;

        if ( ! sslContext.isProtocolForServer() ) {
            throw newSSLError(runtime, "called a function you should not call");
        }

        try {
            if ( ! initialHandshake ) {
                ossl_ssl_setup(context);
                engine.setUseClientMode(false);
                final IRubyObject verify_mode;
                if( ! sslContext.isNil() &&
                    ! ( verify_mode = sslContext.callMethod(context, "verify_mode") ).isNil() ) {
                    final int vfy = RubyNumeric.fix2int(verify_mode);
                    if ( vfy == 0 ) { // VERIFY_NONE
                        engine.setNeedClientAuth(false);
                        engine.setWantClientAuth(false);
                    }
                    if ( ( vfy & 1 ) != 0 ) { // VERIFY_PEER
                        engine.setWantClientAuth(true);
                    }
                    if ( ( vfy & 2 ) != 0 ) { // VERIFY_FAIL_IF_NO_PEER_CERT
                        engine.setNeedClientAuth(true);
                    }
                }
                engine.beginHandshake();
                hsStatus = engine.getHandshakeStatus();
                initialHandshake = true;
            }
            doHandshake(blocking);
        }
        catch (SSLHandshakeException e) {
            throw newSSLError(runtime, e);
        }
        catch (NoSuchAlgorithmException e) {
            throw newSSLError(runtime, e);
        }
        catch (KeyManagementException e) {
            throw newSSLError(runtime, e);
        }
        catch (IOException e) {
            throw newSSLError(runtime, e);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject verify_result(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        if (engine == null) {
            runtime.getWarnings().warn("SSL session is not started yet.");
            return runtime.getNil();
        }
        return runtime.newFixnum(verifyResult);
    }

    // This select impl is a copy of RubyThread.select, then blockingLock is
    // removed. This impl just set
    // SelectableChannel.configureBlocking(false) permanently instead of setting
    // temporarily. SSLSocket requires wrapping IO to be selectable so it should
    // be OK to set configureBlocking(false) permanently.
    private boolean waitSelect(final int operations, final boolean blocking) throws IOException {
        if ( ! ( io.getChannel() instanceof SelectableChannel ) ) {
            return true;
        }
        final Ruby runtime = getRuntime();
        final RubyThread thread = runtime.getCurrentContext().getThread();

        SelectableChannel selectable = (SelectableChannel)io.getChannel();
        selectable.configureBlocking(false);
        final Selector selector = runtime.getSelectorPool().get();
        final SelectionKey key = selectable.register(selector, operations);

        try {
            io.addBlockingThread(thread);

            final int[] result = new int[1];

            thread.executeBlockingTask(new RubyThread.BlockingTask() {
                public void run() throws InterruptedException {
                    try {
                        if (!blocking) {
                            result[0] = selector.selectNow();
                            if (result[0] == 0) {
                                if ((operations & SelectionKey.OP_READ) != 0 && (operations & SelectionKey.OP_WRITE) != 0) {
                                    if (key.isReadable()) {
                                        writeWouldBlock();
                                    } else if (key.isWritable()) {
                                        readWouldBlock();
                                    } else { //neither, pick one
                                        readWouldBlock();
                                    }
                                } else if ((operations & SelectionKey.OP_READ) != 0) {
                                    readWouldBlock();
                                } else if ((operations & SelectionKey.OP_WRITE) != 0) {
                                    writeWouldBlock();
                                }
                            }
                        } else {
                            result[0] = selector.select();
                        }
                    } catch (IOException ioe) {
                        throw runtime.newRuntimeError("Error with selector: " + ioe.getMessage());
                    }
                }

                public void wakeup() {
                    selector.wakeup();
                }
            });

            if (result[0] >= 1) {
                Set<SelectionKey> keySet = selector.selectedKeys();

                if (keySet.iterator().next() == key) {
                    return true;
                }
            }

            return false;
        }
        catch (InterruptedException ie) {
            return false;
        }
        finally {
            // Note: I don't like ignoring these exceptions, but it's
            // unclear how likely they are to happen or what damage we
            // might do by ignoring them. Note that the pieces are separate
            // so that we can ensure one failing does not affect the others
            // running.

            // clean up the key in the selector
            try {
                if ( key != null ) key.cancel();
                if ( selector != null ) selector.selectNow();
            }
            catch (Exception e) { // ignore
                debugStackTrace(getRuntime(), e);
            }

            // shut down and null out the selector
            try {
                if ( selector != null ) {
                    runtime.getSelectorPool().put(selector);
                }
            }
            catch (Exception e) { // ignore
                debugStackTrace(getRuntime(), e);
            }

            // remove this thread as a blocker against the given IO
            io.removeBlockingThread(thread);

            // clear thread state from blocking call
            thread.afterBlockingCall();
        }
    }

    private void readWouldBlock() {
        throw newSSLErrorWaitReadable(getRuntime(), "read would block");
    }

    private void writeWouldBlock() {
        throw newSSLErrorWaitWritable(getRuntime(), "write would block");
    }

    private void doHandshake(boolean blocking) throws IOException {
        while (true) {
            SSLEngineResult res;
            boolean ready = waitSelect(SelectionKey.OP_READ | SelectionKey.OP_WRITE, blocking);

            // if not blocking, raise EAGAIN
            if ( ! blocking && ! ready ) {
                throw getRuntime().newErrnoEAGAINError("Resource temporarily unavailable");
            }

            // otherwise, proceed as before

            switch (hsStatus) {
            case FINISHED:
                if ( initialHandshake ) finishInitialHandshake();
                return;
            case NEED_TASK:
                doTasks();
                break;
            case NEED_UNWRAP:
                if (readAndUnwrap(blocking) == -1 && hsStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
                    throw new SSLHandshakeException("Socket closed");
                }
                // during initialHandshake, calling readAndUnwrap that results UNDERFLOW
                // does not mean writable. we explicitly wait for readable channel to avoid
                // busy loop.
                if (initialHandshake && status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    waitSelect(SelectionKey.OP_READ, blocking);
                }
                break;
            case NEED_WRAP:
                if ( netData.hasRemaining() ) {
                    while ( flushData(blocking) ) { /* loop */ }
                }
                netData.clear();
                res = engine.wrap(dummy, netData);
                hsStatus = res.getHandshakeStatus();
                netData.flip();
                flushData(blocking);
                break;
            case NOT_HANDSHAKING:
                // Opposite side could close while unwrapping. Handle this as same as FINISHED
                return;
            default:
                throw new IllegalStateException("Unknown handshaking status: " + hsStatus);
            }
        }
    }

    private void doTasks() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            task.run();
        }
        hsStatus = engine.getHandshakeStatus();
        verifyResult = sslContext.getLastVerifyResult();
    }

    private boolean flushData(boolean blocking) throws IOException {
        try {
            writeToChannel(netData, blocking);
        }
        catch (IOException ioe) {
            netData.position(netData.limit());
            throw ioe;
        }
        if ( netData.hasRemaining() ) {
            return true;
        }
        return false;
    }

    private int writeToChannel(ByteBuffer buffer, boolean blocking) throws IOException {
        int totalWritten = 0;
        while ( buffer.hasRemaining() ) {
            totalWritten += getSocketChannel().write(buffer);
            if ( ! blocking ) break; // don't continue attempting to read
        }
        return totalWritten;
    }

    private void finishInitialHandshake() {
        initialHandshake = false;
    }

    public int write(ByteBuffer src, boolean blocking) throws SSLException, IOException {
        if ( initialHandshake ) {
            throw new IOException("Writing not possible during handshake");
        }

        SelectableChannel selectable = getSocketChannel();
        boolean blockingMode = selectable.isBlocking();
        if ( ! blocking ) selectable.configureBlocking(false);

        try {
            if ( netData.hasRemaining() ) {
                flushData(blocking);
            }
            netData.clear();
            SSLEngineResult res = engine.wrap(src, netData);
            if (res.getStatus()==SSLEngineResult.Status.CLOSED) {
            	  throw getRuntime().newIOError("closed SSL engine");
            }
            netData.flip();
            flushData(blocking);
            return res.bytesConsumed();
        }
        finally {
            if ( ! blocking ) selectable.configureBlocking(blockingMode);
        }
    }

    public int read(ByteBuffer dst, boolean blocking) throws IOException {
        if ( initialHandshake ) return 0;
        if ( engine.isInboundDone() ) return -1;

        if ( ! peerAppData.hasRemaining() ) {
            int appBytesProduced = readAndUnwrap(blocking);
            if (appBytesProduced == -1 || appBytesProduced == 0) {
                return appBytesProduced;
            }
        }
        int limit = Math.min(peerAppData.remaining(), dst.remaining());
        peerAppData.get(dst.array(), dst.arrayOffset(), limit);
        dst.position(dst.arrayOffset() + limit);
        return limit;
    }

    private int readAndUnwrap(boolean blocking) throws IOException {
        int bytesRead = getSocketChannel().read(peerNetData);
        if (bytesRead == -1) {
            if (!peerNetData.hasRemaining() || (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)) {
                closeInbound();
                return -1;
            }
            // inbound channel has been already closed but closeInbound() must
            // be defered till the last engine.unwrap() call.
            // peerNetData could not be empty.
        }
        peerAppData.clear();
        peerNetData.flip();
        SSLEngineResult res;
        do {
            res = engine.unwrap(peerNetData, peerAppData);
        } while (res.getStatus() == SSLEngineResult.Status.OK &&
				res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP &&
				res.bytesProduced() == 0);
        if(res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
            finishInitialHandshake();
        }
        if(peerAppData.position() == 0 &&
            res.getStatus() == SSLEngineResult.Status.OK &&
            peerNetData.hasRemaining()) {
            res = engine.unwrap(peerNetData, peerAppData);
        }
        status = res.getStatus();
        hsStatus = res.getHandshakeStatus();
        if ( bytesRead == -1 && !peerNetData.hasRemaining() ) {
            // now it's safe to call closeInbound().
            closeInbound();
        }
        if ( status == SSLEngineResult.Status.CLOSED ) {
            doShutdown();
            return -1;
        }
        peerNetData.compact();
        peerAppData.flip();
        if(!initialHandshake && (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK ||
                                 hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP ||
                                 hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)) {
            doHandshake(blocking);
        }
        return peerAppData.remaining();
    }

    private void closeInbound() {
        try {
            engine.closeInbound();
        }
        catch (SSLException e) {
            debug(getRuntime(), "SSLSocket.closeInbound", e);
            // ignore any error on close. possibly an error like this;
            // Inbound closed before receiving peer's close_notify: possible truncation attack?
        }
    }

    private void doShutdown() throws IOException {
        if ( engine.isOutboundDone() ) return;

        netData.clear();
        try {
            engine.wrap(dummy, netData);
        }
        catch (SSLException e) {
            debug(getRuntime(), "SSLSocket.doShutdown", e);
            return;
        }
        catch (RuntimeException e) {
            debugStackTrace(getRuntime(), e);
            return;
        }
        netData.flip();
        flushData(true);
    }

    private IRubyObject do_sysread(final ThreadContext context,
        final IRubyObject[] args, final boolean blocking) {
        final Ruby runtime = context.runtime;

        final int len = RubyNumeric.fix2int(args[0]);
        final RubyString buff;

        if ( args.length == 2 && ! args[1].isNil() ) {
            buff = args[1].asString();
        } else {
            buff = runtime.newString();
        }
        if ( len == 0 ) {
            buff.clear();
            return buff;
        }
        if ( len < 0 ) {
            throw runtime.newArgumentError("negative string size (or size too big)");
        }

        try {
            // So we need to make sure to only block when there is no data left to process
            if ( engine == null || ! ( peerAppData.hasRemaining() || peerNetData.position() > 0 ) ) {
                waitSelect(SelectionKey.OP_READ, blocking);
            }

            ByteBuffer dst = ByteBuffer.allocate(len);
            int rr = -1;
            // ensure >0 bytes read; sysread is blocking read.
            while ( rr <= 0 ) {
                if ( engine == null ) {
                    rr = getSocketChannel().read(dst);
                } else {
                    rr = read(dst, blocking);
                }

                if ( rr == -1 ) throw runtime.newEOFError();

                if ( rr == 0 && status == SSLEngineResult.Status.BUFFER_UNDERFLOW ) {
                    // If we didn't get any data back because we only read in a partial TLS record,
                    // instead of spinning until the rest comes in, call waitSelect to either block
                    // until the rest is available, or throw a "read would block" error if we are in
                    // non-blocking mode.
                    waitSelect(SelectionKey.OP_READ, blocking);
                }
            }
            byte[] bss = new byte[rr];
            dst.position(dst.position() - rr);
            dst.get(bss);
            buff.setValue(new ByteList(bss, false));
            return buff;
        }
        catch (IOException ioe) {
            throw runtime.newIOError(ioe.getMessage());
        }
    }

    @JRubyMethod(rest = true, required = 1, optional = 1)
    public IRubyObject sysread(ThreadContext context, IRubyObject[] args) {
        return do_sysread(context, args, true);
    }

    @JRubyMethod(rest = true, required = 1, optional = 2)
    public IRubyObject sysread_nonblock(ThreadContext context, IRubyObject[] args) {
        // TODO: options for exception raising
        return do_sysread(context, args, false);
    }

    private IRubyObject do_syswrite(final ThreadContext context,
        final IRubyObject arg, final boolean blocking)  {
        final Ruby runtime = context.runtime;
        try {
            checkClosed();

            waitSelect(SelectionKey.OP_WRITE, blocking);

            ByteList bls = arg.asString().getByteList();
            ByteBuffer b1 = ByteBuffer.wrap(bls.getUnsafeBytes(), bls.getBegin(), bls.getRealSize());
            final int written;
            if ( engine == null ) {
                written = writeToChannel(b1, blocking);
            } else {
                written = write(b1, blocking);
            }

            this.callMethod(context, "io").callMethod(context, "flush");

            return runtime.newFixnum(written);
        }
        catch (IOException ioe) {
            throw runtime.newIOError(ioe.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject syswrite(ThreadContext context, IRubyObject arg) {
        return do_syswrite(context, arg, true);
    }

    @JRubyMethod
    public IRubyObject syswrite_nonblock(ThreadContext context, IRubyObject arg) {
        return do_syswrite(context, arg, false);
    }

    @JRubyMethod
    public IRubyObject syswrite_nonblock(ThreadContext context, IRubyObject arg, IRubyObject options) {
        // TODO: options for exception raising
        return do_syswrite(context, arg, false);
    }

    private void checkClosed() {
        if ( ! getSocketChannel().isOpen() ) {
            throw getRuntime().newIOError("closed stream");
        }
    }

    // do shutdown even if we have remaining data to be sent.
    // call this when you get an exception from client side.
    private void forceClose() {
        close(true);
    }

    private void close(boolean force)  {
        if ( engine == null ) throw getRuntime().newEOFError();

        engine.closeOutbound();

        if ( ! force && netData.hasRemaining() ) return;

        try {
            doShutdown();
        }
        catch (IOException e) {
            // ignore?
            debug(getRuntime(), "SSLSocket.close doShutdown failed", e);
        }
    }

    @JRubyMethod
    public IRubyObject sysclose(final ThreadContext context) {
        // no need to try shutdown when it's a server
        close( sslContext.isProtocolForClient() );

        if ( this.callMethod(context, "sync_close").isTrue() ) {
            this.callMethod(context, "io").callMethod(context, "close");
        }
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject cert() {
        final Ruby runtime = getRuntime();
        if ( engine == null ) return runtime.getNil();

        try {
            Certificate[] cert = engine.getSession().getLocalCertificates();
            if ( cert != null && cert.length > 0 ) {
                return X509Cert.wrap(runtime, cert[0]);
            }
        }
        catch (CertificateEncodingException e) {
            throw X509Cert.newCertificateError(runtime, e);
        }
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject peer_cert() {
        final Ruby runtime = getRuntime();
        if ( engine == null ) return runtime.getNil();

        try {
            Certificate[] cert = engine.getSession().getPeerCertificates();
            if ( cert.length > 0 ) {
                return X509Cert.wrap(runtime, cert[0]);
            }
        }
        catch (CertificateEncodingException e) {
            throw X509Cert.newCertificateError(runtime, e);
        }
        catch (SSLPeerUnverifiedException e) {
            if (runtime.isVerbose()) {
                runtime.getWarnings().warning(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject peer_cert_chain() {
        final Ruby runtime = getRuntime();
        if ( engine == null ) return runtime.getNil();

        try {
            javax.security.cert.Certificate[] certs = engine.getSession().getPeerCertificateChain();
            RubyArray arr = runtime.newArray(certs.length);
            for ( int i = 0; i < certs.length; i++ ) {
                arr.append( X509Cert.wrap(runtime, certs[i]) );
            }
            return arr;
        }
        catch (javax.security.cert.CertificateEncodingException e) {
            throw X509Cert.newCertificateError(getRuntime(), e);
        }
        catch (SSLPeerUnverifiedException e) {
            if (runtime.isVerbose()) {
                runtime.getWarnings().warning(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }
        }
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject cipher() {
        return getRuntime().newString( engine.getSession().getCipherSuite() );
    }

    @JRubyMethod
    public IRubyObject state() {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: SSLSocket#state");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject pending() {
        warn(getRuntime().getCurrentContext(), "WARNING: unimplemented method called: SSLSocket#pending");
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "session_reused?")
    public IRubyObject session_reused_p() {
        warn(getRuntime().getCurrentContext(), "WARNING: SSLSocket#session_reused? is not supported");
        return getRuntime().getNil(); // throw new UnsupportedOperationException();
    }

    @JRubyMethod(name = "session=")
    public IRubyObject set_session(IRubyObject session) {
        warn(getRuntime().getCurrentContext(), "WARNING: SSLSocket#session= is not supported");
        return getRuntime().getNil(); // throw new UnsupportedOperationException();
    }

    private SocketChannel getSocketChannel() {
        return (SocketChannel) io.getChannel();
    }
}// SSLSocket
