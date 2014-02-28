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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Digest extends RubyObject {
    private static final long serialVersionUID = 1L;

    private static ObjectAllocator DIGEST_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Digest(runtime, klass);
        }
    };

    public static void createDigest(Ruby runtime, RubyModule mOSSL) {
        runtime.getLoadService().require("digest");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestClass = mDigest.getClass("Class");
        RubyClass cDigest = mOSSL.defineClassUnder("Digest", cDigestClass, DIGEST_ALLOCATOR);
        cDigest.defineAnnotatedMethods(Digest.class);
        RubyClass openSSLError = mOSSL.getClass("OpenSSLError");
        mOSSL.defineClassUnder("DigestError", openSSLError, openSSLError.getAllocator());
    }

    static MessageDigest getDigest(final String name, final Ruby runtime) {
        String algorithm = transformDigest(name);
        try {
            return OpenSSLReal.getMessageDigest(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            throw runtime.newNotImplementedError("Unsupported digest algorithm (" + name + ")");
        }
    }

    // name mapping for openssl -> JCE
    private static String transformDigest(String inp) {
        String[] sp = inp.split("::");
        if (sp.length > 1) { // We only want Digest names from the last part of class name
            inp = sp[sp.length - 1];
        }
        // MessageDigest algorithm name normalization.
        // BC accepts "SHA1" but it should be "SHA-1" per spec.
        if ("DSS".equalsIgnoreCase(inp)) {
            return "SHA";   // why?
        } else if ("DSS1".equalsIgnoreCase(inp)) {
            return "SHA-1";
        } else if (inp.toUpperCase().startsWith("SHA") && inp.length() > 3 && inp.charAt(3) != '-') {
            inp = "SHA-" + inp.substring(3);
        }
        return inp;
    }

    public Digest(Ruby runtime, RubyClass type) {
        super(runtime,type);
        // do not initialize MessageDigest at allocation time (same as the ruby-openssl)
        name = null;
        algo = null;
    }
    private MessageDigest algo;
    private String name;

    public String getRealName() {
        return transformDigest(name);
    }

    public String getName() {
        return name;
    }

    @JRubyMethod(required = 1, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args) {
        IRubyObject type = args[0];
        IRubyObject data = getRuntime().getNil();
        if (args.length > 1) {
            data = args[1];
        }
        name = type.toString();
        algo = getDigest(name, getRuntime());
        if (!data.isNil()) {
            update(data.convertToString());
        }
        return this;
    }

    @Override
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject obj) {
        checkFrozen();
        if(this == obj) {
            return this;
        }
        name = ((Digest)obj).algo.getAlgorithm();
        try {
            algo = (MessageDigest)((Digest)obj).algo.clone();
        } catch(CloneNotSupportedException e) {
            throw getRuntime().newTypeError("Could not initialize copy of digest (" + name + ")");
        }
        return this;
    }

    @JRubyMethod(name={"update","<<"})
    public IRubyObject update(IRubyObject obj) {
        ByteList bytes = obj.convertToString().getByteList();
        algo.update(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
        return this;
    }

    @JRubyMethod
    public IRubyObject reset() {
        algo.reset();
        return this;
    }

    @JRubyMethod
    public IRubyObject finish() {
        IRubyObject digest = RubyString.newStringNoCopy(getRuntime(), algo.digest());
        algo.reset();
        return digest;
    }

    @JRubyMethod
    public IRubyObject name() {
        return getRuntime().newString(name);
    }

    @JRubyMethod()
    public IRubyObject digest_length() {
        return RubyFixnum.newFixnum(getRuntime(), algo.getDigestLength());
    }

    // from http://www.win.tue.nl/pinpasjc/docs/apis/jc222/javacard/security/MessageDigest.html
    private enum BlockLength {
        DUMMY(-1),
        SHA(64),
        MD5(64),
        SHA_256(64),
        SHA_384(128),
        SHA_512(128);

        public static BlockLength forAlgorithm(String algorithm) {
            if (algorithm.equalsIgnoreCase("SHA-1")) {
                return SHA;
            } else if (algorithm.equalsIgnoreCase("MD5")) {
                return MD5;
            } else if (algorithm.equalsIgnoreCase("SHA-256")) {
                return SHA_256;
            } else if (algorithm.equalsIgnoreCase("SHA-384")) {
                return SHA_384;
            } else if (algorithm.equalsIgnoreCase("SHA-512")) {
                return SHA_512;
            }

            return DUMMY;
        }

        public int getLength() {
            return length;
        }

        private BlockLength(int length) {
            this.length = length;
        }

        private final int length;
    }

    @JRubyMethod()
    public IRubyObject block_length(ThreadContext context) {
        Ruby runtime = context.runtime;

        BlockLength bl = BlockLength.forAlgorithm(algo.getAlgorithm());

        if (bl.getLength() != -1) {
            return runtime.newFixnum(bl.getLength());
        }

        // TODO: All algorithms should be supported here?
        throw getRuntime().newRuntimeError(
                this.getMetaClass() + " doesn't implement block_length()");
    }

    String getAlgorithm() {
        return this.algo.getAlgorithm();
    }

    String getShortAlgorithm() {
        return getAlgorithm().replace("-", "");
    }
}

