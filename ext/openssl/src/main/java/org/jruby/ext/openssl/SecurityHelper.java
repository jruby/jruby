/*
 * The MIT License
 *
 * Copyright 2014 Karol Bucek.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jruby.ext.openssl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyFactorySpi;
import java.security.KeyPairGenerator;
import java.security.KeyPairGeneratorSpi;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.MessageDigestSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateFactorySpi;
import java.security.cert.X509CRL;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.Mac;
import javax.crypto.MacSpi;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKeyFactorySpi;
import javax.net.ssl.SSLContext;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.jce.provider.X509CRLObject;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;

/**
 * Java Security (and JCE) helpers.
 *
 * @author kares
 */
public abstract class SecurityHelper {

    private static String BC_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    static boolean setBouncyCastleProvider = true; // (package access for tests)
    static Provider securityProvider; // 'BC' provider (package access for tests)
    private static Boolean registerProvider = null;

    public static Provider getSecurityProvider() {
        if ( setBouncyCastleProvider && securityProvider == null ) {
            synchronized(SecurityHelper.class) {
                if ( setBouncyCastleProvider && securityProvider == null ) {
                    setBouncyCastleProvider(); setBouncyCastleProvider = false;
                }
            }
        }
        if ( registerProvider != null ) {
            synchronized(SecurityHelper.class) {
                if ( registerProvider != null && registerProvider.booleanValue() ) {
                    if ( securityProvider != null ) {
                        Security.addProvider(securityProvider);
                    }
                }
            }
            registerProvider = null;
        }
        return securityProvider;
    }

    public static synchronized void setSecurityProvider(final Provider provider) {
        securityProvider = provider;
    }

    static synchronized void setBouncyCastleProvider() {
        setSecurityProvider( newBouncyCastleProvider() );
    }

    private static Provider newBouncyCastleProvider() {
        try {
            return (Provider) Class.forName(BC_PROVIDER_CLASS).newInstance();
        }
        catch (Throwable ignored) { /* no bouncy castle available */ }
        return null;
    }

    public static synchronized void setRegisterProvider(boolean register) {
        registerProvider = Boolean.valueOf(register);
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static CertificateFactory getCertificateFactory(final String type)
        throws CertificateException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getCertificateFactory(type, provider);
        }
        catch (CertificateException e) { }
        return CertificateFactory.getInstance(type);
    }

    static CertificateFactory getCertificateFactory(final String type, final Provider provider)
        throws CertificateException {
        final CertificateFactorySpi spi = (CertificateFactorySpi) getImplEngine("CertificateFactory", type);
        if ( spi == null ) throw new CertificateException(type + " not found");
        return newInstance(CertificateFactory.class,
                new Class[]{ CertificateFactorySpi.class, Provider.class, String.class },
                new Object[]{ spi, provider, type }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyFactory getKeyFactory(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getKeyFactory(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return KeyFactory.getInstance(algorithm);
    }

    static KeyFactory getKeyFactory(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        KeyFactorySpi spi = (KeyFactorySpi) getImplEngine("KeyFactory", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");
        return newInstance(KeyFactory.class,
            new Class[] { KeyFactorySpi.class, Provider.class, String.class },
            new Object[] { spi, provider, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyPairGenerator getKeyPairGenerator(final String algorithm)
        throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getKeyPairGenerator(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return KeyPairGenerator.getInstance(algorithm);
    }

    static KeyPairGenerator getKeyPairGenerator(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final Object spi = getImplEngine("KeyPairGenerator", algorithm);
        if ( spi == null ) {
            throw new NoSuchAlgorithmException(algorithm + " KeyPairGenerator not available");
        }

        final KeyPairGenerator keyPairGenerator;
        if ( spi instanceof KeyPairGenerator ) {
            keyPairGenerator = (KeyPairGenerator) spi;
        }
        else {
            final Class<? extends KeyPairGenerator> delegate;
            try {
                delegate = (Class<? extends KeyPairGenerator>)
                    Class.forName(KeyPairGenerator.class.getName() + "$Delegate");
            } catch (ClassNotFoundException e) { throw new RuntimeException(e); }

            keyPairGenerator = newInstance(delegate,
                new Class[] { KeyPairGeneratorSpi.class, String.class }, spi, algorithm
            );
        }
        setField(keyPairGenerator, KeyPairGenerator.class, "provider", provider);
        return keyPairGenerator;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyStore getKeyStore(final String type)
        throws KeyStoreException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getKeyStore(type, provider);
        }
        catch (KeyStoreException e) { }
        return KeyStore.getInstance(type);
    }

    static KeyStore getKeyStore(final String type, final Provider provider)
        throws KeyStoreException {
        return KeyStore.getInstance(type, provider);
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static MessageDigest getMessageDigest(final String algorithm) throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getMessageDigest(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return MessageDigest.getInstance(algorithm);
    }

    static MessageDigest getMessageDigest(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final Object spi = getImplEngine("MessageDigest", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        final MessageDigest messageDigest;
        if ( spi instanceof MessageDigest ) {
            messageDigest = (MessageDigest) spi;
        }
        else {
            final Class<? extends MessageDigest> delegate;
            try {
                delegate = (Class<? extends MessageDigest>)
                    Class.forName(MessageDigest.class.getName() + "$Delegate");
            } catch (ClassNotFoundException e) { throw new RuntimeException(e); }

            messageDigest = newInstance(delegate,
                new Class[] { MessageDigestSpi.class, String.class }, spi, algorithm
            );
        }
        setField(messageDigest, MessageDigest.class, "provider", provider);
        return messageDigest;
    }

    public static SecureRandom getSecureRandom() {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) {
                final String algorithm = getSecureRandomAlgorithm(provider);
                if ( algorithm != null ) {
                    return getSecureRandom(algorithm, provider);
                }
            }
        }
        catch (NoSuchAlgorithmException e) { }
        return new SecureRandom(); // likely "SHA1PRNG" from SPI sun.security.provider.SecureRandom
    }

    private static SecureRandom getSecureRandom(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final SecureRandomSpi spi = (SecureRandomSpi) getImplEngine("SecureRandom", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        return newInstance(SecureRandom.class,
            new Class[] { SecureRandomSpi.class, Provider.class, String.class },
            new Object[] { spi, provider, algorithm }
        );
    }

    // NOTE: none (at least for BC 1.47)
    private static String getSecureRandomAlgorithm(final Provider provider) {
        for ( Provider.Service service : provider.getServices() ) {
            if ( "SecureRandom".equals( service.getType() ) ) {
                return service.getAlgorithm();
            }
        }
        return null;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static Cipher getCipher(final String transformation)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            final Provider provider = getSecurityProvider();
            if ( securityProvider != null ) return getCipher(transformation, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        catch (NoSuchPaddingException e) { }
        return Cipher.getInstance(transformation);
    }

    static Cipher getCipher(final String transformation, final Provider provider)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance(transformation, provider);
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static Signature getSignature(final String algorithm) throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getSignature(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return Signature.getInstance(algorithm);
    }

    static Signature getSignature(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final Object spi = getImplEngine("Signature", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " Signature not available");

        final Signature signature;
        if ( spi instanceof Signature ) {
            signature = (Signature) spi;
        } else {
            final Class<? extends Signature> delegate;
            try {
                delegate = (Class<? extends Signature>)
                    Class.forName(Signature.class.getName() + "$Delegate");
            } catch (ClassNotFoundException e) { throw new RuntimeException(e); }

            signature = newInstance(delegate,
                new Class[] { SignatureSpi.class, String.class }, spi, algorithm
            );
        }
        setField(signature, Signature.class, "provider", provider);
        return signature;
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static Mac getMac(final String algorithm) throws NoSuchAlgorithmException {
        Mac mac = null;
        final Provider provider = getSecurityProvider();
        if ( provider != null ) {
            mac = getMac(algorithm, provider, true);
        }
        if ( mac == null ) mac = Mac.getInstance(algorithm);
        return mac;
    }

    static Mac getMac(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        return getMac(algorithm, provider, false);
    }

    private static Mac getMac(final String algorithm, final Provider provider, boolean silent)
        throws NoSuchAlgorithmException {
        MacSpi spi = (MacSpi) getImplEngine("Mac", algorithm);
        if ( spi == null ) {
            if ( silent ) return null;
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
        return newInstance(Mac.class,
            new Class[] { MacSpi.class, Provider.class, String.class },
            new Object[] { spi, provider, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static KeyGenerator getKeyGenerator(final String algorithm) throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getKeyGenerator(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return KeyGenerator.getInstance(algorithm);
    }

    static KeyGenerator getKeyGenerator(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final KeyGeneratorSpi spi = (KeyGeneratorSpi) getImplEngine("KeyGenerator", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        return newInstance(KeyGenerator.class,
            new Class[] { KeyGeneratorSpi.class, Provider.class, String.class },
            new Object[] { spi, provider, algorithm }
        );
    }

    /**
     * @note code calling this should not assume BC provider internals !
     */
    public static SecretKeyFactory getSecretKeyFactory(final String algorithm) throws NoSuchAlgorithmException {
        try {
            final Provider provider = getSecurityProvider();
            if ( provider != null ) return getSecretKeyFactory(algorithm, provider);
        }
        catch (NoSuchAlgorithmException e) { }
        return SecretKeyFactory.getInstance(algorithm);
    }

    static SecretKeyFactory getSecretKeyFactory(final String algorithm, final Provider provider)
        throws NoSuchAlgorithmException {
        final SecretKeyFactorySpi spi = (SecretKeyFactorySpi) getImplEngine("SecretKeyFactory", algorithm);
        if ( spi == null ) throw new NoSuchAlgorithmException(algorithm + " not found");

        return newInstance(SecretKeyFactory.class,
            new Class[] { SecretKeyFactorySpi.class, Provider.class, String.class },
            new Object[] { spi, provider, algorithm }
        );
    }

    private static boolean providerSSLContext = false; // BC does not implement + JDK default is fine

    public static SSLContext getSSLContext(final String protocol)
        throws NoSuchAlgorithmException {
        try {
            if ( providerSSLContext ) {
                final Provider provider = getSecurityProvider();
                if ( provider != null ) {
                    return getSSLContext(protocol, provider);
                }
            }
        }
        catch (NoSuchAlgorithmException e) { }
        return SSLContext.getInstance(protocol);
    }

    private static SSLContext getSSLContext(final String protocol, final Provider provider)
        throws NoSuchAlgorithmException {
        return SSLContext.getInstance(protocol, provider);
    }

    public static boolean verify(final X509CRL crl, final PublicKey publicKey)
        throws NoSuchAlgorithmException, CRLException, InvalidKeyException, SignatureException {
        return verify(crl, publicKey, false);
    }

    static boolean verify(final X509CRL crl, final PublicKey publicKey, final boolean silent)
        throws NoSuchAlgorithmException, CRLException, InvalidKeyException, SignatureException {

        if ( crl instanceof X509CRLObject ) {
            final CertificateList crlList = (CertificateList) getCertificateList(crl);
            final AlgorithmIdentifier tbsSignatureId = crlList.getTBSCertList().getSignature();
            if ( ! crlList.getSignatureAlgorithm().equals(tbsSignatureId) ) {
                if ( silent ) return false;
                throw new CRLException("Signature algorithm on CertificateList does not match TBSCertList.");
            }

            final Signature signature = getSignature(crl.getSigAlgName(), securityProvider);

            signature.initVerify(publicKey);
            signature.update(crl.getTBSCertList());

            if ( ! signature.verify( crl.getSignature() ) ) {
                if ( silent ) return false;
                throw new SignatureException("CRL does not verify with supplied public key.");
            }
            return true;
        }

        try {
            crl.verify(publicKey);
            return true;
        }
        catch (NoSuchAlgorithmException ex) {
            if ( silent ) return false; throw ex;
        }
        catch (CRLException ex) {
            if ( silent ) return false; throw ex;
        }
        catch (InvalidKeyException ex) {
            if ( silent ) return false; throw ex;
        }
        catch (SignatureException ex) {
            if ( silent ) return false; throw ex;
        }
        catch (NoSuchProviderException ex) {
            if ( isDebug() ) ex.printStackTrace();
            throw new RuntimeException(ex); // unexpected - might hide a bug
        }
    }

    private static Object getCertificateList(final Object crl) { // X509CRLObject
        try { // private CertificateList c;
            final Field cField = X509CRLObject.class.getDeclaredField("c");
            cField.setAccessible(true);
            return cField.get(crl);
        }
        catch (NoSuchFieldException ex) {
            if ( isDebug() ) ex.printStackTrace(System.out);
            return null;
        }
        catch (IllegalAccessException e) { return null; }
        catch (SecurityException e) { return null; }
    }

    // these are BC JCE (@see javax.crypto.JCEUtil) inspired internals :
    // https://github.com/bcgit/bc-java/blob/master/jce/src/main/java/javax/crypto/JCEUtil.java

    private static Object getImplEngine(String baseName, String algorithm) {
        Object engine = findImplEngine(baseName, algorithm.toUpperCase(Locale.ENGLISH));
        if (engine == null) {
            engine = findImplEngine(baseName, algorithm);
        }
        return engine;
    }

    private static Object findImplEngine(final String baseName, String algorithm) {
        final Provider bcProvider = securityProvider;
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
                } else {
                    klass = Class.forName(className);
                }
                return klass.newInstance();
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException("algorithm " + algorithm + " in provider " + bcProvider.getName() + " but no class \"" + className + "\" found!");
            }
            catch (Exception e) {
                throw new IllegalStateException("algorithm " + algorithm + " in provider " + bcProvider.getName() + " but class \"" + className + "\" inaccessible!");
            }
        }
        return null;
    }

    // the obligratory "reflection crap" :

    private static <T> T newInstance(Class<T> klass, Class<?>[] paramTypes, Object... params) {
        final Constructor<T> constructor;
        try {
            constructor = klass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(params);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object obj, Class<?> fieldOwner, String fieldName, Object value) {
        final Field field;
        try {
            field = fieldOwner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("no field '" + fieldName + "' declared in " + fieldOwner + "", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}