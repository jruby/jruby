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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.math.BigInteger;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CRL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.FileExistsException;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;

/**
 * X509_LOOKUP
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Lookup {
    public boolean init;
    public boolean skip;
    public LookupMethod method;
    public Object methodData;
    public Store store;

    /**
     * c: X509_LOOKUP_new
     */
    public Lookup(LookupMethod method) throws Exception {
        init=false;
        skip=false;
        this.method=method;
        methodData=null;
        store=null;
        if(method.newItem != null && method.newItem != Function1.EMPTY && method.newItem.call(this) == 0) {
            throw new Exception();
        }
    }

    /**
     * c: X509_LOOKUP_load_file
     */
    public int loadFile(CertificateFile.Path file) throws Exception {
        return control(X509Utils.X509_L_FILE_LOAD,file.name,file.type,null);
    }

    /**
     * c: X509_LOOKUP_add_dir
     */
    public int addDir(CertificateHashDir.Dir dir) throws Exception {
        return control(X509Utils.X509_L_ADD_DIR,dir.name,dir.type,null);
    }

    /**
     * c: X509_LOOKUP_hash_dir
     */
    public static LookupMethod hashDirLookup() { 
        return x509DirectoryLookup;
    } 

    /**
     * c: X509_LOOKUP_file
     */
    public static LookupMethod fileLookup() { 
        return x509FileLookup;
    }

    /**
     * c: X509_LOOKUP_ctrl
     */
    public int control(int cmd, String argc, long argl, String[] ret) throws Exception {
        if(method == null) {
            return -1;
        }
        if(method.control != null && method.control != Function5.EMPTY) {
            return method.control.call(this,new Integer(cmd),argc,new Long(argl),ret);
        } else {
            return 1;
        }
    }

    /**
     * c: X509_LOOKUP_load_cert_file
     */
    public int loadCertificateFile(String file, int type) throws Exception {
        if (file == null) {
            return 1;
        }
        int count = 0;
        int ret = 0;
        Reader reader = null;
        try {
            InputStream in = wrapJRubyNormalizedInputStream(file);
            X509AuxCertificate x = null;
            if (type == X509Utils.X509_FILETYPE_PEM) {
                reader = new BufferedReader(new InputStreamReader(in));
                for (;;) {
                    x = PEMInputOutput.readX509Aux(reader, null);
                    if (null == x) {
                        break;
                    }
                    int i = store.addCertificate(x);
                    if (i == 0) {
                        return ret;
                    }
                    count++;
                    x = null;
                }
                ret = count;
            } else if (type == X509Utils.X509_FILETYPE_ASN1) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                x = StoreContext.ensureAux((X509Certificate) cf.generateCertificate(in));
                if (x == null) {
                    X509Error.addError(13);
                    return ret;
                }
                int i = store.addCertificate(x);
                if (i == 0) {
                    return ret;
                }
                ret = i;
            } else {
                X509Error.addError(X509Utils.X509_R_BAD_X509_FILETYPE);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return ret;
    }

    /**
     * c: X509_LOOKUP_load_crl_file
     */
    public int loadCRLFile(String file, int type) throws Exception {
        if (file == null) {
            return 1;
        }
        int count = 0;
        int ret = 0;
        Reader reader = null;
        try {
            InputStream in = wrapJRubyNormalizedInputStream(file);
            CRL x = null;
            if (type == X509Utils.X509_FILETYPE_PEM) {
                reader = new BufferedReader(new InputStreamReader(in));
                for (;;) {
                    x = PEMInputOutput.readX509CRL(reader, null);
                    if (null == x) {
                        break;
                    }
                    int i = store.addCRL(x);
                    if (i == 0) {
                        return ret;
                    }
                    count++;
                    x = null;
                }
                ret = count;
            } else if (type == X509Utils.X509_FILETYPE_ASN1) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                x = cf.generateCRL(in);
                if (x == null) {
                    X509Error.addError(13);
                    return ret;
                }
                int i = store.addCRL(x);
                if (i == 0) {
                    return ret;
                }
                ret = i;
            } else {
                X509Error.addError(X509Utils.X509_R_BAD_X509_FILETYPE);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return ret;
    }

    /**
     * c: X509_LOOKUP_load_cert_crl_file
     */
    public int loadCertificateOrCRLFile(String file, int type) throws Exception {
        if (type != X509Utils.X509_FILETYPE_PEM) {
            return loadCertificateFile(file, type);
        }
        int count = 0;
        Reader reader = null;
        try {
            InputStream in = wrapJRubyNormalizedInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            for (;;) {
                Object v = PEMInputOutput.readPEM(reader, null);
                if (null == v) {
                    break;
                }
                if (v instanceof X509Certificate) {
                    store.addCertificate(StoreContext.ensureAux((X509Certificate) v));
                    count++;
                } else if (v instanceof CRL) {
                    store.addCRL((CRL) v);
                    count++;
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
        return count; 
    }

    private InputStream wrapJRubyNormalizedInputStream(String file) throws IOException {
        Ruby runtime = Ruby.getGlobalRuntime();
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.open(runtime.getCurrentDirectory(), file, new ModeFlags(ModeFlags.RDONLY));
            return ChannelStream.open(runtime, descriptor).newInputStream();
        } catch (NoSuchMethodError nsme) {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (FileExistsException fee) {
            // should not happen because ModeFlag does not contain CREAT.
            fee.printStackTrace(System.err);
            throw new IllegalStateException(fee.getMessage(), fee);
        } catch (InvalidValueException ive) {
            // should not happen because ModeFlasg does not contain APPEND.
            ive.printStackTrace(System.err);
            throw new IllegalStateException(ive.getMessage(), ive);
        }
    }

    /**
     * c: X509_LOOKUP_free
     */
    public void free() throws Exception {
        if(method != null && method.free != null && method.free != Function1.EMPTY) {
            method.free.call(this);
        }
    }

    /**
     * c: X509_LOOKUP_init
     */
    public int init() throws Exception { 
        if(method == null) {
            return 0;
        }
        if(method.init != null && method.init != Function1.EMPTY) {
            return method.init.call(this);
        }
        return 1;
    }

    /**
     * c: X509_LOOKUP_by_subject
     */
    public int bySubject(int type, Name name,X509Object[] ret) throws Exception { 
        if(method == null || method.getBySubject == null || method.getBySubject == Function4.EMPTY) {
            return X509Utils.X509_LU_FAIL;
        }
        if(skip) {
            return 0;
        }
        return method.getBySubject.call(this,new Integer(type),name,ret);
    }

    /**
     * c: X509_LOOKUP_by_issuer_serial
     */
    public int byIssuerSerialNumber(int type, Name name,BigInteger serial, X509Object[] ret) throws Exception { 
        if(method == null || method.getByIssuerSerialNumber == null || method.getByIssuerSerialNumber == Function5.EMPTY) {
            return X509Utils.X509_LU_FAIL;
        }
        return method.getByIssuerSerialNumber.call(this,new Integer(type),name,serial,ret);
    } 

    /**
     * c: X509_LOOKUP_by_fingerprint
     */
    public int byFingerprint(int type,String bytes, X509Object[] ret) throws Exception { 
        if(method == null || method.getByFingerprint == null || method.getByFingerprint == Function4.EMPTY) {
            return X509Utils.X509_LU_FAIL;
        }
        return method.getByFingerprint.call(this,new Integer(type),bytes,ret);
    } 

    /**
     * c: X509_LOOKUP_by_alias
     */
    public int byAlias(int type, String str, X509Object[] ret) throws Exception { 
        if(method == null || method.getByAlias == null || method.getByAlias == Function4.EMPTY) {
            return X509Utils.X509_LU_FAIL;
        }
        return method.getByAlias.call(this,new Integer(type),str,ret);
    } 

    /**
     * c: X509_LOOKUP_shutdown
     */
    public int shutdown() throws Exception { 
        if(method == null) {
            return 0;
        }
        if(method.shutdown != null && method.shutdown != Function1.EMPTY) {
            return method.shutdown.call(this);
        }
        return 1;
    }

    /**
     * c: x509_file_lookup
     */
    private final static LookupMethod x509FileLookup = new LookupMethod();

    /**
     * c: x509_dir_lookup
     */
    private final static LookupMethod x509DirectoryLookup = new LookupMethod();

    static {
        x509FileLookup.name = "Load file into cache";
        x509FileLookup.control = new ByFile();

        x509DirectoryLookup.name = "Load certs from files in a directory";
        x509DirectoryLookup.newItem = new NewLookupDir();
        x509DirectoryLookup.free = new FreeLookupDir();
        x509DirectoryLookup.control = new LookupDirControl();
        x509DirectoryLookup.getBySubject = new GetCertificateBySubject();
    }
    
    /**
     * c: by_file_ctrl
     */
    private static class ByFile implements LookupMethod.ControlFunction {
        public int call(Object _ctx, Object _cmd, Object _argp, Object _argl, Object _ret) throws Exception {
            Lookup ctx = (Lookup)_ctx;
            int cmd = ((Integer)_cmd).intValue();
            String argp = (String)_argp;
            long argl = ((Long)_argl).longValue();

            int ok = 0;
            String file = null;
            
            switch(cmd) {
            case X509Utils.X509_L_FILE_LOAD:
                if (argl == X509Utils.X509_FILETYPE_DEFAULT) {
                    try {
                        file = System.getenv(X509Utils.getDefaultCertificateFileEnvironment());
                    } catch (Error error) {
                    }
                    if (file != null) {
                        ok = ctx.loadCertificateOrCRLFile(file, X509Utils.X509_FILETYPE_PEM) != 0 ? 1 : 0;
                    } else {
                        ok = (ctx.loadCertificateOrCRLFile(X509Utils.getDefaultCertificateFile(), X509Utils.X509_FILETYPE_PEM) != 0) ? 1 : 0;
                    }
                    if (ok == 0) {
                        X509Error.addError(X509Utils.X509_R_LOADING_DEFAULTS);
                    }
                } else {
                    if (argl == X509Utils.X509_FILETYPE_PEM) {
                        ok = (ctx.loadCertificateOrCRLFile(argp, X509Utils.X509_FILETYPE_PEM) != 0) ? 1 : 0;
                    } else {
                        ok = (ctx.loadCertificateFile(argp, (int) argl) != 0) ? 1 : 0;
                    }
                }
                break;
            }

            return ok;
        }
    }

    /**
     * c: BY_DIR, lookup_dir_st
     */
    private static class LookupDir {
        List<String> dirs;
        List<Integer> dirsType;
    }

    /**
     * c: new_dir
     */
    private static class NewLookupDir implements LookupMethod.NewItemFunction {
        public int call(Object _lu) {
            Lookup lu = (Lookup)_lu;
            LookupDir a = new LookupDir();
            a.dirs = new ArrayList<String>();
            a.dirsType = new ArrayList<Integer>();
            lu.methodData = a;
            return 1;
        }
    }

    /**
     * c: free_dir
     */
    private static class FreeLookupDir implements LookupMethod.FreeFunction {
        public int call(Object _lu) {
            Lookup lu = (Lookup)_lu;
            LookupDir a = (LookupDir)lu.methodData;
            a.dirs = null;
            a.dirsType = null;
            lu.methodData = null;
            return -1;
        }
    }

    /**
     * c: dir_ctrl
     */
    private static class LookupDirControl implements LookupMethod.ControlFunction {
        public int call(Object _ctx, Object _cmd, Object _argp, Object _argl, Object _retp) {
            Lookup ctx = (Lookup)_ctx;
            int cmd = ((Integer)_cmd).intValue();
            String argp = (String)_argp;
            long argl = ((Long)_argl).longValue();
            int ret = 0;
            LookupDir ld = (LookupDir)ctx.methodData;
            String dir = null;
            switch(cmd) {
            case X509Utils.X509_L_ADD_DIR:
                if(argl == X509Utils.X509_FILETYPE_DEFAULT) {
                    try {
                        dir = System.getenv(X509Utils.getDefaultCertificateDirectoryEnvironment());
                    } catch (Error error) {
                    }
                    if(null != dir) {
                        ret = addCertificateDirectory(ld,dir,X509Utils.X509_FILETYPE_PEM);
                    } else {
                        ret = addCertificateDirectory(ld,X509Utils.getDefaultCertificateDirectory(),X509Utils.X509_FILETYPE_PEM);
                    }
                    if(ret == 0) {
                        X509Error.addError(X509Utils.X509_R_LOADING_CERT_DIR);
                    }
                } else {
                    ret = addCertificateDirectory(ld,argp,(int)argl);
                }
                break;
            }
            return ret;
        }

        /**
         * c: add_cert_dir
         */
        private int addCertificateDirectory(LookupDir ctx,String dir,int type) {
            if(dir == null || "".equals(dir)) {
                X509Error.addError(X509Utils.X509_R_INVALID_DIRECTORY);
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
                ctx.dirsType.add(type);
                ctx.dirs.add(dirs[i]);
            }

            return 1;
        }
    }

    /**
     * c: get_cert_by_subject
     */
    private static class GetCertificateBySubject implements LookupMethod.BySubjectFunction {
        public int call(Object _xl, Object _type, Object _name, Object _ret) throws Exception {
            Lookup x1 = (Lookup)_xl;
            int type = ((Integer)_type).intValue();
            Name name = (Name)_name;
            X509Object[] ret = (X509Object[])_ret;

            int ok = 0;
            StringBuffer b = new StringBuffer();

            if(null == name) {
                return 0;
            }

            String postfix = "";
            if(type == X509Utils.X509_LU_X509) {
            } else if(type == X509Utils.X509_LU_CRL) {
                postfix = "r";
            } else {
                X509Error.addError(X509Utils.X509_R_WRONG_LOOKUP_TYPE);
                return ok;
            }
            
            LookupDir ctx = (LookupDir)x1.methodData;

            long h = name.hash();
            
            Iterator<Integer> iter = ctx.dirsType.iterator();
            for(String cdir : ctx.dirs) {
                int tp = iter.next();
                int k = 0;
                for(;;) {
                    b.append(String.format("%s%s%08x.%s%d", cdir, File.separator, h, postfix, k));
                    k++;
                    if(!(new File(b.toString()).exists())) {
                        break;
                    }
                    if(type == X509Utils.X509_LU_X509) {
                        if((x1.loadCertificateFile(b.toString(),tp)) == 0) {
                            break;
                        }
                    } else if(type == X509Utils.X509_LU_CRL) {
                        if((x1.loadCRLFile(b.toString(),tp)) == 0) {
                            break;
                        }
                    }
                }
                X509Object tmp = null;
                synchronized(X509Utils.CRYPTO_LOCK_X509_STORE) {
                    for(X509Object o : x1.store.objs) {
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
