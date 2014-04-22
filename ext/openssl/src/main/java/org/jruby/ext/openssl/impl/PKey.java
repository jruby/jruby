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
 * Copyright (C) 2010 Hiroshi Nakamura <nahi@ruby-lang.org>
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

import java.io.IOException;
import java.math.BigInteger;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;

import org.jruby.ext.openssl.SecurityHelper;

/**
 *
 * Handles PKey related ASN.1 handling.
 *
 * @author <a href="mailto:nahi@ruby-lang.org">Hiroshi Nakamura</a>
 */
public class PKey {

    public static KeyPair readPrivateKey(byte[] input, String type) throws IOException, GeneralSecurityException {
        KeySpec pubSpec; KeySpec privSpec;
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(input).readObject();
        if ( type.equals("RSA") ) {
            ASN1Integer mod = (ASN1Integer) seq.getObjectAt(1);
            ASN1Integer pubExp = (ASN1Integer) seq.getObjectAt(2);
            ASN1Integer privExp = (ASN1Integer) seq.getObjectAt(3);
            ASN1Integer p1 = (ASN1Integer) seq.getObjectAt(4);
            ASN1Integer p2 = (ASN1Integer) seq.getObjectAt(5);
            ASN1Integer exp1 = (ASN1Integer) seq.getObjectAt(6);
            ASN1Integer exp2 = (ASN1Integer) seq.getObjectAt(7);
            ASN1Integer crtCoef = (ASN1Integer) seq.getObjectAt(8);
            pubSpec = new RSAPublicKeySpec(mod.getValue(), pubExp.getValue());
            privSpec = new RSAPrivateCrtKeySpec(mod.getValue(), pubExp.getValue(), privExp.getValue(), p1.getValue(), p2.getValue(), exp1.getValue(),
                    exp2.getValue(), crtCoef.getValue());
        } else { // assume "DSA" for now.
            ASN1Integer p = (ASN1Integer) seq.getObjectAt(1);
            ASN1Integer q = (ASN1Integer) seq.getObjectAt(2);
            ASN1Integer g = (ASN1Integer) seq.getObjectAt(3);
            ASN1Integer y = (ASN1Integer) seq.getObjectAt(4);
            ASN1Integer x = (ASN1Integer) seq.getObjectAt(5);
            privSpec = new DSAPrivateKeySpec(x.getValue(), p.getValue(), q.getValue(), g.getValue());
            pubSpec = new DSAPublicKeySpec(y.getValue(), p.getValue(), q.getValue(), g.getValue());
        }
        KeyFactory fact = SecurityHelper.getKeyFactory(type);
        return new KeyPair(fact.generatePublic(pubSpec), fact.generatePrivate(privSpec));
    }

    // d2i_PrivateKey_bio
    public static KeyPair readPrivateKey(byte[] input) throws IOException,
        NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPair key = null;
        try {
            key = readRSAPrivateKey(input);
        }
        catch (NoSuchAlgorithmException e) { throw e; /* should not happen */ }
        catch (InvalidKeySpecException e) {
            // ignore
        }
        if (key == null) {
            try {
                key = readDSAPrivateKey(input);
            }
            catch (NoSuchAlgorithmException e) { throw e; /* should not happen */ }
            catch (InvalidKeySpecException e) {
                // ignore
            }
        }
        return key;
    }

    // d2i_PUBKEY_bio
    public static PublicKey readPublicKey(byte[] input) throws IOException,
        NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey key = null;
        try {
            key = readRSAPublicKey(input);
        }
        catch (NoSuchAlgorithmException e) { throw e; /* should not happen */ }
        catch (InvalidKeySpecException e) {
            // ignore
        }
        if (key == null) {
            try {
                key = readDSAPublicKey(input);
            }
            catch (NoSuchAlgorithmException e) { throw e; /* should not happen */ }
            catch (InvalidKeySpecException e) {
                // ignore
            }
        }
        return key;
    }

    // d2i_RSAPrivateKey_bio
    public static KeyPair readRSAPrivateKey(final byte[] input)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return readRSAPrivateKey(SecurityHelper.getKeyFactory("RSA"), input);
    }

    public static KeyPair readRSAPrivateKey(final KeyFactory rsaFactory, final byte[] input)
        throws IOException, InvalidKeySpecException {
        // KeyFactory fact = SecurityHelper.getKeyFactory("RSA");
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(input).readObject();
        if ( seq.size() == 9 ) {
            BigInteger mod = ((ASN1Integer) seq.getObjectAt(1)).getValue();
            BigInteger pubexp = ((ASN1Integer) seq.getObjectAt(2)).getValue();
            BigInteger privexp = ((ASN1Integer) seq.getObjectAt(3)).getValue();
            BigInteger primep = ((ASN1Integer) seq.getObjectAt(4)).getValue();
            BigInteger primeq = ((ASN1Integer) seq.getObjectAt(5)).getValue();
            BigInteger primeep = ((ASN1Integer) seq.getObjectAt(6)).getValue();
            BigInteger primeeq = ((ASN1Integer) seq.getObjectAt(7)).getValue();
            BigInteger crtcoeff = ((ASN1Integer) seq.getObjectAt(8)).getValue();
            PrivateKey priv = rsaFactory.generatePrivate(new RSAPrivateCrtKeySpec(mod, pubexp, privexp, primep, primeq, primeep, primeeq, crtcoeff));
            PublicKey pub = rsaFactory.generatePublic(new RSAPublicKeySpec(mod, pubexp));
            return new KeyPair(pub, priv);
        }
        return null;
    }

    // d2i_RSAPublicKey_bio
    public static PublicKey readRSAPublicKey(final byte[] input)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return readRSAPublicKey(SecurityHelper.getKeyFactory("RSA"), input);
    }

    public static PublicKey readRSAPublicKey(final KeyFactory rsaFactory, final byte[] input)
        throws IOException, InvalidKeySpecException {
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(input).readObject();
        if ( seq.size() == 2 ) {
            BigInteger mod = ((ASN1Integer) seq.getObjectAt(0)).getValue();
            BigInteger pubexp = ((ASN1Integer) seq.getObjectAt(1)).getValue();
            return rsaFactory.generatePublic(new RSAPublicKeySpec(mod, pubexp));
        }
        return null;
    }

    // d2i_DSAPrivateKey_bio
    public static KeyPair readDSAPrivateKey(final byte[] input)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return readDSAPrivateKey(SecurityHelper.getKeyFactory("DSA"), input);
    }

    public static KeyPair readDSAPrivateKey(final KeyFactory dsaFactory, final byte[] input)
        throws IOException, InvalidKeySpecException {
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(input).readObject();
        if ( seq.size() == 6 ) {
            BigInteger p = ((ASN1Integer) seq.getObjectAt(1)).getValue();
            BigInteger q = ((ASN1Integer) seq.getObjectAt(2)).getValue();
            BigInteger g = ((ASN1Integer) seq.getObjectAt(3)).getValue();
            BigInteger y = ((ASN1Integer) seq.getObjectAt(4)).getValue();
            BigInteger x = ((ASN1Integer) seq.getObjectAt(5)).getValue();
            PrivateKey priv = dsaFactory.generatePrivate(new DSAPrivateKeySpec(x, p, q, g));
            PublicKey pub = dsaFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
            return new KeyPair(pub, priv);
        }
        return null;
    }

    // d2i_DSA_PUBKEY_bio
    public static PublicKey readDSAPublicKey(final byte[] input)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return readDSAPublicKey(SecurityHelper.getKeyFactory("DSA"), input);
    }

    public static PublicKey readDSAPublicKey(final KeyFactory dsaFactory, final byte[] input)
        throws IOException, InvalidKeySpecException {
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(input).readObject();
        if ( seq.size() == 4 ) {
            BigInteger y = ((ASN1Integer) seq.getObjectAt(0)).getValue();
            BigInteger p = ((ASN1Integer) seq.getObjectAt(1)).getValue();
            BigInteger q = ((ASN1Integer) seq.getObjectAt(2)).getValue();
            BigInteger g = ((ASN1Integer) seq.getObjectAt(3)).getValue();
            return dsaFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
        }
        return null;
    }

    // d2i_DHparams_bio
    public static DHParameterSpec readDHParameter(final byte[] input) throws IOException {
        ASN1InputStream aIn = new ASN1InputStream(input);
        ASN1Sequence seq = (ASN1Sequence) aIn.readObject();
        BigInteger p = ((ASN1Integer) seq.getObjectAt(0)).getValue();
        BigInteger g = ((ASN1Integer) seq.getObjectAt(1)).getValue();
        return new DHParameterSpec(p, g);
    }

    public static byte[] toDerRSAKey(RSAPublicKey pubKey, RSAPrivateCrtKey privKey) throws IOException {
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        if (pubKey != null && privKey == null) {
            v1.add(new ASN1Integer(pubKey.getModulus()));
            v1.add(new ASN1Integer(pubKey.getPublicExponent()));
        } else {
            v1.add(new ASN1Integer(0));
            v1.add(new ASN1Integer(privKey.getModulus()));
            v1.add(new ASN1Integer(privKey.getPublicExponent()));
            v1.add(new ASN1Integer(privKey.getPrivateExponent()));
            v1.add(new ASN1Integer(privKey.getPrimeP()));
            v1.add(new ASN1Integer(privKey.getPrimeQ()));
            v1.add(new ASN1Integer(privKey.getPrimeExponentP()));
            v1.add(new ASN1Integer(privKey.getPrimeExponentQ()));
            v1.add(new ASN1Integer(privKey.getCrtCoefficient()));
        }
        return new DLSequence(v1).getEncoded();
    }

    public static byte[] toDerDSAKey(DSAPublicKey pubKey, DSAPrivateKey privKey) throws IOException {
        if (pubKey != null && privKey == null) {
            return pubKey.getEncoded();
        } else if (privKey != null && pubKey != null) {
            DSAParams params = privKey.getParams();
            ASN1EncodableVector v1 = new ASN1EncodableVector();
            v1.add(new ASN1Integer(0));
            v1.add(new ASN1Integer(params.getP()));
            v1.add(new ASN1Integer(params.getQ()));
            v1.add(new ASN1Integer(params.getG()));
            v1.add(new ASN1Integer(pubKey.getY()));
            v1.add(new ASN1Integer(privKey.getX()));
            return new DLSequence(v1).getEncoded();
        } else {
            return privKey.getEncoded();
        }
    }

    public static byte[] toDerDHKey(BigInteger p, BigInteger g) throws IOException {
        ASN1EncodableVector v = new ASN1EncodableVector();
        if (p != null) {
            v.add(new ASN1Integer(p));
        }
        if (g != null) {
            v.add(new ASN1Integer(g));
        }
        return new DLSequence(v).getEncoded();
    }
}


