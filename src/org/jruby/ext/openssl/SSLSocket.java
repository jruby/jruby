/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSLSocket extends RubyObject {
    public static void createSSLSocket(IRuby runtime, RubyModule mSSL) {
        RubyClass cSSLSocket = mSSL.defineClassUnder("SSLSocket",runtime.getObject());

        cSSLSocket.attr_accessor(new IRubyObject[]{runtime.newSymbol("io")});
        cSSLSocket.attr_accessor(new IRubyObject[]{runtime.newSymbol("context")});
        cSSLSocket.attr_accessor(new IRubyObject[]{runtime.newSymbol("sync_close")});

        CallbackFactory sockcb = runtime.callbackFactory(SSLSocket.class);
        cSSLSocket.defineSingletonMethod("new",sockcb.getOptSingletonMethod("newInstance"));
        cSSLSocket.defineAlias("to_io","io");
        cSSLSocket.defineMethod("initialize",sockcb.getOptMethod("_initialize"));
        cSSLSocket.defineMethod("connect",sockcb.getMethod("connect"));
        cSSLSocket.defineMethod("accept",sockcb.getMethod("accept"));
        cSSLSocket.defineMethod("sysread",sockcb.getOptMethod("sysread"));
        cSSLSocket.defineMethod("syswrite",sockcb.getMethod("syswrite",IRubyObject.class));
        cSSLSocket.defineMethod("sysclose",sockcb.getMethod("sysclose"));
        cSSLSocket.defineMethod("cert",sockcb.getMethod("cert"));
        cSSLSocket.defineMethod("peer_cert",sockcb.getMethod("peer_cert"));
        cSSLSocket.defineMethod("peer_cert_chain",sockcb.getMethod("peer_cert_chain"));
        cSSLSocket.defineMethod("cipher",sockcb.getMethod("cipher"));
        cSSLSocket.defineMethod("state",sockcb.getMethod("state"));
        cSSLSocket.defineMethod("pending",sockcb.getMethod("pending"));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        SSLSocket result = new SSLSocket(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    private RubyClass sslError;

    public SSLSocket(IRuby runtime, RubyClass type) {
        super(runtime,type);
        sslError = ((RubyClass)((RubyModule)runtime.getModule("OpenSSL").getConstant("SSL")).getConstant("SSLError"));
    }

    private SSLEngine engine;
    private SocketChannel c = null;

    private ByteBuffer peerAppData;
    private ByteBuffer peerNetData;
    private ByteBuffer netData;
    private ByteBuffer dummy;
    
    private boolean initialHandshake = false;
	
    private SSLEngineResult.HandshakeStatus hsStatus;
    private SSLEngineResult.Status status = null;

    private String type = null;

    private Selector rsel;
    private Selector wsel;
    private Selector asel;
    
    public IRubyObject _initialize(IRubyObject[] args) throws Exception {
        IRubyObject io, ctx;
        ThreadContext tc = getRuntime().getCurrentContext();
        if(checkArgumentCount(args,1,2) == 1) {
            ctx = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("SSL"))).getClass("SSLContext").callMethod(tc,"new");
        } else {
            ctx = args[1];
        }
        io = args[0];
        callMethod(tc,"io=",io);
        c = (SocketChannel)(((RubyIO)io).getChannel());
        callMethod(tc,"context=",ctx);
        callMethod(tc,"sync_close=",getRuntime().getFalse());
        return callMethod(tc,getMetaClass().getSuperClass(),"initialize",args,CallType.SUPER);
    }

    private void ossl_ssl_setup() throws Exception {
        if(null == engine) {
            ThreadContext tc = getRuntime().getCurrentContext();
            SSLContext ctx = SSLContext.getInstance("SSL");
            IRubyObject store = callMethod(tc,"context").callMethod(tc,"cert_store");
            callMethod(tc,"context").callMethod(tc,"verify_mode");

            if(store.isNil()) {
                ctx.init(new javax.net.ssl.KeyManager[]{((org.jruby.ext.openssl.SSLContext)callMethod(tc,"context")).getKM()},new javax.net.ssl.TrustManager[]{((org.jruby.ext.openssl.SSLContext)callMethod(tc,"context")).getTM()},null);
            } else {
                ctx.init(new javax.net.ssl.KeyManager[]{((org.jruby.ext.openssl.SSLContext)callMethod(tc,"context")).getKM()},new javax.net.ssl.TrustManager[]{((X509Store)store).getStore()},null);
            }

            String peerHost = ((SocketChannel)c).socket().getInetAddress().getHostName();
            int peerPort = ((SocketChannel)c).socket().getPort();
            engine = ctx.createSSLEngine(peerHost,peerPort);
            engine.setEnabledCipherSuites(((org.jruby.ext.openssl.SSLContext)callMethod(tc,"context")).getCipherSuites(engine));
            SSLSession session = engine.getSession();
            peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
            peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());		
            netData = ByteBuffer.allocate(session.getPacketBufferSize());
            peerNetData.limit(0);
            peerAppData.limit(0);
            netData.limit(0);
            dummy = ByteBuffer.allocate(0);
            rsel = Selector.open();
            wsel = Selector.open();
            asel = Selector.open();
            c.register(rsel,SelectionKey.OP_READ);
            c.register(wsel,SelectionKey.OP_WRITE);
            c.register(asel,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    public IRubyObject connect() throws Exception {
        try {
            ossl_ssl_setup();
            engine.setUseClientMode(true);
            engine.beginHandshake();
            type = "client";
            hsStatus = engine.getHandshakeStatus();
            initialHandshake = true;
            doHandshake();
        } catch(javax.net.ssl.SSLHandshakeException e) {
            Throwable v = e;
            while(v.getCause() != null && (v instanceof javax.net.ssl.SSLHandshakeException)) {
                v = v.getCause();
            }
            if(v instanceof java.security.cert.CertificateException) {
                throw new RaiseException(getRuntime(),sslError,v.getMessage(),true);
            } else {
                throw new RaiseException(getRuntime(),sslError,null,true);
            }
        }
        return this;
    }

    public IRubyObject accept() throws Exception {
        try {
            ThreadContext tc = getRuntime().getCurrentContext();
            int vfy = 0;
            ossl_ssl_setup();
            engine.setUseClientMode(false);
            IRubyObject ccc = callMethod(tc,"context");
            if(!ccc.isNil() && !ccc.callMethod(tc,"verify_mode").isNil()) {
                vfy = RubyNumeric.fix2int(ccc.callMethod(tc,"verify_mode"));
                if(vfy == 0) { //VERIFY_NONE
                    engine.setNeedClientAuth(false);
                    engine.setWantClientAuth(false);
                }
                if((vfy & 1) != 0) { //VERIFY_PEER
                    engine.setWantClientAuth(true);
                }
                if((vfy & 2) != 0) { //VERIFY_FAIL_IF_NO_PEER_CERT
                    engine.setNeedClientAuth(true);
                }
            }
            engine.beginHandshake();
            type = "server";
            hsStatus = engine.getHandshakeStatus();
            initialHandshake = true;
            doHandshake();
        } catch(javax.net.ssl.SSLHandshakeException e) {
            throw new RaiseException(getRuntime(),sslError,null,true);
        }

        return this;
    }

    private void waitSelect(Selector sel) {
        try {
            sel.select();
        } catch(Exception e) {
            return;
        }
        Iterator it = sel.selectedKeys().iterator();
        while(it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private void doHandshake() throws Exception {
        while (true) {
            SSLEngineResult res;
            waitSelect(asel);
            if(hsStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
                if (initialHandshake) {
                    finishInitialHandshake();
                }
                return;
            } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                doTasks();
            } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                if(readAndUnwrap() == -1 && hsStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
                    throw new javax.net.ssl.SSLHandshakeException("Socket closed");
                }
            } else if(hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                if (netData.hasRemaining()) {
                    while(flushData());
                }
                netData.clear();
                res = engine.wrap(dummy, netData);
                hsStatus = res.getHandshakeStatus();
                netData.flip();
                flushData();
            } else {
                assert false : "doHandshake() should never reach the NOT_HANDSHAKING state";
                return;
            }
        }
    }

    private void doTasks() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            task.run();
        }
        hsStatus = engine.getHandshakeStatus();
    }

    private boolean flushData() throws IOException {		
        try {
            c.write(netData);
        } catch (IOException ioe) {
            netData.position(netData.limit());
            throw ioe;
        }
        if (netData.hasRemaining()) {
            return false;
        }  else {
            return true;
        }
    }

    private void finishInitialHandshake() {
        initialHandshake = false;
    }

    public int write(ByteBuffer src) throws Exception {
        if(initialHandshake) {
            return 0;
        }
        if(netData.hasRemaining()) {
            return 0;
        }
        netData.clear();
        SSLEngineResult res = engine.wrap(src, netData);
        netData.flip();
        flushData();
        return res.bytesConsumed();
    }

    public int read(ByteBuffer dst) throws Exception {
        if(initialHandshake) {
            return 0;
        }
        if (engine.isInboundDone()) {
            return -1;
        }
        if (!peerAppData.hasRemaining()) {
            int appBytesProduced = readAndUnwrap(); 
            if (appBytesProduced == -1 || appBytesProduced == 0) {
                return appBytesProduced;
            } 
        }
        int limit = Math.min(peerAppData.remaining(), dst.remaining());
        for (int i = 0; i < limit; i++) {
            dst.put(peerAppData.get());
        }
        return limit;
    }

    private int readAndUnwrap() throws Exception {
        int bytesRead = c.read(peerNetData);
        if(bytesRead == -1) {
            //            engine.closeInbound();			
            return -1;
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
        
        if(status == SSLEngineResult.Status.CLOSED) {
            doShutdown();
            return -1;
        }	
        peerNetData.compact();
        peerAppData.flip();
        if(!initialHandshake && (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK ||
                                 hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP ||
                                 hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)) {
            doHandshake();
        }
        return peerAppData.remaining();
    }

    private void doShutdown() throws IOException {
        if (engine.isOutboundDone()) {
            return;
        }
        netData.clear();
        try {
            engine.wrap(dummy, netData);
        } catch(Exception e1) {
            return;
        }
        netData.flip();
        flushData();
    }

    public IRubyObject sysread(IRubyObject[] args) throws Exception {
        //        System.err.println("WARNING: unimplemented method called: SSLSocket#sysread");
        checkArgumentCount(args,1,2);
        int len = RubyNumeric.fix2int(args[0]);
        IRubyObject str = args.length == 2 ? args[1] : getRuntime().newString("");
        if(len == 0) {
            return str;
        }
        waitSelect(rsel);
        ByteBuffer dst = ByteBuffer.allocate(len);
        String bef_dst = dst.toString();
        int rr = -1;
        if(engine == null) {
            rr = c.read(dst);
        } else {
            rr = read(dst);
        }
        String aft_dst = dst.toString();
        String out = null;
        boolean eof = false;
        if(rr == -1) {
            eof = true;
        } else {
            byte[] bss = new byte[rr];
            dst.position(dst.position()-rr);
            dst.get(bss);
            out = new String(bss,"ISO8859_1");
        }
        String aft_ag_dst = dst.toString();
        if(eof){
            throw getRuntime().newEOFError();
        }
        str.callMethod(getRuntime().getCurrentContext(),"<<",getRuntime().newString(out));
        return str;
    }

    public IRubyObject syswrite(IRubyObject arg) throws Exception {
        ///        System.err.println("WARNING: unimplemented method called: SSLSocket#syswrite");
        //        System.err.println(type + ".syswrite(" + arg + ")");
        if(engine == null) {
            waitSelect(wsel);
            byte[] bls = arg.toString().getBytes("PLAIN");
            ByteBuffer b1 = ByteBuffer.wrap(bls);
            c.write(b1);
            return getRuntime().newFixnum(bls.length);
        } else {
            waitSelect(wsel);
            byte[] bls = arg.toString().getBytes("PLAIN");
            ByteBuffer b1 = ByteBuffer.wrap(bls);
            write(b1);
            return getRuntime().newFixnum(bls.length);
        }
    }

    private void close() throws Exception {
        engine.closeOutbound();
        if (netData.hasRemaining()) {
            return;
        } else {
            doShutdown();
        }
    }

    public IRubyObject sysclose() throws Exception {
        //        System.err.println("WARNING: unimplemented method called: SSLSocket#sysclose");
        //        System.err.println(type + ".sysclose");
        close();
        ThreadContext tc = getRuntime().getCurrentContext();
        if(callMethod(tc,"sync_close").isTrue()) {
            c.close();
            callMethod(tc,"io").callMethod(tc,"close");
        }
        return getRuntime().getNil();
    }

    public IRubyObject cert() {
        System.err.println("WARNING: unimplemented method called: SSLSocket#cert");
        return getRuntime().getNil();
    }

    public IRubyObject peer_cert() throws Exception {
        java.security.cert.Certificate[] c = engine.getSession().getPeerCertificates();
        if(c.length > 0) {
            return X509Cert.wrap(getRuntime(),c[0]);
        }
        return getRuntime().getNil();
    }

    public IRubyObject peer_cert_chain() {
        System.err.println("WARNING: unimplemented method called: SSLSocket#peer_cert_chain");
        return getRuntime().getNil();
    }

    public IRubyObject cipher() {
        System.err.println("WARNING: unimplemented method called: SSLSocket#cipher");
        return getRuntime().getNil();
    }

    public IRubyObject state() {
        System.err.println("WARNING: unimplemented method called: SSLSocket#state");
        return getRuntime().getNil();
    }

    public IRubyObject pending() {
        System.err.println("WARNING: unimplemented method called: SSLSocket#pending");
        return getRuntime().getNil();
    }
}// SSLSocket
