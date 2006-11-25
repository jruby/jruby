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
public class OpenSSLReal {
    public static void createOpenSSL(IRuby runtime) {
        RubyModule ossl = runtime.defineModule("OpenSSL");
        ossl.defineClassUnder("OpenSSLError",runtime.getClass("StandardError"));

        ASN1.createASN1(runtime, ossl);
        Digest.createDigest(runtime, ossl);
        Cipher.createCipher(runtime, ossl);
        Random.createRandom(runtime, ossl);
        PKey.createPKey(runtime,ossl);
        HMAC.createHMAC(runtime,ossl);
        X509.createX509(runtime,ossl);
        Config.createConfig(runtime,ossl);
        NetscapeSPKI.createNetscapeSPKI(runtime,ossl);
        PKCS7.createPKCS7(runtime,ossl);
        SSL.createSSL(runtime,ossl);

        ossl.setConstant("VERSION",runtime.newString("1.0.0"));
        ossl.setConstant("OPENSSL_VERSION",runtime.newString("OpenSSL 0.9.8b 04 May 2006 (Java fake)"));
        
        try {
            java.security.MessageDigest.getInstance("SHA224");
            ossl.setConstant("OPENSSL_VERSION_NUMBER",runtime.newFixnum(9469999));
        } catch(java.security.NoSuchAlgorithmException e) {
            ossl.setConstant("OPENSSL_VERSION_NUMBER",runtime.newFixnum(9469952));
        }
    }
}// OpenSSLReal
