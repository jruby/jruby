/*
Copyright (c) 2000-2014 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
 */
package org.jruby.ext.openssl.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import org.jruby.ext.openssl.SecurityHelper;

/**
*
* Handles NetScape certificate request (KEYGEN), these are constructed as:
* <pre><code>
* SignedPublicKeyAndChallenge ::= SEQUENCE {
* publicKeyAndChallenge PublicKeyAndChallenge,
* signatureAlgorithm AlgorithmIdentifier,
* signature BIT STRING
* }
* </pre>
*
* PublicKey's encoded-format has to be X.509.
*
* <br/>
* Copy-pasted from BC JCE's NetscapeCertRequest (http://git.io/vpGXPA).
*
* @note This code avoids Java Security public API calls such as
* <code>KeyFactory.getInstance(keyAlgorithm, "BC")</code> that depend on the
* provider being registered.
**/
public class NetscapeCertRequest // extends ASN1Object
{
    AlgorithmIdentifier sigAlg;
    AlgorithmIdentifier keyAlg;
    byte sigBits [];
    String challenge;
    DERBitString content;
    PublicKey pubkey ;

    private static ASN1Sequence getReq(
        byte[] r)
        throws IOException
    {
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(r));

        return ASN1Sequence.getInstance(aIn.readObject());
    }

    public NetscapeCertRequest(
        byte[] req)
        throws IOException
    {
        this(getReq(req));
    }

    public NetscapeCertRequest (ASN1Sequence spkac)
    {
        try
        {

            //
            // SignedPublicKeyAndChallenge ::= SEQUENCE {
            // publicKeyAndChallenge PublicKeyAndChallenge,
            // signatureAlgorithm AlgorithmIdentifier,
            // signature BIT STRING
            // }
            //
            if (spkac.size() != 3)
            {
                throw new IllegalArgumentException("invalid SPKAC (size):"
                        + spkac.size());
            }

            sigAlg = new AlgorithmIdentifier((ASN1Sequence)spkac
                    .getObjectAt(1));
            sigBits = ((DERBitString)spkac.getObjectAt(2)).getBytes();

            //
            // PublicKeyAndChallenge ::= SEQUENCE {
            // spki SubjectPublicKeyInfo,
            // challenge IA5STRING
            // }
            //
            ASN1Sequence pkac = (ASN1Sequence)spkac.getObjectAt(0);

            if (pkac.size() != 2)
            {
                throw new IllegalArgumentException("invalid PKAC (len): "
                        + pkac.size());
            }

            challenge = ((DERIA5String)pkac.getObjectAt(1)).getString();

            //this could be dangerous, as ASN.1 decoding/encoding
            //could potentially alter the bytes
            content = new DERBitString(pkac);

            SubjectPublicKeyInfo pubkeyinfo = new SubjectPublicKeyInfo(
                    (ASN1Sequence)pkac.getObjectAt(0));

            X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(
                    pubkeyinfo).getBytes());

            keyAlg = pubkeyinfo.getAlgorithmId();
            String keyAlgorithm = keyAlg.getObjectId().getId();
            // NOTE: KeyFactory.getInstance(keyAlgorithm, "BC")
            KeyFactory keyFactory = SecurityHelper.getKeyFactory(keyAlgorithm);
            pubkey = keyFactory.generatePublic(xspec);

        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e.toString());
        }
    }

    public NetscapeCertRequest(
        String challenge,
        AlgorithmIdentifier signing_alg,
        PublicKey pub_key) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException
    {

        this.challenge = challenge;
        sigAlg = signing_alg;
        pubkey = pub_key;

        ASN1EncodableVector content_der = new ASN1EncodableVector();
        content_der.add(getKeySpec());
        //content_der.add(new SubjectPublicKeyInfo(sigAlg, new RSAPublicKeyStructure(pubkey.getModulus(), pubkey.getPublicExponent()).getDERObject()));
        content_der.add(new DERIA5String(challenge));

        try
        {
            content = new DERBitString(new DERSequence(content_der));
        }
        catch (Exception e) // IOException
        {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new InvalidKeySpecException("exception encoding key: " + e.toString());
        }
    }

    public String getChallenge()
    {
        return challenge;
    }

    public void setChallenge(String value)
    {
        challenge = value;
    }

    public AlgorithmIdentifier getSigningAlgorithm()
    {
        return sigAlg;
    }

    public void setSigningAlgorithm(AlgorithmIdentifier value)
    {
        sigAlg = value;
    }

    public AlgorithmIdentifier getKeyAlgorithm()
    {
        return keyAlg;
    }

    public void setKeyAlgorithm(AlgorithmIdentifier value)
    {
        keyAlg = value;
    }

    public PublicKey getPublicKey()
    {
        return pubkey;
    }

    public void setPublicKey(PublicKey value)
    {
        pubkey = value;
    }

    public boolean verify(String challenge) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException
    {
        if (!challenge.equals(this.challenge))
        {
            return false;
        }

        //
        // Verify the signature .. shows the response was generated
        // by someone who knew the associated private key
        //
        String signAlgorithm = sigAlg.getObjectId().getId();
        // NOTE: Signature.getInstance(signAlgorithm, "BC");
        Signature sig = SecurityHelper.getSignature(signAlgorithm);
        sig.initVerify(pubkey);
        sig.update(content.getBytes());

        return sig.verify(sigBits);
    }

    public void sign(PrivateKey priv_key) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException, NoSuchProviderException,
            InvalidKeySpecException
    {
        sign(priv_key, null);
    }

    public void sign(PrivateKey priv_key, SecureRandom rand)
            throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, NoSuchProviderException,
            InvalidKeySpecException
    {
        String signAlgorithm = sigAlg.getObjectId().getId();
        // NOTE: Signature.getInstance(signAlgorithm, "BC");
        Signature sig = SecurityHelper.getSignature(signAlgorithm);

        if (rand != null)
        {
            sig.initSign(priv_key, rand);
        }
        else
        {
            sig.initSign(priv_key);
        }

        ASN1EncodableVector pkac = new ASN1EncodableVector();

        pkac.add(getKeySpec());
        pkac.add(new DERIA5String(challenge));

        try
        {
            sig.update(new DERSequence(pkac).getEncoded(ASN1Encoding.DER));
        }
        catch (IOException ioe)
        {
            throw new SignatureException(ioe.getMessage());
        }

        sigBits = sig.sign();
    }

    private ASN1Primitive getKeySpec() throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ASN1Primitive obj = null;
        try
        {

            baos.write(pubkey.getEncoded());
            baos.close();

            ASN1InputStream derin = new ASN1InputStream(
                    new ByteArrayInputStream(baos.toByteArray()));

            obj = derin.readObject();
        }
        catch (IOException ioe)
        {
            throw new InvalidKeySpecException(ioe.getMessage());
        }
        return obj;
    }

    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector spkac = new ASN1EncodableVector();
        ASN1EncodableVector pkac = new ASN1EncodableVector();

        try
        {
            pkac.add(getKeySpec());
        }
        catch (Exception e)
        {
            //ignore
        }

        pkac.add(new DERIA5String(challenge));

        spkac.add(new DERSequence(pkac));
        spkac.add(sigAlg);
        spkac.add(new DERBitString(sigBits));

        return new DERSequence(spkac);
    }

}
