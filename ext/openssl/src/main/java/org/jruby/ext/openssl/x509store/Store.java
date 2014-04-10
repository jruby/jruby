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
package org.jruby.ext.openssl.x509store;

import java.io.FileNotFoundException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.X509TrustManager;

/**
 * c: X509_STORE
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Store implements X509TrustManager {

    public static interface VerifyFunction extends Function1<StoreContext> {
        public static final VerifyFunction EMPTY = new VerifyFunction(){
            public int call(StoreContext context) {
                return -1;
            }
        };
    }
    public static interface VerifyCallbackFunction extends Function2<StoreContext, Integer> {
        public static final VerifyCallbackFunction EMPTY = new VerifyCallbackFunction(){
            public int call(StoreContext context, Integer outcome) {
                return -1;
            }
        };
    }
    static interface GetIssuerFunction extends Function3<StoreContext, X509AuxCertificate[], X509AuxCertificate> {
        public static final GetIssuerFunction EMPTY = new GetIssuerFunction(){
            public int call(StoreContext context, X509AuxCertificate[] issuer, X509AuxCertificate cert) {
                return -1;
            }
        };
    }
    static interface CheckIssuedFunction extends Function3<StoreContext, X509AuxCertificate, X509AuxCertificate> {
        public static final CheckIssuedFunction EMPTY = new CheckIssuedFunction(){
            public int call(StoreContext context, X509AuxCertificate cert, X509AuxCertificate issuer) throws Exception {
                return -1;
            }
        };
    }
    static interface CheckRevocationFunction extends Function1<StoreContext> {
        public static final CheckRevocationFunction EMPTY = new CheckRevocationFunction(){
            public int call(StoreContext context) {
                return -1;
            }
        };
    }
    static interface GetCRLFunction extends Function3<StoreContext, java.security.cert.X509CRL[], X509AuxCertificate> {
        public static final GetCRLFunction EMPTY = new GetCRLFunction(){
            public int call(StoreContext context, java.security.cert.X509CRL[] crls, X509AuxCertificate cert) {
                return -1;
            }
        };
    }
    static interface CheckCRLFunction extends Function2<StoreContext, java.security.cert.X509CRL> {
        public static final CheckCRLFunction EMPTY = new CheckCRLFunction(){
            public int call(StoreContext context, java.security.cert.X509CRL crl) {
                return -1;
            }
        };
    }
    static interface CertificateCRLFunction extends Function3<StoreContext, java.security.cert.X509CRL, X509AuxCertificate> {
        public static final CertificateCRLFunction EMPTY = new CertificateCRLFunction(){
            public int call(StoreContext context, java.security.cert.X509CRL crl, X509AuxCertificate cert) {
                return -1;
            }
        };
    }
    static interface CleanupFunction extends Function1<StoreContext> {
        public static final CleanupFunction EMPTY = new CleanupFunction(){
            public int call(StoreContext context) {
                return -1;
            }
        };
    }

    @Deprecated int cache; // not-used

    final List<X509Object> objects;
    final List<Lookup> certificateMethods;

    public final VerifyParameter verifyParameter;

    VerifyFunction verify = VerifyFunction.EMPTY;
    VerifyCallbackFunction verifyCallback = VerifyCallbackFunction.EMPTY;

    GetIssuerFunction getIssuer = GetIssuerFunction.EMPTY;
    CheckIssuedFunction checkIssued = CheckIssuedFunction.EMPTY;
    CheckRevocationFunction checkRevocation = CheckRevocationFunction.EMPTY;
    GetCRLFunction getCRL = GetCRLFunction.EMPTY;
    CheckCRLFunction checkCRL = CheckCRLFunction.EMPTY;
    CertificateCRLFunction certificateCRL = CertificateCRLFunction.EMPTY;
    CleanupFunction cleanup = CleanupFunction.EMPTY;

    public final List<Object> extraData;
    public final int references;

    /**
     * c: X509_STORE_new
     */
    public Store() {
        objects = new ArrayList<X509Object>();
        cache = 1;
        certificateMethods = new ArrayList<Lookup>();

        verifyParameter = new VerifyParameter();

        references = 1;
        extraData = new ArrayList<Object>(10);
        this.extraData.add(null); this.extraData.add(null); this.extraData.add(null);
        this.extraData.add(null); this.extraData.add(null); this.extraData.add(null);
        this.extraData.add(null); this.extraData.add(null); this.extraData.add(null);
    }

    public List<X509Object> getObjects() {
        return objects;
    }

    public List<Lookup> getCertificateMethods() {
        return certificateMethods;
    }

    public VerifyParameter getVerifyParameter() {
        return verifyParameter;
    }

    public VerifyFunction getVerifyFunction() {
        return verify;
    }

    /**
     * c: X509_STORE_set_verify_func
     */
    public void setVerifyFunction(VerifyFunction func) {
        verify = func;
    }

    public VerifyCallbackFunction getVerifyCallback() {
        return verifyCallback;
    }

    /**
     * c: X509_STORE_set_verify_cb_func
     */
    public void setVerifyCallbackFunction(VerifyCallbackFunction func) {
        verifyCallback = func;
    }

    /**
     * c: X509_STORE_free
     */
    public void free() throws Exception {
        for (Lookup lu : certificateMethods) {
            lu.shutdown();
            lu.free();
        }
        if (verifyParameter != null) {
            verifyParameter.free();
        }
    }

    /**
     * c: X509_set_ex_data
     */
    public int setExtraData(int idx, Object data) {
        extraData.set(idx,data);
        return 1;
    }

    /**
     * c: X509_get_ex_data
     */
    public Object getExtraData(int idx) {
        return extraData.get(idx);
    }

    /**
     * c: X509_STORE_set_depth
     */
    public int setDepth(int depth) {
        verifyParameter.setDepth(depth);
        return 1;
    }

    /**
     * c: X509_STORE_set_flags
     */
    public int setFlags(long flags) {
        return verifyParameter.setFlags(flags);
    }

    /**
     * c: X509_STORE_set_purpose
     */
    public int setPurpose(int purpose) {
        return verifyParameter.setPurpose(purpose);
    }

    /**
     * c: X509_STORE_set_trust
     */
    public int setTrust(int trust) {
        return verifyParameter.setTrust(trust);
    }

    /**
     * c: X509_STORE_set1_param
     */
    public int setParam(VerifyParameter pm) {
        return verifyParameter.set(verifyParameter);
    }

    /**
     * c: X509_STORE_add_lookup
     */
    public Lookup addLookup(final LookupMethod method) throws Exception {
        for ( Lookup lookup : certificateMethods ) {
            if ( lookup.equals(method) ) return lookup;
        }
        Lookup lookup = new Lookup(method);
        lookup.store = this;
        certificateMethods.add(lookup);
        return lookup;
    }

    /**
     * c: X509_STORE_add_cert
     */
    public int addCertificate(final X509Certificate cert) {
        if ( cert == null ) return 0;

        final Certificate certObj = new Certificate();
        certObj.x509 = StoreContext.ensureAux(cert);

        int ret = 1;
        synchronized(X509Utils.CRYPTO_LOCK_X509_STORE) {
            if ( X509Object.retrieveMatch(objects,certObj) != null ) {
                X509Error.addError(X509Utils.X509_R_CERT_ALREADY_IN_HASH_TABLE);
                ret = 0;
            }
            else {
                objects.add(certObj);
            }
        }
        return ret;
    }

    /**
     * c: X509_STORE_add_crl
     */
    public int addCRL(final java.security.cert.CRL crl) {
        if ( crl == null ) return 0;

        final CRL crlObj = new CRL(); crlObj.crl = crl;

        int ret = 1;
        synchronized(X509Utils.CRYPTO_LOCK_X509_STORE) {
            if ( X509Object.retrieveMatch(objects,crlObj) != null ) {
                X509Error.addError(X509Utils.X509_R_CERT_ALREADY_IN_HASH_TABLE);
                ret = 0;
            }
            else {
                objects.add(crlObj);
            }
        }
        return ret;
    }

    /**
     * c: X509_STORE_load_locations
     */
    public int loadLocations(String file, String path) throws Exception {
        if ( file != null ) {
            final Lookup lookup = addLookup( Lookup.fileLookup() );
            if ( lookup == null ) {
                return 0;
            }
            if ( lookup.loadFile(new CertificateFile.Path(file,X509Utils.X509_FILETYPE_PEM)) != 1 ) {
                return 0;
            }
        }

        if ( path != null ) {
            final Lookup lookup = addLookup( Lookup.hashDirLookup() );
            if ( lookup == null ) {
                return 0;
            }
            if ( lookup.addDir(new CertificateHashDir.Dir(path,X509Utils.X509_FILETYPE_PEM)) != 1 ) {
                return 0;
            }
        }

        if ( path == null && file == null ) return 0;
        return 1;
    }

    /**
     * c: X509_STORE_set_default_paths
     */
    public int setDefaultPaths() throws Exception {

        Lookup lookup = addLookup(Lookup.fileLookup());
        //if ( lookup == null ) return 0;

        try {
            lookup.loadFile(new CertificateFile.Path(null, X509Utils.X509_FILETYPE_DEFAULT));
        }
        catch (FileNotFoundException e) {
            // set_default_paths ignores FileNotFound
        }

        lookup = addLookup(Lookup.hashDirLookup());
        //if ( lookup == null ) return 0;

        try {
            lookup.addDir(new CertificateHashDir.Dir(null, X509Utils.X509_FILETYPE_DEFAULT));
        }
        catch(FileNotFoundException e) {
            // set_default_paths ignores FileNotFound
        }

        X509Error.clearErrors();
        return 1;
    }


    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    public X509Certificate[] getAcceptedIssuers() {
        ArrayList<X509Certificate> issuers = new ArrayList<X509Certificate>(objects.size());
        for ( X509Object object : objects ) {
            if ( object instanceof Certificate ) {
                issuers.add( ( (Certificate) object ).x509 );
            }
        }
        return issuers.toArray( new X509Certificate[ issuers.size() ] );
    }

}// X509_STORE
