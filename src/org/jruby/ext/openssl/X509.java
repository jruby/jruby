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

import org.jruby.IRuby;
import org.jruby.RubyModule;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509 {
    public static void createX509(IRuby runtime, RubyModule ossl) {
        RubyModule mX509 = ossl.defineModuleUnder("X509");

        X509Name.createX509Name(runtime,mX509);
        X509Cert.createX509Cert(runtime,mX509);
        X509Extensions.createX509Ext(runtime,mX509);
        X509CRL.createX509CRL(runtime,mX509);
        X509Revoked.createX509Revoked(runtime,mX509);
        X509Store.createX509Store(runtime,mX509);
        Request.createRequest(runtime,mX509);
        Attribute.createAttribute(runtime,mX509);

        mX509.setConstant("V_OK",runtime.newFixnum(0));
        mX509.setConstant("V_ERR_UNABLE_TO_GET_ISSUER_CERT",runtime.newFixnum(2));
        mX509.setConstant("V_ERR_UNABLE_TO_GET_CRL",runtime.newFixnum(3));
        mX509.setConstant("V_ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE",runtime.newFixnum(4));
        mX509.setConstant("V_ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE",runtime.newFixnum(5));
        mX509.setConstant("V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY",runtime.newFixnum(6));
        mX509.setConstant("V_ERR_CERT_SIGNATURE_FAILURE",runtime.newFixnum(7));
        mX509.setConstant("V_ERR_CRL_SIGNATURE_FAILURE",runtime.newFixnum(8));
        mX509.setConstant("V_ERR_CERT_NOT_YET_VALID",runtime.newFixnum(9));
        mX509.setConstant("V_ERR_CERT_HAS_EXPIRED",runtime.newFixnum(10));
        mX509.setConstant("V_ERR_CRL_NOT_YET_VALID",runtime.newFixnum(11));
        mX509.setConstant("V_ERR_CRL_HAS_EXPIRED",runtime.newFixnum(12));
        mX509.setConstant("V_ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD",runtime.newFixnum(13));
        mX509.setConstant("V_ERR_ERROR_IN_CERT_NOT_AFTER_FIELD",runtime.newFixnum(14));
        mX509.setConstant("V_ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD",runtime.newFixnum(15));
        mX509.setConstant("V_ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD",runtime.newFixnum(16));
        mX509.setConstant("V_ERR_OUT_OF_MEM",runtime.newFixnum(17));
        mX509.setConstant("V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT",runtime.newFixnum(18));
        mX509.setConstant("V_ERR_SELF_SIGNED_CERT_IN_CHAIN",runtime.newFixnum(19));
        mX509.setConstant("V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY",runtime.newFixnum(20));
        mX509.setConstant("V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE",runtime.newFixnum(21));
        mX509.setConstant("V_ERR_CERT_CHAIN_TOO_LONG",runtime.newFixnum(22));
        mX509.setConstant("V_ERR_CERT_REVOKED",runtime.newFixnum(23));
        mX509.setConstant("V_ERR_INVALID_CA",runtime.newFixnum(24));
        mX509.setConstant("V_ERR_PATH_LENGTH_EXCEEDED",runtime.newFixnum(25));
        mX509.setConstant("V_ERR_INVALID_PURPOSE",runtime.newFixnum(26));
        mX509.setConstant("V_ERR_CERT_UNTRUSTED",runtime.newFixnum(27));
        mX509.setConstant("V_ERR_CERT_REJECTED",runtime.newFixnum(28));
        mX509.setConstant("V_ERR_SUBJECT_ISSUER_MISMATCH",runtime.newFixnum(29));
        mX509.setConstant("V_ERR_AKID_SKID_MISMATCH",runtime.newFixnum(30));
        mX509.setConstant("V_ERR_AKID_ISSUER_SERIAL_MISMATCH",runtime.newFixnum(31));
        mX509.setConstant("V_ERR_KEYUSAGE_NO_CERTSIGN",runtime.newFixnum(32));
        mX509.setConstant("V_ERR_APPLICATION_VERIFICATION",runtime.newFixnum(50));
        mX509.setConstant("V_FLAG_CRL_CHECK",runtime.newFixnum(4));
        mX509.setConstant("V_FLAG_CRL_CHECK_ALL",runtime.newFixnum(8));
        mX509.setConstant("PURPOSE_SSL_CLIENT",runtime.newFixnum(1));
        mX509.setConstant("PURPOSE_SSL_SERVER",runtime.newFixnum(2));
        mX509.setConstant("PURPOSE_NS_SSL_SERVER",runtime.newFixnum(3));
        mX509.setConstant("PURPOSE_SMIME_SIGN",runtime.newFixnum(4));
        mX509.setConstant("PURPOSE_SMIME_ENCRYPT",runtime.newFixnum(5));
        mX509.setConstant("PURPOSE_CRL_SIGN",runtime.newFixnum(6));
        mX509.setConstant("PURPOSE_ANY",runtime.newFixnum(7));
        mX509.setConstant("PURPOSE_OCSP_HELPER",runtime.newFixnum(8));
        mX509.setConstant("TRUST_COMPAT",runtime.newFixnum(1));
        mX509.setConstant("TRUST_SSL_CLIENT",runtime.newFixnum(2));
        mX509.setConstant("TRUST_SSL_SERVER",runtime.newFixnum(3));
        mX509.setConstant("TRUST_EMAIL",runtime.newFixnum(4));
        mX509.setConstant("TRUST_OBJECT_SIGN",runtime.newFixnum(5));
        mX509.setConstant("TRUST_OCSP_SIGN",runtime.newFixnum(6));
        mX509.setConstant("TRUST_OCSP_REQUEST",runtime.newFixnum(7));

        // These should eventually point to correct things.
        mX509.setConstant("DEFAULT_CERT_AREA", runtime.newString("/usr/lib/ssl"));
        mX509.setConstant("DEFAULT_CERT_DIR", runtime.newString("/usr/lib/ssl/certs"));
        mX509.setConstant("DEFAULT_CERT_FILE", runtime.newString("/usr/lib/ssl/cert.pem"));
        mX509.setConstant("DEFAULT_CERT_DIR_ENV", runtime.newString("SSL_CERT_DIR"));
        mX509.setConstant("DEFAULT_CERT_FILE_ENV", runtime.newString("SSL_CERT_FILE"));
        mX509.setConstant("DEFAULT_PRIVATE_DIR", runtime.newString("/usr/lib/ssl/private"));
    }
}// X509
