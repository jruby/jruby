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

import java.security.cert.X509Certificate;
import java.security.cert.CRL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.X509TrustManager;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_STORE implements X509TrustManager {
    public int cache;
    public List objs; // List<X509_OBJECT>
    public List get_cert_methods; // List<X509_LOOKUP>
    public X509_VERIFY_PARAM param;

    public Function1 verify;
    public Function2 verify_cb;
    public Function3 get_issuer;
    public Function3 check_issued;
    public Function1 check_revocation;
    public Function3 get_crl;
    public Function2 check_crl;
    public Function3 cert_crl;
    public Function1 cleanup;

    public List ex_data;
    public int references;

    public X509_STORE() {
        objs = new ArrayList();
        cache = 1;
        get_cert_methods = new ArrayList();

        verify = Function1.iZ;
        verify_cb = Function2.iZ;

        param = new X509_VERIFY_PARAM();
        
        get_issuer = Function3.iZ;
        check_issued = Function3.iZ;
        check_revocation = Function1.iZ;
        get_crl = Function3.iZ;
        check_crl = Function2.iZ;
        cert_crl = Function3.iZ;
        cleanup = Function1.iZ;

        references = 1;
        ex_data = new ArrayList();
        this.ex_data.add(null);this.ex_data.add(null);this.ex_data.add(null);
        this.ex_data.add(null);this.ex_data.add(null);this.ex_data.add(null);
        this.ex_data.add(null);this.ex_data.add(null);this.ex_data.add(null);
    }

    public void set_verify_func(Function1 func) {
        verify = func;
    }
    public void set_verify_cb_func(Function2 func) {
        verify_cb = func;
    }

    public void free() throws Exception {
        for(Iterator iter = get_cert_methods.iterator();iter.hasNext();) {
            X509_LOOKUP lu = (X509_LOOKUP)iter.next();
            lu.shutdown();
            lu.free();
        }
        if(param != null) {
            param.free();
        }
    }

    public int set_ex_data(int idx,Object data) { 
        ex_data.set(idx,data);
        return 1; 
    } 
    public Object get_ex_data(int idx) { 
        return ex_data.get(idx); 
    }

    public int set_depth(int depth) { 
        param.set_depth(depth);
	return 1;
    }

    public int set_flags(long flags) { 
	return param.set_flags(flags);
    }

    public int set_purpose(int purpose) { 
	return param.set_purpose(purpose);
    }

    public int set_trust(int trust) { 
	return param.set_trust(trust);
    }

    public int set1_param(X509_VERIFY_PARAM pm) { 
	return param.set1(param);
    }

    public X509_LOOKUP add_lookup(X509_LOOKUP_METHOD m) throws Exception { 
        X509_LOOKUP lu;

        for(Iterator iter = get_cert_methods.iterator();iter.hasNext();) {
            lu = (X509_LOOKUP)iter.next();
            if(lu.equals(m)) {
                return lu;
            }
        }
        lu = new X509_LOOKUP(m);
        lu.store_ctx = this;
        get_cert_methods.add(lu);
        return lu;
    } 

    public int add_cert(X509Certificate x) { 
        int ret = 1;
        if(x == null) {
            return 0;
        }

        X509_OBJECT_CERT obj = new X509_OBJECT_CERT();
        obj.x509 = X509_STORE_CTX.transform(x);

	synchronized(X509.CRYPTO_LOCK_X509_STORE) {
            if(X509_OBJECT.retrieve_match(objs,obj) != null) {
                Err.PUT_err(X509.X509_R_CERT_ALREADY_IN_HASH_TABLE);
		ret=0;
            } else {
                objs.add(obj);
            }
        }
	return ret;
    } 

    public int add_crl(CRL x) { 
        int ret = 1;
        if(null == x) {
            return 0;
        }
        X509_OBJECT_CRL obj = new X509_OBJECT_CRL();
        obj.crl = x;

	synchronized(X509.CRYPTO_LOCK_X509_STORE) {
            if(X509_OBJECT.retrieve_match(objs,obj) != null) {
                Err.PUT_err(X509.X509_R_CERT_ALREADY_IN_HASH_TABLE);
		ret=0;
            } else {
                objs.add(obj);
            }
        }
	return ret;
    } 

    public int load_locations(String file, String path) throws Exception { 
	X509_LOOKUP lookup;

	if(file != null) {
            lookup = add_lookup(X509_LOOKUP.file());
            if(lookup == null) {
                return 0;
            }
            if(lookup.load_file(new X509_CERT_FILE_CTX.Path(file,X509.X509_FILETYPE_PEM)) != 1) {
                return 0;
            }
        }

	if(path != null) {
            lookup = add_lookup(X509_LOOKUP.hash_dir());
            if(lookup == null) {
                return 0;
            }
            if(lookup.add_dir(new X509_HASH_DIR_CTX.Dir(path,X509.X509_FILETYPE_PEM)) != 1) {
                return 0;
            }
        }
	if((path == null) && (file == null)) {
            return 0;
        }

	return 1;
    } 

    public int set_default_paths() throws Exception { 
	X509_LOOKUP lookup;

	lookup = add_lookup(X509_LOOKUP.file());
	if(lookup == null) {
            return 0;
        }

	lookup.load_file(new X509_CERT_FILE_CTX.Path(null,X509.X509_FILETYPE_DEFAULT));

	lookup = add_lookup(X509_LOOKUP.hash_dir());
	if(lookup == null) {
            return 0;
        }

	lookup.add_dir(new X509_HASH_DIR_CTX.Dir(null,X509.X509_FILETYPE_DEFAULT));

	Err.clear_error();

	return 1;
    } 


    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    public X509Certificate[] getAcceptedIssuers() {
        List l = new ArrayList();
        for(Iterator iter = objs.iterator();iter.hasNext();) {
            Object o = iter.next();
            if(o instanceof X509_OBJECT_CERT) {
                l.add(((X509_OBJECT_CERT)o).x509);
            }
        }
        return (X509Certificate[])l.toArray(new X509Certificate[l.size()]);
    }
}// X509_STORE
