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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.X509_STORE;
import org.jruby.ext.openssl.x509store.X509_STORE_CTX;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509StoreCtx extends RubyObject {
    public static void createX509StoreCtx(IRuby runtime, RubyModule mX509) {
        RubyClass cX509StoreContext = mX509.defineClassUnder("StoreContext",runtime.getObject());
        CallbackFactory storectxcb = runtime.callbackFactory(X509StoreCtx.class);
        cX509StoreContext.defineSingletonMethod("new",storectxcb.getOptSingletonMethod("newInstance"));
        cX509StoreContext.defineMethod("initialize",storectxcb.getOptMethod("_initialize"));
        cX509StoreContext.defineMethod("verify",storectxcb.getMethod("verify"));
        cX509StoreContext.defineMethod("chain",storectxcb.getMethod("chain"));
        cX509StoreContext.defineMethod("error",storectxcb.getMethod("error"));
        cX509StoreContext.defineMethod("error=",storectxcb.getMethod("set_error",IRubyObject.class));
        cX509StoreContext.defineMethod("error_string",storectxcb.getMethod("error_string"));
        cX509StoreContext.defineMethod("error_depth",storectxcb.getMethod("error_depth"));
        cX509StoreContext.defineMethod("current_cert",storectxcb.getMethod("current_cert"));
        cX509StoreContext.defineMethod("current_crl",storectxcb.getMethod("current_crl"));
        cX509StoreContext.defineMethod("cleanup",storectxcb.getMethod("cleanup"));
        cX509StoreContext.defineMethod("flags=",storectxcb.getMethod("set_flags",IRubyObject.class));
        cX509StoreContext.defineMethod("purpose=",storectxcb.getMethod("set_purpose",IRubyObject.class));
        cX509StoreContext.defineMethod("trust=",storectxcb.getMethod("set_trust",IRubyObject.class));
        cX509StoreContext.defineMethod("time=",storectxcb.getMethod("set_time",IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        IRubyObject result = new X509StoreCtx(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    private X509_STORE_CTX ctx;
    private RubyClass cStoreError;
    private RubyClass cX509Cert;

    public X509StoreCtx(IRuby runtime, RubyClass type) {
        super(runtime,type);
        ctx = new X509_STORE_CTX();
        cStoreError = (RubyClass)(((RubyModule)(runtime.getModule("OpenSSL").getConstant("X509"))).getConstant("StoreError")); 
        cX509Cert = (RubyClass)(((RubyModule)(runtime.getModule("OpenSSL").getConstant("X509"))).getConstant("Certificate"));
   }

    private void raise(String msg) {
        throw new RaiseException(getRuntime(),cStoreError, msg, true);
    }

    public IRubyObject _initialize(IRubyObject[] args) throws Exception {
        IRubyObject store;
        IRubyObject cert = getRuntime().getNil();
        IRubyObject chain = getRuntime().getNil();
        X509_STORE x509st;
        X509AuxCertificate x509 = null;
        List x509s = new ArrayList();

        if(checkArgumentCount(args,1,3) > 1) {
            cert = args[1];
        }
        if(args.length > 2) {
            chain = args[2];
        }
        store = args[0];
        x509st = ((X509Store)store).getStore();
        if(!cert.isNil()) {
            x509 = ((X509Cert)cert).getAuxCert();
        }
        if(!chain.isNil()) {
            x509s = new ArrayList();
            for(Iterator iter = ((RubyArray)chain).getList().iterator();iter.hasNext();) {
                x509s.add(((X509Cert)iter.next()).getAuxCert());
            }
        }
        if(ctx.init(x509st,x509,x509s) != 1) {
            raise(null);
        }
        IRubyObject t = store.getInstanceVariable("@time");
        if(!t.isNil()) {
            set_time(t);
        }
        setInstanceVariable("@verify_callback",store.getInstanceVariable("@verify_callback"));
        setInstanceVariable("@cert",cert);
        return this;
    }

    public IRubyObject verify() throws Exception {
        ctx.set_ex_data(1,getInstanceVariable("@verify_callback"));
        int result = ctx.verify_cert();
        return result != 0 ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject chain() throws Exception {
        List chain = ctx.get_chain();
        if(chain == null) {
            return getRuntime().getNil();
        }
        List ary = new ArrayList();
        for(Iterator iter = chain.iterator();iter.hasNext();) {
            X509AuxCertificate x509 = (X509AuxCertificate)iter.next();
            ary.add(cX509Cert.callMethod(getRuntime().getCurrentContext(),"new",getRuntime().newString(new String(x509.getEncoded(),"ISO8859_1"))));
        }
        return getRuntime().newArray(ary);
   }

    public IRubyObject error() {
        return getRuntime().newFixnum(ctx.get_error());
    }

    public IRubyObject set_error(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_error");
        return getRuntime().getNil();
    }

    public IRubyObject error_string() {
        int err = ctx.get_error();
        return getRuntime().newString(org.jruby.ext.openssl.x509store.X509.verify_cert_error_string(err));
    }

    public IRubyObject error_depth() {
        System.err.println("WARNING: unimplemented method called: StoreContext#error_depth");
        return getRuntime().getNil();
    }

    public IRubyObject current_cert() {
        System.err.println("WARNING: unimplemented method called: StoreContext#current_cert");
        return getRuntime().getNil();
    }

    public IRubyObject current_crl() {
        System.err.println("WARNING: unimplemented method called: StoreContext#current_crl");
        return getRuntime().getNil();
    }

    public IRubyObject cleanup() {
        System.err.println("WARNING: unimplemented method called: StoreContext#cleanup");
        return getRuntime().getNil();
    }

    public IRubyObject set_flags(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_flags");
        return getRuntime().getNil();
    }

    public IRubyObject set_purpose(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_purpose");
        return getRuntime().getNil();
    }

    public IRubyObject set_trust(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_trust");
        return getRuntime().getNil();
    }

    public IRubyObject set_time(IRubyObject arg) {
        ctx.set_time(0,((RubyTime)arg).getJavaDate());
        return arg;
    }
}// X509StoreCtx
