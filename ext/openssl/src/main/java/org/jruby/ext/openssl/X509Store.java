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
package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;
import org.jruby.ext.openssl.x509store.X509Utils;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Store extends RubyObject {
    private static final long serialVersionUID = 7987156710610206194L;

    private static ObjectAllocator X509STORE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509Store(runtime, klass);
        }
    };
    
    public static void createX509Store(Ruby runtime, RubyModule mX509) {
        RubyClass cX509Store = mX509.defineClassUnder("Store",runtime.getObject(),X509STORE_ALLOCATOR);
        RubyClass openSSLError = runtime.getModule("OpenSSL").getClass("OpenSSLError");
        mX509.defineClassUnder("StoreError",openSSLError,openSSLError.getAllocator());
        cX509Store.addReadWriteAttribute(runtime.getCurrentContext(), "verify_callback");
        cX509Store.addReadWriteAttribute(runtime.getCurrentContext(), "error");
        cX509Store.addReadWriteAttribute(runtime.getCurrentContext(), "error_string");
        cX509Store.addReadWriteAttribute(runtime.getCurrentContext(), "chain");
        cX509Store.defineAnnotatedMethods(X509Store.class);

        X509StoreCtx.createX509StoreCtx(runtime, mX509);
    }

    private RubyClass cStoreError;
    private RubyClass cStoreContext;

    public X509Store(Ruby runtime, RubyClass type) {
        super(runtime,type);
        store = new Store();
        cStoreError = Utils.getClassFromPath(runtime, "OpenSSL::X509::StoreError");
        cStoreContext = Utils.getClassFromPath(runtime, "OpenSSL::X509::StoreContext");
    }

    private Store store;

    Store getStore() {
        return store;
    }

    private void raise(String msg) {
        throw new RaiseException(getRuntime(),cStoreError, msg, true);
    }

    @JRubyMethod(name="initialize", rest=true)
    public IRubyObject _initialize(IRubyObject[] args, Block block) {
        store.setVerifyCallbackFunction(ossl_verify_cb);
        this.set_verify_callback(getRuntime().getNil());
        this.setInstanceVariable("@flags",RubyFixnum.zero(getRuntime()));
        this.setInstanceVariable("@purpose",RubyFixnum.zero(getRuntime()));
        this.setInstanceVariable("@trust",RubyFixnum.zero(getRuntime()));
        
        this.setInstanceVariable("@error",getRuntime().getNil());
        this.setInstanceVariable("@error_string",getRuntime().getNil());
        this.setInstanceVariable("@chain",getRuntime().getNil());
        this.setInstanceVariable("@time",getRuntime().getNil());
        return this;
    }

    @JRubyMethod(name="verify_callback=")
    public IRubyObject set_verify_callback(IRubyObject cb) {
        store.setExtraData(1, cb);
        this.setInstanceVariable("@verify_callback", cb);
        return cb;
    }

    @JRubyMethod(name="flags=")
    public IRubyObject set_flags(IRubyObject arg) {
        store.setFlags(RubyNumeric.fix2long(arg));
        return arg;
    }

    @JRubyMethod(name="purpose=")
    public IRubyObject set_purpose(IRubyObject arg) {
        store.setPurpose(RubyNumeric.fix2int(arg));
        return arg;
    }

    @JRubyMethod(name="trust=")
    public IRubyObject set_trust(IRubyObject arg) {
        store.setTrust(RubyNumeric.fix2int(arg));
        return arg;
    }

    @JRubyMethod(name="time=")
    public IRubyObject set_time(IRubyObject arg) {
        setInstanceVariable("@time",arg);
        return arg;
    }

    @JRubyMethod
    public IRubyObject add_path(IRubyObject arg) {
        getRuntime().getWarnings().warn("unimplemented method called: Store#add_path");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject add_file(IRubyObject arg) {
        String file = arg.toString();
        try {
            store.loadLocations(file, null);
        } catch (Exception e) {
            raise("loading file failed: " + e.getMessage());
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject set_default_paths() {
        try {
            store.setDefaultPaths();
        }
        catch(Exception e) {
            raise("setting default path failed: " + e.getMessage());
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject add_cert(IRubyObject _cert) {
        X509AuxCertificate cert = (_cert instanceof X509Cert) ? ((X509Cert)_cert).getAuxCert() : (X509AuxCertificate)null;
        if(store.addCertificate(cert) != 1) {
            raise(null);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject add_crl(IRubyObject arg) {
        java.security.cert.X509CRL crl = (arg instanceof X509CRL) ? ((X509CRL)arg).getCRL() : null;
        if(store.addCRL(crl) != 1) {
            raise(null);
        }
        return this;
    }

    @JRubyMethod(rest=true)
    public IRubyObject verify(IRubyObject[] args, Block block) {
        IRubyObject cert, chain;
        if(org.jruby.runtime.Arity.checkArgumentCount(getRuntime(),args,1,2) == 2) {
            chain = args[1];
        } else {
            chain = getRuntime().getNil();
        }
        cert = args[0];
        IRubyObject proc, result;
        X509StoreCtx ctx = (X509StoreCtx)cStoreContext.callMethod(getRuntime().getCurrentContext(),"new",new IRubyObject[]{this,cert,chain});
        if (block.isGiven()) {
            proc = getRuntime().newProc(Block.Type.PROC, block);
        } else {
            proc = getInstanceVariable("@verify_callback");
        }
        ctx.setInstanceVariable("@verify_callback",proc);
        result = ctx.callMethod(getRuntime().getCurrentContext(),"verify");
        this.setInstanceVariable("@error",ctx.error());
        this.setInstanceVariable("@error_string",ctx.error_string());
        this.setInstanceVariable("@chain",ctx.chain());
        return result;
    }

    public final static Store.VerifyCallbackFunction ossl_verify_cb = new Store.VerifyCallbackFunction() {

        public int call(Object a1, Object a2) throws Exception {
            StoreContext ctx = (StoreContext) a2;
            int ok = ((Integer) a1).intValue();
            IRubyObject proc = (IRubyObject) ctx.getExtraData(1);
            if (null == proc) {
                proc = (IRubyObject) ctx.ctx.getExtraData(0);
            }
            if (null == proc) {
                return ok;
            }
            if (!proc.isNil()) {
                Ruby rt = proc.getRuntime();
                RubyClass cStoreContext = Utils.getClassFromPath(rt, "OpenSSL::X509::StoreContext");
                X509StoreCtx rctx = new X509StoreCtx(rt, cStoreContext, ctx);
                RubyBoolean rok = rt.newBoolean(ok != 0);
                IRubyObject ret = proc.callMethod(rt.getCurrentContext(), "call", new IRubyObject[]{rok, rctx});
                if (ret.isTrue()) {
                    ctx.setError(X509Utils.V_OK);
                    ok = 1;
                } else {
                    if (ctx.getError() == X509Utils.V_OK) {
                        ctx.setError(X509Utils.V_ERR_CERT_REJECTED);
                    }
                    ok = 0;
                }
            }
            return ok;
        }
    };
}// X509Store
