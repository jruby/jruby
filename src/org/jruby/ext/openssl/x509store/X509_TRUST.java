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

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_TRUST {
    public int trust;
    public int flags;
    public Function3 check_trust;
    public String name;
    public String arg1;
    public Object arg2;

    public X509_TRUST() {}

    public X509_TRUST(int t, int f, Function3 ct, String n, String a1, Object a2) {
        this.trust = t;
        this.flags = f; this.check_trust = ct;
        this.name = n; this.arg1 = a1;
        this.arg2 = a2;
    }

    public static Function3 set_default(Function3 trust) {
        Function3 old_trust = default_trust;
        default_trust = trust;
        return old_trust;
    }

    private final static List trtable = new ArrayList();

    public static int check_trust(X509AuxCertificate x, int id, int flags) throws Exception {
        if(id == -1) {
            return 1;
        }
        int idx = get_by_id(id);
        if(idx == -1) {
            return default_trust.call(new Integer(id),x,new Integer(flags));
        }
        X509_TRUST pt = get0(idx);
        return pt.check_trust.call(pt,x,new Integer(flags));
    }

    public static int get_count() {
        return trtable.size() + trstandard.length;
    }

    public static X509_TRUST get0(int idx) {
        if(idx < 0) {
            return null;
        }
        if(idx < trstandard.length) {
            return trstandard[idx];
        }
        return (X509_TRUST)trtable.get(idx - trstandard.length);
    }

    public static int get_by_id(int id) {
        if(id >= X509.X509_TRUST_MIN && id <= X509.X509_TRUST_MAX) {
            return id - X509.X509_TRUST_MIN;
        }
        int i = 0;
        for(Iterator iter = trtable.iterator();iter.hasNext();i++) {
            if(((X509_TRUST)iter.next()).trust == id) {
                return i + trstandard.length;
            }
        }
        return -1;
    }

    public static int set(int[] t, int trust) {
        if(get_by_id(trust) == -1) {
            Err.PUT_err(X509.X509_R_INVALID_TRUST);
            return 0;
        }
        t[0] = trust;
        return 1;
    }

    public static int add(int id, int flags, Function3 ck, String name, String arg1, Object arg2) {
        int idx;
        X509_TRUST trtmp;
        flags &= ~X509.X509_TRUST_DYNAMIC;
        flags |= X509.X509_TRUST_DYNAMIC_NAME;
        idx = get_by_id(id);
        if(idx == -1) {
            trtmp = new X509_TRUST();
            trtmp.flags = X509.X509_TRUST_DYNAMIC;
        } else {
            trtmp = get0(idx);
        }
        trtmp.name = name;
        trtmp.flags &= X509.X509_TRUST_DYNAMIC;
        trtmp.flags |= flags;
        trtmp.trust = id;
        trtmp.check_trust = ck;
        trtmp.arg1 = arg1;
        trtmp.arg2 = arg2;
        if(idx == -1) {
            trtable.add(trtmp);
        }
        return 1;
    }

    public static void cleanup() {
        trtable.clear();
    }
    
    public int get_flags() {
	return flags;
    }

    public String get0_name() {
	return name;
    }

    public int get_trust() {
	return trust;
    }

    public final static Function3 trust_compat = new Function3() {
            public int call(Object _trust, Object _x, Object _flags) throws Exception {
                X509_TRUST trust = (X509_TRUST)_trust;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int flags = ((Integer)_flags).intValue();

                X509_PURPOSE.check_purpose(x,-1,0);
                if(x.getIssuerX500Principal().equals(x.getSubjectX500Principal())) { // self signed
                    return X509.X509_TRUST_TRUSTED;
                } else {
                    return X509.X509_TRUST_UNTRUSTED;
                }
            }
        };
    public final static Function3 trust_1oidany = new Function3() {
            public int call(Object _trust, Object _x, Object _flags) throws Exception {
                X509_TRUST trust = (X509_TRUST)_trust;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int flags = ((Integer)_flags).intValue();

                X509_AUX ax = x.getAux();
                if(ax != null && (ax.trust.size() > 0 || ax.reject.size() > 0)) {
                    return obj_trust.call(trust.arg1,x,new Integer(flags));
                }
                return trust_compat.call(trust,x,new Integer(flags));
            }
        };
    public final static Function3 trust_1oid = new Function3() {
            public int call(Object _trust, Object _x, Object _flags) throws Exception {
                X509_TRUST trust = (X509_TRUST)_trust;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int flags = ((Integer)_flags).intValue();

                if(x.getAux() != null) {
                    return obj_trust.call(trust.arg1,x,new Integer(flags));
                }
                return X509.X509_TRUST_UNTRUSTED;
            }
        };
    public final static Function3 obj_trust = new Function3() {
            public int call(Object _id, Object _x, Object _flags) {
                String id = (String)_id;
                X509AuxCertificate x = (X509AuxCertificate)_x;
                int flags = ((Integer)_flags).intValue();
                
                X509_AUX ax = x.getAux();
                if(null == ax) {
                    return X509.X509_TRUST_UNTRUSTED;
                }
                for(Iterator iter = ax.reject.iterator(); iter.hasNext(); ) {
                    if(((String)iter.next()).equals(id)) {
                        return X509.X509_TRUST_REJECTED;
                    }
                }
                for(Iterator iter = ax.trust.iterator(); iter.hasNext(); ) {
                    if(((String)iter.next()).equals(id)) {
                        return X509.X509_TRUST_TRUSTED;
                    }
                }
                return X509.X509_TRUST_UNTRUSTED;
            }
        };

    public static Function3 default_trust = obj_trust;

    public final static X509_TRUST[] trstandard = new X509_TRUST[] {
        new X509_TRUST(X509.X509_TRUST_COMPAT, 0, trust_compat, "compatible", null, null),
        new X509_TRUST(X509.X509_TRUST_SSL_CLIENT, 0, trust_1oidany, "SSL Client", "1.3.6.1.5.5.7.3.2", null),
        new X509_TRUST(X509.X509_TRUST_SSL_SERVER, 0, trust_1oidany, "SSL Server", "1.3.6.1.5.5.7.3.1", null),
        new X509_TRUST(X509.X509_TRUST_EMAIL, 0, trust_1oidany, "S/MIME email", "1.3.6.1.5.5.7.3.4", null),
        new X509_TRUST(X509.X509_TRUST_OBJECT_SIGN, 0, trust_1oidany, "Object Signer", "1.3.6.1.5.5.7.3.3", null),
        new X509_TRUST(X509.X509_TRUST_OCSP_SIGN, 0, trust_1oid, "OCSP responder", "1.3.6.1.5.5.7.3.9", null),
        new X509_TRUST(X509.X509_TRUST_OCSP_REQUEST, 0, trust_1oid, "OCSP request", "1.3.6.1.5.5.7.48.1", null)
    };
}// X509_TRUST
