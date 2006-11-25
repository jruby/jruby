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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class PKey extends RubyObject {
    public static void createPKey(IRuby runtime, RubyModule ossl) {
        RubyModule mPKey = ossl.defineModuleUnder("PKey");
        RubyClass cPKey = mPKey.defineClassUnder("PKey",runtime.getObject());
        mPKey.defineClassUnder("PKeyError",ossl.getClass("OpenSSLError"));
        
        CallbackFactory pkeycb = runtime.callbackFactory(PKey.class);

        cPKey.defineSingletonMethod("new",pkeycb.getSingletonMethod("newInstance"));
        cPKey.defineMethod("initialize",pkeycb.getMethod("initialize"));
        cPKey.defineMethod("sign",pkeycb.getMethod("sign",IRubyObject.class,IRubyObject.class));
        cPKey.defineMethod("verify",pkeycb.getMethod("verify",IRubyObject.class,IRubyObject.class,IRubyObject.class));

        PKeyRSA.createPKeyRSA(runtime,mPKey);
        PKeyDSA.createPKeyDSA(runtime,mPKey);
        //        createPKeyDH(runtime,mPKey);
    }

    public static IRubyObject newInstance(IRubyObject recv) {
        throw recv.getRuntime().newNotImplementedError("OpenSSL::PKey::PKey is an abstract class.");
    }

    public PKey(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    public IRubyObject initialize() {
        return this;
    }

    PublicKey getPublicKey() {
        return null;
    }

    PrivateKey getPrivateKey() {
        return null;
    }

    String getAlgorithm() {
        return "NONE";
    }

    public abstract IRubyObject to_der() throws Exception;

    public IRubyObject sign(IRubyObject digest, IRubyObject data) throws Exception {
        if(!this.callMethod(getRuntime().getCurrentContext(),"private?").isTrue()) {
            throw getRuntime().newArgumentError("Private key is needed.");
        }
        Signature sig = Signature.getInstance(((Digest)digest).getAlgorithm() + "WITH" + getAlgorithm(),"BC");
        sig.initSign(getPrivateKey());
        byte[] inp = data.toString().getBytes("PLAIN");
        sig.update(inp);
        byte[] sigge = sig.sign();
        return getRuntime().newString(new String(sigge,"ISO8859_1"));
        /*
    GetPKey(self, pkey);
    EVP_SignInit(&ctx, GetDigestPtr(digest));
    StringValue(data);
    EVP_SignUpdate(&ctx, RSTRING(data)->ptr, RSTRING(data)->len);
    str = rb_str_new(0, EVP_PKEY_size(pkey)+16);
    if (!EVP_SignFinal(&ctx, RSTRING(str)->ptr, &buf_len, pkey))
	ossl_raise(ePKeyError, NULL);
    assert(buf_len <= RSTRING(str)->len);
    RSTRING(str)->len = buf_len;
    RSTRING(str)->ptr[buf_len] = 0;

    return str;
         */
    }

    public IRubyObject verify(IRubyObject digest, IRubyObject sig, IRubyObject data) {
        System.err.println("WARNING: unimplemented method PKey#verify called");
        /*
    GetPKey(self, pkey);
    EVP_VerifyInit(&ctx, GetDigestPtr(digest));
    StringValue(sig);
    StringValue(data);
    EVP_VerifyUpdate(&ctx, RSTRING(data)->ptr, RSTRING(data)->len);
    switch (EVP_VerifyFinal(&ctx, RSTRING(sig)->ptr, RSTRING(sig)->len, pkey)) {
    case 0:
	return Qfalse;
    case 1:
	return Qtrue;
    default:
	ossl_raise(ePKeyError, NULL);
    }
        */
        return getRuntime().getNil();
    }
}// PKey
