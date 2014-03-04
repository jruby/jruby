
package org.jruby.ext.openssl;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class SecurityHelperTest {

    // @BeforeClass
    public static void setBouncyCastleProvider() {
        SecurityHelper.setBouncyCastleProvider();
    }

    private Provider savedProvider;

    @Before
    public void saveSecurityProvider() {
        savedProvider = SecurityHelper.getSecurityProvider();
    }

    @After
    public void restoreSecurityProvider() {
        SecurityHelper.securityProvider = savedProvider;
    }

    public void disableSecurityProvider() {
        SecurityHelper.securityProvider = null;
        SecurityHelper.setBouncyCastleProvider = false;
    }

    @Test
    public void usesBouncyCastleSecurityProviderByDefault() {
        assertNotNull(SecurityHelper.getSecurityProvider());
        assertEquals("org.bouncycastle.jce.provider.BouncyCastleProvider",
            SecurityHelper.getSecurityProvider().getClass().getName()
        );
    }

    @Test
    public void allowsToSetSecurityProvider() {
        final Provider provider;
        try {
            Class providerClass = Class.forName("sun.security.provider.Sun");
            provider = (Provider) providerClass.newInstance();
        }
        catch (Exception e) {
            System.out.println("allowsToSetSecurityProvider() skipped due: " + e);
            return;
        }
        SecurityHelper.setSecurityProvider(provider);

        assertSame(provider, SecurityHelper.getSecurityProvider());
    }

    @Test
    public void doesNotRegisterBouncyCastleSecurityProviderByDefault() {
        SecurityHelper.getSecurityProvider();
        assertNull(java.security.Security.getProvider("BC"));
    }

    @Test
    public void registersSecurityProviderWhenRequested() {
        SecurityHelper.setRegisterProvider(true);
        try {
            SecurityHelper.getSecurityProvider();
            assertNotNull(java.security.Security.getProvider("BC"));
        }
        finally {
            java.security.Security.removeProvider("BC");
            SecurityHelper.setRegisterProvider(false);
        }
    }

    // Standart java.security

    @Test
    public void testGetKeyFactory() throws Exception {
        assertNotNull( SecurityHelper.getKeyFactory("RSA") );
        assertNotNull( SecurityHelper.getKeyFactory("DSA") );
    }

    @Test
    public void testGetKeyFactoryWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getKeyFactory("RSA") );
        assertNotNull( SecurityHelper.getKeyFactory("DSA") );
    }

    @Test
    public void testGetKeyFactoryThrows() throws Exception {
        try {
            SecurityHelper.getKeyFactory("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getKeyFactory("USA", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetKeyPairGenerator() throws Exception {
        assertNotNull( SecurityHelper.getKeyPairGenerator("RSA") );
        assertNotNull( SecurityHelper.getKeyPairGenerator("DSA") );

        assertNotNull( SecurityHelper.getKeyPairGenerator("RSA", savedProvider) );
    }

    @Test
    public void testGetKeyPairGeneratorWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getKeyPairGenerator("RSA") );
        assertNotNull( SecurityHelper.getKeyPairGenerator("DSA") );
    }

    @Test
    public void testGetKeyPairGeneratorThrows() throws Exception {
        try {
            SecurityHelper.getKeyPairGenerator("USA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getKeyPairGenerator("USA", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetMessageDigest() throws Exception {
        assertNotNull( SecurityHelper.getMessageDigest("MD5") );
        assertNotNull( SecurityHelper.getMessageDigest("SHA-1") );

        assertNotNull( SecurityHelper.getMessageDigest("MD5", savedProvider) );
    }

    @Test
    public void testGetMessageDigestWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getMessageDigest("MD5") );
        assertNotNull( SecurityHelper.getMessageDigest("SHA-1") );
    }

    @Test
    public void testGetMessageDigestThrows() throws Exception {
        try {
            SecurityHelper.getMessageDigest("XXL");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getMessageDigest("XXL", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetSignature() throws Exception {
        assertNotNull( SecurityHelper.getSignature("NONEwithRSA") );

        assertNotNull( SecurityHelper.getSignature("NONEwithRSA", savedProvider) );
    }

    @Test
    public void testGetSignatureWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getSignature("NONEwithRSA") );
    }

    @Test
    public void testGetSignatureThrows() throws Exception {
        try {
            SecurityHelper.getSignature("SOMEwithRSA");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getSignature("SOMEwithRSA", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetCertificateFactory() throws Exception {
        assertNotNull( SecurityHelper.getCertificateFactory("X.509") );

        assertNotNull( SecurityHelper.getCertificateFactory("X.509", savedProvider) );
    }

    @Test
    public void testGetCertificateFactoryWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getCertificateFactory("X.509") );
    }

    @Test
    public void testGetCertificateFactoryThrows() throws Exception {
        try {
            SecurityHelper.getCertificateFactory("X.510");
            fail();
        }
        catch (CertificateException e) {
            // OK
        }
        try {
            SecurityHelper.getCertificateFactory("X.510", savedProvider);
            fail();
        }
        catch (CertificateException e) {
            // OK
        }
    }

    // JCE

    @Test
    public void testGetCipher() throws Exception {
        assertNotNull( SecurityHelper.getCipher("DES") );
        assertNotNull( SecurityHelper.getCipher("AES") );

        assertNotNull( SecurityHelper.getCipher("DES/CBC/PKCS5Padding") );
    }

    @Test
    public void testGetCipherBC() throws Exception {
        assertNotNull( SecurityHelper.getCipher("AES", savedProvider) );

        assertNotNull( SecurityHelper.getCipher("DES/CBC/PKCS5Padding", savedProvider) );
    }

    @Test
    public void testGetCipherWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getCipher("DES") );
        assertNotNull( SecurityHelper.getCipher("AES") );
    }

    @Test
    public void testGetSecretKeyFactory() throws Exception {
        assertNotNull( SecurityHelper.getSecretKeyFactory("DES") );

        assertNotNull( SecurityHelper.getSecretKeyFactory("DESede", savedProvider) );
    }

    @Test
    public void testGetSecretKeyFactoryWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getSecretKeyFactory("DES") );
        assertNotNull( SecurityHelper.getSecretKeyFactory("DESede") );
    }

    @Test
    public void testGetSecretKeyFactoryThrows() throws Exception {
        try {
            SecurityHelper.getSecretKeyFactory("MESS");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getSecretKeyFactory("MESS", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

    //

    @Test
    public void testGetMac() throws Exception {
        assertNotNull( SecurityHelper.getMac("HmacMD5") );
        assertNotNull( SecurityHelper.getMac("HmacSHA1") );

        assertNotNull( SecurityHelper.getMac("HmacMD5", savedProvider) );
    }

    @Test
    public void testGetMacWithoutBC() throws Exception {
        disableSecurityProvider();
        assertNotNull( SecurityHelper.getMac("HMacMD5") );
    }

    @Test
    public void testGetMacThrows() throws Exception {
        try {
            SecurityHelper.getMac("HmacMDX");
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
        try {
            SecurityHelper.getMac("HmacMDX", savedProvider);
            fail();
        }
        catch (NoSuchAlgorithmException e) {
            // OK
        }
    }

}
