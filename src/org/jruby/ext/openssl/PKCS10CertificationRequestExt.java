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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import java.io.ByteArrayOutputStream;

import java.math.BigInteger;

import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.PKCS10CertificationRequest;

import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKCS10CertificationRequestExt extends PKCS10CertificationRequest {
    public PKCS10CertificationRequestExt(byte[] bytes) {
        super(bytes);
    }

    public PKCS10CertificationRequestExt(ASN1Sequence sequence) {
        super(sequence);
    }

    public PKCS10CertificationRequestExt(
        String              signatureAlgorithm,
        org.bouncycastle.asn1.x509.X509Name            subject,
        PublicKey           key,
        ASN1Set             attributes,
        PrivateKey          signingKey)
        throws NoSuchAlgorithmException, NoSuchProviderException,
                InvalidKeyException, SignatureException
    {
        super(signatureAlgorithm,subject,key,attributes,signingKey);
    }

    public PKCS10CertificationRequestExt(
        String              signatureAlgorithm,
        X500Principal       subject,
        PublicKey           key,
        ASN1Set             attributes,
        PrivateKey          signingKey)
        throws NoSuchAlgorithmException, NoSuchProviderException,
                InvalidKeyException, SignatureException
    {
        super(signatureAlgorithm,subject,key,attributes,signingKey);
    }

    public PKCS10CertificationRequestExt(
        String              signatureAlgorithm,
        X500Principal       subject,
        PublicKey           key,
        ASN1Set             attributes,
        PrivateKey          signingKey,
        String              provider)
        throws NoSuchAlgorithmException, NoSuchProviderException,
                InvalidKeyException, SignatureException
    {
        super(signatureAlgorithm,subject,key,attributes,signingKey,provider);
    }

    public PKCS10CertificationRequestExt(
        String              signatureAlgorithm,
        org.bouncycastle.asn1.x509.X509Name            subject,
        PublicKey           key,
        ASN1Set             attributes,
        PrivateKey          signingKey,
        String              provider)
        throws NoSuchAlgorithmException, NoSuchProviderException,
                InvalidKeyException, SignatureException
    {
        super(signatureAlgorithm,subject,key,attributes,signingKey,provider);
    }

    public void setAttributes(DERSet attrs) {
        ASN1Sequence seq = (ASN1Sequence)this.reqInfo.toASN1Object();
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        for(int i=0;i<(seq.size()-1);i++) {
            v1.add(seq.getObjectAt(i));
        }
        v1.add(new DERTaggedObject(0,attrs));
        this.reqInfo = new CertificationRequestInfo(new DERSequence(v1));
    }

    public void setVersion(int v) {
        DERInteger nVersion = new DERInteger(v);
        ASN1Sequence seq = (ASN1Sequence)this.reqInfo.toASN1Object();
        ASN1EncodableVector v1 = new ASN1EncodableVector();
        v1.add(nVersion);
        for(int i=1;i<seq.size();i++) {
            v1.add(seq.getObjectAt(i));
        }
        this.reqInfo = new CertificationRequestInfo(new DERSequence(v1));
    }

    public int getVersion() {
        return getCertificationRequestInfo().getVersion().getValue().intValue();
    }

    public boolean verify(PublicKey pubkey) throws Exception {
        Signature   sig = Signature.getInstance(sigAlgId.getObjectId().getId(),"BC");
        sig.initVerify(pubkey);

        try
        {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);

            dOut.writeObject(reqInfo);

            sig.update(bOut.toByteArray());
        }
        catch (Exception e)
        {
            throw new SecurityException("exception encoding TBS cert request - " + e);
        }

        return sig.verify(sigBits.getBytes());
    }
}// PKCS10CertificationRequestExt
