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
public class PKCS7DataSigned extends PKCS7Data {
    /* NID_pkcs7_signed */
    private Signed sign;

    public PKCS7DataSigned() {
        this.sign = new Signed();
        this.sign.setVersion(1);
    }

    public PKCS7DataSigned(Signed sign) {
        this.sign = sign;
    }

    public int getType() {
        return ASN1Registry.NID_pkcs7_signed;
    }

    @Override
    public Object ctrl(int cmd, Object v, Object ignored) {
        int ret = 0;
        switch(cmd) {
        case PKCS7.OP_SET_DETACHED_SIGNATURE:
            ret = ((Integer)v).intValue();
            if(ret != 0 && sign.contents.isData()) {
                sign.contents.setData(null);
            }
            break;
        case PKCS7.OP_GET_DETACHED_SIGNATURE:
            if(sign == null || sign.contents.getData() == null) {
                ret = 1;
            } else {
                ret = 0;
            }
            break;
        default:
            throw new RuntimeException("TODO: implement error handling");
        }
        return Integer.valueOf(ret);
    }

    @Override
    public void setSign(Signed sign) {
        this.sign = sign;
    }

    @Override
    public Signed getSign() {
        return this.sign;
    }

    @Override
    public boolean isSigned() {
        return true;
    }

    @Override
    public void addSigner(SignerInfoWithPkey psi) {
        this.sign.getMdAlgs().add(psi.getDigestAlgorithm());
        this.sign.getSignerInfo().add(psi);
    }

    @Override
    public void setContent(PKCS7 p7) {
        this.sign.setContents(p7);
    }

    @Override
    public Collection<SignerInfoWithPkey> getSignerInfo() {
        return this.sign.getSignerInfo();
    }

    @Override
    public void addCertificate(X509AuxCertificate cert) {
        this.sign.getCert().add(cert);
    }

    @Override
    public void addCRL(X509CRL crl) {
        this.sign.getCrl().add(crl);
    }

    @Override
    public String toString() {
        return this.sign.toString();
    }

    public static PKCS7DataSigned fromASN1(ASN1Encodable content) throws PKCS7Exception {
        return new PKCS7DataSigned(Signed.fromASN1(content));
    }

    @Override
    public ASN1Encodable asASN1() {
        return sign.asASN1();
    }
}// PKCS7DataSigned
