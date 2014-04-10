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
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;
import org.jruby.ext.openssl.x509store.X509Utils;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Store extends RubyObject {

    private static final long serialVersionUID = -2969708892287379665L;

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
        X509StoreContext.createX509StoreContext(runtime, mX509);
    }

    public X509Store(Ruby runtime, RubyClass type) {
        super(runtime,type);
        store = new Store();
    }

    private final Store store;

    final Store getStore() { return store; }

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context, final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;

        final IRubyObject nil = runtime.getNil();
        final IRubyObject zero = RubyFixnum.zero(runtime);

        store.setVerifyCallbackFunction(verifyCallback);

        this.set_verify_callback(nil);
        this.setInstanceVariable("@flags", zero);
        this.setInstanceVariable("@purpose", zero);
        this.setInstanceVariable("@trust", zero);

        this.setInstanceVariable("@error", nil);
        this.setInstanceVariable("@error_string", nil);
        this.setInstanceVariable("@chain", nil);
        this.setInstanceVariable("@time", nil);
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
    public IRubyObject set_time(final IRubyObject arg) {
        setInstanceVariable("@time", arg);
        return arg;
    }

    @JRubyMethod
    public IRubyObject add_path(final ThreadContext context, final IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: Store#add_path");
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject add_file(final ThreadContext context, final IRubyObject arg) {
        String file = arg.toString();
        try {
            store.loadLocations(file, null);
        }
        catch (Exception e) {
            if ( isDebug(context.runtime) ) e.printStackTrace( context.runtime.getOut() );
            raiseStoreError(context, "loading file failed: " + e.getMessage());
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject set_default_paths(final ThreadContext context) {
        try {
            store.setDefaultPaths();
        }
        catch (Exception e) {
            if ( isDebug(context.runtime) ) e.printStackTrace( context.runtime.getOut() );
            raiseStoreError(context, "setting default path failed: " + e.getMessage());
        }
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject add_cert(final ThreadContext context, final IRubyObject _cert) {
        X509AuxCertificate cert = (_cert instanceof X509Cert) ? ((X509Cert)_cert).getAuxCert() : (X509AuxCertificate)null;
        if ( store.addCertificate(cert) != 1 ) {
            raiseStoreError(context, null);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject add_crl(final ThreadContext context, final IRubyObject arg) {
        java.security.cert.X509CRL crl = (arg instanceof X509CRL) ? ((X509CRL) arg).getCRL() : null;
        if ( store.addCRL(crl) != 1 ) {
            raiseStoreError(context, null);
        }
        return this;
    }

    @JRubyMethod(rest=true)
    public IRubyObject verify(final ThreadContext context, final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final IRubyObject cert = args[0], chain;
        if ( Arity.checkArgumentCount(runtime, args, 1, 2) == 2 ) {
            chain = args[1];
        } else {
            chain = runtime.getNil();
        }

        final IRubyObject verify_callback;
        if (block.isGiven()) {
            verify_callback = runtime.newProc(Block.Type.PROC, block);
        } else {
            verify_callback = getInstanceVariable("@verify_callback");
        }

        final RubyClass _StoreContext = getX509StoreContext(runtime);
        final X509StoreContext storeContext = (X509StoreContext)
            _StoreContext.callMethod(context, "new", new IRubyObject[]{ this, cert, chain });

        storeContext.setInstanceVariable("@verify_callback", verify_callback);

        IRubyObject result = storeContext.callMethod(context, "verify");
        this.setInstanceVariable("@error", storeContext.error(context));
        this.setInstanceVariable("@error_string", storeContext.error_string(context));
        this.setInstanceVariable("@chain", storeContext.chain(context));
        return result;
    }

    private static Store.VerifyCallbackFunction verifyCallback = new Store.VerifyCallbackFunction() {

        public int call(final StoreContext context, Integer outcome) {
            int ok = outcome.intValue();
            IRubyObject proc = (IRubyObject) context.getExtraData(1);
            if (proc == null) {
                proc = (IRubyObject) context.getStore().getExtraData(0);
            }

            if ( proc == null ) return ok;

            if ( ! proc.isNil() ) {
                final Ruby runtime = proc.getRuntime();
                final RubyClass _StoreContext = getX509StoreContext(runtime);
                X509StoreContext rubyContext = new X509StoreContext(runtime, _StoreContext, context);
                IRubyObject ret = proc.callMethod(runtime.getCurrentContext(), "call",
                    new IRubyObject[]{ runtime.newBoolean(ok != 0), rubyContext }
                );
                if (ret.isTrue()) {
                    context.setError(X509Utils.V_OK);
                    ok = 1;
                }
                else {
                    if (context.getError() == X509Utils.V_OK) {
                        context.setError(X509Utils.V_ERR_CERT_REJECTED);
                    }
                    ok = 0;
                }
            }
            return ok;
        }
    };

    private static RubyClass getX509StoreError(final Ruby runtime) {
        return (RubyClass) runtime.getClassFromPath("OpenSSL::X509::StoreError");
    }

    private static RubyClass getX509StoreContext(final Ruby runtime) {
        return (RubyClass) runtime.getClassFromPath("OpenSSL::X509::StoreContext");
    }

    private static void raiseStoreError(final ThreadContext context, final String msg) {
        final RubyClass _StoreError = getX509StoreError(context.runtime);
        throw new RaiseException(context.runtime, _StoreError, msg, true);
    }

}// X509Store
