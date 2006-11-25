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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class HMAC extends RubyObject {
    public static void createHMAC(IRuby runtime, RubyModule ossl) {
        RubyClass cHMAC = ossl.defineClassUnder("HMAC",runtime.getObject());
        ossl.defineClassUnder("HMACError",ossl.getClass("OpenSSLError"));
        
        CallbackFactory hmaccb = runtime.callbackFactory(HMAC.class);

        cHMAC.defineSingletonMethod("new",hmaccb.getOptSingletonMethod("newInstance"));
        cHMAC.defineSingletonMethod("digest",hmaccb.getSingletonMethod("s_digest",IRubyObject.class,IRubyObject.class,IRubyObject.class));
        cHMAC.defineSingletonMethod("hexdigest",hmaccb.getSingletonMethod("s_hexdigest",IRubyObject.class,IRubyObject.class,IRubyObject.class));
        cHMAC.defineMethod("initialize",hmaccb.getMethod("initialize",IRubyObject.class,IRubyObject.class));
        cHMAC.defineMethod("initialize_copy",hmaccb.getMethod("initialize_copy",IRubyObject.class));
        cHMAC.defineMethod("clone",hmaccb.getMethod("rbClone"));
        cHMAC.defineMethod("update",hmaccb.getMethod("update",IRubyObject.class));
        cHMAC.defineMethod("<<",hmaccb.getMethod("update",IRubyObject.class));
        cHMAC.defineMethod("digest",hmaccb.getMethod("digest"));
        cHMAC.defineMethod("hexdigest",hmaccb.getMethod("hexdigest"));
        cHMAC.defineMethod("inspect",hmaccb.getMethod("hexdigest"));
        cHMAC.defineMethod("to_s",hmaccb.getMethod("hexdigest"));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        HMAC result = new HMAC(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public static IRubyObject s_digest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            Mac mac = Mac.getInstance(name);
            byte[] key = kay.toString().getBytes("PLAIN");
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
            return recv.getRuntime().newString(new String(mac.doFinal(data.toString().getBytes("PLAIN")),"ISO8859_1"));
        } catch(Exception e) {
            throw recv.getRuntime().newNotImplementedError("Unsupported HMAC algorithm (" + name + ")");
        }
    }

    public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            Mac mac = Mac.getInstance(name);
            byte[] key = kay.toString().getBytes("PLAIN");
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
            return recv.getRuntime().newString(Utils.toHex(mac.doFinal(data.toString().getBytes("PLAIN"))));
        } catch(Exception e) {
            throw recv.getRuntime().newNotImplementedError("Unsupported HMAC algorithm (" + name + ")");
        }
    }

    public HMAC(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private Mac mac;
    private byte[] key;
    private StringBuffer data = new StringBuffer();

    public IRubyObject initialize(IRubyObject kay, IRubyObject digest) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            mac = Mac.getInstance(name);
            key = kay.toString().getBytes("PLAIN");
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
        } catch(Exception e) {
            throw getRuntime().newNotImplementedError("Unsupported MAC algorithm (" + name + ")");
        }
        return this;
    }

    public IRubyObject initialize_copy(IRubyObject obj) {
        if(this == obj) {
            return this;
        }
        checkFrozen();
        String name = ((HMAC)obj).mac.getAlgorithm();
        try {
            mac = Mac.getInstance(name);
            key = ((HMAC)obj).key;
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
        } catch(Exception e) {
            throw getRuntime().newNotImplementedError("Unsupported MAC algorithm (" + name + ")");
        }
        
        data = new StringBuffer(((HMAC)obj).data.toString());

        return this;
    }

    public IRubyObject update(IRubyObject obj) {
        data.append(obj);
        return this;
    }

    public IRubyObject digest() {
        try {
            mac.reset();
            return getRuntime().newString(new String(mac.doFinal(data.toString().getBytes("PLAIN")),"ISO8859_1"));
        } catch(java.io.UnsupportedEncodingException e) {
            return getRuntime().getNil();
        }
    }

    public IRubyObject hexdigest() {
        try {
            mac.reset();
            return getRuntime().newString(Utils.toHex(mac.doFinal(data.toString().getBytes("PLAIN"))));
        } catch(java.io.UnsupportedEncodingException e) {
            return getRuntime().getNil();
        }
    }

    public IRubyObject rbClone() {
        IRubyObject clone = new HMAC(getRuntime(),getMetaClass().getRealClass());
        clone.setMetaClass(getMetaClass().getSingletonClassClone());
        clone.setTaint(this.isTaint());
        clone.initCopy(this);
        clone.setFrozen(isFrozen());
        return clone;
    }

    String getAlgorithm() {
        return this.mac.getAlgorithm();
    }
}// HMAC
