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

import java.util.Date;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.bouncycastle.asn1.ASN1Primitive;

/**
 * c: X509_VERIFY_PARAM
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class VerifyParameter {
    public String name;
    public Date checkTime;
    public long inheritFlags;
    public long flags;
    public int purpose;
    public int trust;
    public int depth;
    public List<ASN1Primitive> policies;

    /**
     * c: X509_VERIFY_PARAM_new
     */
    public VerifyParameter() { 
        zero();
    }

    public VerifyParameter(String n, long t, long i_f, long f, int p, int trs, int d, List<ASN1Primitive> pol) {
        this.name = n;
        this.checkTime = new Date(t);
        this.inheritFlags = i_f;
        this.flags = f;
        this.purpose = p;
        this.trust = trs;
        this.depth = d;
        this.policies = pol;
    }

    private void zero() {
        name = null;
        purpose = 0;
        trust = 0;
        inheritFlags = X509Utils.X509_VP_FLAG_DEFAULT;
        flags = 0;
        depth = -1;
        policies = null;
    }

    /**
     * c: X509_VERIFY_PARAM_free
     */
    public void free() {
        zero();
    }
    
    /**
     * c: X509_VERIFY_PARAM_inherit
     */
    public int inherit(VerifyParameter src) { 
        long inh_flags;
        boolean to_d, to_o;

        if(src == null) {
            return 1;
        }


        inh_flags = src.inheritFlags | this.inheritFlags;
        if((inh_flags & X509Utils.X509_VP_FLAG_ONCE) != 0) {
            this.inheritFlags = 0;
        }
        if((inh_flags & X509Utils.X509_VP_FLAG_LOCKED) != 0) {
            return 1;
        }
        to_d = ((inh_flags & X509Utils.X509_VP_FLAG_DEFAULT) != 0);
        to_o = ((inh_flags & X509Utils.X509_VP_FLAG_OVERWRITE) != 0);

        if(to_o || ((src.purpose != 0 && (to_d || this.purpose == 0)))) {
            this.purpose = src.purpose;
        }
        if(to_o || ((src.trust != 0 && (to_d || this.trust == 0)))) {
            this.trust = src.trust;
        }
        if(to_o || ((src.depth != -1 && (to_d || this.depth == -1)))) {
            this.depth = src.depth;
        }

        if(to_o || !((this.flags & X509Utils.V_FLAG_USE_CHECK_TIME) != 0)) {
            this.checkTime = src.checkTime;
            this.flags &= ~X509Utils.V_FLAG_USE_CHECK_TIME;
        }

        if((inh_flags & X509Utils.X509_VP_FLAG_RESET_FLAGS) != 0) {
            this.flags = 0;
        }

        this.flags |= src.flags;

        if(to_o || ((src.policies != null && (to_d || this.policies == null)))) {
            setPolicies(src.policies);
        }
        return 1;
    }
    
    /**
     * c: X509_VERIFY_PARAM_set1
     */
    public int set(VerifyParameter from) { 
        inheritFlags |= X509Utils.X509_VP_FLAG_DEFAULT;
        return inherit(from);
    } 
    
    /**
     * c: X509_VERIFY_PARAM_set1_name
     */
    public int setName(String name) { 
        this.name = name;
        return 1;
    }
    
    /**
     * c: X509_VERIFY_PARAM_set_flags
     */
    public int setFlags(long flags) { 
        this.flags |= flags;
        if((flags & X509Utils.V_FLAG_POLICY_MASK) == X509Utils.V_FLAG_POLICY_MASK) {
            this.flags |= X509Utils.V_FLAG_POLICY_CHECK;
        }
        return 1;
    } 
    
    /**
     * c: X509_VERIFY_PARAM_clear_flags
     */
    public int clearFlags(long flags) { 
        this.flags &= ~flags;
        return 1;
    } 
    
    /**
     * c: X509_VERIFY_PARAM_get_flags
     */
    public long getFlags() { 
        return flags;
    } 
    
    /**
     * c: X509_VERIFY_PARAM_set_purpose
     */
    public int setPurpose(int purpose) { 
        int[] arg = new int[]{this.purpose};
        int v = Purpose.set(arg,purpose);
        this.purpose = arg[0];
        return v;
    } 
    
    /**
     * c: X509_VERIFY_PARAM_set_trust
     */
    public int setTrust(int trust) { 
        int[] arg = new int[]{this.trust};
        int v = Trust.set(arg,trust);
        this.trust = arg[0];
        return v;
    }
    
    /**
     * c: X509_VERIFY_PARAM_set_depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    /**
     * c: X509_VERIFY_PARAM_set_time
     */
    public void setTime(Date t) {
        this.checkTime = t;
        this.flags |= X509Utils.V_FLAG_USE_CHECK_TIME;
    } 
    
    /**
     * c: X509_VERIFY_PARAM_add0_policy
     */
    public int addPolicy(ASN1Primitive policy) { 
        if(policies == null) {
            policies = new ArrayList<ASN1Primitive>();
        }
        policies.add(policy);
        return 1;
    }
    
    /**
     * c: X509_VERIFY_PARAM_set1_policies
     */
    public int setPolicies(List<ASN1Primitive> policies) { 
        if(policies == null) {
            this.policies = null;
            return 1;
        }
        this.policies = new ArrayList<ASN1Primitive>();
        this.policies.addAll(policies);
        this.flags |= X509Utils.V_FLAG_POLICY_CHECK;
        return 1;
    }
    
    /**
     * c: X509_VERIFY_PARAM_get_depth
     */
    public int getDepth() { 
        return depth;
    }
    
    /**
     * c: X509_VERIFY_PARAM_add0_table
     */
    public int addTable() { 
        for(Iterator<VerifyParameter> iter = parameterTable.iterator();iter.hasNext();) {
            VerifyParameter v = iter.next();
            if(this.name.equals(v.name)) {
                iter.remove();
            }
        }
        parameterTable.add(this);
        return 1;
    } 

    public static VerifyParameter lookup(String name) { 
        for(VerifyParameter v : parameterTable) {
            if(name.equals(v.name)) {
                return v;
            }
        }
        for(VerifyParameter v : defaultTable) {
            if(name.equals(v.name)) {
                return v;
            }
        }
        return null; 
    }
    
    /**
     * c: X509_VERIFY_PARAM_table_cleanup
     */
    public static void tableCleanup() {
        parameterTable.clear();
    } 

    private final static VerifyParameter[] defaultTable = new VerifyParameter[] {
        new VerifyParameter(
                            "default",	/* X509 default parameters */
                            0,		/* Check time */
                            0,		/* internal flags */
                            0,		/* flags */
                            0,		/* purpose */
                            0,		/* trust */
                            100,	/* depth */
                            null	/* policies */
                            ),
        new VerifyParameter(
                            "pkcs7",			/* SSL/TLS client parameters */
                            0,				/* Check time */
                            0,				/* internal flags */
                            0,				/* flags */
                            X509Utils.X509_PURPOSE_SMIME_SIGN,	/* purpose */
                            X509Utils.X509_TRUST_EMAIL,		/* trust */
                            -1,				/* depth */
                            null				/* policies */
                            ),
        new VerifyParameter(
                            "ssl_client",			/* SSL/TLS client parameters */
                            0,				/* Check time */
                            0,				/* internal flags */
                            0,				/* flags */
                            X509Utils.X509_PURPOSE_SSL_CLIENT,	/* purpose */
                            X509Utils.X509_TRUST_SSL_CLIENT,		/* trust */
                            -1,				/* depth */
                            null				/* policies */
                            ),
        new VerifyParameter(
                            "ssl_server",			/* SSL/TLS server parameters */
                            0,				/* Check time */
                            0,				/* internal flags */
                            0,				/* flags */
                            X509Utils.X509_PURPOSE_SSL_SERVER,	/* purpose */
                            X509Utils.X509_TRUST_SSL_SERVER,		/* trust */
                            -1,				/* depth */
                            null				/* policies */
                            )};

    private final static List<VerifyParameter> parameterTable = new ArrayList<VerifyParameter>();
}// X509_VERIFY_PARAM
