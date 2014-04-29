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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.ext.openssl.impl;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import org.jruby.ext.openssl.SecurityHelper;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class EVP {
    // This is a class that will collect mappings from ASN1 stuff to
    // matching Cipher and other algorithms.

    // Typical examples:
    //  EVP_get_cipherbyobj
    //  EVP_get_digestbynid

    /* c: EVP_get_cipherbyobj
     *
     */
    public static Cipher getCipher(ASN1ObjectIdentifier oid)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        String algorithm = getAlgorithmName(oid);
        String[] cipher = org.jruby.ext.openssl.Cipher.Algorithm.osslToJsse(algorithm);
        String realName = cipher[3];
        return SecurityHelper.getCipher(realName);
    }

    /* c: EVP_get_digestbyobj
     *
     */
    public static MessageDigest getDigest(ASN1ObjectIdentifier oid) throws NoSuchAlgorithmException {
        String algorithm = getAlgorithmName(oid);
        return SecurityHelper.getMessageDigest(algorithm);
    }

    /* c: EVP_sha1
     *
     */
    public static MessageDigest sha1() {
        try {
            return SecurityHelper.getMessageDigest("SHA1");
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static int type(MessageDigest digest) {
        String name = digest.getAlgorithm();
        ASN1ObjectIdentifier oid = ASN1Registry.sym2oid(name);
        if ( oid == null ) {
            name = name.toLowerCase().replace("sha-", "sha");
            oid = ASN1Registry.sym2oid(name);
        }
        return ASN1Registry.obj2nid(oid);
    }

    public static String signatureAlgorithm(MessageDigest digest, Key key) {
        String sig = digest.getAlgorithm().toLowerCase().replace("sha-", "sha");
        String type = key.getAlgorithm().toLowerCase();
        if(sig == null) {
            sig = "none";
        }
        return sig + "with" + type;
    }

    /* c: EVP_PKEY_decrypt
     *
     */
    public static byte[] decrypt(byte[] input, int offset, int len, Key key) throws InvalidKeyException,
                                                                                    NoSuchAlgorithmException,
                                                                                    NoSuchPaddingException,
                                                                                    IllegalBlockSizeException,
                                                                                    BadPaddingException {
        Cipher cipher = SecurityHelper.getCipher(key.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(input, offset, len);
    }

    /* c: EVP_PKEY_decrypt
     *
     */
    public static byte[] decrypt(byte[] input, Key key) throws InvalidKeyException,
                                                               NoSuchAlgorithmException,
                                                               NoSuchPaddingException,
                                                               IllegalBlockSizeException,
                                                               BadPaddingException {
        return decrypt(input, 0, input.length, key);
    }

    private static String getAlgorithmName(ASN1ObjectIdentifier oid) {
        String algorithm = ASN1Registry.o2a(oid);
        if (algorithm != null) {
            return algorithm.toUpperCase();
        } else {
            return oid.getId();
        }
    }
}// EVP
