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
import java.io.IOException;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
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
    private AlgorithmIdentifier sigAlg;
    private AlgorithmIdentifier keyAlg;
    private byte[] signatureBits;
    private final String challenge;
    private final DERBitString content;
    private PublicKey publicKey;

    public NetscapeCertRequest(final byte[] request) throws NoSuchAlgorithmException,
        InvalidKeySpecException, IllegalArgumentException {

        ASN1InputStream input = new ASN1InputStream( new ByteArrayInputStream(request) );
        ASN1Sequence spkac;
        try {
            spkac = ASN1Sequence.getInstance( input.readObject() );
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        //
        // SignedPublicKeyAndChallenge ::= SEQUENCE {
        // publicKeyAndChallenge PublicKeyAndChallenge,
        // signatureAlgorithm AlgorithmIdentifier,
        // signature BIT STRING
        // }
        //
        if ( spkac.size() != 3 ) {
            throw new IllegalArgumentException("invalid SPKAC (size):" + spkac.size());
        }

        final ASN1Sequence signatureId = (ASN1Sequence) spkac.getObjectAt(1);
        this.sigAlg = AlgorithmIdentifier.getInstance(signatureId);
        this.signatureBits = ((DERBitString) spkac.getObjectAt(2)).getBytes();

        //
        // PublicKeyAndChallenge ::= SEQUENCE {
        // spki SubjectPublicKeyInfo,
        // challenge IA5STRING
        // }
        //
        ASN1Sequence pkac = (ASN1Sequence) spkac.getObjectAt(0);

        if ( pkac.size() != 2 ) {
            throw new IllegalArgumentException("invalid PKAC (len): " + pkac.size());
        }

        this.challenge = ((DERIA5String) pkac.getObjectAt(1)).getString();

        final String keyAlgorithm; final X509EncodedKeySpec encodedKeySpec;
        try {
            //this could be dangerous, as ASN.1 decoding/encoding
            //could potentially alter the bytes
            this.content = new DERBitString(pkac);

            final SubjectPublicKeyInfo pubKeyInfo =
                new SubjectPublicKeyInfo((ASN1Sequence) pkac.getObjectAt(0));

            encodedKeySpec = new X509EncodedKeySpec( new DERBitString(pubKeyInfo).getBytes() );

            this.keyAlg = pubKeyInfo.getAlgorithm();
            keyAlgorithm = keyAlg.getAlgorithm().getId();
        }
        catch (Exception e) {
            // new DERBitString throw IOExcetpion since BC 1.49
            //if ( e instanceof IOException ) {
            //    throw new IllegalArgumentException(e);
            //}
            if ( e instanceof RuntimeException ) throw (RuntimeException) e;
            throw new IllegalArgumentException(e);
        }

        KeyFactory keyFactory = SecurityHelper.getKeyFactory(keyAlgorithm);
        this.publicKey = keyFactory.generatePublic(encodedKeySpec);
    }

    public NetscapeCertRequest(final String challenge, final AlgorithmIdentifier signingAlg,
        final PublicKey publicKey) throws InvalidKeySpecException {

        this.challenge = challenge;
        this.sigAlg = signingAlg;
        this.publicKey = publicKey;

        ASN1EncodableVector contentDER = new ASN1EncodableVector();
        try {
            contentDER.add(getKeySpec());
        }
        catch (IOException e) {
            throw new InvalidKeySpecException(e);
        }
        //content_der.add(new SubjectPublicKeyInfo(sigAlg, new RSAPublicKeyStructure(pubkey.getModulus(), pubkey.getPublicExponent()).getDERObject()));
        contentDER.add(new DERIA5String(challenge));

        try {
            this.content = new DERBitString(new DERSequence(contentDER));
        }
        catch (Exception e) {
            // new DERBitString throw IOExcetpion since BC 1.49
            if ( e instanceof RuntimeException ) throw (RuntimeException) e;
            throw new InvalidKeySpecException("exception encoding key: " + e.toString());
        }
    }

    public String getChallenge()
    {
        return challenge;
    }

    /*
    public void setChallenge(String value)
    {
        challenge = value;
    } */

    public AlgorithmIdentifier getSigningAlgorithm()
    {
        return sigAlg;
    }

    /*
    public void setSigningAlgorithm(AlgorithmIdentifier value)
    {
        sigAlg = value;
    } */

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
        return publicKey;
    }

    public void setPublicKey(PublicKey value)
    {
        publicKey = value;
    }

    public boolean verify(String challenge) throws NoSuchAlgorithmException,
        InvalidKeyException, SignatureException
    {
        if ( ! challenge.equals(this.challenge) ) return false;
        //
        // Verify the signature .. shows the response was generated
        // by someone who knew the associated private key
        //
        final Signature signature = getSignature();

        signature.initVerify(publicKey);
        signature.update(content.getBytes());

        return signature.verify(signatureBits);
    }

    public void sign(final PrivateKey privateKey) throws NoSuchAlgorithmException,
        InvalidKeyException, SignatureException, InvalidKeySpecException
    {
        sign(privateKey, null);
    }

    public void sign(final PrivateKey privateKey, SecureRandom random)
        throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException
    {
        final Signature signature = getSignature();

        if ( random != null ) {
            signature.initSign(privateKey, random);
        }
        else {
            signature.initSign(privateKey);
        }

        ASN1EncodableVector pkac = new ASN1EncodableVector();

        try {
            pkac.add(getKeySpec());
        }
        catch (IOException e) {
            throw new InvalidKeySpecException(e);
        }
        pkac.add(new DERIA5String(challenge));

        try {
            signature.update(new DERSequence(pkac).getEncoded(ASN1Encoding.DER));
        }
        catch (IOException e) {
            throw new SignatureException(e);
        }

        signatureBits = signature.sign();
    }

    private Signature getSignature() throws NoSuchAlgorithmException {
        String algorithm = sigAlg.getAlgorithm().getId();
        return SecurityHelper.getSignature(algorithm);
    }


    private ASN1Primitive getKeySpec() throws IOException {
        ASN1InputStream input = new ASN1InputStream(
            new ByteArrayInputStream( publicKey.getEncoded() )
        );
        return input.readObject();
    }

    public ASN1Primitive toASN1Primitive() throws IOException {
        ASN1EncodableVector spkac = new ASN1EncodableVector();
        ASN1EncodableVector pkac = new ASN1EncodableVector();

        try {
            pkac.add( getKeySpec() );
        }
        catch (IOException e) {
            // TODO is this really fine shouldn't it be thrown ?
        }

        pkac.add(new DERIA5String(challenge));

        spkac.add(new DERSequence(pkac));
        spkac.add(sigAlg);
        spkac.add(new DERBitString(signatureBits));

        return new DERSequence(spkac);
    }

}
