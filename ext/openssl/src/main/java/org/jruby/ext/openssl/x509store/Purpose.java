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

import java.util.ArrayList;
import java.util.List;

import java.security.cert.CertificateException;

/**
 * c: X509_PURPOSE
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Purpose {
    private static final String XKU_EMAIL_PROTECT = "1.3.6.1.5.5.7.3.4";    // Email protection
    private static final String XKU_SSL_CLIENT = "1.3.6.1.5.5.7.3.2";       // SSL Client Authentication
    private static final String[] XKU_SSL_SERVER = new String[]{
        "1.3.6.1.5.5.7.3.1",        // SSL Server Authentication
        "2.16.840.1.113730.4.1",    // Netscape Server Gated Crypto
        "1.3.6.1.4.1.311.10.3.3"    // Microsoft Server Gated Crypto
    };

    static interface CheckPurposeFunction extends Function3<Purpose, X509AuxCertificate, Integer> {

        int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException ;

    }

    public int purpose;
    public int trust;		/* Default trust ID */
    public int flags;
    CheckPurposeFunction checkPurpose;
    public String name;
    public String sname;
    public Object userData;

    private Purpose() {}

    Purpose(int p, int t, int f, CheckPurposeFunction cp, String n, String s, Object u) {
        this.purpose = p; this.trust = t;
        this.flags = f; this.checkPurpose = cp;
        this.name = n; this.sname = s;
        this.userData = u;
    }

    /**
     * c: X509_check_purpose
     */
    public static int checkPurpose(X509AuxCertificate x, int id, int ca) throws CertificateException {
        if ( id == -1 ) return 1;

        int idx = getByID(id);
        if ( idx == -1 ) return -1;

        Purpose pt = getFirst(idx);
        return pt.checkPurpose.call(pt, x ,Integer.valueOf(ca));
    }

    /**
     * c: X509_PURPOSE_set
     */
    public static int set(int[] p, int purpose) {
        if(getByID(purpose) == -1) {
            X509Error.addError(X509Utils.X509V3_R_INVALID_PURPOSE);
            return 0;
        }
        p[0] = purpose;
        return 1;
    }

    private final static List<Purpose> xptable = new ArrayList<Purpose>();

    /**
     * c: X509_PURPOSE_get_count
     */
    public static int getCount() {
        return xptable.size() + xstandard.length;
    }

    /**
     * c: X509_PURPOSE_get0
     */
    public static Purpose getFirst(int idx) {
        if(idx < 0) {
            return null;
        }
        if(idx < xstandard.length) {
            return xstandard[idx];
        }
        return xptable.get(idx - xstandard.length);
    }

    /**
     * c: X509_PURPOSE_get_by_sname
     */
    public static int getBySName(String sname) {
        for(int i=0;i<getCount();i++) {
            Purpose xptmp = getFirst(i);
            if(xptmp.sname.equals(sname)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * c: X509_PURPOSE_getby_id
     */
    public static int getByID(int purpose) {
        if(purpose >= X509Utils.X509_PURPOSE_MIN && (purpose <= X509Utils.X509_PURPOSE_MAX)) {
            return purpose - X509Utils.X509_PURPOSE_MIN;
        }
        int i = 0;
        for(Purpose p : xptable) {
            if(p.purpose == purpose) {
                return i + xstandard.length;
            }
        }
        return -1;
    }

    /**
     * c: X509_PURPOSE_add
     */
    public static int add(int id, int trust, int flags, CheckPurposeFunction ck, String name, String sname, Object arg) {
        flags &= ~X509Utils.X509_PURPOSE_DYNAMIC;
        flags |= X509Utils.X509_PURPOSE_DYNAMIC_NAME;
        int idx = getByID(id);
        Purpose ptmp;
        if(idx == -1) {
            ptmp = new Purpose();
            ptmp.flags = X509Utils.X509_PURPOSE_DYNAMIC;
        } else {
            ptmp = getFirst(idx);
        }
        ptmp.name = name;
        ptmp.sname = sname;
        ptmp.flags &= X509Utils.X509_PURPOSE_DYNAMIC;
        ptmp.flags |= flags;
        ptmp.purpose = id;
        ptmp.trust = trust;
        ptmp.checkPurpose = ck;
        ptmp.userData = arg;
        if(idx == -1) {
            xptable.add(ptmp);
        }
        return 1;
    }

    /**
     * c: X509_PURPOSE_cleanup
     */
    public static void cleanup() {
        xptable.clear();
    }

    /**
     * c: X509_PURPOSE_get_id
     */
    public int getID() {
        return purpose;
    }

    /**
     * c: X509_PURPOSE_get0_name
     */
    public String getName() {
        return name;
    }

    /**
     * c: X509_PURPOSE_get0_sname
     */
    public String getSName() {
        return sname;
    }

    /**
     * c: X509_PURPOSE_get_trust
     */
    public int getTrust() {
        return trust;
    }

    /**
     * c: X509_check_ca
     */
    public static int checkCA(X509AuxCertificate x) throws CertificateException {
        if(x.getKeyUsage() != null && !x.getKeyUsage()[5]) { // KEY_CERT_SIGN
            return 0;
        }
        if(x.getExtensionValue("2.5.29.19") != null) { // BASIC_CONSTRAINTS
            if(x.getBasicConstraints() != -1) { // is CA.
                return 1;
            } else {
                return 0;
            }
        } else {
            if(x.getVersion() == 1 && x.getIssuerX500Principal().equals(x.getSubjectX500Principal())) { // V1_ROOT
                return 3;
            }
            if(x.getKeyUsage() != null) {
                return 4;
            }
            Integer nsCertType = x.getNsCertType();
            if (nsCertType != null && (nsCertType & X509Utils.NS_ANY_CA) != 0) {
                return 5;
            }
            return 0;
        }
    }

     /**
     * c: check_ssl_ca
     */
    public static int checkSSLCA(X509AuxCertificate x) throws CertificateException {
        int ca_ret = checkCA(x);
        if(ca_ret == 0) {
            return 0;
        }
        Integer nsCertType = x.getNsCertType();
        boolean v2 = nsCertType != null && (nsCertType & X509Utils.NS_SSL_CA) != 0;
        if(ca_ret != 5 || v2) {
            return ca_ret;
        }
        return 0;
    }

     /**
     * c: xku_reject: check if the cert must be rejected(true) or not
     */
    public static boolean xkuReject(X509AuxCertificate x, String mustHaveXku) throws CertificateException {
        List<String> xku = x.getExtendedKeyUsage();
        return (xku != null) && !xku.contains(mustHaveXku);
    }
    public static boolean xkuReject(X509AuxCertificate x, String[] mustHaveOneOfXku) throws CertificateException {
        List<String> xku = x.getExtendedKeyUsage();
        if(xku == null) {
            return false;
        }
        for (String mustHaveXku : mustHaveOneOfXku) {
            if(xku.contains(mustHaveXku)) {
                return false;
            }
        }
        return true;
    }

     /**
     * c: ns_reject
     */
    public static boolean nsReject(X509AuxCertificate x, int mustHaveCertType) throws CertificateException {
        Integer nsCertType = x.getNsCertType();
        return (nsCertType != null) && (nsCertType & mustHaveCertType) == 0;
    }

     /**
     * c: purpose_smime
     */
    public static int purposeSMIME(X509AuxCertificate x, int ca) throws CertificateException {
        if(xkuReject(x,XKU_EMAIL_PROTECT)) {
            return 0; // must allow email protection
        }
        if(ca != 0) {
            int ca_ret = checkCA(x);
            if(ca_ret == 0) {
                return 0;
            }
            Integer nsCertType = x.getNsCertType();
            boolean v2 = nsCertType != null && (nsCertType & X509Utils.NS_SMIME_CA) != 0;
            if(ca_ret != 5 || v2) {
                return ca_ret;
            } else {
                return 0;
            }
        }
        Integer nsCertType = x.getNsCertType();
        if (nsCertType != null) {
            if ((nsCertType & X509Utils.NS_SMIME) != 0) {
                return 1;
            }
            if ((nsCertType & X509Utils.NS_SSL_CLIENT) != 0) {
                return 2;
            }
            return 0;
        }
        return 1;
    }

    /**
     * c: check_purpose_ssl_client
     */
     final static CheckPurposeFunction checkPurposeSSLClient = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            if ( xkuReject(x, XKU_SSL_CLIENT) ) {
                return 0;
            }
            if (ca.intValue() != 0) {
                return checkSSLCA(x);
            }
            if ( x.getKeyUsage() != null && ! x.getKeyUsage()[0] ) {
                return 0;
            }
            if ( nsReject(x, X509Utils.NS_SSL_CLIENT) ) {
                // when the cert has nsCertType, it must include NS_SSL_CLIENT
                return 0;
            }
            return 1;
        }
    };

    /**
     * c: check_purpose_ssl_server
     */
    final static CheckPurposeFunction checkPurposeSSLServer = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            if ( xkuReject(x, XKU_SSL_SERVER) ) {
                return 0;
            }
            if ( ca.intValue() != 0 ) {
                return checkSSLCA(x);
            }
            if ( nsReject(x, X509Utils.NS_SSL_SERVER) ) {
                // when the cert has nsCertType, it must include NS_SSL_SERVER
                return 0;
            }
            /* Now as for keyUsage: we'll at least need to sign OR encipher */
            if ( x.getKeyUsage() != null && ! ( x.getKeyUsage()[0] || x.getKeyUsage()[2] ) ) {
                return 0;
            }
            return 1;
        }
    };

    /**
     * c: check_purpose_ns_ssl_server
     */
    final static CheckPurposeFunction checkPurposeNSSSLServer = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            int ret = checkPurposeSSLServer.call(purpose, x, ca);
            if ( ret == 0 || ca != 0 ) {
                return ret;
            }
            if ( x.getKeyUsage() != null && ! x.getKeyUsage()[2] ) {
                return 0;
            }
            return 1;
        }
    };

    /**
     * c: check_purpose_smime_sign
     */
    final static CheckPurposeFunction checkPurposeSMIMESign = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            int ret = purposeSMIME(x, ca);
            if ( ret == 0 || ca != 0 ) {
                return ret;
            }
            if ( x.getKeyUsage() != null && ( ! x.getKeyUsage()[0] || ! x.getKeyUsage()[1] ) ) {
                return 0;
            }
            return ret;
        }
    };

    /**
     * c: check_purpose_smime_encrypt
     */
    final static CheckPurposeFunction checkPurposeSMIMEEncrypt = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            int ret = purposeSMIME(x,ca);
            if ( ret == 0 || ca != 0 ) {
                return ret;
            }
            if ( x.getKeyUsage() != null && ! x.getKeyUsage()[2] ) {
                return 0;
            }
            return ret;
        }
    };

    /**
     * c: check_purpose_crl_sign
     */
    final static CheckPurposeFunction checkPurposeCRLSign = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            if ( ca.intValue() != 0 ) {
                int ca_ret = checkCA(x);
                if ( ca_ret != 2 ) {
                    return ca_ret;
                }
                return 0;
            }
            if ( x.getKeyUsage() != null && ! x.getKeyUsage()[6] ) {
                return 0;
            }
            return 1;
        }
    };

    /**
     * c: no_check
     */
    final static CheckPurposeFunction noCheck = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            return 1;
        }
    };

    /**
     * c: ocsp_helper
     */
    final static CheckPurposeFunction oscpHelper = new CheckPurposeFunction() {
        public int call(Purpose purpose, X509AuxCertificate x, Integer ca) throws CertificateException {
            if ( ca.intValue() != 0 ) {
                return checkCA(x);
            }
            return 1;
        }
    };

    private final static Purpose[] xstandard = new Purpose[] {
        new Purpose(X509Utils.X509_PURPOSE_SSL_CLIENT, X509Utils.X509_TRUST_SSL_CLIENT, 0, checkPurposeSSLClient, "SSL client", "sslclient", null),
        new Purpose(X509Utils.X509_PURPOSE_SSL_SERVER, X509Utils.X509_TRUST_SSL_SERVER, 0, checkPurposeSSLServer, "SSL server", "sslserver", null),
        new Purpose(X509Utils.X509_PURPOSE_NS_SSL_SERVER, X509Utils.X509_TRUST_SSL_SERVER, 0, checkPurposeNSSSLServer, "Netscape SSL server", "nssslserver", null),
        new Purpose(X509Utils.X509_PURPOSE_SMIME_SIGN, X509Utils.X509_TRUST_EMAIL, 0, checkPurposeSMIMESign, "S/MIME signing", "smimesign", null),
        new Purpose(X509Utils.X509_PURPOSE_SMIME_ENCRYPT, X509Utils.X509_TRUST_EMAIL, 0, checkPurposeSMIMEEncrypt, "S/MIME encryption", "smimeencrypt", null),
        new Purpose(X509Utils.X509_PURPOSE_CRL_SIGN, X509Utils.X509_TRUST_COMPAT, 0, checkPurposeCRLSign, "CRL signing", "crlsign", null),
        new Purpose(X509Utils.X509_PURPOSE_ANY, X509Utils.X509_TRUST_DEFAULT, 0, noCheck, "Any Purpose", "any", null),
        new Purpose(X509Utils.X509_PURPOSE_OCSP_HELPER, X509Utils.X509_TRUST_COMPAT, 0, oscpHelper, "OCSP helper", "ocsphelper", null),
    };
}// X509_PURPOSE
