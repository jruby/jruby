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

import java.security.cert.X509CRL;
import java.util.Collection;
import org.bouncycastle.asn1.ASN1Encodable;
import org.jruby.ext.openssl.x509store.X509AuxCertificate;

/**
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class PKCS7DataSignedAndEnveloped extends PKCS7Data  {
    /* NID_pkcs7_signedAndEnveloped */
    private SignEnvelope signedAndEnveloped;

    public PKCS7DataSignedAndEnveloped() {
        this.signedAndEnveloped = new SignEnvelope();
        this.signedAndEnveloped.setVersion(1);
        this.signedAndEnveloped.getEncData().setContentType(ASN1Registry.NID_pkcs7_data);
    }

    public int getType() {
        return ASN1Registry.NID_pkcs7_signedAndEnveloped;
    }

    @Override
    public boolean isSignedAndEnveloped() {
        return true;
    }

    @Override
    public SignEnvelope getSignedAndEnveloped() {
        return signedAndEnveloped;
    }

    @Override
    public void setCipher(CipherSpec cipher) {
        this.signedAndEnveloped.getEncData().setCipher(cipher);
    }

    @Override
    public void addRecipientInfo(RecipInfo ri) {
        this.signedAndEnveloped.getRecipientInfo().add(ri);
    }

    @Override
    public void addSigner(SignerInfoWithPkey psi) {
        this.signedAndEnveloped.getMdAlgs().add(psi.getDigestAlgorithm());
        this.signedAndEnveloped.getSignerInfo().add(psi);
    }

    @Override
    public Collection<SignerInfoWithPkey> getSignerInfo() {
        return this.signedAndEnveloped.getSignerInfo();
    }

    @Override
    public void addCertificate(X509AuxCertificate cert) {
        this.signedAndEnveloped.getCert().add(cert);
    }

    @Override
    public void addCRL(X509CRL crl) {
        this.signedAndEnveloped.getCrl().add(crl);
    }

    public static PKCS7DataSignedAndEnveloped fromASN1(ASN1Encodable content) {
        throw new UnsupportedOperationException("TODO: can't create DataSignedAndEnveloped from ASN1 yet");
    }
}// PKCS7DataSignedAndEnveloped
