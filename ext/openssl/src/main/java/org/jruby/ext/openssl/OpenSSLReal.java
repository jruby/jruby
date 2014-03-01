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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyFactorySpi;
import java.security.KeyPairGenerator;
import java.security.KeyPairGeneratorSpi;
import java.security.MessageDigest;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureSpi;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateFactorySpi;
import javax.crypto.CipherSpi;
import javax.crypto.MacSpi;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactorySpi;

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

    static String BC_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    static volatile Provider BC_PROVIDER;

    synchronized static void setBouncyCastleProvider() {
        if ( BC_PROVIDER == null ) BC_PROVIDER = newBouncyCastleProvider();
    }

    static Provider newBouncyCastleProvider() {
        try {
            return (Provider) Class.forName(BC_PROVIDER_CLASS).newInstance();
        }
        catch (Throwable ignored) { /* no bouncy castle available */ }
        return null;
    }

    @Deprecated
    public static interface Runnable {
        public void run() throws GeneralSecurityException;
    }

    public static interface Callable<T> {
        public T call() throws GeneralSecurityException;
    }

    /**
     * Run a block of code with 'BC' provider installed.
     *
     * @deprecated No longer used within the JRuby-OpenSSL code-base, please avoid!
     *
     * @param block
     * @throws GeneralSecurityException
     */
    @Deprecated
    public static void doWithBCProvider(final Runnable block) throws GeneralSecurityException {
        getWithBCProvider(new Callable<Void>() {
            public Void call() throws GeneralSecurityException {
                block.run(); return null;
            }
        });
    }

    /**
     * Adds BouncyCastleProvider if it's allowed (no security exceptions thrown)
     * and runs the block of code. Once added the provider will stay registered
     * within <code>java.security.Security</code> API. This might lead to memory
     * leaks e.g. when the Ruby runtime that loaded BC is teared down.
     *
     * Removing the 'BC' provided (once the block run) can remove pre-installed
     * or another runtime-added BC provider thus causing unknown runtime errors.
     *
     * @deprecated No longer used within the JRuby-OpenSSL code-base, please avoid!
     *
     * @param <T>
     * @param block
     * @return
     * @throws GeneralSecurityException
     */
    @Deprecated
    public static <T> T getWithBCProvider(final Callable<T> block) throws GeneralSecurityException {
        try {
            if (BC_PROVIDER != null && java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(BC_PROVIDER);
            }
            return block.call();
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

    public static void createOpenSSL(final Ruby runtime) {
        setBouncyCastleProvider();

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
        ossl.setConstant("OPENSSL_VERSION", runtime.newString("jruby-ossl " + jopensslVersion));
        ossl.setConstant("OPENSSL_VERSION_NUMBER", runtime.newFixnum(9469999));

        OpenSSLModule.setDebug(ossl,  runtime.getFalse());
    }

    @JRubyModule(name = "OpenSSL")
    public static class OpenSSLModule {

        @JRubyMethod(name = "errors", meta = true)
        public static IRubyObject errors(IRubyObject self) {
            Ruby runtime = self.getRuntime();
            RubyArray result = runtime.newArray();
            for (Map.Entry<Integer, String> e : X509Error.getErrors().entrySet()) {
                result.add(runtime.newString(e.getValue()));
            }
            return result;
        }

        @JRubyMethod(name = "debug", meta = true)
        public static IRubyObject getDebug(IRubyObject self) {
            return (IRubyObject)((RubyModule) self).getInternalVariable("debug");
        }

        @JRubyMethod(name = "debug=", meta = true)
        public static IRubyObject setDebug(IRubyObject self, IRubyObject debug) {
            ((RubyModule) self).setInternalVariable("debug", debug);
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

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static javax.crypto.Cipher getCipher(final String transformation)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            if ( BC_PROVIDER != null ) return getCipherBC(transformation);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return javax.crypto.Cipher.getInstance(transformation);
    }

    private static final Class[] STRING_ARG = { String.class };

    static javax.crypto.Cipher getCipherBC(final String transformation)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        // these are BC JCE (@see javax.crypto.Cipher) inspired internals :
        CipherSpi spi = (CipherSpi) getBCImplEngine("Cipher", transformation);

        if ( spi == null ) {
            //
            // try the long way
            //
            StringTokenizer tok = new StringTokenizer(transformation, "/");
            String algorithm = tok.nextToken();

            spi = (CipherSpi) getBCImplEngine("Cipher", algorithm);

            if ( spi == null ) throw new NoSuchAlgorithmException(transformation + " not found");

            //
            // make sure we don't get fooled by a "//" in the string
            //
            if (tok.hasMoreTokens() && ! transformation.regionMatches(algorithm.length(), "//", 0, 2)) {
                // cipherSpi.engineSetMode(tok.nextToken());
                doInvoke(spi, CipherSpi.class, "engineSetMode", STRING_ARG, tok.nextToken());
            }

            if (tok.hasMoreTokens()) {
                // cipherSpi.engineSetPadding(tok.nextToken());
                doInvoke(spi, CipherSpi.class, "engineSetPadding", STRING_ARG, tok.nextToken());
            }
        }

        // new javax.crypto.Cipher(spi, BC_PROVIDER, transformation);
        return newInstance(javax.crypto.Cipher.class,
            new Class[] { CipherSpi.class, Provider.class, String.class },
            new Object[] { spi, BC_PROVIDER, transformation }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static javax.crypto.SecretKeyFactory getSecretKeyFactory(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            if ( BC_PROVIDER != null ) return getSecretKeyFactoryBC(algorithm);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return javax.crypto.SecretKeyFactory.getInstance(algorithm);
    }

    static javax.crypto.SecretKeyFactory getSecretKeyFactoryBC(final String algorithm)
        throws NoSuchAlgorithmException {
        // these are BC JCE (@see javax.crypto.SecretKey) inspired internals :
        SecretKeyFactorySpi spi = (SecretKeyFactorySpi) getBCImplEngine("SecretKeyFactory", algorithm);

        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        // return new SecretKeyFactory(spi, BC_PROVIDER, algorithm);
        return newInstance(javax.crypto.SecretKeyFactory.class,
            new Class[] { SecretKeyFactorySpi.class, Provider.class, String.class },
            new Object[] { spi, BC_PROVIDER, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static javax.crypto.Mac getMac(final String algorithm)
        throws NoSuchAlgorithmException {
        javax.crypto.Mac mac = null;
        if ( BC_PROVIDER != null ) mac = getMacBC(algorithm, true);
        if ( mac == null ) mac = javax.crypto.Mac.getInstance(algorithm);
        return mac;
    }

    static javax.crypto.Mac getMacBC(final String algorithm)
        throws NoSuchAlgorithmException {
        return getMacBC(algorithm, false);
    }

    private static javax.crypto.Mac getMacBC(final String algorithm, boolean silent)
        throws NoSuchAlgorithmException {
        // these are BC JCE (@see javax.crypto.Mac) inspired internals :
        MacSpi spi = (MacSpi) getBCImplEngine("Mac", algorithm);

        if ( spi == null ) {
            if ( silent ) return null;
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }

        // return new Mac(spi, BC_PROVIDER, algorithm);
        return newInstance(javax.crypto.Mac.class,
            new Class[] { MacSpi.class, Provider.class, String.class },
            new Object[] { spi, BC_PROVIDER, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyFactory getKeyFactory(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            if ( BC_PROVIDER != null ) return getKeyFactoryBC(algorithm);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return KeyFactory.getInstance(algorithm);
    }

    public static KeyFactory getKeyFactoryBC(final String algorithm)
        throws NoSuchAlgorithmException {

        final Class<?>[] paramTypes = { KeyFactorySpi.class, Provider.class, String.class };

        KeyFactorySpi spi = (KeyFactorySpi) getBCImplEngine("KeyFactory", algorithm);

        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        // return new KeyFactory(spi, provider, algorithm)
        return newInstance(KeyFactory.class, paramTypes,
            new Object[] { spi, BC_PROVIDER, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyPairGenerator getKeyPairGenerator(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            if ( BC_PROVIDER != null ) return getKeyPairGeneratorBC(algorithm);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return KeyPairGenerator.getInstance(algorithm);
    }

    static KeyPairGenerator getKeyPairGeneratorBC(final String algorithm)
        throws NoSuchAlgorithmException {

        Object spi = getBCImplEngine("KeyPairGenerator", algorithm);

        if ( spi == null ) {
            throw new NoSuchAlgorithmException(algorithm + " KeyPairGenerator not available");
        }

        final KeyPairGenerator keyPairGenerator;
        if ( spi instanceof KeyPairGenerator ) { // ! (spi instanceof KeyPairGeneratorSpi)
            keyPairGenerator = (KeyPairGenerator) spi;
        }
        else { // emulate what KeyPairGenerator.getInstance would do :
            final Class<? extends KeyPairGenerator> delegate;
            try {
                delegate = (Class<? extends KeyPairGenerator>)
                    Class.forName(KeyPairGenerator.class.getName() + "$Delegate");
            }
            catch (ClassNotFoundException e) {
                // it's in the JDK - not supposed to happen !
                throw new RuntimeException(e);
            }
            // Delegate(KeyPairGeneratorSpi spi, String algorithm)
            keyPairGenerator = newInstance(delegate,
                new Class[] { KeyPairGeneratorSpi.class, String.class }, spi, algorithm
            );
        }

        // keyPairGeneratorSpi.provider = BC_PROVIDER
        setField(keyPairGenerator, KeyPairGenerator.class, "provider", BC_PROVIDER);
        return keyPairGenerator;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static MessageDigest getMessageDigest(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            if ( BC_PROVIDER != null ) return getMessageDigestBC(algorithm);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return MessageDigest.getInstance(algorithm);
    }

    public static MessageDigest getMessageDigestBC(final String algorithm)
        throws NoSuchAlgorithmException {

        final Object spi = getBCImplEngine("MessageDigest", algorithm);

        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        final MessageDigest messageDigest;
        // likely the case with BC
        // e.g. org.bouncycastle.jcajce.provider.digest.MD5$Digest.class
        if ( spi instanceof MessageDigest ) {
            messageDigest = (MessageDigest) spi;
        }
        else { // still emulate what MessageDigest.getDigest would do :
            final Class<? extends MessageDigest> delegate;
            try {
                delegate = (Class<? extends MessageDigest>)
                    Class.forName(MessageDigest.class.getName() + "$Delegate");
            }
            catch (ClassNotFoundException e) {
                // it's in the JDK - not supposed to happen !
                throw new RuntimeException(e);
            }
            // public Delegate(MessageDigestSpi digestSpi, String algorithm)
            messageDigest = newInstance(delegate,
                new Class[] { MessageDigestSpi.class, String.class }, spi, algorithm
            );
        }

        // messageDigest.provider = BC_PROVIDER
        setField(messageDigest, MessageDigest.class, "provider", BC_PROVIDER);
        return messageDigest;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static Signature getSignature(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            if ( BC_PROVIDER != null ) return getSignatureBC(algorithm);
        } // still try java.security :
        catch (NoSuchAlgorithmException e) { }
        return Signature.getInstance(algorithm);
    }

    public static Signature getSignatureBC(final String algorithm)
        throws NoSuchAlgorithmException {

        final Object spi = getBCImplEngine("Signature", algorithm);

        if ( spi == null ) {
            throw new NoSuchAlgorithmException(algorithm + " Signature not available");
        }

        // logic similar to whar Signature.getInstance does :
        final Signature signature;
        if ( spi instanceof Signature ) {
            signature = (Signature) spi;
        }
        else {
            // wrap it up: new Signature.Delegate(spi, algorithm);
            final Class<? extends Signature> delegate;
            try {
                delegate = (Class<? extends Signature>)
                    Class.forName(Signature.class.getName() + "$Delegate");
            }
            catch (ClassNotFoundException e) {
                // it's in the JDK - not supposed to happen !
                throw new RuntimeException(e);
            }
            signature = newInstance(delegate,
                new Class[] { SignatureSpi.class, String.class }, spi, algorithm
            );
        }

        // signature.provider = BC_PROVIDER
        setField(signature, Signature.class, "provider", BC_PROVIDER);
        return signature;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static CertificateFactory getCertificateFactory(final String type)
        throws CertificateException {
        try {
            if ( BC_PROVIDER != null ) return getCertificateFactoryBC(type);
        } // still try java.security :
        catch (CertificateException e) { }
        return CertificateFactory.getInstance(type);
    }

    public static CertificateFactory getCertificateFactoryBC(final String type)
        throws CertificateException {

        final CertificateFactorySpi spi = (CertificateFactorySpi)
            getBCImplEngine("CertificateFactory", type);

        if ( spi == null ) throw new CertificateException(type + " not found");

        // return new CertificateFactory(spi, provider, type);
        return newInstance(CertificateFactory.class,
            new Class[] { CertificateFactorySpi.class, Provider.class, String.class },
            new Object[] { spi, BC_PROVIDER, type }
        );
    }

    public static CertificateFactory getX509CertificateFactoryBC() throws CertificateException {
        // BC's CertificateFactorySpi :
        // org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory.class
        return getCertificateFactoryBC("X.509");
    }

    // these are BC JCE (@see javax.crypto.JCEUtil) inspired internals :
    // https://github.com/bcgit/bc-java/blob/master/jce/src/main/java/javax/crypto/JCEUtil.java

    private static Object getBCImplEngine(String baseName, String algorithm) {
        //
        // try case insensitive
        //
        Object engine = findBCImplEngine(baseName, algorithm.toUpperCase(Locale.ENGLISH));
        if ( engine == null ) engine = findBCImplEngine(baseName, algorithm);
        return engine;
    }

    private static Object findBCImplEngine(String baseName, String algorithm) {
        final Provider bcProvider = BC_PROVIDER;

        String alias;
        while ((alias = bcProvider.getProperty("Alg.Alias." + baseName + "." + algorithm)) != null) {
            algorithm = alias;
        }

        final String className = bcProvider.getProperty(baseName + "." + algorithm);

        if (className != null) {
            try {
                Class klass;
                ClassLoader loader = bcProvider.getClass().getClassLoader();

                if (loader != null) {
                    klass = loader.loadClass(className);
                }
                else {
                    klass = Class.forName(className);
                }

                return klass.newInstance();
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + bcProvider.getName() + " but no class \"" + className + "\" found!");
            }
            catch (Exception e) {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + bcProvider.getName() + " but class \"" + className + "\" inaccessible!");
            }
        }

        return null;
    }

    private static <T> T newInstance(Class<T> klass, Class<?>[] paramTypes, Object... params) {
        final Constructor<T> constructor;
        try {
            constructor = klass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(params);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
        catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object doInvoke(Object obj, Class<?> methodOwner, String methodName, Class<?>[] paramTypes, Object... params) {
        final Method method;
        try {
            method = methodOwner.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(obj, params);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("no method '"+ methodName +"' declared in "+ obj.getClass() +"", e);
        }
        catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object obj, Class<?> fieldOwner, String fieldName, Object value) {
        final Field field;
        try {
            field = fieldOwner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException("no field '"+ fieldName +"' declared in "+ fieldOwner +"", e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}// OpenSSLReal

