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
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

/** PKCS7_SIGNED
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Signed {
    /**
     * Describe version here.
     */
    private int version;

    /**
     * Describe crl here.
     */
    private Collection<X509CRL> crl = new ArrayList<X509CRL>();

    /**
     * Describe cert here.
     */
    private Collection<X509AuxCertificate> cert = new ArrayList<X509AuxCertificate>();

    /**
     * Describe mdAlgs here.
     */
    private Set<AlgorithmIdentifier> mdAlgs = new HashSet<AlgorithmIdentifier>();

    /**
     * Describe signerInfo here.
     */
    private Collection<SignerInfoWithPkey> signerInfo = new ArrayList<SignerInfoWithPkey>();

    PKCS7 contents;

    /**
     * Get the <code>Version</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getVersion() {
        return version;
    }

    /**
     * Set the <code>Version</code> value.
     *
     * @param newVersion The new Version value.
     */
    public final void setVersion(final int newVersion) {
        this.version = newVersion;
    }

    /**
     * Get the <code>SignerInfo</code> value.
     *
     * @return a <code>Collection<SignerInfoWithPkey></code> value
     */
    public final Collection<SignerInfoWithPkey> getSignerInfo() {
        return signerInfo;
    }

    /**
     * Set the <code>SignerInfo</code> value.
     *
     * @param newSignerInfo The new SignerInfo value.
     */
    public final void setSignerInfo(final Collection<SignerInfoWithPkey> newSignerInfo) {
        this.signerInfo = newSignerInfo;
    }

    /**
     * Get the <code>MdAlgs</code> value.
     *
     * @return a <code>Set<AlgorithmIdentifier></code> value
     */
    public final Set<AlgorithmIdentifier> getMdAlgs() {
        return mdAlgs;
    }

    /**
     * Set the <code>MdAlgs</code> value.
     *
     * @param newMdAlgs The new MdAlgs value.
     */
    public final void setMdAlgs(final Set<AlgorithmIdentifier> newMdAlgs) {
        this.mdAlgs = newMdAlgs;
    }

    /**
     * Get the <code>Contents</code> value.
     *
     * @return a <code>PKCS7</code> value
     */
    public final PKCS7 getContents() {
        return contents;
    }

    /**
     * Set the <code>Contents</code> value.
     *
     * @param newContents The new Contents value.
     */
    public final void setContents(final PKCS7 newContents) {
        this.contents = newContents;
    }

    /**
     * Get the <code>Cert</code> value.
     *
     * @return a <code>Collection<X509AuxCertificate></code> value
     */
    public final Collection<X509AuxCertificate> getCert() {
        return cert;
    }

    /**
     * Set the <code>Cert</code> value.
     *
     * @param newCert The new Cert value.
     */
    public final void setCert(final Collection<X509AuxCertificate> newCert) {
        this.cert = newCert;
    }

    /**
     * Get the <code>Crl</code> value.
     *
     * @return a <code>Set<X509CRL></code> value
     */
    public final Collection<X509CRL> getCrl() {
        return crl;
    }

    /**
     * Set the <code>Crl</code> value.
     *
     * @param newCrl The new Crl value.
     */
    public final void setCrl(final Collection<X509CRL> newCrl) {
        this.crl = newCrl;
    }

    @Override
    public String toString() {
        return "#<Signed version=" + version + " mdAlgs="+mdAlgs+" content="+contents+" cert="+cert+" crls="+crl+" signerInfos="+signerInfo+">";
    }

    public ASN1Encodable asASN1() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(new DERInteger(version));
        vector.add(digestAlgorithmsToASN1Set());
        vector.add(contents.asASN1());
        if (cert != null && cert.size() > 0) {
            if (cert.size() > 1) {
                vector.add(new DERTaggedObject(false, 0, certificatesToASN1Set()));
            } else {
                // Encode the signer certificate directly for OpenSSL compatibility.
                // OpenSSL does not support multiple signer signature.
                // And OpenSSL requires EXPLICIT tagging.
                vector.add(new DERTaggedObject(true, 0, firstCertificatesToASN1()));
            }
        }
        if (crl != null && crl.size() > 0) {
            vector.add(new DERTaggedObject(false, 1, crlsToASN1Set()));
        }
        vector.add(signerInfosToASN1Set());
        return new DERSequence(vector);
    }

    private ASN1Set digestAlgorithmsToASN1Set() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        for(AlgorithmIdentifier ai : mdAlgs) {
            vector.add(ai.toASN1Object());
        }
        return new DERSet(vector);
    }

    // This imlementation is stupid and wasteful. Ouch.
    private ASN1Set certificatesToASN1Set() {
        try {
            ASN1EncodableVector vector = new ASN1EncodableVector();
            for(X509AuxCertificate c : cert) {
                vector.add(new ASN1InputStream(new ByteArrayInputStream(c.getEncoded())).readObject());
            }
            return new DERSet(vector);
        } catch(Exception e) {
            return null;
        }
    }

    private DERSequence firstCertificatesToASN1() {
        try {
            X509AuxCertificate c = cert.iterator().next();
            return (DERSequence) (new ASN1InputStream(new ByteArrayInputStream(c.getEncoded())).readObject());
        } catch (Exception e) {}
        return null;
    }

    private ASN1Set crlsToASN1Set() {
        throw new RuntimeException("TODO: implement CRL part");
    }

    private ASN1Set signerInfosToASN1Set() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        for(SignerInfoWithPkey si : signerInfo) {
            vector.add(si.toASN1Object());
        }
        return new DERSet(vector);
    }

    /**
     * SignedData ::= SEQUENCE {
     *   version Version,
     *   digestAlgorithms DigestAlgorithmIdentifiers,
     *   contentInfo ContentInfo,
     *   certificates [0] IMPLICIT ExtendedCertificatesAndCertificates OPTIONAL,
     *   crls [1] IMPLICIT CertificateRevocationLists OPTIONAL,
     *   signerInfos SignerInfos }
     *
     * Version ::= INTEGER
     *
     * DigestAlgorithmIdentifiers ::= SET OF DigestAlgorithmIdentifier
     *
     * SignerInfos ::= SET OF SignerInfo
     */
    public static Signed fromASN1(DEREncodable content) throws PKCS7Exception{
        ASN1Sequence sequence = (ASN1Sequence)content;
        DERInteger version = (DERInteger)sequence.getObjectAt(0);
        ASN1Set digestAlgos = (ASN1Set)sequence.getObjectAt(1);
        DEREncodable contentInfo = sequence.getObjectAt(2);

        DEREncodable certificates = null;
        DEREncodable crls = null;

        int index = 3;
        DEREncodable tmp = sequence.getObjectAt(index);
        if((tmp instanceof DERTaggedObject) && ((DERTaggedObject)tmp).getTagNo() == 0) {
            certificates = ((DERTaggedObject)tmp).getObject();
            index++;
        }

        tmp = sequence.getObjectAt(index);
        if((tmp instanceof DERTaggedObject) && ((DERTaggedObject)tmp).getTagNo() == 1) {
            crls = ((DERTaggedObject)tmp).getObject();
            index++;
        }

        ASN1Set signerInfos = (ASN1Set)sequence.getObjectAt(index);

        Signed signed = new Signed();
        signed.setVersion(version.getValue().intValue());
        signed.setMdAlgs(algorithmIdentifiersFromASN1Set(digestAlgos));
        signed.setContents(PKCS7.fromASN1(contentInfo));
        if(certificates != null) {
            signed.setCert(certificatesFromASN1Set(certificates));
        }
        if(crls != null) {
            throw new RuntimeException("TODO: implement CRL part");
        }
        signed.setSignerInfo(signerInfosFromASN1Set(signerInfos));

        return signed;
    }

    private static Collection<X509AuxCertificate> certificatesFromASN1Set(DEREncodable content) throws PKCS7Exception {
        Collection<X509AuxCertificate> result = new ArrayList<X509AuxCertificate>();
        if (content instanceof DERSequence) {
            try {
                for (Enumeration<?> enm = ((DERSequence) content).getObjects(); enm.hasMoreElements();) {
                    DEREncodable current = (DEREncodable) enm.nextElement();
                    result.add(certificateFromASN1(current));
                }
            } catch (IllegalArgumentException iae) {
                result.add(certificateFromASN1(content));
            }
        } else if (content instanceof DERSet) {
            // EXPLICIT Set shouldn't apper here but keep this for backward compatibility.
            for (Enumeration<?> enm = ((DERSet) content).getObjects(); enm.hasMoreElements();) {
                DEREncodable current = (DEREncodable) enm.nextElement();
                result.add(certificateFromASN1(current));
            }
        } else {
            throw new PKCS7Exception(PKCS7.F_B64_READ_PKCS7, PKCS7.R_CERTIFICATE_VERIFY_ERROR, "unknown certificates format");
        }
        return result;
    }

    private static X509AuxCertificate certificateFromASN1(DEREncodable current) throws PKCS7Exception {
        X509CertificateStructure struct = X509CertificateStructure.getInstance(current);
        try {
            return new X509AuxCertificate(new X509CertificateObject(struct));
        } catch (CertificateParsingException cpe) {
            throw new PKCS7Exception(PKCS7.F_B64_READ_PKCS7, PKCS7.R_CERTIFICATE_VERIFY_ERROR, cpe);
        }
    }

    private static Set<AlgorithmIdentifier> algorithmIdentifiersFromASN1Set(DEREncodable content) {
        ASN1Set set = (ASN1Set)content;
        Set<AlgorithmIdentifier> result = new HashSet<AlgorithmIdentifier>();
        for(Enumeration<?> e = set.getObjects(); e.hasMoreElements();) {
            result.add(AlgorithmIdentifier.getInstance(e.nextElement()));
        }
        return result;
    }

    private static Collection<SignerInfoWithPkey> signerInfosFromASN1Set(DEREncodable content) {
        ASN1Set set = (ASN1Set)content;
        Collection<SignerInfoWithPkey> result = new ArrayList<SignerInfoWithPkey>();
        for(Enumeration<?> e = set.getObjects(); e.hasMoreElements();) {
            result.add(SignerInfoWithPkey.getInstance(e.nextElement()));
        }
        return result;
    }
}// Signed
