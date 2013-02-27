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
 * Copyright (C) 2013 Matt Hauck <matthauck@gmail.com>
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

import java.util.List;
import java.util.Enumeration;
import java.io.OutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x500.X500Name;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.pkcs.Attribute;

import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;

import org.bouncycastle.pkcs.PKCSException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.security.Signature;
import java.security.SignatureException;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;

public class PKCS10Request {
    
    private X500Name subject;
    private SubjectPublicKeyInfo publicKeyInfo;
    private PKCS10CertificationRequestBuilder builder;
    private PKCS10CertificationRequest signedRequest;
    private boolean valid = false;

    // For generating new requests

    public PKCS10Request(X500Name subject, 
        SubjectPublicKeyInfo publicKeyInfo, 
        List<Attribute> attrs)
    {
        this.subject        = subject;
        this.publicKeyInfo  = publicKeyInfo;

        resetBuilder();
        setAttributes(attrs);
    }

    public PKCS10Request(X500Name subject, 
        PublicKey publicKey,
        List<Attribute> attrs)
    {
        this.subject        = subject;
        this.publicKeyInfo  = makePublicKeyInfo(publicKey);

        resetBuilder();
        setAttributes(attrs);
    }

    // For reading existing requests

    public PKCS10Request(CertificationRequest req) {
        
        subject       = req.getCertificationRequestInfo().getSubject();
        publicKeyInfo = req.getCertificationRequestInfo().getSubjectPublicKeyInfo();
        signedRequest = new PKCS10CertificationRequest(req);
        valid = true;
    }
    public PKCS10Request(byte[] bytes) {
        this(CertificationRequest.getInstance(bytes));
    }
    public PKCS10Request(ASN1Sequence sequence) {
        this(CertificationRequest.getInstance(sequence));
    }

    // sign

    public PKCS10CertificationRequest sign(PrivateKey privateKey, 
        AlgorithmIdentifier sigAlg) 
        throws IOException
    {
        ContentSigner signer;
        try {
            signer = new PKCS10Signer(privateKey, sigAlg);
        } catch (Exception e) {
            throw new IOException("Could not create PKCS10 signer: " + e);
        }
        signedRequest = builder.build(signer);
        valid = true;

        return signedRequest;
    }
    public PKCS10CertificationRequest sign(PrivateKey privateKey, String digestAlg) 
        throws IOException
    {
        PublicKey pk = getPublicKey();

        String sigAlg = digestAlg + "WITH" + pk.getAlgorithm();

        return sign(
            privateKey,
            new DefaultSignatureAlgorithmIdentifierFinder().find( sigAlg )
        );
    }
    
    // verify

    public boolean verify(PublicKey publicKey) throws IOException, InvalidKeyException {
        if (signedRequest == null) return false;
        if (!isValid()) return false;

        try {
            ContentVerifierProvider verifier = new PKCS10VerifierProvider( publicKey );
            return signedRequest.isSignatureValid( verifier );
        } catch (Exception e) {
            throw new IOException("Error verifying signature: " + e);
        }
    }

    // privates

    private void resetBuilder() {
        builder = new PKCS10CertificationRequestBuilder(
           subject, publicKeyInfo
        );
        valid = false;
    }

    private boolean isValid() {
        return valid;
    }

    private SubjectPublicKeyInfo makePublicKeyInfo(PublicKey publicKey) {
        if (publicKey == null) 
            return null;
        else
            return SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
    }

    // statics 

    // Have to obey some artificial constraints of the OpenSSL implementation. Stupid.
    public static boolean algorithmMismatch(String keyAlg, String digAlg, String digName) {
        if(("DSA".equalsIgnoreCase(keyAlg) && "MD5".equalsIgnoreCase(digAlg)) || 
           ("RSA".equalsIgnoreCase(keyAlg) && "DSS1".equals(digName)) ||
           ("DSA".equalsIgnoreCase(keyAlg) && "SHA1".equals(digName))) {
            return true;
        } else {
            return false;
        }
    }

    // conversion

    public ASN1Sequence toASN1Structure() {
        // TODO: outputting previous structure without checking isValid() is weird...
        if (signedRequest != null) 
            return ASN1Sequence.getInstance(signedRequest.toASN1Structure());
        else
            return new DLSequence();
    }
    
    // getters and setters

    public void setSubject(X500Name subject) {
        this.subject = subject;
        resetBuilder();
    }

    public X500Name getSubject() {
        return subject;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKeyInfo = makePublicKeyInfo(publicKey);
        resetBuilder();
    }
    
    public PublicKey getPublicKey() throws IOException {
        
        AsymmetricKeyParameter keyParams = PublicKeyFactory.createKey(publicKeyInfo);

        KeySpec keySpec = null;
        KeyFactory keyFact = null;

        try {
            if (keyParams instanceof RSAKeyParameters) {
                RSAKeyParameters rsa = (RSAKeyParameters) keyParams;
                keySpec = new RSAPublicKeySpec(
                    rsa.getModulus(), rsa.getExponent()
                );
                keyFact = KeyFactory.getInstance("RSA");

            } else if (keyParams instanceof DSAPublicKeyParameters) {
                DSAPublicKeyParameters dsa = (DSAPublicKeyParameters) keyParams;
                DSAParameters params = dsa.getParameters();
                keySpec = new DSAPublicKeySpec(
                    dsa.getY(), params.getP(), params.getQ(), params.getG()
                );
                keyFact = KeyFactory.getInstance("DSA");
            }

            if (keySpec != null && keyFact != null) {
                return keyFact.generatePublic(keySpec);
            }
        }
        catch (NoSuchAlgorithmException e) { } 
        catch (InvalidKeySpecException e) { } 
        
        throw new IOException("Could not read public key");
    }

    public Attribute[] getAttributes() {
        return (signedRequest != null) ? signedRequest.getAttributes() : new Attribute[0];
    }

    public void setAttributes(List<Attribute> attrs) {
        resetBuilder();
        addAttributes(attrs);
    }
    
    private void addAttributes(List<Attribute> attrs) {
        if (attrs == null) return;

        for(Attribute attr : attrs) {
            addAttribute(attr);
        }
    }
    public void addAttribute(Attribute attr) {
        for (ASN1Encodable value : attr.getAttributeValues()) {
          addAttribute(attr.getAttrType(), value);
        }
    }
    public void addAttribute(ASN1ObjectIdentifier oid, ASN1Encodable value) {
        valid = false;
        builder.addAttribute(oid, value);
    }

    public int getVersion() {
        if (!isValid()) return 0;

        return signedRequest.toASN1Structure().getCertificationRequestInfo()
                    .getVersion().getValue().intValue();
    }
 

    private class PKCS10Signer implements ContentSigner
    {
        AlgorithmIdentifier sigAlg;
        Signature sig;
        SignatureOutputStream sigOut;

        public PKCS10Signer(PrivateKey pkey, AlgorithmIdentifier sigAlg) 
            throws NoSuchAlgorithmException, InvalidKeyException
        {
            this.sigAlg = sigAlg;
            sig = Signature.getInstance( sigAlg.getAlgorithm().getId() );
            sig.initSign( pkey );
            sigOut = new SignatureOutputStream(sig);
        }

        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return sigAlg;
        }

        public OutputStream getOutputStream() {
            return sigOut;
        }

        public byte[] getSignature() {
            try {
                return sig.sign();
            } catch (SignatureException e) {
                throw new RuntimeException("Could not read signature: " + e);
            }
        }
    }

    private class PKCS10VerifierProvider implements ContentVerifierProvider
    {
        PublicKey publicKey;

        public PKCS10VerifierProvider(PublicKey key) {
            publicKey = key;
        }

        public ContentVerifier get(AlgorithmIdentifier sigAlg) {
            try {
                return new PKCS10Verifier(publicKey, sigAlg);
            } catch (Exception e) {
                throw new RuntimeException("Could not create content verifier: " + e);
            }
        }
        
        public boolean hasAssociatedCertificate() {
            return false;
        }

        public org.bouncycastle.cert.X509CertificateHolder getAssociatedCertificate() {
            return null;
        }
    }

    private class PKCS10Verifier implements ContentVerifier
    {
        AlgorithmIdentifier sigAlg;
        Signature sig;
        SignatureOutputStream sigOut;

        public PKCS10Verifier(PublicKey publicKey, AlgorithmIdentifier sigAlg) 
            throws NoSuchAlgorithmException, InvalidKeyException
        {
            this.sigAlg = sigAlg;
            sig = Signature.getInstance( sigAlg.getAlgorithm().getId() );
            sig.initVerify( publicKey );
            sigOut = new SignatureOutputStream(sig);
        }

        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return sigAlg;
        }

        public OutputStream getOutputStream() {
            return sigOut;
        }

        public boolean verify(byte[] expected) {
            try {
                return sig.verify( expected );
            } catch (SignatureException e) {
                throw new RuntimeException("Could not verify signature: " + e);
            }
        }
    }

    private class SignatureOutputStream extends OutputStream
    {
        private Signature sig;

        public SignatureOutputStream(Signature sig) {
            this.sig = sig;
        }
        
        public void write(byte[] bytes, int off, int len) throws IOException {
            try {
                sig.update(bytes, off, len);
            } catch (SignatureException e) {
                throw new IOException("exception in pkcs10 signer: " + e.getMessage(), e);
            }
        }

        public void write(byte[] bytes) throws IOException {
            try {
                sig.update(bytes);
            } catch (SignatureException e) {
                throw new IOException("exception in pkcs10 signer: " + e.getMessage(), e);
            }
        }

        public void write(int b) throws IOException {
            try {
                sig.update((byte)b);
            } catch (SignatureException e) {
                throw new IOException("exception in pkcs10 signer: " + e.getMessage(), e);
            }
        }
    }
}

