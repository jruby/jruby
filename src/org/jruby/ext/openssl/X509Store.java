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
package org.jruby.ext.openssl;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.Function2;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.X509_STORE;
import org.jruby.ext.openssl.x509store.X509_STORE_CTX;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Store extends RubyObject {
    public static void createX509Store(IRuby runtime, RubyModule mX509) {
        RubyClass cX509Store = mX509.defineClassUnder("Store",runtime.getObject());
        mX509.defineClassUnder("StoreError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));
        cX509Store.attr_accessor(new IRubyObject[]{runtime.newSymbol("verify_callback"),runtime.newSymbol("error"),
                                                   runtime.newSymbol("error_string"),runtime.newSymbol("chain")});

        CallbackFactory storecb = runtime.callbackFactory(X509Store.class);

        cX509Store.defineSingletonMethod("new",storecb.getOptSingletonMethod("newInstance"));
        cX509Store.defineMethod("initialize",storecb.getOptMethod("_initialize"));
        cX509Store.defineMethod("verify_callback=",storecb.getMethod("set_verify_callback",IRubyObject.class));
        cX509Store.defineMethod("flags=",storecb.getMethod("set_flags",IRubyObject.class));
        cX509Store.defineMethod("purpose=",storecb.getMethod("set_purpose",IRubyObject.class));
        cX509Store.defineMethod("trust=",storecb.getMethod("set_trust",IRubyObject.class));
        cX509Store.defineMethod("time=",storecb.getMethod("set_time",IRubyObject.class));
        cX509Store.defineMethod("add_path",storecb.getMethod("add_path",IRubyObject.class));
        cX509Store.defineMethod("add_file",storecb.getMethod("add_file",IRubyObject.class));
        cX509Store.defineMethod("set_default_paths",storecb.getMethod("set_default_paths"));
        cX509Store.defineMethod("add_cert",storecb.getMethod("add_cert",IRubyObject.class));
        cX509Store.defineMethod("add_crl",storecb.getMethod("add_crl",IRubyObject.class));
        cX509Store.defineMethod("verify",storecb.getOptMethod("verify"));
        
        X509StoreCtx.createX509StoreCtx(runtime, mX509);
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        IRubyObject result = new X509Store(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    private RubyClass cStoreError;
    private RubyClass cStoreContext;

    public X509Store(IRuby runtime, RubyClass type) {
        super(runtime,type);
        store = new X509_STORE();
        cStoreError = (RubyClass)(((RubyModule)(runtime.getModule("OpenSSL").getConstant("X509"))).getConstant("StoreError"));
        cStoreContext = (RubyClass)(((RubyModule)(runtime.getModule("OpenSSL").getConstant("X509"))).getConstant("StoreContext"));
    }

    private X509_STORE store;

    X509_STORE getStore() {
        return store;
    }

    private void raise(String msg) {
        throw new RaiseException(getRuntime(),cStoreError, msg, true);
    }

    public IRubyObject _initialize(IRubyObject[] args) throws Exception {
        store.set_verify_cb_func(ossl_verify_cb);
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

    public IRubyObject set_verify_callback(IRubyObject cb) {
        store.set_ex_data(1, cb);
        this.setInstanceVariable("@verify_callback", cb);
        return cb;
    }

    public IRubyObject set_flags(IRubyObject arg) {
        store.set_flags(RubyNumeric.fix2long(arg));
        return arg;
    }

    public IRubyObject set_purpose(IRubyObject arg) throws Exception {
        store.set_purpose(RubyNumeric.fix2int(arg));
        return arg;
    }

    public IRubyObject set_trust(IRubyObject arg) {
        store.set_trust(RubyNumeric.fix2int(arg));
        return arg;
    }

    public IRubyObject set_time(IRubyObject arg) {
        setInstanceVariable("@time",arg);
        return arg;
    }

    public IRubyObject add_path(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: Store#add_path");
        return getRuntime().getNil();
    }

    public IRubyObject add_file(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: Store#add_file");
        return getRuntime().getNil();
    }

    public IRubyObject set_default_paths() {
        System.err.println("WARNING: unimplemented method called: Store#set_default_paths");
        return getRuntime().getNil();
    }

    public IRubyObject add_cert(IRubyObject _cert) {
        X509AuxCertificate cert = (_cert instanceof X509Cert) ? ((X509Cert)_cert).getAuxCert() : (X509AuxCertificate)null;
        if(store.add_cert(cert) != 1) {
            raise(null);
        }
        return this;
    }

    public IRubyObject add_crl(IRubyObject arg) {
        java.security.cert.X509CRL crl = (arg instanceof X509CRL) ? ((X509CRL)arg).getCRL() : null;
        if(store.add_crl(crl) != 1) {
            raise(null);
        }
        return this;
    }

    public IRubyObject verify(IRubyObject[] args) throws Exception {
        IRubyObject cert, chain;
        if(checkArgumentCount(args,1,2) == 2) {
            chain = args[1];
        } else {
            chain = getRuntime().getNil();
        }
        cert = args[0];
        IRubyObject proc, result;
        X509StoreCtx ctx = (X509StoreCtx)cStoreContext.callMethod(getRuntime().getCurrentContext(),"new",new IRubyObject[]{this,cert,chain});
        if(getRuntime().getCurrentContext().isBlockGiven()) {
            proc = RubyProc.newProc(getRuntime(),false);
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

    private final static Function2 ossl_verify_cb = new Function2() {
            public int call(Object a1, Object a2) throws Exception {
                X509_STORE_CTX ctx = (X509_STORE_CTX)a2;
                int ok = ((Integer)a1).intValue();
                IRubyObject proc = (IRubyObject)ctx.get_ex_data(1);
                if(null == proc) {
                    proc = (IRubyObject)ctx.ctx.get_ex_data(0);
                }
                if(null == proc) {
                    return ok;
                }
                if(!proc.isNil()) {
                    System.err.println("WARNING: unimplemented method called: ossl_verify_cb");
                    System.err.println("GOJS");
                }

                /*
    if (!NIL_P(proc)) {
	rctx = rb_protect((VALUE(*)(VALUE))ossl_x509stctx_new,
			  (VALUE)ctx, &state);
	ret = Qfalse;
	if (!state) {
	    args.proc = proc;
	    args.preverify_ok = ok ? Qtrue : Qfalse;
	    args.store_ctx = rctx;
	    ret = rb_ensure(ossl_call_verify_cb_proc, (VALUE)&args,
			    ossl_x509stctx_clear_ptr, rctx);
	}
	if (ret == Qtrue) {
	    X509_STORE_CTX_set_error(ctx, X509_V_OK);
	    ok = 1;
	}
	else{
	    if (X509_STORE_CTX_get_error(ctx) == X509_V_OK) {
		X509_STORE_CTX_set_error(ctx, X509_V_ERR_CERT_REJECTED);
	    }
	    ok = 0;
	}
    }
                */
                return ok;
            }
        };
}// X509Store
