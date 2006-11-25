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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.math.BigInteger;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CRL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_LOOKUP {
    public boolean init;
    public boolean skip;
    public X509_LOOKUP_METHOD method;
    public Object method_data;
    public X509_STORE store_ctx;

    public final static int X509_L_FILE_LOAD = 1;
    public final static int X509_L_ADD_DIR = 2;

    public X509_LOOKUP(X509_LOOKUP_METHOD method) throws Exception {
	init=false;
	skip=false;
	this.method=method;
	method_data=null;
	store_ctx=null;
        if(method.new_item != null && method.new_item != Function1.iZ && method.new_item.call(this) == 0) {
            throw new Exception();
        }
    }

    public int load_file(X509_CERT_FILE_CTX.Path file) throws Exception {
        return ctrl(X509_L_FILE_LOAD,file.name,file.type,null);
    }

    public int add_dir(X509_HASH_DIR_CTX.Dir dir) throws Exception {
        return ctrl(X509_L_ADD_DIR,dir.name,dir.type,null);
    }

    public static X509_LOOKUP_METHOD hash_dir() { 
        return x509_dir_lookup;
    } 

    public static X509_LOOKUP_METHOD file() { 
        return x509_file_lookup;
    }

    public int ctrl(int cmd, String argc, long argl, String[] ret) throws Exception {
        if(method == null) {
            return -1;
        }
        if(method.ctrl != null && method.ctrl != Function5.iZ) {
            return method.ctrl.call(this,new Integer(cmd),argc,new Long(argl),ret);
        } else {
            return 1;
        }
    }

    public int load_cert_file(String file, int type) throws Exception { 
        if(file == null) {
            return 1;
        }
        int count = 0;
        int ret = 0;
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        X509AuxCertificate x = null;

        if(type == X509.X509_FILETYPE_PEM) {
            Reader r = new InputStreamReader(in);
            for(;;) {
                x = PEM.read_X509_AUX(r,null);
                if(null == x) {
                    break;
                }
                int i = store_ctx.add_cert(x);
                if(i == 0) {
                    return ret;
                }
                count++;
                x = null;
            }
            ret = count;
        } else if(type == X509.X509_FILETYPE_ASN1) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509","BC");
            x = X509_STORE_CTX.transform((X509Certificate)cf.generateCertificate(in));
            if(x == null) {
                Err.PUT_err(13);
                return ret;
            }
            int i = store_ctx.add_cert(x);
            if(i == 0) {
                return ret;
            }
            ret = i;
        } else {
            Err.PUT_err(X509.X509_R_BAD_X509_FILETYPE);
        }

        return ret;
    } 

    public int load_crl_file(String file, int type) throws Exception { 
        if(file == null) {
            return 1;
        }
        int count = 0;
        int ret = 0;
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        CRL x = null;

        if(type == X509.X509_FILETYPE_PEM) {
            Reader r = new InputStreamReader(in);
            for(;;) {
                x = PEM.read_X509_CRL(r,null);;
                if(null == x) {
                    break;
                }
                int i = store_ctx.add_crl(x);
                if(i == 0) {
                    return ret;
                }
                count++;
                x = null;
            }
            ret = count;
        } else if(type == X509.X509_FILETYPE_ASN1) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509","BC");
            x = cf.generateCRL(in);
            if(x == null) {
                Err.PUT_err(13);
                return ret;
            }
            int i = store_ctx.add_crl(x);
            if(i == 0) {
                return ret;
            }
            ret = i;
        } else {
            Err.PUT_err(X509.X509_R_BAD_X509_FILETYPE);
        }

        return ret;
    }

    public int load_cert_crl_file(String file, int type) throws Exception { 
        if(type != X509.X509_FILETYPE_PEM) {
            return load_cert_file(file,type);
        }
        int count = 0;
        Reader r  = new FileReader(file);
        for(;;) {
            Object v = PEM.read(r,null);
            if(null == v) {
                break;
            }
            if(v instanceof X509Certificate) {
                store_ctx.add_cert(X509_STORE_CTX.transform((X509Certificate)v));
                count++;
            } else if(v instanceof CRL) {
                store_ctx.add_crl((CRL)v);
                count++;
            }
        }

        return count; 
    } 

    public void free() throws Exception {
        if(method != null && method.free != null && method.free != Function1.iZ) {
            method.free.call(this);
        }
    }

    public int init() throws Exception { 
        if(method == null) {
            return 0;
        }
        if(method.init != null && method.init != Function1.iZ) {
            return method.init.call(this);
        }
        return 1;
    }

    public int by_subject(int type, X509_NAME name,X509_OBJECT[] ret) throws Exception { 
        if(method == null || method.get_by_subject == null || method.get_by_subject == Function4.iZ) {
            return X509.X509_LU_FAIL;
        }
        if(skip) {
            return 0;
        }
        return method.get_by_subject.call(this,new Integer(type),name,ret);
    }

    public int by_issuer_serial(int type, X509_NAME name,BigInteger serial, X509_OBJECT[] ret) throws Exception { 
        if(method == null || method.get_by_issuer_serial == null || method.get_by_issuer_serial == Function5.iZ) {
            return X509.X509_LU_FAIL;
        }
        return method.get_by_issuer_serial.call(this,new Integer(type),name,serial,ret);
    } 

    public int by_fingerprint(int type,String bytes, X509_OBJECT[] ret) throws Exception { 
        if(method == null || method.get_by_fingerprint == null || method.get_by_fingerprint == Function4.iZ) {
            return X509.X509_LU_FAIL;
        }
        return method.get_by_fingerprint.call(this,new Integer(type),bytes,ret);
    } 

    public int by_alias(int type, String str, X509_OBJECT[] ret) throws Exception { 
        if(method == null || method.get_by_alias == null || method.get_by_alias == Function4.iZ) {
            return X509.X509_LU_FAIL;
        }
        return method.get_by_alias.call(this,new Integer(type),str,ret);
    } 

    public int shutdown() throws Exception { 
        if(method == null) {
            return 0;
        }
        if(method.shutdown != null && method.shutdown != Function1.iZ) {
            return method.shutdown.call(this);
        }
        return 1;
    }

    private final static X509_LOOKUP_METHOD x509_file_lookup = new X509_LOOKUP_METHOD();
    private final static X509_LOOKUP_METHOD x509_dir_lookup = new X509_LOOKUP_METHOD();
    static {
        x509_file_lookup.name = "Load file into cache";
        x509_file_lookup.ctrl = new File_ByFileCtrl();

        x509_dir_lookup.name = "Load certs from files in a directory";
        x509_dir_lookup.new_item = new Dir_New();
        x509_dir_lookup.free = new Dir_Free();
        x509_dir_lookup.ctrl = new Dir_Ctrl();
        x509_dir_lookup.get_by_subject = new Dir_GetCertBySubject();
    }

    private static class File_ByFileCtrl implements Function5 {
        public int call(Object _ctx, Object _cmd, Object _argp, Object _argl, Object _ret) throws Exception {
            X509_LOOKUP ctx = (X509_LOOKUP)_ctx;
            int cmd = ((Integer)_cmd).intValue();
            String argp = (String)_argp;
            long argl = ((Long)_argl).longValue();
            String[] ret = (String[])_ret;

            int ok = 0;
            String file = null;
            
            switch(cmd) {
            case X509.X509_L_FILE_LOAD:
		if(argl == X509.X509_FILETYPE_DEFAULT) {
			file = System.getenv(X509.get_default_cert_file_env());
			if(file != null) {
                            ok = ctx.load_cert_crl_file(file,X509.X509_FILETYPE_PEM) != 0 ? 1 : 0;
                        } else {
                            ok = (ctx.load_cert_crl_file(X509.get_default_cert_file(),X509.X509_FILETYPE_PEM) != 0) ? 1 : 0;
                        }
                        if(ok == 0) {
                            Err.PUT_err(X509.X509_R_LOADING_DEFAULTS);
                        }
                } else {
                    if(argl == X509.X509_FILETYPE_PEM) {
                        ok = (ctx.load_cert_crl_file(argp,X509.X509_FILETYPE_PEM) != 0) ? 1 : 0;
                    } else {
                        ok = (ctx.load_cert_file(argp,(int)argl) != 0) ? 1 : 0;
                    }
                }
                break;
            }

            return ok;
        }
    }

    private static class BY_DIR {
	StringBuffer buffer;
        List dirs;
        List dirs_type;
    }

    private static class Dir_New implements Function1 {
        public int call(Object _lu) {
            X509_LOOKUP lu = (X509_LOOKUP)_lu;
            BY_DIR a = new BY_DIR();
            a.buffer = new StringBuffer();
            a.dirs = new ArrayList();
            a.dirs_type = new ArrayList();
            lu.method_data = a;
            return 1;
        }
    }
    private static class Dir_Free implements Function1 {
        public int call(Object _lu) {
            X509_LOOKUP lu = (X509_LOOKUP)_lu;
            BY_DIR a = (BY_DIR)lu.method_data;
            a.dirs = null;
            a.dirs_type = null;
            a.buffer = null;
            lu.method_data = null;
            return -1;
        }
    }
    private static class Dir_Ctrl implements Function5 {
        public int call(Object _ctx, Object _cmd, Object _argp, Object _argl, Object _retp) {
            X509_LOOKUP ctx = (X509_LOOKUP)_ctx;
            int cmd = ((Integer)_cmd).intValue();
            String argp = (String)_argp;
            long argl = ((Long)_argl).longValue();
            String[] retp = (String[])_retp;
            int ret = 0;
            BY_DIR ld = (BY_DIR)ctx.method_data;
            String dir = null;
            switch(cmd) {
            case X509.X509_L_ADD_DIR:
		if(argl == X509.X509_FILETYPE_DEFAULT) {
                    dir = System.getenv(X509.get_default_cert_dir_env());
                    if(null != dir) {
                        ret = add_cert_dir(ld,dir,X509.X509_FILETYPE_PEM);
                    } else {
                        ret = add_cert_dir(ld,X509.get_default_cert_dir(),X509.X509_FILETYPE_PEM);
                    }
                    if(ret == 0) {
                        Err.PUT_err(X509.X509_R_LOADING_CERT_DIR);
                    }
                } else {
                    ret = add_cert_dir(ld,argp,(int)argl);
                }
		break;
            }
            return ret;
        }

        private int add_cert_dir(BY_DIR ctx,String dir,int type) {
            int[] ip;

            if(dir == null || "".equals(dir)) {
                Err.PUT_err(X509.X509_R_INVALID_DIRECTORY);
                return 0;
            }
 
            String[] dirs = dir.split(System.getProperty("path.separator"));

            for(int i=0;i<dirs.length;i++) {
                if(dirs[i].length() == 0) {
                    continue;
                }
                if(ctx.dirs.contains(dirs[i])) {
                    continue;
                }
                ctx.dirs_type.add(new Integer(type));
                ctx.dirs.add(dirs[i]);
            }

            return 1;
        }
    }
    private static class Dir_GetCertBySubject implements Function4 {
        public int call(Object _xl, Object _type, Object _name, Object _ret) throws Exception {
            X509_LOOKUP x1 = (X509_LOOKUP)_xl;
            int type = ((Integer)_type).intValue();
            X509_NAME name = (X509_NAME)_name;
            X509_OBJECT[] ret = (X509_OBJECT[])_ret;

            X509_OBJECT tmp = null;

            int ok = 0;
            StringBuffer b = new StringBuffer();

            if(null == name) {
                return 0;
            }

            String postfix = "";
            if(type == X509.X509_LU_X509) {
            } else if(type == X509.X509_LU_CRL) {
                postfix = "r";
            } else {
                Err.PUT_err(X509.X509_R_WRONG_LOOKUP_TYPE);
                return ok;
            }
            
            BY_DIR ctx = (BY_DIR)x1.method_data;

            long h = name.hash();
            
            for(Iterator iter = ctx.dirs.iterator(), iter2 = ctx.dirs_type.iterator();iter.hasNext();) {
                String cdir = (String)iter.next();
                int tp = ((Integer)iter2.next()).intValue();
                int k = 0;
                for(;;) {
                    char c = '/';
                    b.append(String.format("%s/%08lx.%s%d",new Object[]{cdir,new Long(h),postfix,new Integer(k)}));
                    k++;
                    if(!(new File(b.toString()).exists())) {
                        break;
                    }
                    if(type == X509.X509_LU_X509) {
                        if((x1.load_cert_file(b.toString(),tp)) == 0) {
                            break;
                        }
                    } else if(type == X509.X509_LU_CRL) {
                        if((x1.load_crl_file(b.toString(),tp)) == 0) {
                            break;
                        }
                    }
                }
                synchronized(X509.CRYPTO_LOCK_X509_STORE) {
                    tmp = null;
                    for(Iterator iterx = x1.store_ctx.objs.iterator();iterx.hasNext();) {
                        X509_OBJECT o = (X509_OBJECT)iterx.next();
                        if(o.type() == type && o.isName(name)) {
                            tmp = o;
                            break;
                        }
                    }
                }
                if(tmp != null) {
                    ok = 1;
                    ret[0] = tmp;
                    break;
                }
            }

            return ok;
        }
    }
}// X509_LOOKUP
