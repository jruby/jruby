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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSL {

    public static final int VERIFY_NONE =                                   0x00;
    public static final int VERIFY_PEER =                                   0x01;
    public static final int VERIFY_FAIL_IF_NO_PEER_CERT =                   0x02;
    public static final int VERIFY_CLIENT_ONCE =                            0x04;

    public static final long OP_ALL =                                       0x00000FFFL;
    public static final long OP_NO_TICKET =                                 0x00004000L;
    public static final long OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION =    0x00010000L;
    public static final long OP_SINGLE_ECDH_USE =                           0x00080000L;
    public static final long OP_SINGLE_DH_USE =                             0x00100000L;
    public static final long OP_EPHEMERAL_RSA =                             0x00200000L;
    public static final long OP_CIPHER_SERVER_PREFERENCE =                  0x00400000L;
    public static final long OP_TLS_ROLLBACK_BUG =                          0x00800000L;
    public static final long OP_NO_SSLv2 =                                  0x01000000L; // supported
    public static final long OP_NO_SSLv3 =                                  0x02000000L; // supported
    public static final long OP_NO_TLSv1 =                                  0x04000000L; // supported
    public static final long OP_PKCS1_CHECK_1 =                             0x08000000L;
    public static final long OP_PKCS1_CHECK_2 =                             0x10000000L;
    public static final long OP_NETSCAPE_CA_DN_BUG =                        0x20000000L;
    public static final long OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG =           0x40000000L;

    public static void createSSL(Ruby runtime, RubyModule ossl) {
        RubyModule mSSL = ossl.defineModuleUnder("SSL");
        RubyClass openSSLError = ossl.getClass("OpenSSLError");
        RubyClass sslError = mSSL.defineClassUnder("SSLError",openSSLError,openSSLError.getAllocator());
        RubyClass sslErrorWaitReadable = mSSL.defineClassUnder("SSLErrorWaitReadable",sslError,openSSLError.getAllocator());
        sslErrorWaitReadable.include(new IRubyObject[]{runtime.getIO().getConstant("WaitReadable")});
        RubyClass sslErrorWaitWritable = mSSL.defineClassUnder("SSLErrorWaitWritable",sslError,openSSLError.getAllocator());
        sslErrorWaitWritable.include(new IRubyObject[]{runtime.getIO().getConstant("WaitWritable")});

        SSLContext.createSSLContext(runtime,mSSL);
        SSLSocket.createSSLSocket(runtime,mSSL);

        mSSL.setConstant("VERIFY_NONE", runtime.newFixnum(VERIFY_NONE));
        mSSL.setConstant("VERIFY_PEER", runtime.newFixnum(VERIFY_PEER));
        mSSL.setConstant("VERIFY_FAIL_IF_NO_PEER_CERT", runtime.newFixnum(VERIFY_FAIL_IF_NO_PEER_CERT));
        mSSL.setConstant("VERIFY_CLIENT_ONCE", runtime.newFixnum(VERIFY_CLIENT_ONCE));

        mSSL.setConstant("OP_ALL", runtime.newFixnum(OP_ALL));
        mSSL.setConstant("OP_NO_TICKET", runtime.newFixnum(OP_NO_TICKET));
        mSSL.setConstant("OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION", runtime.newFixnum(OP_NO_SESSION_RESUMPTION_ON_RENEGOTIATION));
        mSSL.setConstant("OP_SINGLE_ECDH_USE", runtime.newFixnum(OP_SINGLE_ECDH_USE));
        mSSL.setConstant("OP_SINGLE_DH_USE", runtime.newFixnum(OP_SINGLE_DH_USE));
        mSSL.setConstant("OP_EPHEMERAL_RSA", runtime.newFixnum(OP_EPHEMERAL_RSA));
        mSSL.setConstant("OP_CIPHER_SERVER_PREFERENCE", runtime.newFixnum(OP_CIPHER_SERVER_PREFERENCE));
        mSSL.setConstant("OP_TLS_ROLLBACK_BUG", runtime.newFixnum(OP_TLS_ROLLBACK_BUG));
        mSSL.setConstant("OP_NO_SSLv2", runtime.newFixnum(OP_NO_SSLv2));
        mSSL.setConstant("OP_NO_SSLv3", runtime.newFixnum(OP_NO_SSLv3));
        mSSL.setConstant("OP_NO_TLSv1", runtime.newFixnum(OP_NO_TLSv1));
        mSSL.setConstant("OP_PKCS1_CHECK_1", runtime.newFixnum(OP_PKCS1_CHECK_1));
        mSSL.setConstant("OP_PKCS1_CHECK_2", runtime.newFixnum(OP_PKCS1_CHECK_2));
        mSSL.setConstant("OP_NETSCAPE_CA_DN_BUG", runtime.newFixnum(OP_NETSCAPE_CA_DN_BUG));
        mSSL.setConstant("OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG", runtime.newFixnum(OP_NETSCAPE_DEMO_CIPHER_CHANGE_BUG));
    }

    @Deprecated // confusing since it throws instead of returning
    public static RaiseException newSSLError(Ruby runtime, Throwable t) {
        throw Utils.newError(runtime, "OpenSSL::SSL::SSLError", t.getMessage());
    }

    public static RaiseException newSSLError(Ruby runtime, Exception e) {
        return SSLSocket.newSSLError(runtime, e);
    }

}// SSL
