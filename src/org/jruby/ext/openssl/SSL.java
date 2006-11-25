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
import org.jruby.RubyClass;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSL {
    public static void createSSL(IRuby runtime, RubyModule ossl) {
        RubyModule mSSL = ossl.defineModuleUnder("SSL");
        mSSL.defineClassUnder("SSLError",ossl.getClass("OpenSSLError"));

        SSLContext.createSSLContext(runtime,mSSL);
        SSLSocket.createSSLSocket(runtime,mSSL);

        mSSL.setConstant("VERIFY_NONE",runtime.newFixnum(0));
        mSSL.setConstant("VERIFY_PEER",runtime.newFixnum(1));
        mSSL.setConstant("VERIFY_FAIL_IF_NO_PEER_CERT",runtime.newFixnum(2));
        mSSL.setConstant("VERIFY_CLIENT_ONCE",runtime.newFixnum(4));

        mSSL.setConstant("OP_ALL",runtime.newFixnum(4095));
        mSSL.setConstant("OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION",runtime.newFixnum(65536));
        mSSL.setConstant("OP_SINGLE_ECDH_USE",runtime.newFixnum(524288));
        mSSL.setConstant("OP_SINGLE_DH_USE",runtime.newFixnum(1048576));
        mSSL.setConstant("OP_EPHEMERAL_RSA",runtime.newFixnum(2097152));
        mSSL.setConstant("OP_CIPHER_SERVER_PREFERENCE",runtime.newFixnum(4194304));
        mSSL.setConstant("OP_TLS_ROLLBACK_BUG",runtime.newFixnum(8388608));
        mSSL.setConstant("OP_NO_SSLv2",runtime.newFixnum(16777216));
        mSSL.setConstant("OP_NO_SSLv3",runtime.newFixnum(33554432));
        mSSL.setConstant("OP_NO_TLSv1",runtime.newFixnum(67108864));
        mSSL.setConstant("OP_PKCS1_CHECK_1",runtime.newFixnum(134217728));
        mSSL.setConstant("OP_PKCS1_CHECK_2",runtime.newFixnum(268435456));
        mSSL.setConstant("OP_NETSCAPE_CA_DN_BUG",runtime.newFixnum(536870912));
        mSSL.setConstant("OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG",runtime.newFixnum(-1073741824));
    }    
}// SSL
