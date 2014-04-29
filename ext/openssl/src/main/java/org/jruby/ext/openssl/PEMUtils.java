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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.GeneralSecurityException;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
//import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.OperatorCreationException;

import org.jruby.ext.openssl.impl.pem.MiscPEMGenerator;
import org.jruby.ext.openssl.impl.pem.PEMDecryptor;
import org.jruby.ext.openssl.impl.pem.PEMDecryptorProvider;
import org.jruby.ext.openssl.impl.pem.PEMEncryptedKeyPair;
import org.jruby.ext.openssl.impl.pem.PEMException;
import org.jruby.ext.openssl.impl.pem.PEMKeyPair;
import org.jruby.ext.openssl.impl.pem.PEMParser;
//import org.bouncycastle.util.io.pem.PemReader;

import static org.jruby.ext.openssl.x509store.PEMInputOutput.getKeyFactory;

/**
 * PEM Utilities, for now mostly to replace {@link PEMHandler}.
 *
 * @author kares
 */
public abstract class PEMUtils {

    /*
    private static boolean bcPEMParser;
    private static Class<?> pemReaderImpl;

    private static Reader newPemReader(final Reader reader) {
        if ( pemReaderImpl == null ) {
            synchronized(BouncyCastlePEMHandler.class) {
                if ( pemReaderImpl == null ) {
                    try {
                        pemReaderImpl = Class.forName("org.bouncycastle.openssl.PEMParser");
                        bcPEMParser = true;
                    }
                    catch (ClassNotFoundException ex) {
                        pemReaderImpl = org.jruby.ext.openssl.impl.pem.PEMParser.class;
                    }
                }
            }
        }
        try {
            Constructor<? extends PemReader> constructor = (Constructor<? extends PemReader>)
                    pemReaderImpl.getConstructor(new Class[] { Reader.class });
            return constructor.newInstance(reader);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        //catch (InstantiationException e) {
        //}
        catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
        catch (Exception e) {
            if ( e instanceof RuntimeException ) throw (RuntimeException) e;
            throw new IllegalStateException(e);
        }
    }

    private static Object doInvoke(Object obj, String methodName, Class<?>[] paramTypes, Object... params)
        throws IOException {
        final Method method;
        try {
            method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(obj, params);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        catch (InvocationTargetException e) {
            final Throwable target = e.getTargetException();
            if ( target instanceof IOException ) throw (IOException) target;
            if ( target instanceof RuntimeException ) throw (RuntimeException) target;
            throw new IllegalStateException(target);
        }
        catch (Exception e) {
            if ( e instanceof IOException ) throw (IOException) e;
            if ( e instanceof RuntimeException ) throw (RuntimeException) e;
            throw new IllegalStateException(e);
        }
    }

    */

    public static KeyPair readKeyPair(final Reader reader) throws IOException {
        return readKeyPair(reader, null);
    }

    public static KeyPair readKeyPair(final Reader reader, final char[] password) throws IOException {
        PEMKeyPair pemKeyPair = readInternal(reader, password);
        return toKeyPair(pemKeyPair);
    }

    static PEMKeyPair readInternal(final Reader reader, final char[] password) throws IOException {
        Object keyPair = new PEMParser(reader).readObject();
        if ( keyPair instanceof PEMEncryptedKeyPair ) {
            return ((PEMEncryptedKeyPair) keyPair).decryptKeyPair(new PEMDecryptorImpl(password));
        }
        return (PEMKeyPair) keyPair;
    }

    private static KeyPair toKeyPair(final PEMKeyPair pemKeyPair) throws IOException {
        try {
            KeyFactory keyFactory = getKeyFactory( pemKeyPair.getPrivateKeyInfo().getPrivateKeyAlgorithm() );
            return new KeyPair(
                keyFactory.generatePublic( new X509EncodedKeySpec( pemKeyPair.getPublicKeyInfo().getEncoded() ) ),
                keyFactory.generatePrivate( new PKCS8EncodedKeySpec( pemKeyPair.getPrivateKeyInfo().getEncoded() ) )
            );
        }
        catch (Exception e) {
            throw new PEMException("unable to convert key pair: " + e.getMessage(), e);
        }
    }

    public static void writePEM(final Writer writer, final Object obj,
        final String algorithm, final char[] password) throws IOException {

        final PEMWriter pemWriter = new PEMWriter(writer);

        final SecureRandom random = SecurityHelper.getSecureRandom();

        pemWriter.writeObject(MiscPEMGenerator.newInstance(obj, algorithm, password, random));
        pemWriter.flush();
    }

    public static void writePEM(final Writer writer, final Object obj) throws IOException {
        writePEM(writer, obj, null, null);
    }

    public static byte[] generatePKCS12(final Reader keyReader, final byte[] cert,
        final String aliasName, final char[] password)
        throws IOException, GeneralSecurityException {

        final Collection<? extends Certificate> certChain =
            SecurityHelper.getCertificateFactory("X.509").generateCertificates(new ByteArrayInputStream(cert));

        final PEMKeyPair pemKeyPair = readInternal(keyReader, null);
        final KeyFactory keyFactory = getKeyFactory( pemKeyPair.getPrivateKeyInfo().getPrivateKeyAlgorithm() );
        Key privateKey = keyFactory.generatePrivate( new PKCS8EncodedKeySpec( pemKeyPair.getPrivateKeyInfo().getEncoded() ) );

        final KeyStore keyStore = SecurityHelper.getKeyStore("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry( aliasName, privateKey, null, certChain.toArray(new Certificate[certChain.size()]) );

        final ByteArrayOutputStream pkcs12Out = new ByteArrayOutputStream();
        keyStore.store(pkcs12Out, password == null ? new char[0] : password);

        return pkcs12Out.toByteArray();
    }

    private static class PEMDecryptorImpl implements PEMDecryptorProvider, PEMDecryptor {

        PEMDecryptorImpl(char[] password) { this.password = password; }

        private char[] password;
        private String dekAlgName;

        public PEMDecryptor get(String dekAlgName) throws OperatorCreationException {
            this.dekAlgName = dekAlgName;
            return this; // PEMDecryptor
        }

        public byte[] decrypt(byte[] keyBytes, byte[] iv) throws PEMException {
            return decrypt(keyBytes, password, dekAlgName, iv);
        }

        static byte[] decrypt(
            byte[] bytes,
            char[] password,
            String dekAlgName,
            byte[] iv)
            throws PEMException
        {
            return decrypt(SecurityHelper.getSecurityProvider(), bytes, password, dekAlgName, iv);
        }

        static byte[] decrypt(
            Provider provider,
            byte[] bytes,
            char[] password,
            String dekAlgName,
            byte[] iv)
            throws PEMException
        {
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            String alg;
            String blockMode = "CBC";
            String padding = "PKCS5Padding";
            Key sKey;

            // Figure out block mode and padding.
            if (dekAlgName.endsWith("-CFB"))
            {
                blockMode = "CFB";
                padding = "NoPadding";
            }
            if (dekAlgName.endsWith("-ECB") ||
                "DES-EDE".equals(dekAlgName) ||
                "DES-EDE3".equals(dekAlgName))
            {
                // ECB is actually the default (though seldom used) when OpenSSL
                // uses DES-EDE (des2) or DES-EDE3 (des3).
                blockMode = "ECB";
                paramSpec = null;
            }
            if (dekAlgName.endsWith("-OFB"))
            {
                blockMode = "OFB";
                padding = "NoPadding";
            }


            // Figure out algorithm and key size.
            if (dekAlgName.startsWith("DES-EDE"))
            {
                alg = "DESede";
                // "DES-EDE" is actually des2 in OpenSSL-speak!
                // "DES-EDE3" is des3.
                boolean des2 = !dekAlgName.startsWith("DES-EDE3");
                sKey = secretKeySpec(password, alg, 24, iv, des2);
            }
            else if (dekAlgName.startsWith("DES-"))
            {
                alg = "DES";
                sKey = secretKeySpec(password, alg, 8, iv);
            }
            else if (dekAlgName.startsWith("BF-"))
            {
                alg = "Blowfish";
                sKey = secretKeySpec(password, alg, 16, iv);
            }
            else if (dekAlgName.startsWith("RC2-"))
            {
                alg = "RC2";
                int keyBits = 128;
                if (dekAlgName.startsWith("RC2-40-"))
                {
                    keyBits = 40;
                }
                else if (dekAlgName.startsWith("RC2-64-"))
                {
                    keyBits = 64;
                }
                sKey = secretKeySpec(password, alg, keyBits / 8, iv);
                if (paramSpec == null) // ECB block mode
                {
                    paramSpec = new RC2ParameterSpec(keyBits);
                }
                else
                {
                    paramSpec = new RC2ParameterSpec(keyBits, iv);
                }
            }
            else if (dekAlgName.startsWith("AES-"))
            {
                alg = "AES";
                byte[] salt = iv;
                if (salt.length > 8)
                {
                    salt = new byte[8];
                    System.arraycopy(iv, 0, salt, 0, 8);
                }

                int keyBits;
                if (dekAlgName.startsWith("AES-128-"))
                {
                    keyBits = 128;
                }
                else if (dekAlgName.startsWith("AES-192-"))
                {
                    keyBits = 192;
                }
                else if (dekAlgName.startsWith("AES-256-"))
                {
                    keyBits = 256;
                }
                else
                {
                    throw new PEMException("unknown AES encryption with private key");
                }
                sKey = secretKeySpec(password, "AES", keyBits / 8, salt);
            }
            else
            {
                throw new PEMException("unknown encryption with private key");
            }

            String transformation = alg + "/" + blockMode + "/" + padding;

            try
            {
                javax.crypto.Cipher cipher = SecurityHelper.getCipher(transformation);
                final int decryptMode = javax.crypto.Cipher.DECRYPT_MODE;

                if (paramSpec == null) // ECB block mode
                {
                    cipher.init(decryptMode, sKey);
                }
                else
                {
                    cipher.init(decryptMode, sKey, paramSpec);
                }
                return cipher.doFinal(bytes);
            }
            catch (Exception e)
            {
                throw new PEMException("exception using cipher - please check password and data.", e);
            }
        }

        private static SecretKey secretKeySpec(
            char[] password,
            String algorithm,
            int keyLength,
            byte[] salt)
        {
            return secretKeySpec(password, algorithm, keyLength, salt, false);
        }

        private static SecretKey secretKeySpec(
            char[] password,
            String algorithm,
            int keyLength,
            byte[] salt,
            boolean des2)
        {
            OpenSSLPBEParametersGenerator pGen = new OpenSSLPBEParametersGenerator();

            pGen.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt);

            KeyParameter keyParam;
            keyParam = (KeyParameter)pGen.generateDerivedParameters(keyLength * 8);
            byte[] key = keyParam.getKey();
            if (des2 && key.length >= 24)
            {
                // For DES2, we must copy first 8 bytes into the last 8 bytes.
                System.arraycopy(key, 0, key, 16, 8);
            }
            return new SecretKeySpec(key, algorithm);
        }

    }

}
