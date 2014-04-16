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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

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

    // Mapping table for OpenSSL's SSL_METHOD -> JSSE's SSLContext algorithm.
    private static final HashMap<String, String> SSL_VERSION_OSSL2JSSE;
    // Mapping table for JSEE's enabled protocols for the algorithm.
    private static final Map<String, String[]> ENABLED_PROTOCOLS;

    static {
        SSL_VERSION_OSSL2JSSE = new LinkedHashMap<String, String>(16);
        ENABLED_PROTOCOLS = new HashMap<String, String[]>(8);

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

    public static void createSSLContext(final Ruby runtime, final RubyModule _SSL) { // OpenSSL::SSL
        RubyClass _SSLContext = _SSL.defineClassUnder("SSLContext", runtime.getObject(), SSLCONTEXT_ALLOCATOR);

        final String[] attributes = {
            "cert", "key", "client_ca", "ca_file", "ca_path",
            "timeout", "verify_mode", "verify_depth",
            "verify_callback", "options", "cert_store", "extra_chain_cert",
            "client_cert_cb", "tmp_dh_callback", "session_id_context"
        };
        final ThreadContext context = runtime.getCurrentContext();
        for ( int i = 0; i < attributes.length; i++ ) {
            _SSLContext.addReadWriteAttribute(context, attributes[i]);
        }

        _SSLContext.defineAlias("ssl_timeout", "timeout");
        _SSLContext.defineAlias("ssl_timeout=", "timeout=");

        _SSLContext.defineAnnotatedMethods(SSLContext.class);

        final Set<String> methodKeys = SSL_VERSION_OSSL2JSSE.keySet();
        final RubyArray methods = runtime.newArray( methodKeys.size() );
        for ( String method : methodKeys ) {
            methods.append( runtime.newSymbol(method) );
        }
        _SSLContext.defineConstant("METHODS", methods);
        // in 1.8.7 as well as 1.9.3 :
        // [:TLSv1, :TLSv1_server, :TLSv1_client, :SSLv3, :SSLv3_server, :SSLv3_client, :SSLv23, :SSLv23_server, :SSLv23_client]

        // MRI (1.9.3) has a bunch of SESSION_CACHE constants :
        // :SESSION_CACHE_OFF, :SESSION_CACHE_CLIENT, :SESSION_CACHE_SERVER,
        // :SESSION_CACHE_BOTH, :SESSION_CACHE_NO_AUTO_CLEAR, :SESSION_CACHE_NO_INTERNAL_LOOKUP,
        // :SESSION_CACHE_NO_INTERNAL_STORE, :SESSION_CACHE_NO_INTERNAL,
        //
        // NOTE probably worth doing are :
        // OpenSSL::SSL::SSLContext::DEFAULT_CERT_STORE
        // => #<OpenSSL::X509::Store:0x00000001c69f48>
        // OpenSSL::SSL::SSLContext::DEFAULT_PARAMS
        // => {:ssl_version=>"SSLv23", :verify_mode=>1, :ciphers=>"ALL:!ADH:!EXPORT:!SSLv2:RC4+RSA:+HIGH:+MEDIUM:+LOW", :options=>-2147480577}
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
    private PKey t_key;
    private X509Cert t_cert;
    /* TODO: should move to SSLSession after implemented */
    private int verifyResult = 1; /* avoid 0 (= X509_V_OK) just in case */

    private InternalContext internalContext;

    @JRubyMethod(rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args) {
        return this;
    }

    @JRubyMethod
    public IRubyObject setup(final ThreadContext context) {
        final Ruby runtime = context.runtime;

        if ( isFrozen() ) return runtime.getNil();

        this.freeze(context);

        internalContext = new InternalContext();
        internalContext.protocol = protocol;
        internalContext.protocolForServer = protocolForServer;
        internalContext.protocolForClient = protocolForClient;

        // TODO: handle tmp_dh_callback

        X509Store certStore = getCertStore();
        if (certStore != null) {
            internalContext.store = certStore.getStore();
        } else {
            internalContext.store = new Store();
        }

        IRubyObject value = getInstanceVariable("@extra_chain_cert");
        if (value != null && !value.isNil()) {
            internalContext.extraChainCert = new ArrayList<X509AuxCertificate>();
            for (X509Cert ele : convertToX509Certs(context, value)) {
                internalContext.extraChainCert.add(ele.getAuxCert());
            }
        }

        value = getInstanceVariable("@key");
        final PKey key;
        if (value != null && !value.isNil()) {
            Utils.checkKind(runtime, value, "OpenSSL::PKey::PKey");
            key = (PKey) value;
        } else {
            key = getCallbackKey(context);
        }
        value = getInstanceVariable("@cert");
        final X509Cert cert;
        if (value != null && !value.isNil()) {
            Utils.checkKind(runtime, value, "OpenSSL::X509::Certificate");
            cert = (X509Cert) value;
        } else {
            cert = getCallbackCert(context);
        }
        if (key != null && cert != null) {
            internalContext.keyAlgorithm = key.getAlgorithm();
            internalContext.privateKey = key.getPrivateKey();
            internalContext.cert = cert.getAuxCert();
        }

        value = getInstanceVariable("@client_ca");
        if (value != null && !value.isNil()) {
            if (value.respondsTo("each")) {
                for (X509Cert ele : convertToX509Certs(context, value)) {
                    internalContext.clientCert.add(ele.getAuxCert());
                }
            } else {
                Utils.checkKind(runtime, value, "OpenSSL::X509::Certificate");
                internalContext.clientCert.add(((X509Cert) value).getAuxCert());
            }
        }

        String caFile = getCaFile();
        String caPath = getCaPath();
        if (caFile != null || caPath != null) {
            try {
                if (internalContext.store.loadLocations(caFile, caPath) == 0) {
                    runtime.getWarnings().warn(ID.MISCELLANEOUS, "can't set verify locations");
                }
            } catch (Exception e) {
                throw newSSLError(runtime, e.getMessage());
            }
        }

        value = getInstanceVariable("@verify_mode");
        if (value != null && !value.isNil()) {
            internalContext.verifyMode = RubyNumeric.fix2int(value);
        } else {
            internalContext.verifyMode = SSL.VERIFY_NONE;
        }
        value = getInstanceVariable("@verify_callback");
        if (value != null && !value.isNil()) {
            internalContext.store.setExtraData(1, value);
        } else {
            internalContext.store.setExtraData(1, null);
        }

        value = getInstanceVariable("@timeout");
        if (value != null && !value.isNil()) {
            internalContext.timeout = RubyNumeric.fix2int(value);
        }

        value = getInstanceVariable("@verify_depth");
        if (value != null && !value.isNil()) {
            internalContext.store.setDepth(RubyNumeric.fix2int(value));
        } else {
            internalContext.store.setDepth(-1);
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
            internalContext.init();
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
    public IRubyObject set_ssl_version(IRubyObject version) {
        final String versionStr;
        if ( version instanceof RubyString ) {
            versionStr = version.convertToString().toString();
        } else {
            versionStr = version.toString();
        }
        final String mapped = SSL_VERSION_OSSL2JSSE.get(versionStr);
        if ( mapped == null ) {
            throw newSSLError(getRuntime(), String.format("unknown SSL method `%s'.", versionStr));
        }
        protocol = mapped;
        protocolForServer = ! versionStr.endsWith("_client");
        protocolForClient = ! versionStr.endsWith("_server");
        return version;
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
        javax.net.ssl.SSLContext sslContext = SecurityHelper.getSSLContext(protocol);
        sslContext.init(null, null, null);
        return sslContext.createSSLEngine();
    }

    // should keep SSLContext as a member for introducin SSLSession. later...
    SSLEngine createSSLEngine(String peerHost, int peerPort) throws NoSuchAlgorithmException, KeyManagementException {
        SSLEngine engine;
        // an empty peerHost implies no SNI (RFC 3546) support requested
        if (peerHost == null || peerHost.length() == 0) {
            engine = internalContext.getSSLContext().createSSLEngine();
        }
        // SNI is attempted for valid peerHost hostname on Java >= 7
        // if peerHost is set to an IP address Java does not use SNI
        else {
            engine = internalContext.getSSLContext().createSSLEngine(peerHost, peerPort);
        }
        engine.setEnabledCipherSuites(getCipherSuites(engine));
        engine.setEnabledProtocols(getEnabledProtocols(engine));
        return engine;
    }

    private String[] getCipherSuites(final SSLEngine engine) {
        List<CipherStrings.Def> ciphs = CipherStrings.getMatchingCiphers(ciphers, engine.getSupportedCipherSuites());
        String[] result = new String[ciphs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ciphs.get(i).cipherSuite;
        }
        return result;
    }

    private String[] getEnabledProtocols(final SSLEngine engine) {
        List<String> candidates = new ArrayList<String>();
        final long options = getOptions();
        final String[] enabledProtocols = ENABLED_PROTOCOLS.get(protocol);
        if ( enabledProtocols != null ) {
            final String[] engineProtocols = engine.getEnabledProtocols();
            for ( String enabled : enabledProtocols ) {
                if (((options & SSL.OP_NO_SSLv2) != 0) && enabled.equals("SSLv2")) {
                    continue;
                }
                if (((options & SSL.OP_NO_SSLv3) != 0) && enabled.equals("SSLv3")) {
                    continue;
                }
                if (((options & SSL.OP_NO_TLSv1) != 0) && enabled.equals("TLSv1")) {
                    continue;
                }
                for ( String allowed : engineProtocols ) {
                    if ( allowed.equals(enabled) ) candidates.add(allowed);
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

    private PKey getCallbackKey(final ThreadContext context) {
        if ( t_key != null ) return t_key;
        initFromCallback(context);
        return t_key;
    }

    private X509Cert getCallbackCert(final ThreadContext context) {
        if ( t_cert != null ) return t_cert;
        initFromCallback(context);
        return t_cert;
    }

    private void initFromCallback(final ThreadContext context) {
        final IRubyObject callback = getInstanceVariable("@client_cert_cb");
        if ( callback != null && ! callback.isNil() ) {
            IRubyObject arr = callback.callMethod(context, "call", this);
            if ( ! ( arr instanceof RubyArray ) ) {
                throw context.runtime.newTypeError("expected @client_cert_cb.call to return an Array but got: " + arr.getMetaClass().getName());
            }
            final IRubyObject cert = ((RubyArray) arr).entry(0);
            final IRubyObject key = ((RubyArray) arr).entry(1);
            if ( ! ( cert instanceof X509Cert ) ) {
                throw context.runtime.newTypeError(cert.inspect() + " is not an instance of OpenSSL::X509::Certificate");
            }
            if ( ! ( key instanceof PKey ) ) {
                throw context.runtime.newTypeError(key.inspect() + " is not an instance of OpenSSL::PKey::PKey");
            }
            t_cert = (X509Cert) cert;
            t_key = (PKey) key;
        }
    }

    private X509Store getCertStore() {
        IRubyObject value = getInstanceVariable("@cert_store");
        if ( value instanceof X509Store ) {
            return (X509Store) value;
        }
        return null;
    }

    private String getCaFile() {
        IRubyObject value = getInstanceVariable("@ca_file");
        if ( value != null && ! value.isNil() ) {
            return value.convertToString().toString();
        }
        return null;
    }

    private String getCaPath() {
        IRubyObject value = getInstanceVariable("@ca_path");
        if ( value != null && ! value.isNil() ) {
            return value.convertToString().toString();
        }
        return null;
    }

    private long getOptions() {
        IRubyObject value = getInstanceVariable("@options");
        if ( value != null && ! value.isNil() ) {
            return RubyNumeric.fix2long(value);
        }
        return 0;
    }

    private X509Cert[] convertToX509Certs(final ThreadContext context, IRubyObject value) {
        final ArrayList<X509Cert> result = new ArrayList<X509Cert>();
        final RubyModule _SSLContext = context.runtime.getClassFromPath("OpenSSL::SSL::SSLContext");
        final RubyModule _Certificate = context.runtime.getClassFromPath("OpenSSL::X509::Certificate");
        Helpers.invoke(context, value, "each",
            CallBlock.newCallClosure(value, _SSLContext, Arity.NO_ARGUMENTS, new BlockCallback() {

                public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
                    final IRubyObject cert = args[0];
                    if ( _Certificate.isInstance(cert) ) {
                        throw context.runtime.newTypeError("wrong argument : " + cert.inspect() + " is not a " + _Certificate.getName());
                    }
                    result.add((X509Cert) cert);
                    return context.runtime.getNil();
                }

            }, context)
        );
        return result.toArray( new X509Cert[ result.size() ] );
    }

    /**
     * c: SSL_CTX
     */
    private class InternalContext {

        Store store = null;
        int verifyMode = SSL.VERIFY_NONE;
        X509AuxCertificate cert;
        String keyAlgorithm;
        PrivateKey privateKey;
        List<X509AuxCertificate> extraChainCert;
        final List<X509AuxCertificate> clientCert = new ArrayList<X509AuxCertificate>();
        int timeout = 0;
        String protocol = null;
        boolean protocolForServer = true;
        boolean protocolForClient = true;
        private javax.net.ssl.SSLContext sslContext;

        void setLastVerifyResultInternal(int lastVerifyResult) {
            setLastVerifyResult(lastVerifyResult);
        }

        javax.net.ssl.SSLContext getSSLContext() {
            return sslContext;
        }

        void init() throws GeneralSecurityException {
            sslContext = SecurityHelper.getSSLContext(protocol);
            if (protocolForClient) {
                sslContext.getClientSessionContext().setSessionTimeout(timeout);
            }
            if (protocolForServer) {
                sslContext.getServerSessionContext().setSessionTimeout(timeout);
            }
            sslContext.init(
                new KeyManager[] { new KeyManagerImpl(this) },
                new TrustManager[] { new TrustManagerImpl(this) },
                null
            );
        }

        // part of ssl_verify_cert_chain
        StoreContext createStoreContext(final String purpose) {
            if ( store == null ) return null;

            final StoreContext storeContext = new StoreContext();
            if ( storeContext.init(store, null, null) == 0 ) {
                return null;
            }
            // for verify_cb
            storeContext.setExtraData(1, store.getExtraData(1));
            if ( purpose != null ) {
                storeContext.setDefault(purpose);
            }
            storeContext.verifyParameter.inherit(store.verifyParameter);
            return storeContext;
        }
    }

    private static class KeyManagerImpl extends X509ExtendedKeyManager {

        final InternalContext internalContext;

        KeyManagerImpl(InternalContext internalContext) {
            super();
            this.internalContext = internalContext;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, java.security.Principal[] issuers, javax.net.ssl.SSLEngine engine) {
            if (internalContext == null) {
                return null;
            }
            if (internalContext.privateKey == null) {
                return null;
            }
            for (int i = 0; i < keyType.length; i++) {
                if (keyType[i].equalsIgnoreCase(internalContext.keyAlgorithm)) {
                    return keyType[i];
                }
            }
            return null;
        }

        @Override
        public String chooseEngineServerAlias(String keyType, java.security.Principal[] issuers, javax.net.ssl.SSLEngine engine) {
            if (internalContext == null || internalContext.privateKey == null) {
                return null;
            }
            if (keyType.equalsIgnoreCase(internalContext.keyAlgorithm)) {
                return keyType;
            }
            return null;
        }

        @Override
        public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, java.security.Principal[] issuers, java.net.Socket socket) {
            return null;
        }

        @Override // c: ssl3_output_cert_chain
        public java.security.cert.X509Certificate[] getCertificateChain(String alias) {
            if (internalContext == null) {
                return null;
            }
            ArrayList<java.security.cert.X509Certificate> chain = new ArrayList<java.security.cert.X509Certificate>();
            if (internalContext.extraChainCert != null) {
                chain.addAll(internalContext.extraChainCert);
            } else if (internalContext.cert != null) {
                StoreContext storeCtx = internalContext.createStoreContext(null);
                X509AuxCertificate x = internalContext.cert;
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

        @Override
        public String[] getClientAliases(String keyType, java.security.Principal[] issuers) {
            return null;
        }

        @Override
        public java.security.PrivateKey getPrivateKey(String alias) {
            if (internalContext == null || internalContext.privateKey == null) {
                return null;
            }
            return internalContext.privateKey;
        }

        @Override
        public String[] getServerAliases(String keyType, java.security.Principal[] issuers) {
            return null;
        }

    }

    private static class TrustManagerImpl implements X509TrustManager {

        final InternalContext internalContext;

        TrustManagerImpl(InternalContext internalContext) {
            super();
            this.internalContext = internalContext;
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("ssl_client", chain);
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            checkTrusted("ssl_server", chain);
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            if (internalContext == null) {
                return null;
            }
            ArrayList<java.security.cert.X509Certificate> chain = new ArrayList<java.security.cert.X509Certificate>();
            chain.addAll(internalContext.clientCert);
            return chain.toArray(new java.security.cert.X509Certificate[0]);
        }

        // c: ssl_verify_cert_chain
        private void checkTrusted(String purpose, X509Certificate[] chain) throws CertificateException {
            if (internalContext == null) {
                throw new CertificateException("uninitialized trust manager");
            }
            if (chain != null && chain.length > 0) {
                if ((internalContext.verifyMode & SSL.VERIFY_PEER) != 0) {
                    // verify_peer
                    final StoreContext storeContext = internalContext.createStoreContext(purpose);
                    if ( storeContext == null ) {
                        throw new CertificateException("couldn't initialize store");
                    }
                    storeContext.setCertificate(chain[0]);
                    storeContext.setChain(chain);
                    verifyChain(storeContext);
                }
            } else {
                if ((internalContext.verifyMode & SSL.VERIFY_FAIL_IF_NO_PEER_CERT) != 0) {
                    // fail if no peer cert
                    throw new CertificateException("no peer certificate");
                }
            }
        }

        private void verifyChain(final StoreContext storeContext) throws CertificateException {
            try {
                int ok = storeContext.verifyCertificate();
                internalContext.setLastVerifyResultInternal(storeContext.error);
                if (ok == 0) {
                    throw new CertificateException("certificate verify failed");
                }
            } catch (Exception e) {
                internalContext.setLastVerifyResultInternal(storeContext.error);
                if (storeContext.error == X509Utils.V_OK) {
                    internalContext.setLastVerifyResultInternal(X509Utils.V_ERR_CERT_REJECTED);
                }
                throw new CertificateException("certificate verify failed", e);
            }
        }
    }
}// SSLContext
