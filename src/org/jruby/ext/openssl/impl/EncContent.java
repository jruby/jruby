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

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.jruby.util.ByteList;

/** PKCS7_ENC_CONTENT
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class EncContent {
    /**
     * Describe contentType here.
     */
    private int contentType;

    /**
     * Describe cipher here.
     */
    private CipherSpec cipher;

    /**
     * Describe algorithm here.
     */
    private AlgorithmIdentifier algorithm;

    /**
     * Describe encData here.
     */
    private ASN1OctetString encData;

    /**
     * Get the <code>ContentType</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getContentType() {
        return contentType;
    }

    /**
     * Set the <code>ContentType</code> value.
     *
     * @param newContentType The new ContentType value.
     */
    public final void setContentType(final int newContentType) {
        this.contentType = newContentType;
    }

    /**
     * Get the <code>Cipher</code> value.
     *
     * @return a <code>Cipher</code> value
     */
    public final CipherSpec getCipher() {
        return cipher;
    }

    /**
     * Set the <code>Cipher</code> value.
     *
     * @param newCipher The new Cipher value.
     */
    public final void setCipher(final CipherSpec newCipher) {
        this.cipher = newCipher;
    }

    /**
     * Get the <code>Algorithm</code> value.
     *
     * @return an <code>AlgorithmIdentifier</code> value
     */
    public final AlgorithmIdentifier getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the <code>Algorithm</code> value.
     *
     * @param newAlgorithm The new Algorithm value.
     */
    public final void setAlgorithm(final AlgorithmIdentifier newAlgorithm) {
        this.algorithm = newAlgorithm;
    }

    /**
     * Get the <code>EncData</code> value.
     *
     * @return an <code>ASN1OctetString</code> value
     */
    public final ASN1OctetString getEncData() {
        return encData;
    }

    /**
     * Set the <code>EncData</code> value.
     *
     * @param newEncData The new EncData value.
     */
    public final void setEncData(final ASN1OctetString newEncData) {
        this.encData = newEncData;
    }

    @Override
    public String toString() {
        return "#<EncContent contentType="+contentType+" algorithm="+(algorithm == null ? "null" : ASN1Registry.o2a(algorithm.getAlgorithm()))+" content="+encData+">";
    }

    /**
     * EncryptedContentInfo ::= SEQUENCE {
     *   contentType ContentType,
     *   contentEncryptionAlgorithm ContentEncryptionAlgorithmIdentifier,
     *   encryptedContent [0] IMPLICIT EncryptedContent OPTIONAL }
     *
     * EncryptedContent ::= OCTET STRING
     */
    public static EncContent fromASN1(ASN1Encodable content) {
        ASN1Sequence sequence = (ASN1Sequence)content;
        ASN1ObjectIdentifier contentType = (ASN1ObjectIdentifier)(sequence.getObjectAt(0));
        int nid = ASN1Registry.obj2nid(contentType);

        EncContent ec = new EncContent();
        ec.setContentType(nid);
        ec.setAlgorithm(AlgorithmIdentifier.getInstance(sequence.getObjectAt(1)));
        if(sequence.size() > 2 && sequence.getObjectAt(2) instanceof ASN1TaggedObject && ((ASN1TaggedObject)(sequence.getObjectAt(2))).getTagNo() == 0) {
            ASN1Encodable ee = ((ASN1TaggedObject)(sequence.getObjectAt(2))).getObject();
            if(ee instanceof ASN1Sequence && ((ASN1Sequence)ee).size() > 0) {
                ByteList combinedOctets = new ByteList();
                Enumeration enm = ((ASN1Sequence)ee).getObjects();
                while (enm.hasMoreElements()) {
                    byte[] octets = ((ASN1OctetString)enm.nextElement()).getOctets();
                    combinedOctets.append(octets);
                }
                ec.setEncData(new DEROctetString(combinedOctets.bytes()));
            } else {
                ec.setEncData((ASN1OctetString)ee);
            }
        }
        return ec;
    }

    public ASN1Encodable asASN1() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add(ASN1Registry.nid2obj(contentType).toASN1Primitive());
        vector.add(algorithm.toASN1Primitive());
        if(encData != null) {
            vector.add(new DERTaggedObject(false, 0, encData));
        }
        return new DLSequence(vector);
   }
}// EncContent
