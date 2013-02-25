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


import org.bouncycastle.asn1.ASN1Encodable;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class PKCS7DataEnveloped extends PKCS7Data  {
    /* NID_pkcs7_enveloped */
    private Envelope enveloped;

    public PKCS7DataEnveloped() {
        this.enveloped = new Envelope();
        this.enveloped.setVersion(0);
        this.enveloped.getEncData().setContentType(ASN1Registry.NID_pkcs7_data);
    }

    public PKCS7DataEnveloped(Envelope enveloped) {
        this.enveloped = enveloped;
    }

    public int getType() {
        return ASN1Registry.NID_pkcs7_enveloped;
    }

    @Override
    public Envelope getEnveloped() {
        return this.enveloped;
    }

    @Override
    public boolean isEnveloped() {
        return true;
    }

    @Override
    public void setCipher(CipherSpec cipher) {
        this.enveloped.getEncData().setCipher(cipher);
    }

    @Override
    public void addRecipientInfo(RecipInfo ri) {
        this.enveloped.getRecipientInfo().add(ri);
    }

    @Override
    public String toString() {
        return this.enveloped.toString();
    }

    public static PKCS7DataEnveloped fromASN1(ASN1Encodable content) {
        return new PKCS7DataEnveloped(Envelope.fromASN1(content));
    }

    @Override
    public ASN1Encodable asASN1() {
        return enveloped.asASN1();
    }
}// PKCS7DataEnveloped
