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

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateFactory;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.ext.openssl.x509store.X509Error;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class OpenSSLReal {
    private static java.security.Provider BC_PROVIDER = null;

    static {
        try {
            BC_PROVIDER = (java.security.Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
        } catch (Throwable ignored) {
            // no bouncy castle available
        }
    }

    public interface Runnable {
        public void run() throws GeneralSecurityException;
    }

    public interface Callable {
        public Object call() throws GeneralSecurityException;
    }

    public static void doWithBCProvider(final Runnable toRun) throws GeneralSecurityException {
        getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                toRun.run();
                return null;
            }
        });
    }

    // This method just adds BouncyCastleProvider if it's allowed.  Removing
    // "BC" can remove pre-installed or runtime-added BC provider by elsewhere
    // and it causes unknown runtime error anywhere.  We avoid this. To use
    // part of jruby-openssl feature (X.509 and PKCS), users must be aware of
    // dynamic BC provider adding.
    public static Object getWithBCProvider(Callable toCall) throws GeneralSecurityException {
        try {
            if (BC_PROVIDER != null && java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(BC_PROVIDER);
            }
            return toCall.call();
        } catch (NoSuchProviderException nspe) {
            throw new GeneralSecurityException(bcExceptionMessage(nspe), nspe);
        } catch (Exception e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }

    public static String bcExceptionMessage(NoSuchProviderException nspe) {
        return "You need to configure JVM/classpath to enable BouncyCastle Security Provider: " + nspe.getMessage();
    }

    public static String bcExceptionMessage(NoClassDefFoundError ncdfe) {
        return "You need to configure JVM/classpath to enable BouncyCastle Security Provider: NoClassDefFoundError: " + ncdfe.getMessage();
    }

    public static void createOpenSSL(Ruby runtime) {
        RubyModule ossl = runtime.getOrCreateModule("OpenSSL");
        RubyClass standardError = runtime.getClass("StandardError");
        ossl.defineClassUnder("OpenSSLError", standardError, standardError.getAllocator());
        ossl.defineAnnotatedMethods(OpenSSLModule.class);

        // those are BC provider free (uses BC class but does not use BC provider)
        PKey.createPKey(runtime, ossl);
        BN.createBN(runtime, ossl);
        Digest.createDigest(runtime, ossl);
        Cipher.createCipher(runtime, ossl);
        Random.createRandom(runtime, ossl);
        HMAC.createHMAC(runtime, ossl);
        Config.createConfig(runtime, ossl);
        ASN1.createASN1(runtime, ossl);
        X509.createX509(runtime, ossl);
        NetscapeSPKI.createNetscapeSPKI(runtime, ossl);
        PKCS7.createPKCS7(runtime, ossl);
        SSL.createSSL(runtime, ossl);

        runtime.getLoadService().require("jopenssl/version");
        String jopensslVersion = runtime.getClassFromPath("Jopenssl::Version").getConstant("VERSION").toString();
        ossl.setConstant("VERSION", runtime.newString("1.0.0"));
        ossl.setConstant("OPENSSL_VERSION",
                runtime.newString("jruby-ossl " + jopensslVersion));
        ossl.setConstant("OPENSSL_VERSION_NUMBER", runtime.newFixnum(9469999));
        OpenSSLModule.setDebug(ossl,  runtime.getFalse());
    }

    @JRubyModule(name = "OpenSSL")
    public static class OpenSSLModule {

        @JRubyMethod(name = "errors", meta = true)
        public static IRubyObject errors(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            RubyArray result = runtime.newArray();
            for (Map.Entry<Integer, String> e : X509Error.getErrors().entrySet()) {
                result.add(runtime.newString(e.getValue()));
            }
            return result;
        }

        @JRubyMethod(name = "debug", meta = true)
        public static IRubyObject getDebug(IRubyObject recv) {
            return (IRubyObject)((RubyModule) recv).getInternalVariable("debug");
        }

        @JRubyMethod(name = "debug=", meta = true)
        public static IRubyObject setDebug(IRubyObject recv, IRubyObject debug) {
            ((RubyModule) recv).setInternalVariable("debug", debug);
            return debug;
        }

        // Added in 2.0; not masked because it does nothing anyway
        @JRubyMethod(meta = true)
        public static IRubyObject fips_mode(ThreadContext context, IRubyObject self) {
            return context.runtime.getFalse();
        }

        // Added in 2.0; not masked because it does nothing anyway
        @JRubyMethod(name = "fips_mode=", meta = true)
        public static IRubyObject fips_mode_set(ThreadContext context, IRubyObject self, IRubyObject value) {
            if (value.isTrue()) {
                context.runtime.getWarnings().warn("FIPS mode not supported on JRuby OpenSSL");
            }

            return self;
        }
    }

    public static javax.crypto.Cipher getCipherBC(final String algorithm) throws GeneralSecurityException {
        return (javax.crypto.Cipher) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return javax.crypto.Cipher.getInstance(algorithm, "BC");
            }
        });
    }

    public static SecretKeyFactory getSecretKeyFactoryBC(final String algorithm) throws GeneralSecurityException {
        return (SecretKeyFactory) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return SecretKeyFactory.getInstance(algorithm, "BC");
            }
        });
    }

    public static MessageDigest getMessageDigestBC(final String algorithm) throws GeneralSecurityException {
        return (MessageDigest) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return MessageDigest.getInstance(algorithm, "BC");
            }
        });
    }

    public static CertificateFactory getX509CertificateFactoryBC() throws GeneralSecurityException {
        return (CertificateFactory) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return CertificateFactory.getInstance("X.509", "BC");
            }
        });
    }
}// OpenSSLReal

