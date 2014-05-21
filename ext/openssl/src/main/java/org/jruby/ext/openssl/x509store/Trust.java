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

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

/**
 * c: X509_TRUST
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Trust {

    static interface Checker<T> extends Function3<T, X509AuxCertificate, Integer> {}

    public int trust;
    public int flags;
    private Checker<Trust> checkTrust;
    private String name;
    private String arg1;
    private Object arg2;

    private Trust() {}

    Trust(int trust, int flags, Checker<Trust> checkTrust,
        String n, String a1, Object a2) {
        this.trust = trust;
        this.flags = flags;
        this.checkTrust = checkTrust;
        this.name = n;
        this.arg1 = a1; this.arg2 = a2;
    }

    /**
     * c: X509_TRUST_set_default
     */
    /*
    public static Checker setDefault(Checker<String> trust) {
        Checker old_trust = defaultTrust;
        defaultTrust = trust;
        return old_trust;
    } */

    private final static List<Trust> trustable = new ArrayList<Trust>();

    /**
     * c: X509_check_trust
     */
    public static int checkTrust(X509AuxCertificate x, int id, int flags) throws Exception {
        if ( id == -1 ) return 1;

        int idx = getByID(id);
        if (idx == -1) {
            return defaultTrust.call(Integer.toString(id), x, Integer.valueOf(flags));
        }
        Trust pt = getFirst(idx);
        return pt.checkTrust.call(pt, x, Integer.valueOf(flags));
    }

    /**
     * c: X509_TRUST_get_count
     */
    public static int getCount() {
        return trustable.size() + trstandard.length;
    }

    /**
     * c: X509_TRUST_get0
     */
    public static Trust getFirst(int idx) {
        if(idx < 0) {
            return null;
        }
        if(idx < trstandard.length) {
            return trstandard[idx];
        }
        return trustable.get(idx - trstandard.length);
    }

    /**
     * c: X509_TRUST_get_by_id
     */
    public static int getByID(int id) {
        if ( id >= X509Utils.X509_TRUST_MIN && id <= X509Utils.X509_TRUST_MAX ) {
            return id - X509Utils.X509_TRUST_MIN;
        }
        int i = 0;
        for ( Trust t : trustable ) {
            if(t.trust == id) {
                return i + trstandard.length;
            }
        }
        return -1;
    }

    /**
     * c: X509_TRUST_set
     */
    public static int set(int[] t, int trust) {
        if(getByID(trust) == -1) {
            X509Error.addError(X509Utils.X509_R_INVALID_TRUST);
            return 0;
        }
        t[0] = trust;
        return 1;
    }

    /**
     * c: X509_TRUST_add
     */
    static int add(int id, int flags, Checker<Trust> ck, String name, String arg1, Object arg2) {
        int idx;
        Trust trtmp;
        flags &= ~X509Utils.X509_TRUST_DYNAMIC;
        flags |= X509Utils.X509_TRUST_DYNAMIC_NAME;
        idx = getByID(id);
        if(idx == -1) {
            trtmp = new Trust();
            trtmp.flags = X509Utils.X509_TRUST_DYNAMIC;
        } else {
            trtmp = getFirst(idx);
        }
        trtmp.name = name;
        trtmp.flags &= X509Utils.X509_TRUST_DYNAMIC;
        trtmp.flags |= flags;
        trtmp.trust = id;
        trtmp.checkTrust = ck;
        trtmp.arg1 = arg1;
        trtmp.arg2 = arg2;
        if(idx == -1) {
            trustable.add(trtmp);
        }
        return 1;
    }

    /**
     * c: X509_TRUST_cleanup
     */
    public static void cleanup() {
        trustable.clear();
    }

    /**
     * c: X509_TRUST_get_flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * c: X509_TRUST_get0_name
     */
    public String getName() {
        return name;
    }

    /**
     * c: X509_TRUST_get_trust
     */
    public int getTrust() {
        return trust;
    }

    /**
     * c: trust_compat
     */
    final static Checker<Trust> trustCompatibe = new Checker<Trust>() {
        public int call(final Trust trust,
            final X509AuxCertificate x, final Integer flags) throws CertificateException {
            Purpose.checkPurpose(x,-1,0);
            if ( x.getIssuerX500Principal().equals( x.getSubjectX500Principal() ) ) { // self signed
                return X509Utils.X509_TRUST_TRUSTED;
            } else {
                return X509Utils.X509_TRUST_UNTRUSTED;
            }
        }
    };

    /**
     * c: trust_1oidany
     */
    final static Checker<Trust> trust1OIDAny = new Checker<Trust>() {
        public int call(final Trust trust,
            final X509AuxCertificate x, final Integer flags) throws Exception {
            final X509Aux aux = x.getAux();
            if ( aux != null && ( aux.trust.size() > 0 || aux.reject.size() > 0 ) ) {
                return objTrust.call(trust.arg1, x, flags);
            }
            return trustCompatibe.call(trust, x, flags);
        }
    };

    /**
     * c: trust_1oid
     */
    final static Checker<Trust> trust1OID = new Checker<Trust>() {
        public int call(final Trust trust,
            final X509AuxCertificate x, final Integer flags) throws Exception {
            if ( x.getAux() != null ) {
                return objTrust.call(trust.arg1, x, flags);
            }
            return X509Utils.X509_TRUST_UNTRUSTED;
        }
    };

    /**
     * c: obj_trust
     */
    final static Checker<String> objTrust = new Checker<String>() {
        public int call(final String id,
            final X509AuxCertificate x, final Integer flags) {
            final X509Aux aux = x.getAux();
            if ( aux == null ) {
                return X509Utils.X509_TRUST_UNTRUSTED;
            }
            for ( String rejectId : aux.reject ) {
                if ( rejectId.equals(id) ) {
                    return X509Utils.X509_TRUST_REJECTED;
                }
            }
            for ( String trustId : aux.trust ) {
                if ( trustId.equals(id) ) {
                    return X509Utils.X509_TRUST_TRUSTED;
                }
            }
            return X509Utils.X509_TRUST_UNTRUSTED;
        }
    };

    /**
     * c: default_trust
     */
    static Checker<String> defaultTrust = objTrust;

    private final static Trust[] trstandard = new Trust[] {
        new Trust(X509Utils.X509_TRUST_COMPAT, 0, trustCompatibe, "compatible", null, null),
        new Trust(X509Utils.X509_TRUST_SSL_CLIENT, 0, trust1OIDAny, "SSL Client", "1.3.6.1.5.5.7.3.2", null),
        new Trust(X509Utils.X509_TRUST_SSL_SERVER, 0, trust1OIDAny, "SSL Server", "1.3.6.1.5.5.7.3.1", null),
        new Trust(X509Utils.X509_TRUST_EMAIL, 0, trust1OIDAny, "S/MIME email", "1.3.6.1.5.5.7.3.4", null),
        new Trust(X509Utils.X509_TRUST_OBJECT_SIGN, 0, trust1OIDAny, "Object Signer", "1.3.6.1.5.5.7.3.3", null),
        new Trust(X509Utils.X509_TRUST_OCSP_SIGN, 0, trust1OID, "OCSP responder", "1.3.6.1.5.5.7.3.9", null),
        new Trust(X509Utils.X509_TRUST_OCSP_REQUEST, 0, trust1OID, "OCSP request", "1.3.6.1.5.5.7.48.1", null),
    };
}// X509_TRUST
