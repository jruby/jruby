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

import java.security.PublicKey;
import java.security.cert.CRL;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERInteger;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_STORE_CTX {
    public X509_STORE ctx;
    public int current_method;

    public X509AuxCertificate cert;
    public List untrusted; //List<X509AuxCertificate>
    public List crls; //List<CRL>

    public X509_VERIFY_PARAM param;

    public List other_ctx;

    public Function1 verify;
    public Function2 verify_cb;
    public Function3 get_issuer;
    public Function3 check_issued;
    public Function1 check_revocation;
    public Function3 get_crl;
    public Function2 check_crl;
    public Function3 cert_crl;
    public Function1 check_policy;
    public Function1 cleanup;

    public boolean valid;
    public int last_untrusted;
    
    public List chain; //List<X509AuxCertificate>
    public X509_POLICY_TREE tree;

    public int explicit_policy;

    public int error_depth;
    public int error;
    public X509AuxCertificate current_cert;
    public X509AuxCertificate current_issuer;
    public CRL current_crl;

    public List ex_data;

    public void set_depth(int depth) { 
        param.set_depth(depth);
    }

    public void set_app_data(Object data) {
        set_ex_data(0,data);
    }

    public Object get_app_data() {
        return get_ex_data(0);
    }

    public int get1_issuer(X509AuxCertificate[] issuer, X509AuxCertificate x) throws Exception { 
        X509_NAME xn = new X509_NAME(x.getIssuerX500Principal());
        X509_OBJECT[] s_obj = new X509_OBJECT[1];
        int ok = get_by_subject(X509.X509_LU_X509,xn,s_obj);
        if(ok != X509.X509_LU_X509) {
            if(ok == X509.X509_LU_RETRY) {
                Err.PUT_err(X509.X509_R_SHOULD_RETRY);
                return -1;
            } else if (ok != X509.X509_LU_FAIL) {
                return -1;
            }
            return 0;
        }
        X509_OBJECT obj = s_obj[0];
        if(this.check_issued.call(this,x,((X509_OBJECT_CERT)obj).x509) != 0) {
            issuer[0] = ((X509_OBJECT_CERT)obj).x509;
            return 1;
        }

        int idx = X509_OBJECT.idx_by_subject(ctx.objs,X509.X509_LU_X509, xn);
	if(idx == -1) {
            return 0;
        }

	/* Look through all matching certificates for a suitable issuer */
	for(int i = idx; i < ctx.objs.size(); i++) {
            X509_OBJECT pobj = (X509_OBJECT)ctx.objs.get(i);
            if(pobj.type() != X509.X509_LU_X509) {
                return 0;
            }
            if(!xn.isEqual((((X509_OBJECT_CERT)pobj).x509).getSubjectX500Principal())) {
                return 0;
            }
            if(this.check_issued.call(this,x,((X509_OBJECT_CERT)pobj).x509) != 0) {
                issuer[0] = ((X509_OBJECT_CERT)pobj).x509;
                return 1;
            }
        }
	return 0;
    }

    public static List transform(Collection inp) {
        List o = new ArrayList();
        for(Iterator iter = inp.iterator();iter.hasNext();) {
            o.add(transform((X509Certificate)iter.next()));
        }
        return o;
    }

    public static List transform(X509Certificate[] inp) {
        List o = new ArrayList();
        for(int i=0;i<inp.length;i++) {
            o.add(transform(inp[i]));
        }
        return o;
    }

    public static X509AuxCertificate transform(X509Certificate i) {
        if(i instanceof X509AuxCertificate) {
            return (X509AuxCertificate)i;
        } else {
            return new X509AuxCertificate((X509Certificate)i);
        }
    }

    public int init(X509_STORE store, X509AuxCertificate x509, List chain) { 
	int ret = 1;
	ctx=store;
	current_method=0;
	cert=x509;
	untrusted=transform(chain);
	crls = new ArrayList();
	last_untrusted=0;
	other_ctx = new ArrayList();
	valid=false;
	chain = new ArrayList();
	error=0;
	explicit_policy=0;
	error_depth=0;
	current_cert=null;
	current_issuer=null;
        tree = null;

	param = new X509_VERIFY_PARAM();


        if(store != null) {
            ret = param.inherit(store.param);
        } else {
            param.flags |= X509.X509_VP_FLAG_DEFAULT | X509.X509_VP_FLAG_ONCE;
        }
        if(store != null) {
            verify_cb = store.verify_cb;
            cleanup = store.cleanup;
        } else {
            cleanup = Function1.iZ;
        }

        if(ret != 0) {
            ret = param.inherit(X509_VERIFY_PARAM.lookup("default"));
        }

	if(ret == 0) {
            Err.PUT_err(X509.ERR_R_MALLOC_FAILURE);
            return 0;
        }

        if(store != null && store.check_issued != null && store.check_issued != Function3.iZ) {
            this.check_issued = store.check_issued;
        } else {
            this.check_issued = default_check_issued;
        }

        if(store != null && store.get_issuer != null && store.get_issuer != Function3.iZ) {
            this.get_issuer = store.get_issuer;
        } else {
            this.get_issuer = new Function3() {
                    public int call(Object arg1, Object arg2, Object arg3) throws Exception {
                        return ((X509_STORE_CTX)arg2).get1_issuer((X509AuxCertificate[])arg1,(X509AuxCertificate)arg3);
                    }
                };
        }

        if(store != null && store.verify_cb != null && store.verify_cb != Function2.iZ) {
            this.verify_cb = store.verify_cb;
        } else {
            this.verify_cb = null_callback;
        }

        if(store != null && store.verify != null && store.verify != Function1.iZ) {
            this.verify = store.verify;
        } else {
            this.verify = internal_verify;
        }

        if(store != null && store.check_revocation != null && store.check_revocation != Function1.iZ) {
            this.check_revocation = store.check_revocation;
        } else {
            this.check_revocation = default_check_revocation;
        }

        if(store != null && store.get_crl != null && store.get_crl != Function3.iZ) {
            this.get_crl = store.get_crl;
        } else {
            this.get_crl = default_get_crl;
        }

        if(store != null && store.check_crl != null && store.check_crl != Function2.iZ) {
            this.check_crl = store.check_crl;
        } else {
            this.check_crl = default_check_crl;
        }

        if(store != null && store.cert_crl != null && store.cert_crl != Function3.iZ) {
            this.cert_crl = store.cert_crl;
        } else {
            this.cert_crl = default_cert_crl;
        }

        this.check_policy = default_check_policy;

        this.ex_data = new ArrayList();
        this.ex_data.add(null);this.ex_data.add(null);this.ex_data.add(null);
        this.ex_data.add(null);this.ex_data.add(null);this.ex_data.add(null);
	return 1;
    } 

    public void trusted_stack(List sk) {
        other_ctx = sk;
        get_issuer = get_issuer_sk;
    }

    public void cleanup() throws Exception {
        if(cleanup != null && cleanup != Function1.iZ) {
            cleanup.call(this);
        }
        param = null;
        tree = null;
        chain = null;
        ex_data = null;
    } 

    public X509AuxCertificate find_issuer(List sk, X509AuxCertificate x) throws Exception {
        X509AuxCertificate issuer = null;
        for(Iterator iter = sk.iterator();iter.hasNext();) {
            issuer = (X509AuxCertificate)iter.next();
            if(check_issued.call(this,x,issuer) != 0) {
                return issuer;
            }
        }
        return null;
    }
    public int set_ex_data(int idx,Object data) { 
        ex_data.set(idx,data);
        return 1; 
    } 
    public Object get_ex_data(int idx) { 
        return ex_data.get(idx); 
    }
    public int get_error() { 
        return error;
    }
    public void set_error(int s) {
        this.error = s;
    } 
    public int get_error_depth() { 
        return error_depth; 
    } 
    public X509AuxCertificate get_current_cert() { 
        return current_cert; 
    }
    public List get_chain() { 
        return chain; 
    } 
    public List get1_chain() { 
        return new ArrayList(chain); 
    } 
    public void set_cert(X509AuxCertificate x) {
        this.cert = x;
    } 
    public void set_chain(List sk) {
        this.untrusted = transform(sk);
    } 
    public void set0_crls(List sk) {
        this.crls = sk;
    } 
    public int set_purpose(int purpose) { 
        return purpose_inherit(0,purpose,0);
    }
    public int set_trust(int trust) { 
        return purpose_inherit(0,0,trust);
    }
    public int purpose_inherit(int def_purpose,int purpose, int trust) { 
        int idx;
        if(purpose == 0) {
            purpose = def_purpose;
        }
        if(purpose != 0) {
            idx = X509_PURPOSE.get_by_id(purpose);
            if(idx == -1) {
                Err.PUT_err(X509.X509_R_UNKNOWN_PURPOSE_ID);
                return 0;
            }
            X509_PURPOSE ptmp = X509_PURPOSE.get0(idx);
            if(ptmp.trust == X509.X509_TRUST_DEFAULT) {
                idx = X509_PURPOSE.get_by_id(def_purpose);
                if(idx == -1) {
                    Err.PUT_err(X509.X509_R_UNKNOWN_PURPOSE_ID);
                    return 0;
                }
                ptmp = X509_PURPOSE.get0(idx);
            }
            if(trust == 0) {
                trust = ptmp.trust;
            }
        }
        if(trust != 0) {
            idx = X509_TRUST.get_by_id(trust);
            if(idx == -1) {
                Err.PUT_err(X509.X509_R_UNKNOWN_TRUST_ID);
                return 0;
            }
        }

        if(purpose != 0 && param.purpose == 0) {
            param.purpose = purpose;
        }
        if(trust != 0 && param.trust == 0) {
            param.trust = trust;
        }
        return 1;
    } 
    public void set_flags(long flags) {
        param.set_flags(flags);
    } 
    public void set_time(long flags,Date t) {
        param.set_time(t);
    } 
    public void set_verify_cb(Function2 verify_cb) {
        this.verify_cb = verify_cb;
    } 
    X509_POLICY_TREE get0_policy_tree() {
        return tree;
    }
    public int get_explicit_policy() { 
        return explicit_policy;
    } 
    public X509_VERIFY_PARAM get0_param() { 
        return param; 
    } 
    public void set0_param(X509_VERIFY_PARAM param) {
        this.param = param;
    } 
    public int set_default(String name) { 
        X509_VERIFY_PARAM p = X509_VERIFY_PARAM.lookup(name);
        if(p == null) {
            return 0;
        }
        return param.inherit(p);
    }

    public int get_by_subject(int type,X509_NAME name,X509_OBJECT[] ret) throws Exception { 
        X509_STORE c = ctx;

        X509_OBJECT tmp = X509_OBJECT.retrieve_by_subject(c.objs,type,name);
        if(tmp == null) {
            for(int i=current_method; i<c.get_cert_methods.size(); i++) {
                X509_LOOKUP lu = (X509_LOOKUP)c.get_cert_methods.get(i);
                X509_OBJECT[] stmp = new X509_OBJECT[1];
                int j = lu.by_subject(type,name,stmp);
                if(j<0) {
                    current_method = i;
                    return j;
                } else if(j>0) {
                    tmp = stmp[0];
                    break;
                }
            }
            current_method = 0;
            if(tmp == null) {
                return 0;
            }
        }
        ret[0] = tmp;
	return 1;
    }

    public int verify_cert() throws Exception {
        X509AuxCertificate x,xtmp=null,chain_ss = null;
        X509_NAME xn;
        int bad_chain = 0;
        X509_VERIFY_PARAM p = param;
        int depth,i,ok=0;
        int num;
        Function2 cb;
        List sktmp = null;
        if(cert == null) {
            Err.PUT_err(X509.X509_R_NO_CERT_SET_FOR_US_TO_VERIFY);
            return -1;
        }
	cb=verify_cb;

        if(null == chain) {
            chain = new ArrayList();
            chain.add(cert);
            last_untrusted = 1;
        }

        if(untrusted != null) {
            sktmp = new ArrayList(untrusted);
        }
        num = chain.size();
        x = (X509AuxCertificate)chain.get(num-1);
        depth = param.depth;

	for(;;) {
            if(depth < num) {
                break;
            }
            xn = new X509_NAME(x.getIssuerX500Principal());
            if(check_issued.call(this,x,x) != 0) {
                break;
            }
            if(untrusted != null) {
                xtmp = find_issuer(sktmp,x);
                if(xtmp != null) {
                    chain.add(xtmp);
                    sktmp.remove(xtmp);
                    last_untrusted++;
                    x = xtmp;
                    num++;
                    continue;
                }
            }
            break;
        }

        i = chain.size();
        x = (X509AuxCertificate)chain.get(i-1);
        xn = new X509_NAME(x.getSubjectX500Principal());
        if(check_issued.call(this,x,x) != 0) {
            if(chain.size() == 1) {
                X509AuxCertificate[] p_xtmp = new X509AuxCertificate[]{xtmp};
                ok = get_issuer.call(p_xtmp,this,x);
                xtmp = p_xtmp[0];
                if(ok <= 0 || !x.equals(xtmp)) {
                    error = X509.V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT;
                    current_cert = x;
                    error_depth = i-1;
                    bad_chain = 1;
                    ok = cb.call(new Integer(0),this);
                    if(ok == 0) {
                        return ok;
                    }
                } else {
                    x = xtmp;
                    chain.set(i-1,x);
                    last_untrusted = 0;
                }
            } else {
                chain_ss = (X509AuxCertificate)chain.remove(chain.size()-1);
                last_untrusted--;
                num--;
                x = (X509AuxCertificate)chain.get(num-1);
            }
        }

        for(;;) {
            if(depth<num) {
                break;
            }
            xn = new X509_NAME(x.getIssuerX500Principal());
            if(check_issued.call(this,x,x) != 0) {
                break;
            }
            X509AuxCertificate[] p_xtmp = new X509AuxCertificate[]{xtmp};
            ok = get_issuer.call(p_xtmp,this,x);
            xtmp = p_xtmp[0];
            if(ok < 0) {
                return ok;
            }
            if(ok == 0) {
                break;
            }
            x = xtmp;
            chain.add(x);
            num++;
        }

        xn = new X509_NAME(x.getIssuerX500Principal());

        if(check_issued.call(this,x,x) == 0) {
            if(chain_ss == null || check_issued.call(this,x,chain_ss) == 0) {
                if(last_untrusted >= num) {
                    error = X509.V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
                } else {
                    error = X509.V_ERR_UNABLE_TO_GET_ISSUER_CERT;
                }
                current_cert = x;
            } else {
                chain.add(chain_ss);
                num++;
                last_untrusted = num;
                current_cert = chain_ss;
                error = X509.V_ERR_SELF_SIGNED_CERT_IN_CHAIN;
                chain_ss = null;
            }
            error_depth = num-1;
            bad_chain = 1;
            ok = cb.call(new Integer(0),this);
            if(ok == 0) {
                return ok;
            }
        }

        ok = check_chain_extensions();
        if(ok == 0) {
            return ok;
        }

        if(param.trust > 0) {
            ok = check_trust();
        }
        if(ok == 0) {
            return ok;
        }

        ok = check_revocation.call(this);
        if(ok == 0) {
            return ok;
        }

        if(verify != null && verify != Function1.iZ) {
            ok = verify.call(this);
        } else {
            ok = internal_verify.call(this);
        }
        if(ok == 0) {
            return ok;
        }

        if(bad_chain == 0 && (param.flags & X509.V_FLAG_POLICY_CHECK) != 0) {
            ok = check_policy.call(this);
        }
        return ok;
    }


    private final static Set SUPPORT_CRIT_EXT = new HashSet();
    static {
        SUPPORT_CRIT_EXT.add("2.16.840.1.113730.1.1"); // netscape cert type, NID 71
        SUPPORT_CRIT_EXT.add("2.5.29.15"); // key usage, NID 83
        SUPPORT_CRIT_EXT.add("2.5.29.17"); // subject alt name, NID 85
        SUPPORT_CRIT_EXT.add("2.5.29.19"); // basic constraints, NID 87
        SUPPORT_CRIT_EXT.add("2.5.29.37"); // ext key usage, NID 126
        SUPPORT_CRIT_EXT.add("1.3.6.1.5.5.7.1.14"); // proxy cert info, NID 661
    }

    private static boolean supported_extension(String oid) {
        return SUPPORT_CRIT_EXT.contains(oid);
    }

    private static boolean unhandledCritical(X509Extension xx) {
        if(xx.getCriticalExtensionOIDs() == null || xx.getCriticalExtensionOIDs().size() == 0) {
            return false;
        }
        for(Iterator iter = xx.getCriticalExtensionOIDs().iterator();iter.hasNext();) {
            String ss = (String)iter.next();
            if(!supported_extension(ss)) {
                return true;
            }
        }
        return false;
    }

    public int check_chain_extensions() throws Exception {
	int ok=0, must_be_ca;
	X509AuxCertificate x;
        Function2 cb;
	int proxy_path_length = 0;
	int allow_proxy_certs = (param.flags & X509.V_FLAG_ALLOW_PROXY_CERTS) != 0 ? 1 : 0;
        cb = verify_cb;
	must_be_ca = -1;

	if(System.getenv("OPENSSL_ALLOW_PROXY_CERTS") != null && !"false".equalsIgnoreCase((String)System.getenv("OPENSSL_ALLOW_PROXY_CERTS"))) {
            allow_proxy_certs = 1;
        }

        for(int i = 0; i<last_untrusted;i++) {
            int ret;
            x = (X509AuxCertificate)chain.get(i);
            if((param.flags & X509.V_FLAG_IGNORE_CRITICAL) == 0 && unhandledCritical(x)) {
                error = X509.V_ERR_UNHANDLED_CRITICAL_EXTENSION;
                error_depth = i;
                current_cert = x;
                ok = cb.call(new Integer(0),this);
                if(ok == 0) {
                    return ok;
                }
            }
            if(allow_proxy_certs == 0 && x.getExtensionValue("1.3.6.1.5.5.7.1.14") != null) {
                error = X509.V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED;
                error_depth = i;
                current_cert = x;
                ok = cb.call(new Integer(0),this);
                if(ok == 0) {
                    return ok;
                }
            }

            ret = X509_PURPOSE.check_ca(x);
            switch(must_be_ca) {
            case -1:
                if((param.flags & X509.V_FLAG_X509_STRICT) != 0 && ret != 1 && ret != 0) {
                    ret = 0;
                    error = X509.V_ERR_INVALID_CA;
                } else {
                    ret = 1;
                }
                break;
            case 0:
                if(ret != 0) {
                    ret = 0;
                    error = X509.V_ERR_INVALID_NON_CA;
                } else {
                    ret = 1;
                }
                break;
            default:
                if(ret == 0 || ((param.flags & X509.V_FLAG_X509_STRICT) != 0 && ret != 1)) {
                    ret = 0;
                    error = X509.V_ERR_INVALID_CA;
                } else {
                    ret = 1;
                }
                break;
            }
            if(ret == 0) {
                error_depth = i;
                current_cert = x;
                ok = cb.call(new Integer(0),this);
                if(ok == 0) {
                    return ok;
                }
            }
            if(param.purpose > 0) {
                ret = X509_PURPOSE.check_purpose(x,param.purpose, must_be_ca > 0 ? 1 : 0);
                if(ret == 0 || ((param.flags & X509.V_FLAG_X509_STRICT) != 0 && ret != 1)) {
                    error = X509.V_ERR_INVALID_PURPOSE;
                    error_depth = i;
                    current_cert = x;
                    ok = cb.call(new Integer(0),this);
                    if(ok == 0) {
                        return ok;
                    }
                }
            }

            if(i > 1 && x.getBasicConstraints() != -1 && (i > (x.getBasicConstraints() + proxy_path_length + 1))) {
                error = X509.V_ERR_PATH_LENGTH_EXCEEDED;
                error_depth = i;
                current_cert = x;
                ok = cb.call(new Integer(0),this);
                if(ok == 0) {
                    return ok;
                }
            }

            if(x.getExtensionValue("1.3.6.1.5.5.7.1.14") != null) {
                DERSequence pci = (DERSequence)new ASN1InputStream(x.getExtensionValue("1.3.6.1.5.5.7.1.14")).readObject();
                if(pci.size() > 0 && pci.getObjectAt(0) instanceof DERInteger) {
                    int pcpathlen = ((DERInteger)pci.getObjectAt(0)).getValue().intValue();
                    if(i > pcpathlen) {
                        error = X509.V_ERR_PROXY_PATH_LENGTH_EXCEEDED;
                        error_depth = i;
                        current_cert = x;
                        ok = cb.call(new Integer(0),this);
                        if(ok == 0) {
                            return ok;
                        }
                    }
                }
                proxy_path_length++;
                must_be_ca = 0;
            } else {
                must_be_ca = 1;
            }
        }
	return 1;
    }

    public int check_trust() throws Exception {
        int i,ok;
        X509AuxCertificate x;
        Function2 cb;
        cb = verify_cb;
        i = chain.size()-1;
        x = (X509AuxCertificate)chain.get(i);
        ok = X509_TRUST.check_trust(x,param.trust,0);
        if(ok == X509.X509_TRUST_TRUSTED) {
            return 1;
        }
        error_depth = 1;
        current_cert = x;
        if(ok == X509.X509_TRUST_REJECTED) {
            error = X509.V_ERR_CERT_REJECTED;
        } else {
            error = X509.V_ERR_CERT_UNTRUSTED;
        }
        return cb.call(new Integer(0),this);
    }

    public int check_cert_time(X509AuxCertificate x) throws Exception {
        Date ptime = null;

        if((param.flags & X509.V_FLAG_USE_CHECK_TIME) != 0) {
            ptime = this.param.check_time;
        } else {
            ptime = Calendar.getInstance().getTime();
        }
        if(!x.getNotBefore().before(ptime)) {
            error = X509.V_ERR_CERT_NOT_YET_VALID;
            current_cert = x;
            if(verify_cb.call(new Integer(0),this) == 0) {
                return 0;
            }
        }
        if(!x.getNotAfter().after(ptime)) {
            error = X509.V_ERR_CERT_HAS_EXPIRED;
            current_cert = x;
            if(verify_cb.call(new Integer(0),this) == 0) {
                return 0;
            }
        }
	return 1;
    }

    public int check_cert() throws Exception {
        X509CRL[] crl = new X509CRL[1];
        X509AuxCertificate x;
        int ok,cnum;
        cnum = error_depth;
        x = (X509AuxCertificate)chain.get(cnum);
        current_cert = x;
        ok = get_crl.call(this,crl,x);
	if(ok == 0) {
            error = X509.V_ERR_UNABLE_TO_GET_CRL;
            ok = verify_cb.call(new Integer(0), this);
            current_crl = null;
            return ok;
        }
	current_crl = crl[0];
	ok = check_crl.call(this, crl[0]);
        if(ok == 0) {
            current_crl = null;
            return ok;
        }
        ok = cert_crl.call(this,crl[0],x);
	current_crl = null;
	return ok;
    }

    public int check_crl_time(X509CRL crl, int notify) throws Exception {
        current_crl = crl;
        Date ptime = null;

        if((param.flags & X509.V_FLAG_USE_CHECK_TIME) != 0) {
            ptime = this.param.check_time;
        } else {
            ptime = Calendar.getInstance().getTime();
        }
        
        if(!crl.getThisUpdate().before(ptime)) {
            error=X509.V_ERR_CRL_NOT_YET_VALID;
            if(notify == 0 || verify_cb.call(new Integer(0),this) == 0) {
                return 0;
            }
        }
        if(crl.getNextUpdate() != null && !crl.getNextUpdate().after(ptime)) {
            error=X509.V_ERR_CRL_HAS_EXPIRED;
            if(notify == 0 || verify_cb.call(new Integer(0),this) == 0) {
                return 0;
            }
        }

        current_crl = null;
	return 1;
    }

    public int get_crl_sk(X509CRL[] pcrl, X509_NAME nm, List crls) throws Exception { 
        X509CRL crl, best_crl = null;
        for(int i=0;i<crls.size();i++) {
            crl = (X509CRL)crls.get(i);
            if(!nm.isEqual(crl.getIssuerX500Principal())) {
                continue;
            }
            if(check_crl_time(crl,0) != 0) {
                pcrl[0] = crl;
                return 1;
            }
            best_crl = crl;
        }
        if(best_crl != null) {
            pcrl[0] = best_crl;
        }
        return 0;
    }

    public final static Function3 get_issuer_sk = new Function3() { 
            public int call(Object a1, Object a2, Object a3) throws Exception {
                X509AuxCertificate[] issuer = (X509AuxCertificate[])a1;
                X509_STORE_CTX ctx = (X509_STORE_CTX)a2;
                X509AuxCertificate x = (X509AuxCertificate)a3;
                issuer[0] = ctx.find_issuer(ctx.other_ctx,x);
                if(issuer[0] != null) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

    public final static Function3 default_check_issued = new Function3() { 
            public int call(Object a1, Object a2, Object a3) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                X509AuxCertificate x = (X509AuxCertificate)a2;
                X509AuxCertificate issuer = (X509AuxCertificate)a3;
                int ret = X509.check_issued(issuer,x);
                if(ret == X509.V_OK) {
                    return 1;
                }
                if((ctx.param.flags & X509.V_FLAG_CB_ISSUER_CHECK) == 0) {
                    return 0;
                }
                ctx.error = ret;
                ctx.current_cert = x;
                ctx.current_issuer = issuer;
                return ctx.verify_cb.call(new Integer(0),ctx);
            }
        };

    public final static Function2 null_callback = new Function2() { 
            public int call(Object a1, Object a2) {
                return ((Integer)a1).intValue();
            }
        };

    public final static Function1 internal_verify = new Function1() { 
            public int call(Object a1) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                Function2 cb = ctx.verify_cb;
                int n = ctx.chain.size();
                ctx.error_depth = n-1;
                n--;
                X509AuxCertificate xi = (X509AuxCertificate)ctx.chain.get(n);
                X509AuxCertificate xs = null;
                int ok = 0;
                if(ctx.check_issued.call(ctx,xi,xi) != 0) {
                    xs = xi;
                } else {
                    if(n<=0) {
                        ctx.error = X509.V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE;
                        ctx.current_cert = xi;
                        ok = cb.call(new Integer(0),ctx);
                        return ok;
                    } else {
                        n--;
                        ctx.error_depth = n;
                        xs = (X509AuxCertificate)ctx.chain.get(n);
                    }
                }
                while(n>=0) {
                    ctx.error_depth = n;
                    if(!xs.isValid()) {
                        try {
                            xs.verify(xi.getPublicKey());
                        } catch(Exception e) {
                            System.err.println("n: " + n);
                            System.err.println("verifying: " + xs);
                            System.err.println("verifying with issuer?: " + xi);
                            System.err.println("verifying with issuer.key?: " + xi.getPublicKey());
                            System.err.println("exception: " + e);
                            ctx.error = X509.V_ERR_CERT_SIGNATURE_FAILURE;
                            ctx.current_cert = xs;
                            ok = cb.call(new Integer(0),ctx);
                            if(ok == 0) {
                                return ok;
                            }
                        }
                    }
                    xs.setValid(true);
                    ok = ctx.check_cert_time(xs);
                    if(ok == 0) {
                        return ok;
                    }
                    ctx.current_issuer = xi;
                    ctx.current_cert = xs;
                    ok = cb.call(new Integer(1),ctx);
                    if(ok == 0) {
                        return ok;
                    }
                    n--;
                    if(n>=0) {
                        xi = xs;
                        xs = (X509AuxCertificate)ctx.chain.get(n);
                    }
                }
                ok = 1;
                return ok;
            }
        };

    public final static Function1 default_check_revocation = new Function1() { 
            public int call(Object a1) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                int last,ok=0;
                if((ctx.param.flags & X509.V_FLAG_CRL_CHECK) == 0) {
                    return 1;
                }
                if((ctx.param.flags & X509.V_FLAG_CRL_CHECK_ALL) != 0) {
                    last = ctx.chain.size() -1;
                } else {
                    last = 0;
                }
                for(int i=0;i<=last;i++) {
                    ctx.error_depth = i;
                    ok = ctx.check_cert();
                    if(ok == 0) {
                        return 0;
                    }
                }
                return 1;
            }
        };

    public final static Function3 default_get_crl = new Function3() { 
            public int call(Object a1, Object a2, Object a3) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                X509CRL[] pcrl = (X509CRL[])a2;
                X509AuxCertificate x = (X509AuxCertificate)a3;
                X509_NAME nm = new X509_NAME(x.getIssuerX500Principal());
                X509CRL[] crl = new X509CRL[1];
                int ok = ctx.get_crl_sk(crl,nm,ctx.crls);
                if(ok != 0) {
                    pcrl[0] = crl[0];
                    return 1;
                }
                X509_OBJECT[] xobj = new X509_OBJECT[1];
                ok = ctx.get_by_subject(X509.X509_LU_CRL,nm,xobj);
                if(ok == 0) {
                    if(crl[0] != null) {
                        pcrl[0] = crl[0];
                        return 1;
                    }
                    return 0;
                }
                pcrl[0] = (X509CRL)(((X509_OBJECT_CRL)xobj[0]).crl);
                return 1;
            }
        };

    public final static Function2 default_check_crl = new Function2() { 
            public int call(Object a1, Object a2) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                X509CRL crl = (X509CRL)a2;
                X509AuxCertificate issuer = null;
                int ok = 0,chnum,cnum;
                cnum = ctx.error_depth;
                chnum = ctx.chain.size()-1;
                if(cnum < chnum) {
                    issuer = (X509AuxCertificate)ctx.chain.get(cnum+1);
                } else {
                    issuer = (X509AuxCertificate)ctx.chain.get(chnum);
                    if(ctx.check_issued.call(ctx,issuer,issuer) == 0) {
                        ctx.error = X509.V_ERR_UNABLE_TO_GET_CRL_ISSUER;
                        ok = ctx.verify_cb.call(new Integer(0),ctx);
                        if(ok == 0) {
                            return ok;
                        }
                    }
                }

                if(issuer != null) {
                    if(issuer.getKeyUsage() != null && !issuer.getKeyUsage()[6]) {
                        ctx.error = X509.V_ERR_KEYUSAGE_NO_CRL_SIGN;
                        ok = ctx.verify_cb.call(new Integer(0),ctx);
                        if(ok == 0) {
                            return ok;
                        }
                    }
                    PublicKey ikey = issuer.getPublicKey();
                    if(ikey == null) {
                        ctx.error = X509.V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY;
                        ok = ctx.verify_cb.call(new Integer(0),ctx);
                        if(ok == 0) {
                            return ok;
                        }
                    } else {
                        try {
                            crl.verify(ikey);
                        } catch(Exception e) {
                            ctx.error= X509.V_ERR_CRL_SIGNATURE_FAILURE;
                            ok = ctx.verify_cb.call(new Integer(0),ctx);
                            if(ok == 0) {
                                return ok;
                            }
                        }
                    }
                }

                ok = ctx.check_crl_time(crl,1);
                if(ok == 0) {
                    return ok;
                }
                return 1;
            }
        };

    public final static Function3 default_cert_crl = new Function3() { 
            public int call(Object a1, Object a2, Object a3) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a1;
                X509CRL crl = (X509CRL)a2;
                X509AuxCertificate x = (X509AuxCertificate)a3;
                int ok;
                if(crl.getRevokedCertificate(x.getSerialNumber()) != null) {
                    ctx.error = X509.V_ERR_CERT_REVOKED;
                    ok = ctx.verify_cb.call(new Integer(0), ctx);
                    if(ok == 0) {
                        return 0;
                    }
                }
                if((ctx.param.flags & X509.V_FLAG_IGNORE_CRITICAL) != 0) {
                    return 1;
                }

                if(crl.getCriticalExtensionOIDs() != null && crl.getCriticalExtensionOIDs().size()>0) {
                    ctx.error = X509.V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION;
                    ok = ctx.verify_cb.call(new Integer(0), ctx);
                    if(ok == 0) {
                        return 0;
                    }
                }
                return 1;
            }
        };

    public final static Function1 default_check_policy = new Function1() { 
            public int call(Object a1) throws Exception {
                return 1;
            }
        };
}// X509_STORE_CTX
