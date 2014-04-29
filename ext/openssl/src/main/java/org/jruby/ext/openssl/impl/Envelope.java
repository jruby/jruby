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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DLSequence;

/** PKCS7_ENVELOPE
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class Envelope {
    private int version;

    /**
     * Describe encContent here.
     */
    private EncContent encData = new EncContent();

    /**
     * Describe recipientInfo here.
     */
    private Collection<RecipInfo> recipientInfo = new ArrayList<RecipInfo>();

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
     * Get the <code>EncData</code> value.
     *
     * @return an <code>EncContent</code> value
     */
    public final EncContent getEncData() {
        return encData;
    }

    /**
     * Set the <code>EncData</code> value.
     *
     * @param newEncContent The new EncContent value.
     */
    public final void setEncData(final EncContent newEncData) {
        this.encData = newEncData;
    }

    /**
     * Get the <code>RecipientInfo</code> value.
     *
     * @return a <code>Collection<RecipInfo></code> value
     */
    public final Collection<RecipInfo> getRecipientInfo() {
        return recipientInfo;
    }

    /**
     * Set the <code>RecipientInfo</code> value.
     *
     * @param newRecipientInfo The new RecipientInfo value.
     */
    public final void setRecipientInfo(final Collection<RecipInfo> newRecipientInfo) {
        this.recipientInfo = newRecipientInfo;
    }

    @Override
    public String toString() {
        return "#<Envelope version=" + version + " encData="+encData+" recipientInfo="+recipientInfo+">";
    }

    /**
     * EnvelopedData ::= SEQUENCE {
     *   version Version,
     *   recipientInfos RecipientInfos,
     *   encryptedContentInfo EncryptedContentInfo }
     *
     * Version ::= INTEGER
     *
     * RecipientInfos ::= SET OF RecipientInfo
     *
     */
    public static Envelope fromASN1(ASN1Encodable content) {
        ASN1Sequence sequence = (ASN1Sequence) content;
        ASN1Integer version = (ASN1Integer) sequence.getObjectAt(0);
        ASN1Set recipients = (ASN1Set) sequence.getObjectAt(1);
        ASN1Encodable encContent = sequence.getObjectAt(2);

        Envelope envelope = new Envelope();
        envelope.setVersion(version.getValue().intValue());
        envelope.setRecipientInfo(recipientInfosFromASN1Set(recipients));
        envelope.setEncData(EncContent.fromASN1(encContent));

        return envelope;
    }

    public ASN1Encodable asASN1() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        vector.add( new ASN1Integer( BigInteger.valueOf(version) ) );
        vector.add( receipientInfosToASN1Set() );
        vector.add( encData.asASN1() );
        return new DLSequence(vector);
    }

    private ASN1Set receipientInfosToASN1Set() {
        ASN1EncodableVector vector = new ASN1EncodableVector();
        for (RecipInfo ri : getRecipientInfo()) {
            vector.add(ri.asASN1());
        }
        return new DERSet(vector);
    }

    private static Collection<RecipInfo> recipientInfosFromASN1Set(ASN1Encodable content) {
        ASN1Set set = (ASN1Set)content;
        Collection<RecipInfo> result = new ArrayList<RecipInfo>();
        for(Enumeration<?> e = set.getObjects(); e.hasMoreElements();) {
            result.add(RecipInfo.fromASN1((ASN1Encodable)e.nextElement()));
        }
        return result;
    }
}// Envelope
