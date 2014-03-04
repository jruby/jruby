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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import org.jruby.ext.openssl.x509store.Certificate;
import org.jruby.ext.openssl.x509store.Name;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.X509Object;
import org.jruby.ext.openssl.x509store.X509Utils;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSLContext extends RubyObject {
    private static final long serialVersionUID = -6203496135962974777L;

    private final static String[] ctx_attrs = {
        "cert", "key", "client_ca", "ca_file", "ca_path",
        "timeout", "verify_mode", "verify_depth",
        "verify_callback", "options", "cert_store", "extra_chain_cert",
        "client_cert_cb", "tmp_dh_callback", "session_id_context"};

    // Mapping table for OpenSSL's SSL_METHOD -> JSSE's SSLContext algorithm.
    private final static Map<String, String> SSL_VERSION_OSSL2JSSE;
    // Mapping table for JSEE's enabled protocols for the algorithm.
    private final static Map<String, String[]> ENABLED_PROTOCOLS;

    static {
        SSL_VERSION_OSSL2JSSE = new HashMap<String, String>();
        ENABLED_PROTOCOLS = new HashMap<String, String[]>();

        SSL_VERSION_OSSL2JSSE.put("TLSv1", "TLSv1");
        SSL_VERSION_OSSL2JSSE.put("TLSv1_server", "TLSv1");
        SSL_VERSION_OSSL2JSSE.put("TLSv1_client", "TLSv1");
        ENABLED_PROTOCOLS.put("TLSv1", new String[] { "TLSv1" });

        SSL_VERSION_OSSL2JSSE.put("SSLv2", "SSLv2");
        SSL_VERSION_OSSL2JSSE.put("SSLv2_server", "SSLv2");
        SSL_VERSION_OSSL2JSSE.put("SSLv2_client", "SSLv2");
        ENABLED_PROTOCOLS.put("SSLv2", new String[] { "SSLv2" });

        SSL_VERSION_OSSL2JSSE.put("SSLv3", "SSLv3");
        SSL_VERSION_OSSL2JSSE.put("SSLv3_server", "SSLv3");
        SSL_VERSION_OSSL2JSSE.put("SSLv3_client", "SSLv3");
        ENABLED_PROTOCOLS.put("SSLv3", new String[] { "SSLv3" });

        SSL_VERSION_OSSL2JSSE.put("SSLv23", "SSL");
        SSL_VERSION_OSSL2JSSE.put("SSLv23_server", "SSL");
        SSL_VERSION_OSSL2JSSE.put("SSLv23_client", "SSL");
        ENABLED_PROTOCOLS.put("SSL", new String[] { "SSLv2", "SSLv3", "TLSv1" });

        // Followings(TLS, TLSv1.1) are JSSE only methods at present. Let's allow user to use it.

        SSL_VERSION_OSSL2JSSE.put("TLS", "TLS");
        ENABLED_PROTOCOLS.put("TLS", new String[] { "TLSv1", "TLSv1.1" });

        SSL_VERSION_OSSL2JSSE.put("TLSv1.1", "TLSv1.1");
        ENABLED_PROTOCOLS.put("TLSv1.1", new String[] { "TLSv1.1" });
    }

    private static ObjectAllocator SSLCONTEXT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new SSLContext(runtime, klass);
        }
    };

    public static void createSSLContext(Ruby runtime, RubyModule mSSL) {
        RubyClass cSSLContext = mSSL.defineClassUnder("SSLContext",runtime.getObject(),SSLCONTEXT_ALLOCATOR);
        for(int i=0;i<ctx_attrs.length;i++) {
            cSSLContext.addReadWriteAttribute(runtime.getCurrentContext(), ctx_attrs[i]);
        }

        cSSLContext.defineAlias("ssl_timeout", "timeout");
        cSSLContext.defineAlias("ssl_timeout=", "timeout=");

        cSSLContext.defineAnnotatedMethods(SSLContext.class);
        
        cSSLContext.defineConstant("METHODS", runtime.newEmptyArray());
    }

    public SSLContext(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    public static RaiseException newSSLError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::SSL::SSLError", message, false);
    }

    private String ciphers = CipherStrings.SSL_DEFAULT_CIPHER_LIST;
    private String protocol = "SSL"; // SSLv23 in OpenSSL by default
    private boolean protocolForServer = true;
    private boolean protocolForClient = true;
    private PKey t_key = null;
    private X509Cert t_cert = null;
    /* TODO: should move to SSLSession after implemented */
    private int verifyResult = 1; /* avoid 0 (= X509_V_OK) just in case */

    private InternalContext internalCtx = null;

    @JRubyMethod(rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args) {
        return this;
    }

    @JRubyMethod
    public IRubyObject setup(final ThreadContext context) {
        final Ruby runtime = context.runtime;

        if ( isFrozen() ) return runtime.getNil();

        this.freeze(context);

        internalCtx = new InternalContext();
        internalCtx.protocol = protocol;
        internalCtx.protocolForServer = protocolForServer;
        internalCtx.protocolForClient = protocolForClient;

        // TODO: handle tmp_dh_callback

        X509Store certStore = getCertStore();
        if (certStore != null) {
            internalCtx.store = certStore.getStore();
        } else {
            internalCtx.store = new Store();
        }

        IRubyObject value = getInstanceVariable("@extra_chain_cert");
        if (value != null && !value.isNil()) {
            internalCtx.extraChainCert = new ArrayList<X509AuxCertificate>();
            for (X509Cert ele : convertToX509Certs(value)) {
                internalCtx.extraChainCert.add(ele.getAuxCert());
            }
        }

        value = getInstanceVariable("@key");
        PKey key = null;
        if (value != null && !value.isNil()) {
            Utils.checkKind(runtime, value, "OpenSSL::PKey::PKey");
            key = (PKey) value;
        } else {
            key = getCallbackKey();
        }
        value = getInstanceVariable("@cert");
        X509Cert cert = null;
        if (value != null && !value.isNil()) {
            Utils.checkKind(runtime, value, "OpenSSL::X509::Certificate");
            cert = (X509Cert) value;
        } else {
            cert = getCallbackCert();
        }
        if (key != null && cert != null) {
            internalCtx.keyAlgorithm = key.getAlgorithm();
            internalCtx.privateKey = key.getPrivateKey();
            internalCtx.cert = cert.getAuxCert();
        }

        value = getInstanceVariable("@client_ca");
        if (value != null && !value.isNil()) {
            if (value.respondsTo("each")) {
                for (X509Cert ele : convertToX509Certs(value)) {
                    internalCtx.clientCa.add(ele.getAuxCert());
                }
            } else {
                Utils.checkKind(runtime, value, "OpenSSL::X509::Certificate");
                internalCtx.clientCa.add(((X509Cert) value).getAuxCert());
            }
        }

        String caFile = getCaFile();
        String caPath = getCaPath();
        if (caFile != null || caPath != null) {
            try {
                if (internalCtx.store.loadLocations(caFile, caPath) == 0) {
                    runtime.getWarnings().warn(ID.MISCELLANEOUS, "can't set verify locations");
                }
            } catch (Exception e) {
                throw newSSLError(runtime, e.getMessage());
            }
        }

        value = getInstanceVariable("@verify_mode");
        if (value != null && !value.isNil()) {
            internalCtx.verifyMode = RubyNumeric.fix2int(value);
        } else {
            internalCtx.verifyMode = SSL.VERIFY_NONE;
        }
        value = getInstanceVariable("@verify_callback");
        if (value != null && !value.isNil()) {
            internalCtx.store.setExtraData(1, value);
        } else {
            internalCtx.store.setExtraData(1, null);
        }

        value = getInstanceVariable("@timeout");
        if (value != null && !value.isNil()) {
            internalCtx.timeout = RubyNumeric.fix2int(value);
        }

        value = getInstanceVariable("@verify_depth");
        if (value != null && !value.isNil()) {
            internalCtx.store.setDepth(RubyNumeric.fix2int(value));
        } else {
            internalCtx.store.setDepth(-1);
        }

        /* TODO: should be implemented for SSLSession
    val = ossl_sslctx_get_sess_id_ctx(self);
    if (!NIL_P(val)){
        StringValue(val);
        if (!SSL_CTX_set_session_id_context(ctx, (unsigned char *)RSTRING_PTR(val),
                                            RSTRING_LEN(val))){
            ossl_raise(eSSLError, "SSL_CTX_set_session_id_context:");
        }
    }

    if (RTEST(rb_iv_get(self, "@session_get_cb"))) {
        SSL_CTX_sess_set_get_cb(ctx, ossl_sslctx_session_get_cb);
        OSSL_Debug("SSL SESSION get callback added");
    }
    if (RTEST(rb_iv_get(self, "@session_new_cb"))) {
        SSL_CTX_sess_set_new_cb(ctx, ossl_sslctx_session_new_cb);
        OSSL_Debug("SSL SESSION new callback added");
    }
    if (RTEST(rb_iv_get(self, "@session_remove_cb"))) {
        SSL_CTX_sess_set_remove_cb(ctx, ossl_sslctx_session_remove_cb);
        OSSL_Debug("SSL SESSION remove callback added");
    }

    val = rb_iv_get(self, "@servername_cb");
    if (!NIL_P(val)) {
        SSL_CTX_set_tlsext_servername_callback(ctx, ssl_servername_cb);
        OSSL_Debug("SSL TLSEXT servername callback added");
    }
         */

        try {
            internalCtx.init();
        } catch(GeneralSecurityException gse) {
            throw newSSLError(runtime, gse.getMessage());
        }
        return runtime.getTrue();
    }

    @JRubyMethod
    @SuppressWarnings("unchecked")
    public IRubyObject ciphers(final ThreadContext context) {
        return context.runtime.newArray( (List) matchedCiphers(context) );
    }

    private List<RubyArray> matchedCiphers(final ThreadContext context) {
        final Ruby runtime = context.runtime;

        final ArrayList<RubyArray> cipherList = new ArrayList<RubyArray>();
        try {
            String[] supported = getCipherSuites( createDummySSLEngine() );
            List<CipherStrings.Def> ciphs = CipherStrings.getMatchingCiphers(ciphers, supported);
            cipherList.ensureCapacity( ciphs.size() );

            for ( CipherStrings.Def def : ciphs ) {
                final RubyArray cipher = runtime.newArray(4);
                cipher.set(0, runtime.newString(def.name));
                cipher.set(1, runtime.newString(sslVersionString(def.algorithms)));
                cipher.set(2, runtime.newFixnum(def.strength_bits));
                cipher.set(3, runtime.newFixnum(def.alg_bits));

                cipherList.add(cipher);
            }
        }
        catch (GeneralSecurityException gse) {
            throw newSSLError(runtime, gse.getMessage());
        }
        return cipherList;
    }

    @JRubyMethod(name = "ciphers=")
    public IRubyObject set_ciphers(final ThreadContext context, IRubyObject val) {
        if (val.isNil()) {
            ciphers = CipherStrings.SSL_DEFAULT_CIPHER_LIST;
        } else if (val instanceof RubyArray) {
            StringBuilder builder = new StringBuilder();
            String sep = "";
            for (IRubyObject obj : ((RubyArray) val).toJavaArray()) {
                builder.append(sep).append(obj.toString());
                sep = ":";
            }
            ciphers = builder.toString();
        } else {
            ciphers = val.convertToString().toString();
            if (ciphers.equals("DEFAULT")) {
                ciphers = CipherStrings.SSL_DEFAULT_CIPHER_LIST;
            }
        }
        if ( matchedCiphers(context).isEmpty() ) {
            throw newSSLError(context.runtime, "no cipher match");
        }
        return val;
    }

    @JRubyMethod(name = "ssl_version=")
    public IRubyObject set_ssl_version(IRubyObject val) {
        String given;

        if (val instanceof RubyString) {
            RubyString str = val.convertToString();
            given = str.toString();
        } else {
            given = val.toString();
        }
        String mapped = SSL_VERSION_OSSL2JSSE.get(given);
        if (mapped == null) {
            throw newSSLError(getRuntime(), String.format("unknown SSL method `%s'.", given));
        }
        protocol = mapped;
        protocolForServer = protocolForClient = true;
        if (given.endsWith("_client")) {
            protocolForServer = false;
        }
        if (given.endsWith("_server")) {
            protocolForClient = false;
        }
        return val;
    }

    boolean isProtocolForServer() {
        return protocolForServer;
    }

    boolean isProtocolForClient() {
        return protocolForClient;
    }

    int getLastVerifyResult() {
        return verifyResult;
    }

    void setLastVerifyResult(int verifyResult) {
        this.verifyResult = verifyResult;
    }

    SSLEngine createDummySSLEngine() throws GeneralSecurityException {
        javax.net.ssl.SSLContext ctx = SecurityHelper.getSSLContext(protocol);
        ctx.init(null, null, null);
        return ctx.createSSLEngine();
    }

    // should keep SSLContext as a member for introducin SSLSession. later...
    SSLEngine createSSLEngine(String peerHost, int peerPort) throws NoSuchAlgorithmException, KeyManagementException {
        SSLEngine engine;
        // an empty peerHost implies no SNI (RFC 3546) support requested
        if (peerHost == null || peerHost.length() == 0) {
            engine = internalCtx.getSSLContext().createSSLEngine();
        }
        // SNI is attempted for valid peerHost hostname on Java >= 7
        // if peerHost is set to an IP address Java does not use SNI
        else {
            engine = internalCtx.getSSLContext().createSSLEngine(peerHost, peerPort);
        }
        engine.setEnabledCipherSuites(getCipherSuites(engine));
        engine.setEnabledProtocols(getEnabledProtocols(engine));
        return engine;
    }

    private String[] getCipherSuites(SSLEngine engine) {
        List<CipherStrings.Def> ciphs = CipherStrings.getMatchingCiphers(ciphers, engine.getSupportedCipherSuites());
        String[] result = new String[ciphs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ciphs.get(i).cipherSuite;
        }
        return result;
    }

    private String[] getEnabledProtocols(SSLEngine engine) {
        List<String> candidates = new ArrayList<String>();
        long options = getOptions();
        if (ENABLED_PROTOCOLS.get(protocol) != null) {
            for (String enabled : ENABLED_PROTOCOLS.get(protocol)) {
                if (((options & SSL.OP_NO_SSLv2) != 0) && enabled.equals("SSLv2")) {
                    continue;
                }
                if (((options & SSL.OP_NO_SSLv3) != 0) && enabled.equals("SSLv3")) {
                    continue;
                }
                if (((options & SSL.OP_NO_TLSv1) != 0) && enabled.equals("TLSv1")) {
                    continue;
                }
                for (String allowed : engine.getEnabledProtocols()) {
                    if (allowed.equals(enabled)) {
                        candidates.add(allowed);
                    }
                }
            }
        }
        return candidates.toArray(new String[candidates.size()]);
    }

    private String sslVersionString(long bits) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if ((bits & CipherStrings.SSL_SSLV3) != 0) {
            if (!first) {
                sb.append("/");
            }
            first = false;
            sb.append("TLSv1/SSLv3");
        }
        if ((bits & CipherStrings.SSL_SSLV2) != 0) {
            if (!first) {
                sb.append("/");
            }
            first = false;
            sb.append("SSLv2");
        }
        return sb.toString();
    }

    private PKey getCallbackKey() {
        if (t_key != null) {
            return t_key;
        }
        initFromCallback();
        return t_key;
    }

    private X509Cert getCallbackCert() {
        if (t_cert != null) {
            return t_cert;
        }
        initFromCallback();
        return t_cert;
    }

    private void initFromCallback() {
        IRubyObject value = getInstanceVariable("@client_cert_cb");
        if (value != null && !value.isNil()) {
            IRubyObject out = value.callMethod(getRuntime().getCurrentContext(), "call", this);
            Utils.checkKind(getRuntime(), out, "Array");
            IRubyObject cert = (IRubyObject) ((RubyArray) out).getList().get(0);
            IRubyObject key = (IRubyObject) ((RubyArray) out).getList().get(1);
            Utils.checkKind(getRuntime(), cert, "OpenSSL::X509::Certificate");
            Utils.checkKind(getRuntime(), key, "OpenSSL::PKey::PKey");
            t_cert = (X509Cert) cert;
            t_key = (PKey) key;
        }
    }

    private X509Store getCertStore() {
        IRubyObject value = getInstanceVariable("@cert_store");
        if (value != null && !value.isNil() && (value instanceof X509Store)) {
            Utils.checkKind(getRuntime(), value, "OpenSSL::X509::Store");
            return (X509Store) value;
        } else {
            return null;
        }
    }

    private String getCaFile() {
        IRubyObject value = getInstanceVariable("@ca_file");
        if (value != null && !value.isNil()) {
            return value.convertToString().toString();
        } else {
            return null;
        }
    }

    private String getCaPath() {
        IRubyObject value = getInstanceVariable("@ca_path");
        if (value != null && !value.isNil()) {
            return value.convertToString().toString();
        } else {
            return null;
        }
    }

    private long getOptions() {
        IRubyObject value = getInstanceVariable("@options");
        if (value != null && !value.isNil()) {
            return RubyNumeric.fix2long(value);
        } else {
            return 0;
        }
    }

    private X509Cert[] convertToX509Certs(IRubyObject value) {
        final ArrayList<X509Cert> result = new ArrayList<X509Cert>();
        ThreadContext ctx = getRuntime().getCurrentContext();
        RubyClass klass = Utils.getClassFromPath(ctx.runtime, "OpenSSL::SSL::SSLContext");
        Helpers.invoke(ctx, value, "each", CallBlock.newCallClosure(value, klass, Arity.NO_ARGUMENTS, new BlockCallback() {

            public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                Utils.checkKind(getRuntime(), args[0], "OpenSSL::X509::Certificate");
                result.add((X509Cert) args[0]);
                return context.runtime.getNil();
            }
        }, ctx));
        return result.toArray(new X509Cert[0]);
    }

    /**
     * c: SSL_CTX
     */
    private class InternalContext {

        Store store = null;
        int verifyMode = SSL.VERIFY_NONE;
        X509AuxCertificate cert = null;
        String keyAlgorithm = null;
        PrivateKey privateKey = null;
        List<X509AuxCertificate> extraChainCert = null;
        List<X509AuxCertificate> clientCa = new ArrayList<X509AuxCertificate>();
        int timeout = 0;
        String protocol = null;
        boolean protocolForServer = true;
        boolean protocolForClient = true;
        private javax.net.ssl.SSLContext sslCtx = null;

        void setLastVerifyResultInternal(int lastVerifyResult) {
            setLastVerifyResult(lastVerifyResult);
        }

        javax.net.ssl.SSLContext getSSLContext() {
            return sslCtx;
        }

        void init() throws GeneralSecurityException {
            KM km = new KM(this);
            TM tm = new TM(this);
            sslCtx = javax.net.ssl.SSLContext.getInstance(protocol);
            if (protocolForClient) {
                sslCtx.getClientSessionContext().setSessionTimeout(timeout);
            }
            if (protocolForServer) {
                sslCtx.getServerSessionContext().setSessionTimeout(timeout);
            }
            sslCtx.init(new javax.net.ssl.KeyManager[]{km}, new javax.net.ssl.TrustManager[]{tm}, null);
        }

        // part of ssl_verify_cert_chain
        StoreContext createStoreContext(String purpose) {
            if (store == null) {
                return null;
            }
            StoreContext ctx = new StoreContext();
            if (ctx.init(store, null, null) == 0) {
                return null;
            }
            // for verify_cb
            ctx.setExtraData(1, store.getExtraData(1));
            if (purpose != null) {
                ctx.setDefault(purpose);
            }
            ctx.param.inherit(store.param);
            return ctx;
        }
    }

    private static class KM extends javax.net.ssl.X509ExtendedKeyManager {

        private final InternalContext ctx;

        public KM(InternalContext ctx) {
            super();
            this.ctx = ctx;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, java.security.Principal[] issuers, javax.net.ssl.SSLEngine engine) {
            if (ctx == null) {
                return null;
            }
            if (ctx.privateKey == null) {
                return null;
            }
            for (int i = 0; i < keyType.length; i++) {
                if (keyType[i].equalsIgnoreCase(ctx.keyAlgorithm)) {
                    return keyType[i];
                }
            }
            return null;
        }

        @Override
        public String chooseEngineServerAlias(String keyType, java.security.Principal[] issuers, javax.net.ssl.SSLEngine engine) {
            if (ctx == null || ctx.privateKey == null) {
                return null;
            }
            if (keyType.equalsIgnoreCase(ctx.keyAlgorithm)) {
                return keyType;
            }
            return null;
        }

        public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return null;
        }

        public String chooseServerAlias(String keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return null;
        }

        // c: ssl3_output_cert_chain
        public java.security.cert.X509Certificate[] getCertificateChain(String alias) {
            if (ctx == null) {
                return null;
            }
            ArrayList<java.security.cert.X509Certificate> chain = new ArrayList<java.security.cert.X509Certificate>();
            if (ctx.extraChainCert != null) {
                chain.addAll(ctx.extraChainCert);
            } else if (ctx.cert != null) {
                StoreContext storeCtx = ctx.createStoreContext(null);
                X509AuxCertificate x = ctx.cert;
                while (true) {
                    chain.add(x);
                    if (x.getIssuerDN().equals(x.getSubjectDN())) {
                        break;
                    }
                    try {
                        Name xn = new Name(x.getIssuerX500Principal());
                        X509Object[] s_obj = new X509Object[1];
                        if (storeCtx.getBySubject(X509Utils.X509_LU_X509, xn, s_obj) <= 0) {
                            break;
                        }
                        x = ((Certificate) s_obj[0]).x509;
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            return chain.toArray(new java.security.cert.X509Certificate[0]);
        }

        public String[] getClientAliases(String keyType, java.security.Principal[] issuers) {
            return null;
        }

        public java.security.PrivateKey getPrivateKey(String alias) {
            if (ctx == null || ctx.privateKey == null) {
                return null;
            }
            return ctx.privateKey;
        }

        public String[] getServerAliases(String keyType, java.security.Principal[] issuers) {
            return null;
        }
    }

    private static class TM implements javax.net.ssl.X509TrustManager {

        private InternalContext ctx;

        public TM(InternalContext ctx) {
            super();
            this.ctx = ctx;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("ssl_client", chain);
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("ssl_server", chain);
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            if (ctx == null) {
                return null;
            }
            ArrayList<java.security.cert.X509Certificate> chain = new ArrayList<java.security.cert.X509Certificate>();
            chain.addAll(ctx.clientCa);
            return chain.toArray(new java.security.cert.X509Certificate[0]);
        }

        // c: ssl_verify_cert_chain
        private void checkTrusted(String purpose, X509Certificate[] chain) throws CertificateException {
            if (ctx == null) {
                throw new CertificateException("uninitialized trust manager");
            }
            if (chain != null && chain.length > 0) {
                if ((ctx.verifyMode & SSL.VERIFY_PEER) != 0) {
                    // verify_peer
                    StoreContext storeCtx = ctx.createStoreContext(purpose);
                    if (storeCtx == null) {
                        throw new CertificateException("couldn't initialize store");
                    }
                    storeCtx.setCertificate(chain[0]);
                    storeCtx.setChain(chain);
                    verifyChain(storeCtx);
                }
            } else {
                if ((ctx.verifyMode & SSL.VERIFY_FAIL_IF_NO_PEER_CERT) != 0) {
                    // fail if no peer cert
                    throw new CertificateException("no peer certificate");
                }
            }
        }

        private void verifyChain(StoreContext storeCtx) throws CertificateException {
            try {
                int ok = storeCtx.verifyCertificate();
                ctx.setLastVerifyResultInternal(storeCtx.error);
                if (ok == 0) {
                    throw new CertificateException("certificate verify failed");
                }
            } catch (Exception e) {
                ctx.setLastVerifyResultInternal(storeCtx.error);
                if (storeCtx.error == X509Utils.V_OK) {
                    ctx.setLastVerifyResultInternal(X509Utils.V_ERR_CERT_REJECTED);
                }
                throw new CertificateException("certificate verify failed", e);
            }
        }
    }
}// SSLContext
