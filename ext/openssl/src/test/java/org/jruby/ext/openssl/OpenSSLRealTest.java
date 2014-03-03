
package org.jruby.ext.openssl;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;

import org.junit.*;
import static org.junit.Assert.*;

import static org.jruby.ext.openssl.OpenSSLReal.*;

/**
 * @author kares
 */
public class OpenSSLRealTest {

    @BeforeClass
    public static void setBouncyCastleProvider() {
        OpenSSLReal.setBouncyCastleProvider();
    }

    private Provider savedBouncyCastleProvider;

    @Before
    public void saveBouncyCastleProvider() {
        savedBouncyCastleProvider = OpenSSLReal.BC_PROVIDER;
    }

    @After
    public void restoreBouncyCastleProvider() {
        OpenSSLReal.BC_PROVIDER = savedBouncyCastleProvider;
    }

    public void disableBouncyCastleProvider() {
        OpenSSLReal.BC_PROVIDER = null;
    }

    // Standart java.security

    @Test
    public void testGetKeyFactory() throws Exception {
        assertNotNull( getKeyFactory("RSA") );
        assertNotNull( getKeyFactory("DSA") );

        assertNotNull( getKeyFactoryBC("RSA") );
    }

    @Test
    public void testGetKeyFactoryWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getKeyFactory("RSA") );
        assertNotNull( getKeyFactory("DSA") );
    }

    @Test
    public void testGetKeyFactoryThrows() throws Exception {
        try {
            getKeyFactory("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getKeyFactoryBC("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetKeyPairGenerator() throws Exception {
        assertNotNull( getKeyPairGenerator("RSA") );
        assertNotNull( getKeyPairGenerator("DSA") );

        assertNotNull( getKeyPairGeneratorBC("RSA") );
    }

    @Test
    public void testGetKeyPairGeneratorWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getKeyPairGenerator("RSA") );
        assertNotNull( getKeyPairGenerator("DSA") );
    }

    @Test
    public void testGetKeyPairGeneratorThrows() throws Exception {
        try {
            getKeyPairGenerator("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getKeyPairGeneratorBC("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetMessageDigest() throws Exception {
        assertNotNull( getMessageDigest("MD5") );
        assertNotNull( getMessageDigest("SHA-1") );

        assertNotNull( getMessageDigestBC("MD5") );
    }

    @Test
    public void testGetMessageDigestWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getMessageDigest("MD5") );
        assertNotNull( getMessageDigest("SHA-1") );
    }

    @Test
    public void testGetMessageDigestThrows() throws Exception {
        try {
            getMessageDigest("XXL");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getMessageDigestBC("XXL");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetSignature() throws Exception {
        assertNotNull( getSignature("NONEwithRSA") );

        assertNotNull( getSignatureBC("NONEwithRSA") );
    }

    @Test
    public void testGetSignatureWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getSignature("NONEwithRSA") );
    }

    @Test
    public void testGetSignatureThrows() throws Exception {
        try {
            getSignature("SOMEwithRSA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getSignatureBC("SOMEwithRSA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetCertificateFactory() throws Exception {
        assertNotNull( getCertificateFactory("X.509") );

        assertNotNull( getCertificateFactoryBC("X.509") );
    }

    @Test
    public void testGetCertificateFactoryWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getCertificateFactory("X.509") );
    }

    @Test
    public void testGetCertificateFactoryThrows() throws Exception {
        try {
            getCertificateFactory("X.510");
            fail();
        }
        catch (CertificateException e) {
            // OK
        }
        try {
            getCertificateFactoryBC("X.510");
            fail();
        }
        catch (CertificateException e) {
            // OK
        }
    }

    // JCE

    @Test
    public void testGetCipher() throws Exception {
        assertNotNull( getCipher("DES") );
        assertNotNull( getCipher("AES") );

        assertNotNull( getCipher("DES/CBC/PKCS5Padding") );
    }

    @Test
    public void testGetCipherBC() throws Exception {
        assertNotNull( getCipherBC("AES") );

        assertNotNull( getCipherBC("DES/CBC/PKCS5Padding") );
    }

    @Test
    public void testGetCipherWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getCipher("DES") );
        assertNotNull( getCipher("AES") );
    }

    @Test
    public void testGetSecretKeyFactory() throws Exception {
        assertNotNull( getSecretKeyFactory("DES") );

        assertNotNull( getSecretKeyFactoryBC("DESede") );
    }

    @Test
    public void testGetSecretKeyFactoryWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getSecretKeyFactory("DES") );
        assertNotNull( getSecretKeyFactory("DESede") );
    }

    @Test
    public void testGetSecretKeyFactoryThrows() throws Exception {
        try {
            getSecretKeyFactory("MESS");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getSecretKeyFactoryBC("MESS");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetMac() throws Exception {
        assertNotNull( getMac("HmacMD5") );
        assertNotNull( getMac("HmacSHA1") );

        assertNotNull( getMacBC("HmacMD5") );
    }

    @Test
    public void testGetMacWithoutBC() throws Exception {
        disableBouncyCastleProvider();
        assertNotNull( getMac("HMacMD5") );
    }

    @Test
    public void testGetMacThrows() throws Exception {
        try {
            getMac("HmacMDX");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            getMacBC("HmacMDX");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

}
