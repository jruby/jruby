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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
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

import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509StoreCtx extends RubyObject {
    private static final long serialVersionUID = 2029690161026120504L;

    private static ObjectAllocator X509STORECTX_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509StoreCtx(runtime, klass);
        }
    };

    private static RubyObjectAdapter api = JavaEmbedUtils.newObjectAdapter();
    
    public static void createX509StoreCtx(Ruby runtime, RubyModule mX509) {
        RubyClass cX509StoreContext = mX509.defineClassUnder("StoreContext",runtime.getObject(),X509STORECTX_ALLOCATOR);
        cX509StoreContext.defineAnnotatedMethods(X509StoreCtx.class);
    }

    private StoreContext ctx;
    private RubyClass cX509Cert;

    public X509StoreCtx(Ruby runtime, RubyClass type) {
        super(runtime, type);
        ctx = new StoreContext();
        cX509Cert = Utils.getClassFromPath(runtime, "OpenSSL::X509::Certificate");
    }

    // constructor for creating callback parameter object of verify_cb
    X509StoreCtx(Ruby runtime, RubyClass type, StoreContext ctx) {
        super(runtime, type);
        this.ctx = ctx;
        cX509Cert = Utils.getClassFromPath(runtime, "OpenSSL::X509::Certificate");
    }

    @JRubyMethod(name="initialize", rest=true)
    public IRubyObject _initialize(IRubyObject[] args, Block block) {
        IRubyObject store;
        IRubyObject cert = getRuntime().getNil();
        IRubyObject chain = getRuntime().getNil();
        Store x509st;
        X509AuxCertificate x509 = null;
        List<X509AuxCertificate> x509s = new ArrayList<X509AuxCertificate>();

        if(org.jruby.runtime.Arity.checkArgumentCount(getRuntime(),args,1,3) > 1) {
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
            x509s = new ArrayList<X509AuxCertificate>();
            for (IRubyObject obj : ((RubyArray)chain).toJavaArray()) {
                x509s.add(((X509Cert)obj).getAuxCert());
            }
        }
        if(ctx.init(x509st,x509,x509s) != 1) {
            throw newStoreError(getRuntime(), null);
        }
        IRubyObject t = api.getInstanceVariable(store,"@time");
        if(!t.isNil()) {
            set_time(t);
        }
        api.setInstanceVariable(this, "@verify_callback", api.getInstanceVariable(store, "@verify_callback"));
        api.setInstanceVariable(this, "@cert", cert);
        return this;
    }

    @JRubyMethod
    public IRubyObject verify() {
        ctx.setExtraData(1, getInstanceVariable("@verify_callback"));
        try {
            int result = ctx.verifyCertificate();
            return result != 0 ? getRuntime().getTrue() : getRuntime().getFalse();
        } catch (Exception e) {
            // TODO: define suitable exception for jopenssl and catch it.
            throw newStoreError(getRuntime(), e.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject chain() {
        List<X509AuxCertificate> chain = ctx.getChain();
        if (chain == null) {
            return getRuntime().getNil();
        }
        List<IRubyObject> ary = new ArrayList<IRubyObject>();
        try {
            for (X509AuxCertificate x509 : chain) {
                ary.add(cX509Cert.callMethod(getRuntime().getCurrentContext(), "new", RubyString.newString(getRuntime(), x509.getEncoded())));
            }
        } catch (CertificateEncodingException cee) {
            throw newStoreError(getRuntime(), cee.getMessage());
        }
        return getRuntime().newArray(ary);
    }

    @JRubyMethod
    public IRubyObject error() {
        return getRuntime().newFixnum(ctx.getError());
    }

    @JRubyMethod(name="error=")
    public IRubyObject set_error(IRubyObject arg) {
        ctx.setError(RubyNumeric.fix2int(arg));
        return arg;
    }

    @JRubyMethod
    public IRubyObject error_string() {
        int err = ctx.getError();
        return getRuntime().newString(org.jruby.ext.openssl.x509store.X509Utils.verifyCertificateErrorString(err));
    }

    @JRubyMethod
    public IRubyObject error_depth() {
        System.err.println("WARNING: unimplemented method called: StoreContext#error_depth");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject current_cert() {
        Ruby rt = getRuntime();
        X509AuxCertificate x509 = ctx.getCurrentCertificate();
        try {
            return cX509Cert.callMethod(rt.getCurrentContext(), "new", RubyString.newString(rt, x509.getEncoded()));
        } catch (CertificateEncodingException cee) {
            throw newStoreError(getRuntime(), cee.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject current_crl() {
        System.err.println("WARNING: unimplemented method called: StoreContext#current_crl");
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject cleanup() {
        System.err.println("WARNING: unimplemented method called: StoreContext#cleanup");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="flags=")
    public IRubyObject set_flags(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_flags");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="purpose=")
    public IRubyObject set_purpose(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_purpose");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="trust=")
    public IRubyObject set_trust(IRubyObject arg) {
        System.err.println("WARNING: unimplemented method called: StoreContext#set_trust");
        return getRuntime().getNil();
    }

    @JRubyMethod(name="time=")
    public IRubyObject set_time(IRubyObject arg) {
        ctx.setTime(0,((RubyTime)arg).getJavaDate());
        return arg;
    }

    private static RaiseException newStoreError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::X509::StoreError", message);
    }
}// X509StoreCtx
