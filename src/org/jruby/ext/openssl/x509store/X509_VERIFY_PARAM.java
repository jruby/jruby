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

import java.util.Date;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.DERObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_VERIFY_PARAM {
    public String name;
    public Date check_time;
    public long inh_flags;
    public long flags;
    public int purpose;
    public int trust;
    public int depth;
    public List policies;

    public X509_VERIFY_PARAM() { 
        zero();
    }

    public X509_VERIFY_PARAM(String n, long t, long i_f, long f, int p, int trs, int d, List pol) {
        this.name = n;
        this.check_time = new Date(t);
        this.inh_flags = i_f;
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
        inh_flags = X509.X509_VP_FLAG_DEFAULT;
        flags = 0;
        depth = -1;
        policies = null;
    }

    public void free() {
        zero();
    }
    
    public int inherit(X509_VERIFY_PARAM src) { 
        long inh_flags;
        boolean to_d, to_o;

        if(src == null) {
            return 1;
        }


        inh_flags = src.inh_flags | this.inh_flags;
        if((inh_flags & X509.X509_VP_FLAG_ONCE) != 0) {
            this.inh_flags = 0;
        }
        if((inh_flags & X509.X509_VP_FLAG_LOCKED) != 0) {
            return 1;
        }
        to_d = ((inh_flags & X509.X509_VP_FLAG_DEFAULT) != 0);
        to_o = ((inh_flags & X509.X509_VP_FLAG_OVERWRITE) != 0);

        if(to_o || ((src.purpose != 0 && (to_d || this.purpose == 0)))) {
            this.purpose = src.purpose;
        }
        if(to_o || ((src.trust != 0 && (to_d || this.trust == 0)))) {
            this.trust = src.trust;
        }
        if(to_o || ((src.depth != -1 && (to_d || this.depth == -1)))) {
            this.depth = src.depth;
        }

        if(to_o || !((this.flags & X509.V_FLAG_USE_CHECK_TIME) != 0)) {
            this.check_time = src.check_time;
            this.flags &= ~X509.V_FLAG_USE_CHECK_TIME;
        }

        if((inh_flags & X509.X509_VP_FLAG_RESET_FLAGS) != 0) {
            this.flags = 0;
        }

        this.flags |= src.flags;

        if(to_o || ((src.policies != null && (to_d || this.policies == null)))) {
            set1_policies(src.policies);
        }
	return 1;
    }
    
    public int set1(X509_VERIFY_PARAM from) { 
	inh_flags |= X509.X509_VP_FLAG_DEFAULT;
	return inherit(from);
    } 
    
    public int set1_name(String name) { 
        this.name = name;
        return 1;
    }
    
    public int set_flags(long flags) { 
	this.flags |= flags;
	if((flags & X509.V_FLAG_POLICY_MASK) == X509.V_FLAG_POLICY_MASK) {
            this.flags |= X509.V_FLAG_POLICY_CHECK;
        }
        return 1;
    } 
    
    public int clear_flags(long flags) { 
	this.flags &= ~flags;
	return 1;
    } 
    
    public long get_flags() { 
	return flags;
    } 
    
    public int set_purpose(int purpose) { 
        int[] arg = new int[]{this.purpose};
        int v = X509_PURPOSE.set(arg,purpose);
        this.purpose = arg[0];
        return v;
    } 
    
    public int set_trust(int trust) { 
        int[] arg = new int[]{this.trust};
        int v = X509_TRUST.set(arg,trust);
        this.trust = arg[0];
        return v;
    }
    
    public void set_depth(int depth) {
	this.depth = depth;
    }
    
    public void set_time(Date t) {
	this.check_time = t;
	this.flags |= X509.V_FLAG_USE_CHECK_TIME;
    } 
    
    public int add0_policy(DERObject policy) { 
        if(policies == null) {
            policies = new ArrayList();
        }
        policies.add(policy);
        return 1;
    }
    
    public int set1_policies(List policies) { 
        if(policies == null) {
            this.policies = null;
            return 1;
        }
        this.policies = new ArrayList();
        this.policies.addAll(policies);
        this.flags |= X509.V_FLAG_POLICY_CHECK;
	return 1;
    }
    
    public int get_depth() { 
	return depth;
    }
    
    public int add0_table() { 
        for(Iterator iter = param_table.iterator();iter.hasNext();) {
            X509_VERIFY_PARAM v = (X509_VERIFY_PARAM)iter.next();
            if(this.name.equals(v.name)) {
                iter.remove();
            }
        }
        param_table.add(this);
	return 1;
    } 

    public static X509_VERIFY_PARAM lookup(String name) { 
        for(Iterator iter = param_table.iterator();iter.hasNext();) {
            X509_VERIFY_PARAM v = (X509_VERIFY_PARAM)iter.next();
            if(name.equals(v.name)) {
                return v;
            }
        }
        for(int i=0;i<default_table.length;i++) {
            if(name.equals(default_table[i].name)) {
                return default_table[i];
            }
        }
        return null; 
    }
    
    public static void table_cleanup() {
        param_table.clear();
    } 

    private final static X509_VERIFY_PARAM[] default_table = new X509_VERIFY_PARAM[] {
        new X509_VERIFY_PARAM(
	"default",	/* X509 default parameters */
	0,		/* Check time */
	0,		/* internal flags */
	0,		/* flags */
	0,		/* purpose */
	0,		/* trust */
	9,		/* depth */
	null		/* policies */
                              ),
        new X509_VERIFY_PARAM(
	"pkcs7",			/* SSL/TLS client parameters */
	0,				/* Check time */
	0,				/* internal flags */
	0,				/* flags */
	X509.X509_PURPOSE_SMIME_SIGN,	/* purpose */
	X509.X509_TRUST_EMAIL,		/* trust */
	-1,				/* depth */
	null				/* policies */
                              ),
        new X509_VERIFY_PARAM(
	"ssl_client",			/* SSL/TLS client parameters */
	0,				/* Check time */
	0,				/* internal flags */
	0,				/* flags */
	X509.X509_PURPOSE_SSL_CLIENT,	/* purpose */
	X509.X509_TRUST_SSL_CLIENT,		/* trust */
	-1,				/* depth */
	null				/* policies */
                              ),
        new X509_VERIFY_PARAM(
	"ssl_server",			/* SSL/TLS server parameters */
	0,				/* Check time */
	0,				/* internal flags */
	0,				/* flags */
	X509.X509_PURPOSE_SSL_SERVER,	/* purpose */
	X509.X509_TRUST_SSL_SERVER,		/* trust */
	-1,				/* depth */
	null				/* policies */
                              )};

    private final static List param_table = new ArrayList();
}// X509_VERIFY_PARAM
