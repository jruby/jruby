
package org.jruby.ext.openssl;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class CipherTest {

    @Test
    public void ciphersGetLazyInitialized() {
        assertTrue( Cipher.supportedCiphers.isEmpty() );
        assertFalse( Cipher.isSupportedCipher("UNKNOWN") );
        assertFalse( Cipher.supportedCiphers.isEmpty() );
        assertTrue( Cipher.supportedCiphers.contains("DES") );
        assertTrue( Cipher.isSupportedCipher("DES") );
        assertTrue( Cipher.isSupportedCipher("des") );
    }

    @Test
    public void jsseToOssl() {
        String alg;
        alg = Cipher.Algorithm.jsseToOssl("RC2/CBC/PKCS5Padding", 40);
        assertEquals("RC2-40-CBC", alg);
        alg = Cipher.Algorithm.jsseToOssl("RC2/CFB/PKCS5Padding", 40);
        assertEquals("RC2-40-CFB", alg);
        alg = Cipher.Algorithm.jsseToOssl("Blowfish", 60);
        assertEquals("BF-60-CBC", alg);
        alg = Cipher.Algorithm.jsseToOssl("DESede", 24);
        assertEquals("DES-EDE3-CBC", alg);
    }

    @Test
    public void osslToJsse() {
        String[] alg;
        alg = Cipher.Algorithm.osslToJsse("RC2-40-CBC");
        assertEquals("RC2", alg[0]);
        assertEquals("40", alg[1]);
        assertEquals("CBC", alg[2]);
        assertEquals("RC2/CBC/PKCS5Padding", alg[3]);

        alg = Cipher.Algorithm.osslToJsse("DES-EDE3-CBC");
        assertEquals("DES", alg[0]);
        assertEquals("EDE3", alg[1]);
        assertEquals("CBC", alg[2]);
        assertEquals("DESede/CBC/PKCS5Padding", alg[3]);

        alg = Cipher.Algorithm.osslToJsse("BF");
        assertEquals("Blowfish", alg[0]);
        assertEquals("Blowfish/CBC/PKCS5Padding", alg[3]);
    }

    @Test
    public void osslKeyIvLength() {
        int[] len;
        len = Cipher.Algorithm.osslKeyIvLength("RC2-40-CBC");
        assertEquals(5, len[0]);
        assertEquals(8, len[1]);

        len = Cipher.Algorithm.osslKeyIvLength("DES-EDE3-CBC");
        assertEquals(24, len[0]);
        assertEquals(8, len[1]);

        len = Cipher.Algorithm.osslKeyIvLength("DES");
        assertEquals(8, len[0]);
        assertEquals(8, len[1]);

        len = Cipher.Algorithm.osslKeyIvLength("BF");
        assertEquals(16, len[0]);
        assertEquals(8, len[1]);

        len = Cipher.Algorithm.osslKeyIvLength("CAST");
        assertEquals(16, len[0]);
        assertEquals(8, len[1]);
    }

    @Test
    public void getAlgorithmBase() throws Exception {
        javax.crypto.Cipher cipher; String algBase;
        cipher = javax.crypto.Cipher.getInstance("DES/CBC/PKCS5Padding");
        algBase = Cipher.Algorithm.getAlgorithmBase(cipher);
        assertEquals("DES", algBase);

        cipher = javax.crypto.Cipher.getInstance("DES");
        algBase = Cipher.Algorithm.getAlgorithmBase(cipher);
        assertEquals("DES", algBase);
    }

}
