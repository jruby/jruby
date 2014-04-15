
package org.jruby.ext.openssl;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author kares
 */
public class CipherTest {

    @Test
    public void ciphersGetLazyInitialized() {
        assertTrue( Cipher.CIPHERS.isEmpty() );
        assertFalse( Cipher.isSupportedCipher("") );
        assertFalse( Cipher.CIPHERS.isEmpty() );
        assertTrue( Cipher.CIPHERS.contains("DES") );
    }

}
