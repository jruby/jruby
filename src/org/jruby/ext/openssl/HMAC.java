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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class HMAC extends RubyObject {
    private static ObjectAllocator HMAC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new HMAC(runtime, klass);
        }
    };
    
    public static void createHMAC(Ruby runtime, RubyModule ossl) {
        RubyClass cHMAC = ossl.defineClassUnder("HMAC",runtime.getObject(),HMAC_ALLOCATOR);
        RubyClass openSSLError = ossl.getClass("OpenSSLError");
        ossl.defineClassUnder("HMACError",openSSLError,openSSLError.getAllocator());
        
        CallbackFactory hmaccb = runtime.callbackFactory(HMAC.class);

        cHMAC.getMetaClass().defineFastMethod("digest",hmaccb.getFastSingletonMethod("s_digest",IRubyObject.class,IRubyObject.class,IRubyObject.class));
        cHMAC.getMetaClass().defineFastMethod("hexdigest",hmaccb.getFastSingletonMethod("s_hexdigest",IRubyObject.class,IRubyObject.class,IRubyObject.class));
        cHMAC.defineMethod("initialize",hmaccb.getMethod("initialize",IRubyObject.class,IRubyObject.class));
        cHMAC.defineFastMethod("initialize_copy",hmaccb.getFastMethod("initialize_copy",IRubyObject.class));
        cHMAC.defineFastMethod("update",hmaccb.getFastMethod("update",IRubyObject.class));
        cHMAC.defineFastMethod("<<",hmaccb.getFastMethod("update",IRubyObject.class));
        cHMAC.defineFastMethod("digest",hmaccb.getFastMethod("digest"));
        cHMAC.defineFastMethod("hexdigest",hmaccb.getFastMethod("hexdigest"));
        cHMAC.defineFastMethod("inspect",hmaccb.getFastMethod("hexdigest"));
        cHMAC.defineFastMethod("to_s",hmaccb.getFastMethod("hexdigest"));
    }

    public static IRubyObject s_digest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            Mac mac = Mac.getInstance(name);
            byte[] key = kay.convertToString().getBytes();
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
            return RubyString.newString(recv.getRuntime(), mac.doFinal(data.convertToString().getBytes()));
        } catch(Exception e) {
            throw recv.getRuntime().newNotImplementedError("Unsupported HMAC algorithm (" + name + ")");
        }
    }

    public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            Mac mac = Mac.getInstance(name);
            byte[] key = kay.convertToString().getBytes();
            SecretKey keysp = new SecretKeySpec(key,name);
            mac.init(keysp);
            return RubyString.newString(recv.getRuntime(), ByteList.plain(Utils.toHex(mac.doFinal(data.convertToString().getBytes()))));
        } catch(Exception e) {
            throw recv.getRuntime().newNotImplementedError("Unsupported HMAC algorithm (" + name + ")");
        }
    }

    public HMAC(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private Mac mac;
    private byte[] key;
    private StringBuffer data = new StringBuffer();

    public IRubyObject initialize(IRubyObject kay, IRubyObject digest, Block unusedBlock) {
        String name = "HMAC" + ((Digest)digest).getAlgorithm();
        try {
            mac = Mac.getInstance(name);
            key = kay.convertToString().getBytes();
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
        mac.reset();
        return RubyString.newString(getRuntime(), mac.doFinal(ByteList.plain(data)));
    }

    public IRubyObject hexdigest() {
        mac.reset();
        return RubyString.newString(getRuntime(), ByteList.plain(Utils.toHex(mac.doFinal(ByteList.plain(data)))));
    }

    String getAlgorithm() {
        return this.mac.getAlgorithm();
    }
}// HMAC
