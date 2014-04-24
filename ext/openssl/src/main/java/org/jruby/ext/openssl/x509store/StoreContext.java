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

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.jruby.ext.openssl.SecurityHelper;

/**
 * c: X509_STORE_CTX
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class StoreContext {

    private Store store;

    public int currentMethod;

    public X509AuxCertificate certificate;
    public List<X509AuxCertificate> untrusted;
    public List<X509CRL> crls;

    public VerifyParameter verifyParameter;

    public List<X509AuxCertificate> otherContext;

    public static interface CheckPolicyFunction extends Function1<StoreContext> {
        public static final CheckPolicyFunction EMPTY = new CheckPolicyFunction(){
            public int call(StoreContext context) {
                return -1;
            }
        };
    }

    Store.VerifyFunction verify;
    Store.VerifyCallbackFunction verifyCallback;
    Store.GetIssuerFunction getIssuer;
    Store.CheckIssuedFunction checkIssued;
    Store.CheckRevocationFunction checkRevocation;
    Store.GetCRLFunction getCRL;
    Store.CheckCRLFunction checkCRL;
    Store.CertificateCRLFunction certificateCRL;
    CheckPolicyFunction checkPolicy;
    Store.CleanupFunction cleanup;

    public boolean isValid;
    public int lastUntrusted;

    public List<X509AuxCertificate> chain; //List<X509AuxCertificate>
    public PolicyTree tree;

    public int explicitPolicy;

    public int errorDepth;
    public int error;
    public X509AuxCertificate currentCertificate;
    public X509AuxCertificate currentIssuer;
    public java.security.cert.CRL currentCRL;

    public List<Object> extraData;

    public Store getStore() {
        return store;
    }

    /**
     * c: X509_STORE_CTX_set_depth
     */
    public void setDepth(int depth) {
        verifyParameter.setDepth(depth);
    }

    /**
     * c: X509_STORE_CTX_set_app_data
     */
    public void setApplicationData(Object data) {
        setExtraData(0,data);
    }

    /**
     * c: X509_STORE_CTX_get_app_data
     */
    public Object getApplicationData() {
        return getExtraData(0);
    }

    /**
     * c: X509_STORE_CTX_get1_issuer
     */
    int getFirstIssuer(final X509AuxCertificate[] issuers, final X509AuxCertificate x) throws Exception {
        final Name xn = new Name( x.getIssuerX500Principal() );
        final X509Object[] s_obj = new X509Object[1];
        int ok = store == null ? 0 : getBySubject(X509Utils.X509_LU_X509, xn, s_obj);
        if ( ok != X509Utils.X509_LU_X509 ) {
            if ( ok == X509Utils.X509_LU_RETRY ) {
                X509Error.addError(X509Utils.X509_R_SHOULD_RETRY);
                return -1;
            }
            else if ( ok != X509Utils.X509_LU_FAIL ) {
                return -1;
            }
            return 0;
        }

        X509Object obj = s_obj[0];
        if ( checkIssued.call(this, x, ((Certificate) obj).x509) != 0 ) {
            issuers[0] = ((Certificate) obj).x509;
            return 1;
        }

        int idx = X509Object.indexBySubject(store.objects, X509Utils.X509_LU_X509, xn);
        if ( idx == -1 ) return 0;

        /* Look through all matching certificates for a suitable issuer */
        for ( int i = idx; i < store.objects.size(); i++ ) {
            final X509Object pobj = store.objects.get(i);
            if ( pobj.type() != X509Utils.X509_LU_X509 ) {
                return 0;
            }
            final X509AuxCertificate x509 = ((Certificate) pobj).x509;
            if ( ! xn.isEqual( x509.getSubjectX500Principal() ) ) {
                return 0;
            }
            if ( checkIssued.call(this, x, x509) != 0 ) {
                issuers[0] = x509;
                return 1;
            }
        }
        return 0;
    }

    public static List<X509AuxCertificate> ensureAux(final Collection<X509Certificate> input) {
        if ( input == null ) return null;

        List<X509AuxCertificate> out = new ArrayList<X509AuxCertificate>(input.size());
        for ( X509Certificate cert : input ) out.add( ensureAux(cert) );
        return out;
    }

    public static List<X509AuxCertificate> ensureAux(final X509Certificate[] input) {
        if ( input == null ) return null;

        List<X509AuxCertificate> out = new ArrayList<X509AuxCertificate>(input.length);
        for ( X509Certificate cert : input ) out.add( ensureAux(cert) );
        return out;
    }

    public static X509AuxCertificate ensureAux(final X509Certificate input) {
        if ( input == null ) return null;

        if ( input instanceof X509AuxCertificate ) {
            return (X509AuxCertificate)input;
        } else {
            return new X509AuxCertificate(input);
        }
    }

    /**
     * c: X509_STORE_CTX_init
     */
    public int init(Store store, X509AuxCertificate x509, List<X509AuxCertificate> chain) {
        int ret = 1;
        this.store = store;
        this.currentMethod=0;
        this.certificate=x509;
        this.untrusted=chain;
        this.crls = null;
        this.lastUntrusted=0;
        this.otherContext = null;
        this.isValid=false;
        this.chain = null;
        this.error=0;
        this.explicitPolicy=0;
        this.errorDepth=0;
        this.currentCertificate=null;
        this.currentIssuer=null;
        this.tree = null;

        this.verifyParameter = new VerifyParameter();

        if ( store != null ) {
            ret = verifyParameter.inherit(store.verifyParameter);
        } else {
            verifyParameter.flags |= X509Utils.X509_VP_FLAG_DEFAULT | X509Utils.X509_VP_FLAG_ONCE;
        }

        if ( store != null ) {
            verifyCallback = store.getVerifyCallback();
            cleanup = store.cleanup;
        } else {
            cleanup = Store.CleanupFunction.EMPTY;
        }

        if ( ret != 0 ) {
            ret = verifyParameter.inherit(VerifyParameter.lookup("default"));
        }

        if ( ret == 0 ) {
            X509Error.addError(X509Utils.ERR_R_MALLOC_FAILURE);
            return 0;
        }

        this.checkIssued = defaultCheckIssued;
        this.getIssuer = getFirstIssuer;
        this.verifyCallback = nullCallback;
        this.verify = internalVerify;
        this.checkRevocation = defaultCheckRevocation;
        this.getCRL = defaultGetCRL;
        this.checkCRL = defaultCheckCRL;
        this.certificateCRL = defaultCertificateCRL;

        if ( store != null ) {
            if ( store.checkIssued != null && store.checkIssued != Store.CheckIssuedFunction.EMPTY ) {
                this.checkIssued = store.checkIssued;
            }
            if ( store.getIssuer != null && store.getIssuer != Store.GetIssuerFunction.EMPTY ) {
                this.getIssuer = store.getIssuer;
            }
            if ( store.verifyCallback != null && store.verifyCallback != Store.VerifyCallbackFunction.EMPTY ) {
                this.verifyCallback = store.verifyCallback;
            }
            if ( store.verify != null && store.verify != Store.VerifyFunction.EMPTY) {
                this.verify = store.verify;
            }
            if ( store.checkRevocation != null && store.checkRevocation != Store.CheckRevocationFunction.EMPTY) {
                this.checkRevocation = store.checkRevocation;
            }
            if ( store.getCRL != null && store.getCRL != Store.GetCRLFunction.EMPTY) {
                this.getCRL = store.getCRL;
            }
            if( store.checkCRL != null && store.checkCRL != Store.CheckCRLFunction.EMPTY) {
                this.checkCRL = store.checkCRL;
            }
            if ( store.certificateCRL != null && store.certificateCRL != Store.CertificateCRLFunction.EMPTY) {
                this.certificateCRL = store.certificateCRL;
            }
        }

        this.checkPolicy = defaultCheckPolicy;

        this.extraData = new ArrayList<Object>();
        this.extraData.add(null); this.extraData.add(null); this.extraData.add(null);
        this.extraData.add(null); this.extraData.add(null); this.extraData.add(null);
        return 1;
    }

    /**
     * c: X509_STORE_CTX_trusted_stack
     */
    public void trustedStack(List<X509AuxCertificate> sk) {
        otherContext = sk;
        getIssuer = getIssuerStack;
    }

    /**
     * c: X509_STORE_CTX_cleanup
     */
    public void cleanup() throws Exception {
        if (cleanup != null && cleanup != Store.CleanupFunction.EMPTY) {
            cleanup.call(this);
        }
        verifyParameter = null;
        tree = null;
        chain = null;
        extraData = null;
    }

    /**
     * c: find_issuer
     */
    public X509AuxCertificate findIssuer(final List<X509AuxCertificate> certs, final X509AuxCertificate cert) throws Exception {
        for ( X509AuxCertificate issuer : certs ) {
            if ( checkIssued.call(this, cert, issuer) != 0 ) {
                return issuer;
            }
        }
        return null;
    }

    /**
     * c: X509_STORE_CTX_set_ex_data
     */
    public int setExtraData(int idx,Object data) {
        extraData.set(idx,data);
        return 1;
    }

    /**
     * c: X509_STORE_CTX_get_ex_data
     */
    public Object getExtraData(int idx) {
        return extraData.get(idx);
    }

    /**
     * c: X509_STORE_CTX_get_error
     */
    public int getError() {
        return error;
    }

    /**
     * c: X509_STORE_CTX_set_error
     */
    public void setError(int s) {
        this.error = s;
    }

    /**
     * c: X509_STORE_CTX_get_error_depth
     */
    public int getErrorDepth() {
        return errorDepth;
    }

    /**
     * c: X509_STORE_CTX_get_current_cert
     */
    public X509AuxCertificate getCurrentCertificate() {
        return currentCertificate;
    }

    /**
     * c: X509_STORE_CTX_get_chain
     */
    public List<X509AuxCertificate> getChain() {
        return chain;
    }

    /**
     * c: X509_STORE_CTX_get1_chain
     */
    public List<X509AuxCertificate> getFirstChain() {
        if(null == chain) {
            return null;
        }
        return new ArrayList<X509AuxCertificate>(chain);
    }

    /**
     * c: X509_STORE_CTX_set_cert
     */
    public void setCertificate(X509AuxCertificate x) {
        this.certificate = x;
    }

    public void setCertificate(X509Certificate x) {
        this.certificate = ensureAux(x);
    }

    /**
     * c: X509_STORE_CTX_set_chain
     */
    public void setChain(List<X509Certificate> sk) {
        this.untrusted = ensureAux(sk);
    }

    public void setChain(X509Certificate[] sk) {
        this.untrusted = ensureAux(sk);
    }

    /**
     * c: X509_STORE_CTX_set0_crls
     */
    public void setCRLs(List<X509CRL> sk) {
        this.crls = sk;
    }

    /**
     * c: X509_STORE_CTX_set_purpose
     */
    public int setPurpose(int purpose) {
        return purposeInherit(0,purpose,0);
    }

    /**
     * c: X509_STORE_CTX_set_trust
     */
    public int setTrust(int trust) {
        return purposeInherit(0,0,trust);
    }

    private void resetSettingsToWithoutStore() {
        store = null;
        this.verifyParameter = new VerifyParameter();
        this.verifyParameter.flags |= X509Utils.X509_VP_FLAG_DEFAULT | X509Utils.X509_VP_FLAG_ONCE;
        this.verifyParameter.inherit(VerifyParameter.lookup("default"));
        this.cleanup = Store.CleanupFunction.EMPTY;
        this.checkIssued = defaultCheckIssued;
        this.getIssuer = getFirstIssuer;
        this.verifyCallback = nullCallback;
        this.verify = internalVerify;
        this.checkRevocation = defaultCheckRevocation;
        this.getCRL = defaultGetCRL;
        this.checkCRL = defaultCheckCRL;
        this.certificateCRL = defaultCertificateCRL;
    }

    /**
     * c: SSL_CTX_load_verify_locations
     */
    public int loadVerifyLocations(String CAfile, String CApath) {
        boolean reset = false;
        try {
            if ( store == null ) {
                reset = true;
                store = new Store();
                this.verifyParameter.inherit(store.verifyParameter);
                verifyParameter.inherit(VerifyParameter.lookup("default"));
                this.cleanup = store.cleanup;
                if ( store.checkIssued != null && store.checkIssued != Store.CheckIssuedFunction.EMPTY ) {
                    this.checkIssued = store.checkIssued;
                }
                if ( store.getIssuer != null && store.getIssuer != Store.GetIssuerFunction.EMPTY ) {
                    this.getIssuer = store.getIssuer;
                }
                if ( store.verify != null && store.verify != Store.VerifyFunction.EMPTY ) {
                    this.verify = store.verify;
                }
                if ( store.verifyCallback != null && store.verifyCallback != Store.VerifyCallbackFunction.EMPTY ) {
                    this.verifyCallback = store.verifyCallback;
                }
                if ( store.checkRevocation != null && store.checkRevocation != Store.CheckRevocationFunction.EMPTY ) {
                    this.checkRevocation = store.checkRevocation;
                }
                if ( store.getCRL != null && store.getCRL != Store.GetCRLFunction.EMPTY ) {
                    this.getCRL = store.getCRL;
                }
                if ( store.checkCRL != null && store.checkCRL != Store.CheckCRLFunction.EMPTY ) {
                    this.checkCRL = store.checkCRL;
                }
                if ( store.certificateCRL != null && store.certificateCRL != Store.CertificateCRLFunction.EMPTY ) {
                    this.certificateCRL = store.certificateCRL;
                }
            }

            final int ret = store.loadLocations(CAfile, CApath);
            if ( ret == 0 && reset ) resetSettingsToWithoutStore();

            return ret;
        }
        catch (Exception e) {

            if ( reset ) resetSettingsToWithoutStore();
            return 0;
        }
    }

    /**
     * c: X509_STORE_CTX_purpose_inherit
     */
    public int purposeInherit(int defaultPurpose,int purpose, int trust) {
        int idx;
        if(purpose == 0) {
            purpose = defaultPurpose;
        }
        if(purpose != 0) {
            idx = Purpose.getByID(purpose);
            if(idx == -1) {
                X509Error.addError(X509Utils.X509_R_UNKNOWN_PURPOSE_ID);
                return 0;
            }
            Purpose ptmp = Purpose.getFirst(idx);
            if(ptmp.trust == X509Utils.X509_TRUST_DEFAULT) {
                idx = Purpose.getByID(defaultPurpose);
                if(idx == -1) {
                    X509Error.addError(X509Utils.X509_R_UNKNOWN_PURPOSE_ID);
                    return 0;
                }
                ptmp = Purpose.getFirst(idx);
            }
            if(trust == 0) {
                trust = ptmp.trust;
            }
        }
        if(trust != 0) {
            idx = Trust.getByID(trust);
            if(idx == -1) {
                X509Error.addError(X509Utils.X509_R_UNKNOWN_TRUST_ID);
                return 0;
            }
        }

        if(purpose != 0 && verifyParameter.purpose == 0) {
            verifyParameter.purpose = purpose;
        }
        if(trust != 0 && verifyParameter.trust == 0) {
            verifyParameter.trust = trust;
        }
        return 1;
    }

    /**
     * c: X509_STORE_CTX_set_flags
     */
    public void setFlags(long flags) {
        verifyParameter.setFlags(flags);
    }

    /**
     * c: X509_STORE_CTX_set_time
     */
    public void setTime(long flags,Date t) {
        verifyParameter.setTime(t);
    }

    /**
     * c: X509_STORE_CTX_set_verify_cb
     */
    public void setVerifyCallback(Store.VerifyCallbackFunction verifyCallback) {
        this.verifyCallback = verifyCallback;
    }

    /**
     * c: X509_STORE_CTX_get0_policy_tree
     */
    PolicyTree getPolicyTree() {
        return tree;
    }

    /**
     * c: X509_STORE_CTX_get_explicit_policy
     */
    public int getExplicitPolicy() {
        return explicitPolicy;
    }

    /**
     * c: X509_STORE_CTX_get0_param
     */
    public VerifyParameter getParam() {
        return verifyParameter;
    }

    /**
     * c: X509_STORE_CTX_set0_param
     */
    public void setParam(VerifyParameter param) {
        this.verifyParameter = param;
    }

    /**
     * c: X509_STORE_CTX_set_default
     */
    public int setDefault(String name) {
        VerifyParameter p = VerifyParameter.lookup(name);
        if(p == null) {
            return 0;
        }
        return verifyParameter.inherit(p);
    }

    /**
     * c: X509_STORE_get_by_subject (it gets X509_STORE_CTX as the first parameter)
     */
    public int getBySubject(int type,Name name,X509Object[] ret) throws Exception {
        Store c = store;

        X509Object tmp = X509Object.retrieveBySubject(c.objects,type,name);
        if ( tmp == null ) {
            for(int i=currentMethod; i<c.certificateMethods.size(); i++) {
                Lookup lu = c.certificateMethods.get(i);
                X509Object[] stmp = new X509Object[1];
                int j = lu.bySubject(type,name,stmp);
                if ( j < 0 ) {
                    currentMethod = i;
                    return j;
                }
                else if( j > 0 ) {
                    tmp = stmp[0];
                    break;
                }
            }
            currentMethod = 0;

            if ( tmp == null ) return 0;
        }
        ret[0] = tmp;
        return 1;
    }

    /**
     * c: X509_verify_cert
     */
    public int verifyCertificate() throws Exception {
        X509AuxCertificate x, xtmp = null, chain_ss = null;
        //X509_NAME xn;
        int bad_chain = 0, depth, i, num;
        List<X509AuxCertificate> sktmp = null;
        if ( certificate == null ) {
            X509Error.addError(X509Utils.X509_R_NO_CERT_SET_FOR_US_TO_VERIFY);
            return -1;
        }

        // first we make sure the chain we are going to build is
        // present and that the first entry is in place

        if ( chain == null ) {
            chain = new ArrayList<X509AuxCertificate>();
            chain.add(certificate);
            lastUntrusted = 1;
        }

        // We use a temporary STACK so we can chop and hack at it

        if ( untrusted != null ) {
            sktmp = new ArrayList<X509AuxCertificate>(untrusted);
        }
        num = chain.size();
        x = chain.get(num-1);
        depth = verifyParameter.depth;
        for(;;) {
            if(depth < num) {
                break;
            }

            if(checkIssued.call(this,x,x) != 0) {
                break;
            }

            if ( untrusted != null ) {
                xtmp = findIssuer(sktmp, x);
                if ( xtmp != null ) {
                    chain.add(xtmp);
                    sktmp.remove(xtmp);
                    lastUntrusted++;
                    x = xtmp;
                    num++;
                    continue;
                }
            }
            break;
        }

        // at this point, chain should contain a list of untrusted
        // certificates.  We now need to add at least one trusted one,
        // if possible, otherwise we complain.

        // Examine last certificate in chain and see if it is self signed.

        i = chain.size();
        x = chain.get(i-1);

        if ( checkIssued.call(this, x, x) != 0 ) {
            // we have a self signed certificate
            if ( chain.size() == 1 ) {
                // We have a single self signed certificate: see if
                // we can find it in the store. We must have an exact
                // match to avoid possible impersonation.
                X509AuxCertificate[] p_xtmp = new X509AuxCertificate[]{ xtmp };
                int ok = getIssuer.call(this, p_xtmp, x);
                xtmp = p_xtmp[0];
                if ( ok <= 0 || ! x.equals(xtmp) ) {
                    error = X509Utils.V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT;
                    currentCertificate = x;
                    errorDepth = i-1;
                    bad_chain = 1;
                    ok = verifyCallback.call(this, Integer.valueOf(0));
                    if ( ok == 0 ) return ok;
                } else {
                    // We have a match: replace certificate with store version
                    // so we get any trust settings.
                    x = xtmp;
                    chain.set(i-1,x);
                    lastUntrusted = 0;
                }
            } else {
                // extract and save self signed certificate for later use
                chain_ss = chain.remove(chain.size()-1);
                lastUntrusted--;
                num--;
                x = chain.get(num-1);
            }
        }
        // We now lookup certs from the certificate store
        for(;;) {
            // If we have enough, we break
            if ( depth < num ) break;
            //xn = new X509_NAME(x.getIssuerX500Principal());
            // If we are self signed, we break
            if ( checkIssued.call(this, x, x) != 0 ) break;

            X509AuxCertificate[] p_xtmp = new X509AuxCertificate[]{ xtmp };
            int ok = getIssuer.call(this, p_xtmp, x);
            xtmp = p_xtmp[0];

            if ( ok < 0 ) return ok;
            if ( ok == 0 ) break;

            x = xtmp;
            chain.add(x);
            num++;
        }

        /* we now have our chain, lets check it... */

        //xn = new X509_NAME(x.getIssuerX500Principal());
        /* Is last certificate looked up self signed? */
        if ( checkIssued.call(this, x, x) == 0 ) {
            if ( chain_ss == null || checkIssued.call(this, x, chain_ss) == 0 ) {
                if(lastUntrusted >= num) {
                    error = X509Utils.V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY;
                } else {
                    error = X509Utils.V_ERR_UNABLE_TO_GET_ISSUER_CERT;
                }
                currentCertificate = x;
            } else {
                chain.add(chain_ss);
                num++;
                lastUntrusted = num;
                currentCertificate = chain_ss;
                error = X509Utils.V_ERR_SELF_SIGNED_CERT_IN_CHAIN;
                chain_ss = null;
            }
            errorDepth = num-1;
            bad_chain = 1;
            int ok = verifyCallback.call(this, Integer.valueOf(0));
            if ( ok == 0 ) return ok;
        }

        // We have the chain complete: now we need to check its purpose
        int ok = checkChainExtensions();
        if ( ok == 0 ) return ok;

        /* TODO: Check name constraints (from 1.0.0) */

        // The chain extensions are OK: check trust
        if ( verifyParameter.trust > 0 ) ok = checkTrust();
        if ( ok == 0 ) return ok;

        // Check revocation status: we do this after copying parameters
        // because they may be needed for CRL signature verification.
        ok = checkRevocation.call(this);
        if ( ok == 0 ) return ok;

        /* At this point, we have a chain and need to verify it */
        if ( verify != null && verify != Store.VerifyFunction.EMPTY ) {
            ok = verify.call(this);
        } else {
            ok = internalVerify.call(this);
        }
        if ( ok == 0 ) return ok;

        /* TODO: RFC 3779 path validation, now that CRL check has been done (from 1.0.0) */

        /* If we get this far evaluate policies */
        if ( bad_chain == 0 && (verifyParameter.flags & X509Utils.V_FLAG_POLICY_CHECK) != 0 ) {
            ok = checkPolicy.call(this);
        }
        return ok;
    }


    private final static Set<String> CRITICAL_EXTENSIONS = new HashSet<String>();
    static {
        CRITICAL_EXTENSIONS.add("2.16.840.1.113730.1.1"); // netscape cert type, NID 71
        CRITICAL_EXTENSIONS.add("2.5.29.15"); // key usage, NID 83
        CRITICAL_EXTENSIONS.add("2.5.29.17"); // subject alt name, NID 85
        CRITICAL_EXTENSIONS.add("2.5.29.19"); // basic constraints, NID 87
        CRITICAL_EXTENSIONS.add("2.5.29.37"); // ext key usage, NID 126
        CRITICAL_EXTENSIONS.add("1.3.6.1.5.5.7.1.14"); // proxy cert info, NID 661
    }

    private static boolean supportsCriticalExtension(String oid) {
        return CRITICAL_EXTENSIONS.contains(oid);
    }

    private static boolean unhandledCritical(X509Extension xx) {
        if(xx.getCriticalExtensionOIDs() == null || xx.getCriticalExtensionOIDs().size() == 0) {
            return false;
        }
        for(String ss : xx.getCriticalExtensionOIDs()) {
            if(!supportsCriticalExtension(ss)) {
                return true;
            }
        }
        return false;
    }

    /**
     * c: check_chain_extensions
     */
    public int checkChainExtensions() throws Exception {
        int ok, must_be_ca;
        X509AuxCertificate x;
        int proxy_path_length = 0;
        int allow_proxy_certs = (verifyParameter.flags & X509Utils.V_FLAG_ALLOW_PROXY_CERTS) != 0 ? 1 : 0;
        must_be_ca = -1;

        try {
            final String allowProxyCerts = System.getenv("OPENSSL_ALLOW_PROXY_CERTS");
            if ( allowProxyCerts != null && ! "false".equalsIgnoreCase(allowProxyCerts) ) {
                allow_proxy_certs = 1;
            }
        } catch (SecurityException e) { /* ignore if we can't use System.getenv */ }

        for(int i = 0; i<lastUntrusted;i++) {
            int ret;
            x = chain.get(i);
            if((verifyParameter.flags & X509Utils.V_FLAG_IGNORE_CRITICAL) == 0 && unhandledCritical(x)) {
                error = X509Utils.V_ERR_UNHANDLED_CRITICAL_EXTENSION;
                errorDepth = i;
                currentCertificate = x;
                ok = verifyCallback.call(this, Integer.valueOf(0));
                if ( ok == 0 ) return ok;
            }
            if(allow_proxy_certs == 0 && x.getExtensionValue("1.3.6.1.5.5.7.1.14") != null) {
                error = X509Utils.V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED;
                errorDepth = i;
                currentCertificate = x;
                ok = verifyCallback.call(this, Integer.valueOf(0));
                if ( ok == 0 ) return ok;
            }

            ret = Purpose.checkCA(x);
            switch(must_be_ca) {
            case -1:
                if((verifyParameter.flags & X509Utils.V_FLAG_X509_STRICT) != 0 && ret != 1 && ret != 0) {
                    ret = 0;
                    error = X509Utils.V_ERR_INVALID_CA;
                } else {
                    ret = 1;
                }
                break;
            case 0:
                if(ret != 0) {
                    ret = 0;
                    error = X509Utils.V_ERR_INVALID_NON_CA;
                } else {
                    ret = 1;
                }
                break;
            default:
                if(ret == 0 || ((verifyParameter.flags & X509Utils.V_FLAG_X509_STRICT) != 0 && ret != 1)) {
                    ret = 0;
                    error = X509Utils.V_ERR_INVALID_CA;
                } else {
                    ret = 1;
                }
                break;
            }
            if(ret == 0) {
                errorDepth = i;
                currentCertificate = x;
                ok = verifyCallback.call(this, Integer.valueOf(0));
                if ( ok == 0 ) return ok;
            }
            if(verifyParameter.purpose > 0) {
                ret = Purpose.checkPurpose(x,verifyParameter.purpose, must_be_ca > 0 ? 1 : 0);
                if(ret == 0 || ((verifyParameter.flags & X509Utils.V_FLAG_X509_STRICT) != 0 && ret != 1)) {
                    error = X509Utils.V_ERR_INVALID_PURPOSE;
                    errorDepth = i;
                    currentCertificate = x;
                    ok = verifyCallback.call(this, Integer.valueOf(0));
                    if(ok == 0) {
                        return ok;
                    }
                }
            }

            if(i > 1 && x.getBasicConstraints() != -1 && x.getBasicConstraints() != Integer.MAX_VALUE && (i > (x.getBasicConstraints() + proxy_path_length + 1))) {
                error = X509Utils.V_ERR_PATH_LENGTH_EXCEEDED;
                errorDepth = i;
                currentCertificate = x;
                ok = verifyCallback.call(this, Integer.valueOf(0));
                if ( ok == 0 ) return ok;
            }

            if(x.getExtensionValue("1.3.6.1.5.5.7.1.14") != null) {
                ASN1Sequence pci = (ASN1Sequence)new ASN1InputStream(x.getExtensionValue("1.3.6.1.5.5.7.1.14")).readObject();
                if(pci.size() > 0 && pci.getObjectAt(0) instanceof ASN1Integer) {
                    int pcpathlen = ((ASN1Integer)pci.getObjectAt(0)).getValue().intValue();
                    if(i > pcpathlen) {
                        error = X509Utils.V_ERR_PROXY_PATH_LENGTH_EXCEEDED;
                        errorDepth = i;
                        currentCertificate = x;
                        ok = verifyCallback.call(this, Integer.valueOf(0));
                        if ( ok == 0 ) return ok;
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

    /**
     * c: X509_check_trust
     */
    public int checkTrust() throws Exception {
        int i,ok;
        X509AuxCertificate x;
        i = chain.size()-1;
        x = chain.get(i);
        ok = Trust.checkTrust(x, verifyParameter.trust, 0);
        if ( ok == X509Utils.X509_TRUST_TRUSTED ) {
            return 1;
        }
        errorDepth = 1;
        currentCertificate = x;
        if ( ok == X509Utils.X509_TRUST_REJECTED ) {
            error = X509Utils.V_ERR_CERT_REJECTED;
        } else {
            error = X509Utils.V_ERR_CERT_UNTRUSTED;
        }
        return verifyCallback.call(this, Integer.valueOf(0));
    }

    /**
     * c: check_cert_time
     */
    public int checkCertificateTime(X509AuxCertificate x) throws Exception {
        final Date pTime;
        if ( (verifyParameter.flags & X509Utils.V_FLAG_USE_CHECK_TIME) != 0 ) {
            pTime = this.verifyParameter.checkTime;
        } else {
            pTime = Calendar.getInstance().getTime();
        }

        if ( ! x.getNotBefore().before(pTime) ) {
            error = X509Utils.V_ERR_CERT_NOT_YET_VALID;
            currentCertificate = x;
            if ( verifyCallback.call(this, Integer.valueOf(0)) == 0 ) {
                return 0;
            }
        }
        if ( ! x.getNotAfter().after(pTime) ) {
            error = X509Utils.V_ERR_CERT_HAS_EXPIRED;
            currentCertificate = x;
            if ( verifyCallback.call(this, Integer.valueOf(0)) == 0 ) {
                return 0;
            }
        }
        return 1;
    }

    /**
     * c: check_cert
     */
    public int checkCertificate() throws Exception {
        final X509CRL[] crl = new X509CRL[1];
        X509AuxCertificate x;
        int ok, cnum;
        cnum = errorDepth;
        x = chain.get(cnum);
        currentCertificate = x;
        ok = getCRL.call(this, crl, x);
        if ( ok == 0 ) {
            error = X509Utils.V_ERR_UNABLE_TO_GET_CRL;
            ok = verifyCallback.call(this, Integer.valueOf(0));
            currentCRL = null;
            return ok;
        }
        currentCRL = crl[0];
        ok = checkCRL.call(this, crl[0]);
        if ( ok == 0 ) {
            currentCRL = null;
            return ok;
        }
        ok = certificateCRL.call(this, crl[0], x);
        currentCRL = null;
        return ok;
    }

    /**
     * c: check_crl_time
     */
    public int checkCRLTime(X509CRL crl, int notify) throws Exception {
        currentCRL = crl;
        final Date pTime;
        if ( (verifyParameter.flags & X509Utils.V_FLAG_USE_CHECK_TIME) != 0 ) {
            pTime = this.verifyParameter.checkTime;
        } else {
            pTime = Calendar.getInstance().getTime();
        }

        if ( ! crl.getThisUpdate().before(pTime) ) {
            error = X509Utils.V_ERR_CRL_NOT_YET_VALID;
            if ( notify == 0 || verifyCallback.call(this, Integer.valueOf(0)) == 0 ) {
                return 0;
            }
        }
        if ( crl.getNextUpdate() != null && !crl.getNextUpdate().after(pTime) ) {
            error = X509Utils.V_ERR_CRL_HAS_EXPIRED;
            if ( notify == 0 || verifyCallback.call(this, Integer.valueOf(0)) == 0 ) {
                return 0;
            }
        }

        currentCRL = null;
        return 1;
    }

    /**
     * c: get_crl_sk
     */
    public int getCRLStack(X509CRL[] pcrl, Name name, List<X509CRL> crls) throws Exception {
        X509CRL bestCrl = null;
        if ( crls != null ) {
            for ( final X509CRL crl : crls ) {
                if( ! name.isEqual( crl.getIssuerX500Principal() ) ) {
                    continue;
                }
                if ( checkCRLTime(crl, 0) != 0 ) {
                    pcrl[0] = crl;
                    return 1;
                }
                bestCrl = crl;
            }
        }
        if ( bestCrl != null ) {
            pcrl[0] = bestCrl;
        }
        return 0;
    }

    final static Store.GetIssuerFunction getFirstIssuer = new Store.GetIssuerFunction() {
        public int call(StoreContext context, X509AuxCertificate[] issuer, X509AuxCertificate cert) throws Exception {
            return context.getFirstIssuer(issuer, cert);
        }
    };

    /**
     * c: get_issuer_sk
     */
    final static Store.GetIssuerFunction getIssuerStack = new Store.GetIssuerFunction() {
        public int call(StoreContext context, X509AuxCertificate[] issuer, X509AuxCertificate x) throws Exception {
            issuer[0] = context.findIssuer(context.otherContext, x);
            if ( issuer[0] != null ) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    /**
     * c: check_issued
     */
    final static Store.CheckIssuedFunction defaultCheckIssued = new Store.CheckIssuedFunction() {
        public int call(StoreContext context, X509AuxCertificate cert, X509AuxCertificate issuer) throws Exception {
            int ret = X509Utils.checkIfIssuedBy(issuer, cert);
            if ( ret == X509Utils.V_OK ) return 1;

            if ( (context.verifyParameter.flags & X509Utils.V_FLAG_CB_ISSUER_CHECK) == 0 ) {
                return 0;
            }
            context.error = ret;
            context.currentCertificate = cert;
            context.currentIssuer = issuer;

            return context.verifyCallback.call(context, Integer.valueOf(0));
        }
    };

    /**
     * c: null_callback
     */
    final static Store.VerifyCallbackFunction nullCallback = new Store.VerifyCallbackFunction() {
        public int call(StoreContext context, Integer outcome) {
            return outcome.intValue();
        }
    };

    /**
     * c: internal_verify
     */
    final static Store.VerifyFunction internalVerify = new Store.VerifyFunction() {
        public int call(final StoreContext context) throws Exception {
            Store.VerifyCallbackFunction verifyCallback = context.verifyCallback;

            int n = context.chain.size();
            context.errorDepth = n - 1;
            n--;
            X509AuxCertificate xi = context.chain.get(n);
            X509AuxCertificate xs = null;
            int ok = 0;

            if ( context.checkIssued.call(context,xi,xi) != 0 ) {
                xs = xi;
            }
            else {
                if ( n <= 0 ) {
                    context.error = X509Utils.V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE;
                    context.currentCertificate = xi;
                    ok = verifyCallback.call(context, Integer.valueOf(0));
                    return ok;
                }
                else {
                    n--;
                    context.errorDepth = n;
                    xs = context.chain.get(n);
                }
            }

            while ( n >= 0 ) {
                context.errorDepth = n;
                if ( ! xs.isValid() ) {
                    try {
                        xs.verify(xi.getPublicKey());
                    }
                    catch(Exception e) {
                        /*
                        System.err.println("n: " + n);
                        System.err.println("verifying: " + xs);
                        System.err.println("verifying with issuer?: " + xi);
                        System.err.println("verifying with issuer.key?: " + xi.getPublicKey());
                        System.err.println("exception: " + e);
                        */
                        context.error = X509Utils.V_ERR_CERT_SIGNATURE_FAILURE;
                        context.currentCertificate = xs;
                        ok = verifyCallback.call(context, Integer.valueOf(0));
                        if ( ok == 0 ) return ok;
                    }
                }

                xs.setValid(true);
                ok = context.checkCertificateTime(xs);
                if ( ok == 0 ) return ok;

                context.currentIssuer = xi;
                context.currentCertificate = xs;
                ok = verifyCallback.call(context, Integer.valueOf(1));
                if ( ok == 0 ) return ok;

                n--;
                if ( n >= 0 ) {
                    xi = xs;
                    xs = context.chain.get(n);
                }
            }
            ok = 1;
            return ok;
        }
    };

    /**
     * c: check_revocation
     */
    final static Store.CheckRevocationFunction defaultCheckRevocation = new Store.CheckRevocationFunction() {
        public int call(final StoreContext context) throws Exception {
            if ( (context.verifyParameter.flags & X509Utils.V_FLAG_CRL_CHECK) == 0 ) {
                return 1;
            }
            final int last;
            if ( (context.verifyParameter.flags & X509Utils.V_FLAG_CRL_CHECK_ALL) != 0 ) {
                last = context.chain.size() - 1;
            }
            else {
                last = 0;
            }
            int ok;
            for ( int i=0; i<=last; i++ ) {
                context.errorDepth = i;
                ok = context.checkCertificate();
                if ( ok == 0 ) return 0;
            }
            return 1;
        }
    };

    /**
     * c: get_crl
     */
    final static Store.GetCRLFunction defaultGetCRL = new Store.GetCRLFunction() {
        public int call(final StoreContext context, final X509CRL[] crls, X509AuxCertificate x) throws Exception {
            final Name name = new Name( x.getIssuerX500Principal() );
            final X509CRL[] crl = new X509CRL[1];
            int ok = context.getCRLStack(crl, name, context.crls);
            if ( ok != 0 ) {
                crls[0] = crl[0];
                return 1;
            }
            final X509Object[] xobj = new X509Object[1];
            ok = context.getBySubject(X509Utils.X509_LU_CRL, name, xobj);
            if ( ok == 0 ) {
                if ( crl[0] != null ) {
                    crls[0] = crl[0];
                    return 1;
                }
                return 0;
            }
            crls[0] = (X509CRL) ( (CRL) xobj[0] ).crl;
            return 1;
        }
    };

    /**
     * c: check_crl
     */
    final static Store.CheckCRLFunction defaultCheckCRL = new Store.CheckCRLFunction() {
        public int call(final StoreContext context, final X509CRL crl) throws Exception {
            final int errorDepth = context.errorDepth;
            final int lastInChain = context.chain.size() - 1;

            int ok;
            final X509AuxCertificate issuer;
            if ( errorDepth < lastInChain ) {
                issuer = context.chain.get(errorDepth + 1);
            }
            else {
                issuer = context.chain.get(lastInChain);
                if ( context.checkIssued.call(context,issuer,issuer) == 0 ) {
                    context.error = X509Utils.V_ERR_UNABLE_TO_GET_CRL_ISSUER;
                    ok = context.verifyCallback.call(context, Integer.valueOf(0));
                    if ( ok == 0 ) return ok;
                }
            }

            if ( issuer != null ) {
                if ( issuer.getKeyUsage() != null && ! issuer.getKeyUsage()[6] ) {
                    context.error = X509Utils.V_ERR_KEYUSAGE_NO_CRL_SIGN;
                    ok = context.verifyCallback.call(context, Integer.valueOf(0));
                    if ( ok == 0 ) return ok;
                }
                final PublicKey ikey = issuer.getPublicKey();
                if ( ikey == null ) {
                    context.error = X509Utils.V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY;
                    ok = context.verifyCallback.call(context, Integer.valueOf(0));
                    if ( ok == 0 ) return ok;
                }
                else {
                    try {
                        SecurityHelper.verify(crl, ikey);
                    }
                    catch (GeneralSecurityException ex) {
                        context.error = X509Utils.V_ERR_CRL_SIGNATURE_FAILURE;
                        ok = context.verifyCallback.call(context, Integer.valueOf(0));
                        if ( ok == 0 ) return ok;
                    }
                }
            }

            ok = context.checkCRLTime(crl, 1);
            if ( ok == 0 ) return ok;

            return 1;
        }
    };

    /**
     * c: cert_crl
     */
    final static Store.CertificateCRLFunction defaultCertificateCRL = new Store.CertificateCRLFunction() {
        public int call(final StoreContext context, final X509CRL crl, X509AuxCertificate x) throws Exception {
            int ok;
            if ( crl.getRevokedCertificate( x.getSerialNumber() ) != null ) {
                context.error = X509Utils.V_ERR_CERT_REVOKED;
                ok = context.verifyCallback.call(context, Integer.valueOf(0));
                if ( ok == 0 ) return 0;
            }
            if ( (context.verifyParameter.flags & X509Utils.V_FLAG_IGNORE_CRITICAL) != 0 ) {
                return 1;
            }
            if ( crl.getCriticalExtensionOIDs() != null && crl.getCriticalExtensionOIDs().size() > 0 ) {
                context.error = X509Utils.V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION;
                ok = context.verifyCallback.call(context, Integer.valueOf(0));
                if ( ok == 0 ) return 0;
            }
            return 1;
        }
    };

    /**
     * c: check_policy
     */
    final static CheckPolicyFunction defaultCheckPolicy = new CheckPolicyFunction() {
        public int call(StoreContext context) throws Exception {
            return 1;
        }
    };
}// X509_STORE_CTX
