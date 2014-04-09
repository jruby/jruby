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
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import org.jruby.ext.openssl.x509store.X509AuxCertificate;
import org.jruby.ext.openssl.x509store.Store;
import org.jruby.ext.openssl.x509store.StoreContext;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;
import static org.jruby.ext.openssl.OpenSSLReal.warn;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509StoreContext extends RubyObject {

    private static ObjectAllocator X509STORECTX_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new X509StoreContext(runtime, klass);
        }
    };

    public static void createX509StoreContext(Ruby runtime, RubyModule mX509) {
        RubyClass cX509StoreContext = mX509.defineClassUnder("StoreContext", runtime.getObject(), X509STORECTX_ALLOCATOR);
        cX509StoreContext.defineAnnotatedMethods(X509StoreContext.class);
    }

    private final StoreContext storeContext;

    public X509StoreContext(Ruby runtime, RubyClass type) {
        super(runtime, type);
        this.storeContext = new StoreContext();
    }

    // constructor for creating callback parameter object of verify_cb
    X509StoreContext(Ruby runtime, RubyClass type, StoreContext storeContext) {
        super(runtime, type);
        this.storeContext = storeContext;
    }

    @JRubyMethod(name="initialize", rest=true, visibility = Visibility.PRIVATE)
    public IRubyObject _initialize(final ThreadContext context, final IRubyObject[] args, final Block block) {
        IRubyObject store, cert, chain;
        cert = chain = context.runtime.getNil();

        store = args[0];

        if ( Arity.checkArgumentCount(context.runtime, args, 1, 3) > 1 ) {
            cert = args[1];
        }
        if ( args.length > 2) {
            chain = args[2];
        }

        final Store x509Store = ((X509Store) store).getStore();
        final X509AuxCertificate x509Cert = cert.isNil() ? null : ((X509Cert) cert).getAuxCert();
        final List<X509AuxCertificate> x509Certs = new ArrayList<X509AuxCertificate>();
        if ( ! chain.isNil() ) {
            for (IRubyObject obj : ((RubyArray) chain).toJavaArray()) {
                x509Certs.add( ((X509Cert) obj).getAuxCert() );
            }
        }

        if ( storeContext.init(x509Store, x509Cert, x509Certs) != 1 ) {
            throw newStoreError(context.runtime, null);
        }

        IRubyObject time = store.getInstanceVariables().getInstanceVariable("@time");
        if ( ! time.isNil() ) {
            set_time(time);
        }

        IRubyObject vc = store.getInstanceVariables().getInstanceVariable("@verify_callback");
        this.setInstanceVariable("@verify_callback", vc);
        this.setInstanceVariable("@cert", cert);
        return this;
    }

    @JRubyMethod
    public IRubyObject verify(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        storeContext.setExtraData(1, getInstanceVariable("@verify_callback"));
        try {
            int result = storeContext.verifyCertificate();
            return result != 0 ? runtime.getTrue() : runtime.getFalse();
        }
        catch (Exception e) {
            if ( isDebug(context.runtime) ) e.printStackTrace( context.runtime.getOut() );
            // TODO: define suitable exception for jopenssl and catch it.
            throw newStoreError(runtime, e.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject chain(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final List<X509AuxCertificate> chain = storeContext.getChain();
        if ( chain == null ) return runtime.getNil();

        final RubyArray result = runtime.newArray(chain.size());
        final RubyClass _Certificate = getX509Certificate(runtime);
        try {
            for (X509AuxCertificate x509 : chain) {
                RubyString encoded = RubyString.newString(runtime, x509.getEncoded());
                result.append( _Certificate.callMethod( context, "new", encoded ) );
            }
        }
        catch (CertificateEncodingException e) {
            throw newStoreError(runtime, e.getMessage());
        }
        return result;
    }

    @JRubyMethod
    public IRubyObject error(final ThreadContext context) {
        return context.runtime.newFixnum(storeContext.getError());
    }

    @JRubyMethod(name="error=")
    public IRubyObject set_error(final IRubyObject error) {
        storeContext.setError(RubyNumeric.fix2int(error));
        return error;
    }

    @JRubyMethod
    public IRubyObject error_string(final ThreadContext context) {
        final int err = storeContext.getError();
        return context.runtime.newString(org.jruby.ext.openssl.x509store.X509Utils.verifyCertificateErrorString(err));
    }

    @JRubyMethod
    public IRubyObject error_depth(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: StoreContext#error_depth");
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject current_cert(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        final X509AuxCertificate x509 = storeContext.getCurrentCertificate();
        try {
            final RubyClass _Certificate = getX509Certificate(runtime);
            return _Certificate.callMethod(context, "new", RubyString.newString(runtime, x509.getEncoded()));
        }
        catch (CertificateEncodingException e) {
            throw newStoreError(runtime, e.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject current_crl(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: StoreContext#current_crl");
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject cleanup(final ThreadContext context) {
        warn(context, "WARNING: unimplemented method called: StoreContext#cleanup");
        return context.runtime.getNil();
    }

    @JRubyMethod(name="flags=")
    public IRubyObject set_flags(final ThreadContext context, final IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: StoreContext#set_flags");
        return context.runtime.getNil();
    }

    @JRubyMethod(name="purpose=")
    public IRubyObject set_purpose(final ThreadContext context, final IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: StoreContext#set_purpose");
        return context.runtime.getNil();
    }

    @JRubyMethod(name="trust=")
    public IRubyObject set_trust(final ThreadContext context, final IRubyObject arg) {
        warn(context, "WARNING: unimplemented method called: StoreContext#set_trust");
        return context.runtime.getNil();
    }

    @JRubyMethod(name="time=")
    public IRubyObject set_time(IRubyObject arg) {
        storeContext.setTime( 0, ( (RubyTime) arg ).getJavaDate() );
        return arg;
    }

    private static RaiseException newStoreError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::X509::StoreError", message);
    }

    private static RubyClass getX509Certificate(final Ruby runtime) {
        return (RubyClass) runtime.getClassFromPath("OpenSSL::X509::Certificate");
    }

}// X509StoreContext
