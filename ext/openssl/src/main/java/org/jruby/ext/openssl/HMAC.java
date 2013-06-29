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
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
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

import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class HMAC extends RubyObject {
    private static final long serialVersionUID = 7602535792884680307L;

    private static ObjectAllocator HMAC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new HMAC(runtime, klass);
        }
    };
    
    public static void createHMAC(Ruby runtime, RubyModule ossl) {
        RubyClass cHMAC = ossl.defineClassUnder("HMAC",runtime.getObject(),HMAC_ALLOCATOR);
        RubyClass openSSLError = ossl.getClass("OpenSSLError");
        ossl.defineClassUnder("HMACError",openSSLError,openSSLError.getAllocator());

        cHMAC.defineAnnotatedMethods(HMAC.class);
    }

    static Mac getMac(String algoName) throws NoSuchAlgorithmException {
        // some algorithms need the - removed; this is ugly, I know.
        try {
            return Mac.getInstance("HMAC" + algoName.replaceAll("-", ""));
        } catch (NoSuchAlgorithmException nsae) {
            return Mac.getInstance("HMAC-" + algoName.replaceAll("-", ""));
        }
    }
    
    @JRubyMethod(name = "digest", meta = true)
    public static IRubyObject s_digest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String algoName = getDigestAlgorithmName(digest);
        try {
            Mac mac = getMac(algoName);
            byte[] key = kay.convertToString().getBytes();
            SecretKey keysp = new SecretKeySpec(key, mac.getAlgorithm());
            mac.init(keysp);
            return RubyString.newString(recv.getRuntime(), mac.doFinal(data.convertToString().getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            throw recv.getRuntime().newNotImplementedError(e.getMessage());
        }
    }

    @JRubyMethod(name = "hexdigest", meta = true)
    public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject digest, IRubyObject kay, IRubyObject data) {
        String algoName = getDigestAlgorithmName(digest);
        try {
            Mac mac = getMac(algoName);
            byte[] key = kay.convertToString().getBytes();
            SecretKey keysp = new SecretKeySpec(key, mac.getAlgorithm());
            mac.init(keysp);
            return RubyString.newString(recv.getRuntime(), ByteList.plain(Utils.toHex(mac.doFinal(data.convertToString().getBytes()))));
        } catch (Exception e) {
            throw recv.getRuntime().newNotImplementedError(e.getMessage());
        }
    }

    public HMAC(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private Mac mac;
    private byte[] key;
    private StringBuffer data = new StringBuffer();

    @JRubyMethod
    public IRubyObject initialize(IRubyObject kay, IRubyObject digest) {
        String algoName = getDigestAlgorithmName(digest);
        try {
            mac = getMac(algoName);
            key = kay.convertToString().getBytes();
            SecretKey keysp = new SecretKeySpec(key, mac.getAlgorithm());
            mac.init(keysp);
        } catch (Exception e) {
            throw getRuntime().newNotImplementedError(e.getMessage());
        }
        return this;
    }

    @Override
    @JRubyMethod
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

    @JRubyMethod(name={"update", "<<"})
    public IRubyObject update(IRubyObject obj) {
        data.append(obj);
        return this;
    }

    @JRubyMethod
    public IRubyObject digest() {
        mac.reset();
        return RubyString.newString(getRuntime(), getSignatureBytes());
    }

    @JRubyMethod
    public IRubyObject reset() {
        data.setLength(0);
        return this;
    }

    @JRubyMethod(name={"hexdigest","inspect","to_s"})
    public IRubyObject hexdigest() {
        return RubyString.newString(getRuntime(), ByteList.plain(Utils.toHex(getSignatureBytes())));
    }

    String getAlgorithm() {
        return this.mac.getAlgorithm();
    }

    private byte[] getSignatureBytes() {
        mac.reset();
        return mac.doFinal(data.toString().getBytes());
    }

    private static String getDigestAlgorithmName(IRubyObject digest) {
        String algoName = null;
        if (digest instanceof Digest) {
            algoName = ((Digest) digest).getShortAlgorithm();
        } else {
            algoName = digest.asString().toString();
        }
        return algoName;
    }
}// HMAC
