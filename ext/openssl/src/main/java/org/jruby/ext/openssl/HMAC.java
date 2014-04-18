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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
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
import org.jruby.runtime.Visibility;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;

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

    private static Mac getMacInstance(final String algorithmName) throws NoSuchAlgorithmException {
        // final String algorithmSuffix = algorithmName.replaceAll("-", "");
        final StringBuilder algName = new StringBuilder(5 + algorithmName.length());
        algName.append("HMAC"); // .append(algorithmSuffix);
        for ( int i = 0; i < algorithmName.length(); i++ ) {
            char c = algorithmName.charAt(i);
            if ( c != '-' ) algName.append(c);
        }
        try {
            return SecurityHelper.getMac(algName.toString());
        } // some algorithms need the - removed; this is ugly, I know.
        catch (NoSuchAlgorithmException e) {
            algName.insert(5, '-'); // "HMAC-" + algorithmSuffix
            return SecurityHelper.getMac(algName.toString());
        }
    }

    @JRubyMethod(name = "digest", meta = true)
    public static IRubyObject digest(IRubyObject self, IRubyObject digest, IRubyObject key, IRubyObject data) {
        final Ruby runtime = self.getRuntime();
        final String algName = getDigestAlgorithmName(digest);
        final byte[] keyBytes = key.convertToString().getBytes();
        final ByteList bytes = data.convertToString().getByteList();
        try {
            Mac mac = getMacInstance(algName);
            mac.init( new SecretKeySpec(keyBytes, mac.getAlgorithm()) );
            mac.update(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
            return runtime.newString( new ByteList(mac.doFinal(), false) );
        }
        catch (NoSuchAlgorithmException e) {
            throw runtime.newNotImplementedError("Unsupported MAC algorithm (HMAC[-]" + algName + ")");
        }
        catch (GeneralSecurityException e) {
            if ( isDebug(runtime) ) e.printStackTrace(runtime.getOut());
            throw runtime.newNotImplementedError(e.getMessage());
        }
    }

    @JRubyMethod(name = "hexdigest", meta = true)
    public static IRubyObject hexdigest(IRubyObject self, IRubyObject digest, IRubyObject key, IRubyObject data) {
        final Ruby runtime = self.getRuntime();
        final String algName = getDigestAlgorithmName(digest);
        final byte[] keyBytes = key.convertToString().getBytes();
        final ByteList bytes = data.convertToString().getByteList();
        try {
            final Mac mac = getMacInstance(algName);
            mac.init( new SecretKeySpec(keyBytes, mac.getAlgorithm()) );
            mac.update(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
            return runtime.newString( toHEX( mac.doFinal() ) );
        }
        catch (NoSuchAlgorithmException e) {
            throw runtime.newNotImplementedError("Unsupported MAC algorithm (HMAC[-]" + algName + ")");
        }
        catch (GeneralSecurityException e) {
            if ( isDebug(runtime) ) e.printStackTrace(runtime.getOut());
            throw runtime.newNotImplementedError(e.getMessage());
        }
    }

    public HMAC(Ruby runtime, RubyClass type) {
        super(runtime,type);
    }

    private Mac mac;
    private byte[] key;
    private final StringBuilder data = new StringBuilder(64);

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject key, IRubyObject digest) {
        final String algName = getDigestAlgorithmName(digest);
        try {
            this.mac = getMacInstance(algName);
            this.key = key.convertToString().getBytes();
            mac.init( new SecretKeySpec(this.key, mac.getAlgorithm()) );
        }
        catch (NoSuchAlgorithmException e) {
            throw getRuntime().newNotImplementedError("Unsupported MAC algorithm (HMAC[-]" + algName + ")");
        }
        catch (GeneralSecurityException e) {
            if ( isDebug(getRuntime()) ) e.printStackTrace(getRuntime().getOut());
            throw getRuntime().newNotImplementedError(e.getMessage());
        }
        return this;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(final IRubyObject obj) {
        if ( this == obj ) return this;

        checkFrozen();

        final HMAC that = ((HMAC) obj);
        final String algName = that.mac.getAlgorithm();
        try {
            this.mac = SecurityHelper.getMac(algName);
            this.key = that.key;
            mac.init( new SecretKeySpec(key, algName) );
        }
        catch (NoSuchAlgorithmException e) {
            throw getRuntime().newNotImplementedError("Unsupported MAC algorithm (" + algName + ")");
        }
        catch (GeneralSecurityException e) {
            if ( isDebug(getRuntime()) ) e.printStackTrace(getRuntime().getOut());
            throw getRuntime().newNotImplementedError(e.getMessage());
        }

        data.setLength(0);
        data.append( that.data );

        return this;
    }

    @JRubyMethod(name = { "update", "<<" })
    public IRubyObject update(final IRubyObject obj) {
        data.append(obj);
        return this;
    }

    @JRubyMethod
    public IRubyObject reset() {
        data.setLength(0);
        return this;
    }

    @JRubyMethod
    public IRubyObject digest() {
        return RubyString.newString( getRuntime(), getSignatureBytes() );
    }

    @JRubyMethod(name = { "hexdigest", "inspect", "to_s" })
    public IRubyObject hexdigest() {
        return getRuntime().newString( toHEX(getSignatureBytes()) );
    }

    String getAlgorithm() {
        return mac.getAlgorithm();
    }

    private byte[] getSignatureBytes() {
        mac.reset();
        return mac.doFinal( data.toString().getBytes() );
    }

    private static String getDigestAlgorithmName(final IRubyObject digest) {
        if ( digest instanceof Digest ) {
            return ((Digest) digest).getShortAlgorithm();
        }
        return digest.asString().toString();
    }

    private static final char[] HEX = {
        '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
        '8' , '9' , 'a' , 'b' , 'c' , 'd' , 'e' , 'f'
    };

    private static ByteList toHEX(final byte[] data) {
        final ByteList out = new ByteList(data.length * 2);
        for ( int i = 0; i < data.length; i++ ) {
            final byte b = data[i];
            out.append( HEX[ (b >> 4) & 0xF ] );
            out.append( HEX[ b & 0xF ] );
        }
        return out;
    }

}// HMAC
