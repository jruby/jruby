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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.IssuerAndSerialNumber;
import org.bouncycastle.asn1.pkcs.SignerInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class SignerInfoWithPkey implements ASN1Encodable {
    private ASN1Integer              version;
    private IssuerAndSerialNumber   issuerAndSerialNumber;
    private AlgorithmIdentifier     digAlgorithm;
    private ASN1Set                 authenticatedAttributes;
    private AlgorithmIdentifier     digEncryptionAlgorithm;
    private ASN1OctetString         encryptedDigest;
    private ASN1Set                 unauthenticatedAttributes;

    public static SignerInfoWithPkey getInstance(Object o) {
        if(o instanceof SignerInfo) {
            return (SignerInfoWithPkey)o;
        } else if (o instanceof ASN1Sequence) {
            return new SignerInfoWithPkey((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }

    public SignerInfoWithPkey dup() {
        SignerInfoWithPkey copy = new SignerInfoWithPkey(version,
                                                         issuerAndSerialNumber,
                                                         digAlgorithm,
                                                         authenticatedAttributes,
                                                         digEncryptionAlgorithm,
                                                         encryptedDigest,
                                                         unauthenticatedAttributes);
        copy.pkey = pkey;
        return copy;
    }

    SignerInfoWithPkey() {
    }

    public SignerInfoWithPkey(ASN1Integer              version,
        IssuerAndSerialNumber   issuerAndSerialNumber,
        AlgorithmIdentifier     digAlgorithm,
        ASN1Set                 authenticatedAttributes,
        AlgorithmIdentifier     digEncryptionAlgorithm,
        ASN1OctetString         encryptedDigest,
        ASN1Set                 unauthenticatedAttributes) {
        this.version = version;
        this.issuerAndSerialNumber = issuerAndSerialNumber;
        this.digAlgorithm = digAlgorithm;
        this.authenticatedAttributes = authenticatedAttributes;
        this.digEncryptionAlgorithm = digEncryptionAlgorithm;
        this.encryptedDigest = encryptedDigest;
        this.unauthenticatedAttributes = unauthenticatedAttributes;
    }

    public SignerInfoWithPkey(ASN1Sequence seq) {
        Enumeration     e = seq.getObjects();

        version = (ASN1Integer)e.nextElement();
        issuerAndSerialNumber = IssuerAndSerialNumber.getInstance(e.nextElement());
        digAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());

        Object obj = e.nextElement();

        if(obj instanceof ASN1TaggedObject) {
            authenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject)obj, false);

            digEncryptionAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        }
        else {
            authenticatedAttributes = null;
            digEncryptionAlgorithm = AlgorithmIdentifier.getInstance(obj);
        }

        encryptedDigest = ASN1OctetString.getInstance(e.nextElement());

        if(e.hasMoreElements()) {
            unauthenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject)e.nextElement(), false);
        }
        else {
            unauthenticatedAttributes = null;
        }
    }

    public ASN1Integer getVersion() {
        return version;
    }

    public IssuerAndSerialNumber getIssuerAndSerialNumber() {
        return issuerAndSerialNumber;
    }

    public ASN1Set getAuthenticatedAttributes() {
        return authenticatedAttributes;
    }

    public AlgorithmIdentifier getDigestAlgorithm() {
        return digAlgorithm;
    }

    public ASN1OctetString getEncryptedDigest() {
        return encryptedDigest;
    }

    public AlgorithmIdentifier getDigestEncryptionAlgorithm() {
        return digEncryptionAlgorithm;
    }

    public ASN1Set getUnauthenticatedAttributes() {
        return unauthenticatedAttributes;
    }

    /* c: PKCS7_SIGNER_INFO_set
     *
     */
    public void set(X509AuxCertificate x509, PrivateKey pkey, MessageDigest dgst) throws PKCS7Exception {
        boolean dsa = (pkey instanceof DSAPrivateKey) || (pkey instanceof ECPrivateKey);

        version = new ASN1Integer(1);

        X500Name issuer = X500Name.getInstance(x509.getIssuerX500Principal().getEncoded());
        BigInteger serial = x509.getSerialNumber();
        issuerAndSerialNumber = new IssuerAndSerialNumber(issuer, serial);

        this.pkey = pkey;

        if ( dsa ) {
            digAlgorithm = new AlgorithmIdentifier(ASN1Registry.OID_sha1);
        } else {
            digAlgorithm = new AlgorithmIdentifier(ASN1Registry.nid2obj(EVP.type(dgst)));
        }

        if(pkey instanceof RSAPrivateKey) {
            digEncryptionAlgorithm = new AlgorithmIdentifier(ASN1Registry.OID_rsaEncryption);
        } else if(pkey instanceof DSAPrivateKey) {
            digEncryptionAlgorithm = new AlgorithmIdentifier(ASN1Registry.OID_dsa);
        } else if(pkey instanceof ECPrivateKey) {
            digEncryptionAlgorithm = new AlgorithmIdentifier(ASN1Registry.OID_ecdsa_with_SHA1);
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  SignerInfo ::= SEQUENCE {
     *      version Version,
     *      issuerAndSerialNumber IssuerAndSerialNumber,
     *      digestAlgorithm DigestAlgorithmIdentifier,
     *      authenticatedAttributes [0] IMPLICIT Attributes OPTIONAL,
     *      digestEncryptionAlgorithm DigestEncryptionAlgorithmIdentifier,
     *      encryptedDigest EncryptedDigest,
     *      unauthenticatedAttributes [1] IMPLICIT Attributes OPTIONAL
     *  }
     *
     *  EncryptedDigest ::= OCTET STRING
     *
     *  DigestAlgorithmIdentifier ::= AlgorithmIdentifier
     *
     *  DigestEncryptionAlgorithmIdentifier ::= AlgorithmIdentifier
     * </pre>
     */
    public ASN1Encodable toASN1Object() {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(version);
        v.add(issuerAndSerialNumber);
        v.add(digAlgorithm);

        if (authenticatedAttributes != null) {
            v.add(new DERTaggedObject(false, 0, authenticatedAttributes));
        }

        v.add(digEncryptionAlgorithm);
        v.add(encryptedDigest);

        if (unauthenticatedAttributes != null) {
            v.add(new DERTaggedObject(false, 1, unauthenticatedAttributes));
        }

        return new DLSequence(v);
    }

    /**
     * Describe pkey here.
     */
    private PrivateKey pkey;

    /**
     * Get the <code>Pkey</code> value.
     *
     * @return a <code>PrivateKey</code> value
     */
    public final PrivateKey getPkey() {
        return pkey;
    }

    /**
     * Set the <code>Pkey</code> value.
     *
     * @param newPkey The new Pkey value.
     */
    public final void setPkey(final PrivateKey newPkey) {
        this.pkey = newPkey;
    }

    public void setAuthenticatedAttributes(ASN1Set authAttr) {
        this.authenticatedAttributes = authAttr;
    }

    public void setUnauthenticatedAttributes(ASN1Set unauthAttr) {
        this.unauthenticatedAttributes = unauthAttr;
    }

    public void setEncryptedDigest(ASN1OctetString encryptedDigest) {
        this.encryptedDigest = encryptedDigest;
    }

    /** c: PKCS7_get_signed_attribute
     *
     */
    public ASN1Encodable getSignedAttribute(int nid) {
        return getAttribute(this.authenticatedAttributes, nid);
    }

    /** c: PKCS7_get_attribute
     *
     */
    public ASN1Encodable getAttribute(int nid) {
        return getAttribute(this.unauthenticatedAttributes, nid);
    }

    /** c: static get_attribute
     *
     */
    static ASN1Encodable getAttribute(ASN1Set sk, int nid) {
        final ASN1ObjectIdentifier oid = ASN1Registry.nid2obj(nid);

        if ( oid == null || sk == null ) return null;

        for ( Enumeration e = sk.getObjects(); e.hasMoreElements(); ) {
            Attribute xa = Attribute.getInstance( e.nextElement() );
            if ( oid.equals(xa.getAttrType()) ) {
                if ( xa.getAttrValues().size() > 0 ) {
                    return xa.getAttrValues().getObjectAt(0);
                }
                return null;
            }
        }
        return null;
    }

    /** c: PKCS7_add_signed_attribute
     *
     */
    public void addSignedAttribute(int atrType, ASN1Encodable value) {
        this.authenticatedAttributes = addAttribute(this.authenticatedAttributes, atrType, value);
    }

    /** c: PKCS7_add_attribute
     *
     */
    public void addAttribute(int atrType, ASN1Encodable value) {
        this.unauthenticatedAttributes = addAttribute(this.unauthenticatedAttributes, atrType, value);
    }

    /** c: static add_attribute
     *
     */
    private ASN1Set addAttribute(ASN1Set base, int atrType, ASN1Encodable value) {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        if ( base == null ) base = new DERSet();
        Attribute attr;
        for ( Enumeration e = base.getObjects(); e.hasMoreElements(); ) {
            attr = Attribute.getInstance( e.nextElement() );
            if ( ASN1Registry.obj2nid(attr.getAttrType()) != atrType ) {
                vector.add(attr);
            }
        }
        ASN1ObjectIdentifier ident = ASN1Registry.nid2obj(atrType);
        attr = new Attribute(ident, new DERSet(value));
        vector.add(attr);
        return new DERSet(vector);
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}// SignerInfoWithPkey
