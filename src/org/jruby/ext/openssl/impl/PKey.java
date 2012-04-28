/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Hiroshi Nakamura <nahi@ruby-lang.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.jruby.util.ByteList;

/**
 *
 * Handles PKey related ASN.1 handling.
 *
 * @author <a href="mailto:nahi@ruby-lang.org">Hiroshi Nakamura</a>
 */
public class PKey {

    // d2i_RSAPrivateKey_bio
    public static PrivateKey readRSAPrivateKey(byte[] input) throws IOException, GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        DERSequence seq = (DERSequence) (new ASN1InputStream(input).readObject());
        if (seq.size() == 9) {
            BigInteger mod = ((DERInteger) seq.getObjectAt(1)).getValue();
            BigInteger pubexp = ((DERInteger) seq.getObjectAt(2)).getValue();
            BigInteger privexp = ((DERInteger) seq.getObjectAt(3)).getValue();
            BigInteger primep = ((DERInteger) seq.getObjectAt(4)).getValue();
            BigInteger primeq = ((DERInteger) seq.getObjectAt(5)).getValue();
            BigInteger primeep = ((DERInteger) seq.getObjectAt(6)).getValue();
            BigInteger primeeq = ((DERInteger) seq.getObjectAt(7)).getValue();
            BigInteger crtcoeff = ((DERInteger) seq.getObjectAt(8)).getValue();
            return fact.generatePrivate(new RSAPrivateCrtKeySpec(mod, pubexp, privexp, primep, primeq, primeep, primeeq, crtcoeff));
        } else {
            return null;
        }
    }

    // d2i_RSAPublicKey_bio
    public static PublicKey readRSAPublicKey(byte[] input) throws IOException, GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        DERSequence seq = (DERSequence) (new ASN1InputStream(input).readObject());
        if (seq.size() == 2) {
            BigInteger mod = ((DERInteger) seq.getObjectAt(0)).getValue();
            BigInteger pubexp = ((DERInteger) seq.getObjectAt(1)).getValue();
            return fact.generatePublic(new RSAPublicKeySpec(mod, pubexp));
        } else {
            return null;
        }
    }

    // d2i_DSAPrivateKey_bio
    public static KeyPair readDSAPrivateKey(byte[] input) throws IOException, GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("DSA");
        DERSequence seq = (DERSequence) (new ASN1InputStream(input).readObject());
        if (seq.size() == 6) {
            BigInteger p = ((DERInteger) seq.getObjectAt(1)).getValue();
            BigInteger q = ((DERInteger) seq.getObjectAt(2)).getValue();
            BigInteger g = ((DERInteger) seq.getObjectAt(3)).getValue();
            BigInteger y = ((DERInteger) seq.getObjectAt(4)).getValue();
            BigInteger x = ((DERInteger) seq.getObjectAt(5)).getValue();
            PrivateKey priv = fact.generatePrivate(new DSAPrivateKeySpec(x, p, q, g));
            PublicKey pub = fact.generatePublic(new DSAPublicKeySpec(y, p, q, g));
            return new KeyPair(pub, priv);
        } else {
            return null;
        }
    }

    // d2i_DSA_PUBKEY_bio
    public static PublicKey readDSAPublicKey(byte[] input) throws IOException, GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        DERSequence seq = (DERSequence) (new ASN1InputStream(input).readObject());
        if (seq.size() == 4) {
            BigInteger y = ((DERInteger) seq.getObjectAt(0)).getValue();
            BigInteger p = ((DERInteger) seq.getObjectAt(1)).getValue();
            BigInteger q = ((DERInteger) seq.getObjectAt(2)).getValue();
            BigInteger g = ((DERInteger) seq.getObjectAt(3)).getValue();
            return fact.generatePublic(new DSAPublicKeySpec(y, p, q, g));
        } else {
            return null;
        }
    }

    public static byte[] toDerRSAKey(RSAPublicKey pubKey, RSAPrivateCrtKey privKey) throws IOException {
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        if (pubKey != null && privKey == null) {
            v1.add(new DERInteger(pubKey.getModulus()));
            v1.add(new DERInteger(pubKey.getPublicExponent()));
        } else {
            v1.add(new DERInteger(0));
            v1.add(new DERInteger(privKey.getModulus()));
            v1.add(new DERInteger(privKey.getPublicExponent()));
            v1.add(new DERInteger(privKey.getPrivateExponent()));
            v1.add(new DERInteger(privKey.getPrimeP()));
            v1.add(new DERInteger(privKey.getPrimeQ()));
            v1.add(new DERInteger(privKey.getPrimeExponentP()));
            v1.add(new DERInteger(privKey.getPrimeExponentQ()));
            v1.add(new DERInteger(privKey.getCrtCoefficient()));
        }
        return new DERSequence(v1).getEncoded();
    }

    public static byte[] toDerDSAKey(DSAPublicKey pubKey, DSAPrivateKey privKey) throws IOException {
        if (pubKey != null && privKey == null) {
            return pubKey.getEncoded();
        } else if (privKey != null && pubKey != null) {
            DSAParams params = privKey.getParams();
            ASN1EncodableVector v1 = new ASN1EncodableVector();
            v1.add(new DERInteger(0));
            v1.add(new DERInteger(params.getP()));
            v1.add(new DERInteger(params.getQ()));
            v1.add(new DERInteger(params.getG()));
            v1.add(new DERInteger(pubKey.getY()));
            v1.add(new DERInteger(privKey.getX()));
            return new DERSequence(v1).getEncoded();
        } else {
            return privKey.getEncoded();
        }
    }

    public static byte[] toDerDHKey(BigInteger p, BigInteger g) throws IOException {
        ASN1EncodableVector v = new ASN1EncodableVector();
        if (p != null) {
            v.add(new DERInteger(p));
        }
        if (g != null) {
            v.add(new DERInteger(g));
        }
        return new DERSequence(v).getEncoded();
    }
}


