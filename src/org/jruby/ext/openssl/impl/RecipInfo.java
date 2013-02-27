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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.pkcs.IssuerAndSerialNumber;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.jruby.ext.openssl.x509store.Name;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

/** PKCS7_RECIP_INFO
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class RecipInfo {
    private int version;
    private IssuerAndSerialNumber issuerAndSerial;
    private AlgorithmIdentifier keyEncAlgor;
    private ASN1OctetString encKey;

    /**
     * Describe cert here.
     */
    private X509AuxCertificate cert;

    /** c: PKCS7_RECIP_INFO_set
     *
     */
    public void set(X509AuxCertificate cert) throws PKCS7Exception {
        version = 0;
        X500Name issuer = X500Name.getInstance(cert.getIssuerX500Principal().getEncoded());
        BigInteger serial = cert.getSerialNumber();
        issuerAndSerial = new IssuerAndSerialNumber(issuer, serial);
        String algo = addEncryptionIfNeeded(cert.getPublicKey().getAlgorithm());
        keyEncAlgor = new AlgorithmIdentifier(ASN1Registry.sym2oid(algo));
        this.cert = cert;
    }

    private String addEncryptionIfNeeded(String input) {
        input = input.toLowerCase();
        if(input.equals("rsa")) {
            return input + "Encryption";
        } else if(input.equals("dsa")) {
            return input + "Encryption";
        }
        return input;
    }

    @Override
    public boolean equals(Object other) {
        boolean ret = this == other;
        if(!ret && (other instanceof RecipInfo)) {
            RecipInfo o = (RecipInfo)other;
            ret = 
                this.version == o.version &&
                (this.issuerAndSerial == null ? o.issuerAndSerial == null : (this.issuerAndSerial.equals(o.issuerAndSerial))) &&
                (this.keyEncAlgor == null ? o.keyEncAlgor == null : (this.keyEncAlgor.equals(o.keyEncAlgor))) &&
                (this.encKey == null ? o.encKey == null : (this.encKey.equals(o.encKey)));
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int result = 31;
        result = result + 13 * version;
        result = result + ((issuerAndSerial == null) ? 0 : 13 * issuerAndSerial.hashCode());
        result = result + ((keyEncAlgor == null) ? 0 : 13 * keyEncAlgor.hashCode());
        result = result + ((encKey == null) ? 0 : 13 * encKey.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "#<Recipient version="+version+" issuerAndSerial=["+issuerAndSerial.getName()+","+issuerAndSerial.getCertificateSerialNumber()+"] keyEncAlgor="+ASN1Registry.o2a(keyEncAlgor.getAlgorithm())+" encKey="+encKey+">";
    }

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
     * Get the <code>IssuerAndSerial</code> value.
     *
     * @return an <code>IssuerAndSerialNumber</code> value
     */
    public final IssuerAndSerialNumber getIssuerAndSerial() {
        return issuerAndSerial;
    }

    /**
     * Set the <code>IssuerAndSerial</code> value.
     *
     * @param newIssuerAndSerial The new IssuerAndSerial value.
     */
    public final void setIssuerAndSerial(final IssuerAndSerialNumber newIssuerAndSerial) {
        this.issuerAndSerial = newIssuerAndSerial;
    }

    /**
     * Get the <code>KeyEncAlgor</code> value.
     *
     * @return an <code>AlgorithmIdentifier</code> value
     */
    public final AlgorithmIdentifier getKeyEncAlgor() {
        return keyEncAlgor;
    }

    /**
     * Set the <code>KeyEncAlgor</code> value.
     *
     * @param newKeyEncAlgor The new KeyEncAlgor value.
     */
    public final void setKeyEncAlgor(final AlgorithmIdentifier newKeyEncAlgor) {
        this.keyEncAlgor = newKeyEncAlgor;
    }

    /**
     * Get the <code>EncKey</code> value.
     *
     * @return an <code>ASN1OctetString</code> value
     */
    public final ASN1OctetString getEncKey() {
        return encKey;
    }

    /**
     * Set the <code>EncKey</code> value.
     *
     * @param newEncKey The new EncKey value.
     */
    public final void setEncKey(final ASN1OctetString newEncKey) {
        this.encKey = newEncKey;
    }

    /**
     * Get the <code>Cert</code> value.
     *
     * @return a <code>X509AuxCertificate</code> value
     */
    public final X509AuxCertificate getCert() {
        return cert;
    }

    /**
     * Set the <code>Cert</code> value.
     *
     * @param newCert The new Cert value.
     */
    public final void setCert(final X509AuxCertificate newCert) {
        this.cert = newCert;
    }

    /* c: static pkcs7_cmp_ri
     *
     */
    public boolean compare(X509AuxCertificate pcert) {
        if(!new Name(issuerAndSerial.getName()).isEqual(pcert.getIssuerX500Principal())) {
            return false;
        }
        return pcert.getSerialNumber().compareTo(issuerAndSerial.getCertificateSerialNumber().getValue()) == 0;
    }

    /**
     * RecipientInfo ::= SEQUENCE {
     *   version Version,
     *   issuerAndSerialNumber IssuerAndSerialNumber,
     *   keyEncryptionAlgorithm KeyEncryptionAlgorithmIdentifier,
     *   encryptedKey EncryptedKey }
     * 
     * EncryptedKey ::= OCTET STRING
     */
    public static RecipInfo fromASN1(ASN1Encodable content) {
        ASN1Sequence sequence = (ASN1Sequence)content;
        RecipInfo ri = new RecipInfo();
        ri.setVersion(((ASN1Integer)sequence.getObjectAt(0)).getValue().intValue());
        ri.setIssuerAndSerial(IssuerAndSerialNumber.getInstance(sequence.getObjectAt(1)));
        ri.setKeyEncAlgor(AlgorithmIdentifier.getInstance(sequence.getObjectAt(2)));
        ri.setEncKey((ASN1OctetString)sequence.getObjectAt(3));
        return ri;
    }

    public ASN1Encodable asASN1() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(new ASN1Integer(getVersion()));
        vector.add(issuerAndSerial.toASN1Primitive()); 
        vector.add(keyEncAlgor.toASN1Primitive());
        vector.add(encKey.toASN1Primitive());
        return new DLSequence(vector);
    }
}// RecipInfo
