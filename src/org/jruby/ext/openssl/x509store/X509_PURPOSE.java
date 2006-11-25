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
package org.jruby.ext.openssl.x509store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_PURPOSE {
    public int purpose;
    public int trust;		/* Default trust ID */
    public int flags;
    public Function3 check_purpose;
    public String name;
    public String sname;
    public Object usr_data;

    public X509_PURPOSE() {}

    public X509_PURPOSE(int p, int t, int f, Function3 cp, String n, String s, Object u) {
        this.purpose = p; this.trust = t;
        this.flags = f; this.check_purpose = cp;
        this.name = n; this.sname = s;
        this.usr_data = u;
    }

    public static int check_purpose(X509AuxCertificate x, int id, int ca) throws Exception {
        if(id == -1) {
            return 1;
        }
        int idx = get_by_id(id);
        if(idx == -1) {
            return -1;
        }
        X509_PURPOSE pt = get0(idx);
        return pt.check_purpose.call(pt,x,new Integer(ca));
    }

    public static int set(int[] p, int purpose) {
        if(get_by_id(purpose) == -1) {
            Err.PUT_err(X509.X509V3_R_INVALID_PURPOSE);
            return 0;
        }
        p[0] = purpose;
        return 1;
    }

    private final static List xptable = new ArrayList();

    public static int get_count() {
        return xptable.size() + xstandard.length;
    }

    public static X509_PURPOSE get0(int idx) {
        if(idx < 0) {
            return null;
        }
        if(idx < xstandard.length) {
            return xstandard[idx];
        }
        return (X509_PURPOSE)xptable.get(idx - xstandard.length);
    }

    public static int get_by_sname(String sname) {
        for(int i=0;i<get_count();i++) {
            X509_PURPOSE xptmp = get0(i);
            if(xptmp.sname.equals(sname)) {
                return i;
            }
        }
        return -1;
    }

    public static int get_by_id(int purpose) {
        if(purpose >= X509.X509_PURPOSE_MIN && (purpose <= X509.X509_PURPOSE_MAX)) {
            return purpose - X509.X509_PURPOSE_MIN;
        }
        int i = 0;
        for(Iterator iter = xptable.iterator();iter.hasNext();i++) {
            if(((X509_PURPOSE)iter.next()).purpose == purpose) {
                return i + xstandard.length;
            }
        }
        return -1;
    }

    public static int add(int id, int trust, int flags, Function3 ck, String name, String sname, Object arg) {
        flags &= ~X509.X509_PURPOSE_DYNAMIC;
        flags |= X509.X509_PURPOSE_DYNAMIC_NAME;
        int idx = get_by_id(id);
        X509_PURPOSE ptmp;
        if(idx == -1) {
            ptmp = new X509_PURPOSE();
            ptmp.flags = X509.X509_PURPOSE_DYNAMIC;
        } else {
            ptmp = get0(idx);
        }
        ptmp.name = name;
        ptmp.sname = sname;
        ptmp.flags &= X509.X509_PURPOSE_DYNAMIC;
        ptmp.flags |= flags;
        ptmp.purpose = id;
        ptmp.trust = trust;
        ptmp.check_purpose = ck;
        ptmp.usr_data = arg;
        if(idx == -1) {
            xptable.add(ptmp);
        }
        return 1;
    }

    public static void cleanup() {
        xptable.clear();
    }

    public int get_id() {
        return purpose;
    }
    public String get0_name() {
        return name;
    }
    public String get0_sname() {
        return sname;
    }
    public int get_trust() {
        return trust;
    }
 
    public static int check_ca(X509AuxCertificate x) throws Exception {
        if(x.getKeyUsage() != null && !x.getKeyUsage()[5]) { // KEY_CERT_SIGN
            return 0;
        }
        if(x.getExtensionValue("2.5.29.19") != null) { // BASIC_CONSTRAINTS
            if(x.getBasicConstraints() != -1) {
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
            byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
            if(ns1 != null && (((DERBitString)new ASN1InputStream(ns1).readObject()).intValue() & X509.NS_ANY_CA) != 0) {
                return 5;
            }
            return 0;
        }
    }

    public static int check_ssl_ca(X509AuxCertificate x) throws Exception {
        int ca_ret = check_ca(x);
        if(ca_ret == 0) {
            return 0;
        }
        byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
        boolean v2 = ns1 != null && (((DERBitString)new ASN1InputStream(ns1).readObject()).intValue() & X509.NS_SSL_CA) != 0;
        if(ca_ret != 5 || v2) {
            return ca_ret;
        }
        return 0;
    }

    public static int purpose_smime(X509AuxCertificate x, int ca) throws Exception {
        if(x.getExtendedKeyUsage() != null && !x.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.4")) {
            return 0; // must allow email protection
        }
        if(ca != 0) {
            int ca_ret = check_ca(x);
            if(ca_ret == 0) {
                return 0;
            }
            byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
            boolean v2 = ns1 != null && (((DERBitString)new ASN1InputStream(ns1).readObject()).intValue() & X509.NS_SMIME_CA) != 0;
            if(ca_ret != 5 || v2) {
                return ca_ret;
            } else {
                return 0;
            }
        }
        byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
        if(ns1 != null) {
            int nscert = ((DERBitString)new ASN1InputStream(ns1).readObject()).intValue();
            if((nscert & X509.NS_SMIME) != 0) {
                return 1;
            }
            if((nscert & X509.NS_SSL_CLIENT) != 0) {
                return 2;
            }
            return 0;
        }
        return 1;
    }

    public final static Function3 cp_ssl_client = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();

                if(x.getExtendedKeyUsage() != null && !x.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2")) {
                    return 0;
                }
                if(ca != 0) {
                    return check_ssl_ca(x);
                }
                if(x.getKeyUsage() != null && !x.getKeyUsage()[0]) {
                    return 0;
                }
                byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
                boolean v2 = ns1 != null && (((DERBitString)new ASN1InputStream(ns1).readObject()).intValue() & X509.NS_SSL_CLIENT) != 0;
                if(v2) {
                    return 0;
                }
                return 1;
            }
        };

    public final static Function3 cp_ssl_server =  new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();

                if(x.getExtendedKeyUsage() != null && (!x.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.1") && 
                                                       !x.getExtendedKeyUsage().contains("2.16.840.1.113730.4.1") &&
                                                       !x.getExtendedKeyUsage().contains("1.3.6.1.4.1.311.10.3.3"))) {
                    return 0;
                }
                if(ca != 0) {
                    return check_ssl_ca(x);
                }
                byte[] ns1 = x.getExtensionValue("2.16.840.1.113730.1.1"); //nsCertType
                boolean v2 = ns1 != null && (((DERBitString)new ASN1InputStream(ns1).readObject()).intValue() & X509.NS_SSL_SERVER) != 0;
                if(v2) {
                    return 0;
                }
                if(x.getKeyUsage() != null && (!x.getKeyUsage()[0] || !x.getKeyUsage()[2])) {
                    return 0;
                }
                return 1;
            }
        };

    public final static Function3 cp_ns_ssl_server = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();
                int ret = cp_ssl_server.call(xp,x,_ca);
                if(ret == 0 || ca != 0) {
                    return ret;
                }
                if(x.getKeyUsage() != null && !x.getKeyUsage()[2]) {
                    return 0;
                }
                return 1;
            }
        };

    public final static Function3 cp_smime_sign = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();
                int ret = purpose_smime(x,ca);
                if(ret == 0 || ca != 0) {
                    return ret;
                }
                if(x.getKeyUsage() != null && (!x.getKeyUsage()[0] || !x.getKeyUsage()[1])) {
                    return 0;
                }
                return ret;
            }
        };

    public final static Function3 cp_smime_encrypt = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();
                int ret = purpose_smime(x,ca);
                if(ret == 0 || ca != 0) {
                    return ret;
                }
                if(x.getKeyUsage() != null && !x.getKeyUsage()[2]) {
                    return 0;
                }
                return ret;
            }
        };

    public final static Function3 cp_crl_sign = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                X509_PURPOSE xp = (X509_PURPOSE)_xp;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int ca = ((Integer)_ca).intValue();
                
                if(ca != 0) {
                    int ca_ret = check_ca(x);
                    if(ca_ret != 2) {
                        return ca_ret;
                    }
                    return 0;
                }
                if(x.getKeyUsage() != null && !x.getKeyUsage()[6]) {
                    return 0;
                }
                return 1;
            }
        };

    public final static Function3 cp_no_check = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) {
                return 1;
            }
        };
    public final static Function3 cp_ocsp_helper = new Function3() {
            public int call(Object _xp, Object _x, Object _ca) throws Exception {
                if(((Integer)_ca).intValue() != 0) {
                    return check_ca((X509AuxCertificate)_x);
                }
                return 1;
            }
        };

    public final static X509_PURPOSE[] xstandard = new X509_PURPOSE[] {
	new X509_PURPOSE(X509.X509_PURPOSE_SSL_CLIENT, X509.X509_TRUST_SSL_CLIENT, 0, cp_ssl_client, "SSL client", "sslclient", null),
	new X509_PURPOSE(X509.X509_PURPOSE_SSL_SERVER, X509.X509_TRUST_SSL_SERVER, 0, cp_ssl_server, "SSL server", "sslserver", null),
	new X509_PURPOSE(X509.X509_PURPOSE_NS_SSL_SERVER, X509.X509_TRUST_SSL_SERVER, 0, cp_ns_ssl_server, "Netscape SSL server", "nssslserver", null),
	new X509_PURPOSE(X509.X509_PURPOSE_SMIME_SIGN, X509.X509_TRUST_EMAIL, 0, cp_smime_sign, "S/MIME signing", "smimesign", null),
	new X509_PURPOSE(X509.X509_PURPOSE_SMIME_ENCRYPT, X509.X509_TRUST_EMAIL, 0, cp_smime_encrypt, "S/MIME encryption", "smimeencrypt", null),
	new X509_PURPOSE(X509.X509_PURPOSE_CRL_SIGN, X509.X509_TRUST_COMPAT, 0, cp_crl_sign, "CRL signing", "crlsign", null),
	new X509_PURPOSE(X509.X509_PURPOSE_ANY, X509.X509_TRUST_DEFAULT, 0, cp_no_check, "Any Purpose", "any", null),
	new X509_PURPOSE(X509.X509_PURPOSE_OCSP_HELPER, X509.X509_TRUST_COMPAT, 0, cp_ocsp_helper, "OCSP helper", "ocsphelper", null),
    };
}// X509_PURPOSE
