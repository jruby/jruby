/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2008 Vladimir Sizikov <vsizikov@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

package org.jruby.ext.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.HashMap;
import java.util.Map;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.api.Access.loadService;
import static org.jruby.api.Access.getModule;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.*;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyModule(name="Digest")
public class RubyDigest {

    private static final Map<String, MessageDigest> CLONEABLE_DIGESTS = new HashMap<>(8, 1);
    static {
        // standard digests from JCA specification; if we can retrieve and clone, save them
        for (String name : new String[] {"MD2", "MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512"}) {
            MessageDigest digest = getCloneableMessageDigestInstance(name);
            if (digest != null) CLONEABLE_DIGESTS.put(name, digest);
        }
    }

    @SuppressWarnings("ReturnValueIgnored")
    private static MessageDigest getCloneableMessageDigestInstance(final String name) {
        try {
            MessageDigest digest = MessageDigest.getInstance(name);
            digest.clone(); // ignored return value - we're checking clone-ability
            return digest;
        } catch (NoSuchAlgorithmException e) {
            logger().warn("digest '" + name + "' not supported", e);
        } catch (Exception e) {
            logger().debug("digest '" + name + "' not cloneable", e);
        }
        return null;
    }

    private static Logger logger() { return LoggerFactory.getLogger(RubyDigest.class); }

    private static final String PROVIDER = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    private static Provider provider = null;

    public static void createDigest(ThreadContext context) {
        try {
            provider = (Provider) Class.forName(PROVIDER).getConstructor().newInstance();
        } catch (Throwable t) { /* provider is not available */ }

        var Digest = defineModule(context, "Digest").
                defineMethods(context, RubyDigest.class);
        var DigestInstance = Digest.defineModuleUnder(context, "Instance").
                defineMethods(context, DigestInstance.class);
        RubyClass DigestClass = Digest.defineClassUnder(context, "Class", objectClass(context), DigestClass::new).
                include(context, DigestInstance).
                defineMethods(context, DigestClass.class);
        Digest.defineClassUnder(context, "Base", DigestClass, DigestBase::new).
                defineMethods(context, DigestBase.class);
    }

    private static MessageDigest createMessageDigest(final String name) throws NoSuchAlgorithmException {
        MessageDigest cloneable = CLONEABLE_DIGESTS.get(name);
        if (cloneable != null) {
            try {
                return (MessageDigest) cloneable.clone();
            } catch (CloneNotSupportedException e) {
                // should never happen, since we tested it in static init
            }
        }

        // fall back on JCA mechanisms for getting a digest
        if (provider != null) {
            try {
                return MessageDigest.getInstance(name, provider);
            } catch (NoSuchAlgorithmException e) {
                // bouncy castle doesn't support algorithm
            }
        }

        // fall back to default JCA providers
        return MessageDigest.getInstance(name);
    }

    private final static byte[] digits = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static ByteList toHex(byte[] val) {
        ByteList byteList = new ByteList(val.length * 2);
        for (byte value: val) {
            int b = value & 0xFF;
            byteList.append(digits[b >> 4]);
            byteList.append(digits[b & 0xF]);
        }
        return byteList;
    }

    private static RubyString toHexString(ThreadContext context, byte[] val) {
        return RubyString.newStringNoCopy(context.runtime, new ByteList(ByteList.plain(toHex(val)), USASCIIEncoding.INSTANCE));
    }

    /**
     * @param self
     * @param arg
     * @return ""
     * @deprecated Use {@link RubyDigest#hexencode(ThreadContext, IRubyObject, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyString hexencode(IRubyObject self, IRubyObject arg) {
        return hexencode(self.getRuntime().getCurrentContext(), self, arg);
    }

    @JRubyMethod(name = "hexencode", meta = true)
    public static RubyString hexencode(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return toHexString(context, arg.convertToString().getBytes());
    }

    @JRubyMethod(name = "bubblebabble", meta = true)
    public static RubyString bubblebabble(IRubyObject recv, IRubyObject arg) {
        final ByteList bytes = arg.convertToString().getByteList();
        return RubyString.newString(recv.getRuntime(), BubbleBabble.bubblebabble(bytes.unsafeBytes(), bytes.begin(), bytes.length()));
    }

    private record Metadata(String name, int blockLength) {
    }


    @JRubyClass(name="Digest::MD5", parent="Digest::Base")
    public static class MD5 {}
    @JRubyClass(name="Digest::RMD160", parent="Digest::Base")
    public static class RMD160 {}
    @JRubyClass(name="Digest::SHA1", parent="Digest::Base")
    public static class SHA1 {}
    @JRubyClass(name="Digest::SHA256", parent="Digest::Base")
    public static class SHA256 {}
    @JRubyClass(name="Digest::SHA384", parent="Digest::Base")
    public static class SHA384 {}
    @JRubyClass(name="Digest::SHA512", parent="Digest::Base")
    public static class SHA512 {}

    public static void createDigestMD5(ThreadContext context) {
        loadService(context).require("digest");
        RubyModule Digest = getModule(context, "Digest");
        var Base = Digest.getClass(context, "Base");
        RubyClass MD5 = Digest.defineClassUnder(context, "MD5", Base, Base.getAllocator());
        MD5.setInternalVariable("metadata", new Metadata("MD5", 64));
    }

    public static void createDigestRMD160(ThreadContext context) {
        loadService(context).require("digest");
        if(provider == null) {
            throw context.runtime.newLoadError("RMD160 not supported without BouncyCastle");
        }
        RubyModule Digest = getModule(context, "Digest");
        var Base = Digest.getClass(context, "Base");
        RubyClass RMD160 = Digest.defineClassUnder(context, "RMD160", Base, Base.getAllocator());
        RMD160.setInternalVariable("metadata", new Metadata("RIPEMD160", 64));
    }

    public static void createDigestSHA1(ThreadContext context) {
        loadService(context).require("digest");
        RubyModule Digest = getModule(context, "Digest");
        var Base = Digest.getClass(context, "Base");
        RubyClass SHA1 = Digest.defineClassUnder(context, "SHA1", Base, Base.getAllocator());
        SHA1.setInternalVariable("metadata", new Metadata("SHA1", 64));
    }

    public static void createDigestSHA2(ThreadContext context) {
        loadService(context).require("digest");
        try {
            createMessageDigest("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            RaiseException ex = context.runtime.newLoadError("SHA2 not supported");
            ex.initCause(e);
            throw ex;
        }
        final RubyModule Digest = getModule(context, "Digest");
        var Base = Digest.getClass(context, "Base");
        RubyClass SHA256 = Digest.defineClassUnder(context, "SHA256", Base, Base.getAllocator());
        SHA256.setInternalVariable("metadata", new Metadata("SHA-256", 64));
        RubyClass SHA384 = Digest.defineClassUnder(context, "SHA384", Base, Base.getAllocator());
        SHA384.setInternalVariable("metadata", new Metadata("SHA-384", 128));
        RubyClass SHA512 = Digest.defineClassUnder(context, "SHA512", Base, Base.getAllocator());
        SHA512.setInternalVariable("metadata", new Metadata("SHA-512", 128));
    }

    public static void createDigestBubbleBabble(ThreadContext context) {
        loadService(context).require("digest");
        RubyModule Digest = getModule(context, "Digest");
        var Base = Digest.getClass(context, "Base");
        RubyClass MD5 = Digest.defineClassUnder(context, "BubbleBabble", Base, Base.getAllocator());
        MD5.setInternalVariable("metadata", new Metadata("BubbleBabble", 64));
    }

    @JRubyModule(name = "Digest::Instance")
    public static class DigestInstance {

        private static IRubyObject throwUnimplError(ThreadContext context, IRubyObject self, String name) {
            throw runtimeError(context, String.format("%s does not implement %s()", self.getMetaClass().getRealClass().getName(), name));
        }

        /* instance methods that should be overridden */
        @JRubyMethod(name = {"update", "<<"})
        public static IRubyObject update(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return throwUnimplError(context, self, "update");
        }

        @JRubyMethod()
        public static IRubyObject finish(ThreadContext context, IRubyObject self) {
            return throwUnimplError(context, self, "finish");
        }

        @JRubyMethod()
        public static IRubyObject reset(ThreadContext context, IRubyObject self) {
            return throwUnimplError(context, self, "reset");
        }

        @JRubyMethod()
        public static IRubyObject digest_length(ThreadContext context, IRubyObject self) {
            return digest(context, self, IRubyObject.NULL_ARRAY).convertToString().bytesize(context);
        }

        @JRubyMethod()
        public static IRubyObject block_length(ThreadContext context, IRubyObject self) {
            return throwUnimplError(context, self, "block_length");
        }

        /* instance methods that may be overridden */
        @JRubyMethod(name = "==")
        public static IRubyObject op_equal(ThreadContext context, IRubyObject self, IRubyObject oth) {
            if(oth.isNil()) return context.fals;

            RubyString str1, str2;
            RubyModule instance = getModule(context, "Digest").getModule(context, "Instance");
            if (oth.getMetaClass().getRealClass().hasModuleInHierarchy(instance)) {
                str1 = digest(context, self, IRubyObject.NULL_ARRAY).convertToString();
                str2 = digest(context, oth, IRubyObject.NULL_ARRAY).convertToString();
            } else {
                str1 = to_s(context, self).convertToString();
                str2 = oth.convertToString();
            }
            boolean ret = str1.bytesize(context).eql(str2.bytesize(context)) && (str1.eql(str2));
            return ret ? context.tru : context.fals;
        }

        @JRubyMethod()
        public static IRubyObject inspect(ThreadContext context, IRubyObject self) {
            return RubyString.newStringNoCopy(context.runtime, ByteList.plain("#<" + self.getMetaClass().getRealClass().getName() + ": " + hexdigest(context, self, IRubyObject.NULL_ARRAY) + ">"));
        }

        /* instance methods that need not usually be overridden */
        @JRubyMethod(name = "new")
        public static IRubyObject newObject(ThreadContext context, IRubyObject self) {
            return self.rbClone().callMethod(context, "reset");
        }

        @JRubyMethod
        public static IRubyObject digest(ThreadContext context, IRubyObject self) {
            return digest_bang(context, self.rbClone());
        }

        @JRubyMethod
        public static IRubyObject digest(ThreadContext context, IRubyObject self, IRubyObject arg0) {
            final IRubyObject value;
            self.callMethod(context, "reset");
            self.callMethod(context, "update", arg0);
            value = self.callMethod(context, "finish");
            self.callMethod(context, "reset");
            return value;
        }

        @JRubyMethod(name = "digest!")
        public static IRubyObject digest_bang(ThreadContext context, IRubyObject self) {
            IRubyObject value = self.callMethod(context, "finish");
            self.callMethod(context, "reset");
            return value;
        }

        @JRubyMethod
        public static IRubyObject hexdigest(ThreadContext context, IRubyObject self) {
            return toHexString(context, digest(context, self).convertToString().getBytes());
        }

        @JRubyMethod
        public static IRubyObject hexdigest(ThreadContext context, IRubyObject self, IRubyObject arg0) {
            return toHexString(context, digest(context, self, arg0).convertToString().getBytes());
        }

        @JRubyMethod(name = "hexdigest!")
        public static IRubyObject hexdigest_bang(ThreadContext context, IRubyObject self) {
            return toHexString(context, digest_bang(context, self).convertToString().getBytes());
        }

        @JRubyMethod(name = "bubblebabble", meta = true)
        public static IRubyObject bubblebabble(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
            byte[] digest = sites(context).digest.call(context, recv, recv, arg0).convertToString().getBytes();
            return newString(context, BubbleBabble.bubblebabble(digest, 0, digest.length));
        }

        @JRubyMethod(name = "bubblebabble", meta = true)
        public static IRubyObject bubblebabble(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            byte[] digest = sites(context).digest.call(context, recv, recv, arg0, arg1).convertToString().getBytes();
            return newString(context, BubbleBabble.bubblebabble(digest, 0, digest.length));
        }

        @JRubyMethod()
        public static IRubyObject to_s(ThreadContext context, IRubyObject self) {
            return sites(context).hexdigest.call(context, self, self);
        }

        @JRubyMethod(name = {"length", "size"})
        public static IRubyObject length(ThreadContext context, IRubyObject self) {
            return sites(context).digest_length.call(context, self, self);
        }

        @Deprecated
        public static IRubyObject digest(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return switch (args.length) {
                case 0 -> digest(context, self);
                case 1 -> digest(context, self, args[0]);
                default -> throw argumentError(context, args.length, 0, 1);
            };
        }

        @Deprecated
        public static IRubyObject hexdigest(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return toHexString(context, digest(context, self, args).convertToString().getBytes());
        }

        @Deprecated
        public static IRubyObject bubblebabble(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
            return switch (args.length) {
                case 1 -> bubblebabble(context, recv, args[0]);
                case 2 -> bubblebabble(context, recv, args[0], args[1]);
                default -> throw argumentError(context, args.length, 1, 2);
            };
        }
    }

    @JRubyClass(name="Digest::Class")
    public static class DigestClass extends RubyObject {
        public DigestClass(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name = "digest", required = 1, rest = true, checkArity = false, meta = true)
        public static IRubyObject s_digest(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            switch (args.length) {
                case 0:
                    return s_digest(context, recv);
                case 1:
                    return s_digest(context, recv, args[0]);
                case 2:
                    return s_digest(context, recv, args[0], args[1]);
                case 3:
                    return s_digest(context, recv, args[0], args[1], args[2]);
            }

            RubyString str = args[0].convertToString();
            args = ArraySupport.newCopy(args, 1, args.length - 1); // skip first arg
            IRubyObject obj = ((RubyClass) recv).newInstance(context, args, Block.NULL_BLOCK);
            return sites(context).digest.call(context, obj, obj, str);
        }

        @JRubyMethod(name = "digest", checkArity = false, meta = true)
        public static IRubyObject s_digest(ThreadContext context, IRubyObject recv) {
            throw argumentError(context, "no data given");
        }

        @JRubyMethod(name = "digest", checkArity = false, meta = true)
        public static IRubyObject s_digest(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
            RubyString str = arg0.convertToString();
            IRubyObject obj = ((RubyClass) recv).newInstance(context, Block.NULL_BLOCK);
            return sites(context).digest.call(context, obj, obj, str);
        }

        @JRubyMethod(name = "digest", checkArity = false, meta = true)
        public static IRubyObject s_digest(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            RubyString str = arg0.convertToString();
            IRubyObject obj = ((RubyClass) recv).newInstance(context, arg1, Block.NULL_BLOCK);
            return sites(context).digest.call(context, obj, obj, str);
        }

        @JRubyMethod(name = "digest", checkArity = false, meta = true)
        public static IRubyObject s_digest(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            RubyString str = arg0.convertToString();
            IRubyObject obj = ((RubyClass) recv).newInstance(context, arg1, arg2, Block.NULL_BLOCK);
            return sites(context).digest.call(context, obj, obj, str);
        }

        @JRubyMethod(name = "hexdigest", required = 1, optional = 1, checkArity = false, meta = true)
        public static IRubyObject s_hexdigest(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            byte[] digest = recv.callMethod(context, "digest", args, Block.NULL_BLOCK).convertToString().getBytes();
            return RubyDigest.toHexString(context, digest);
        }

        @JRubyMethod(name = "hexdigest", meta = true)
        public static IRubyObject s_hexdigest(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
            byte[] digest = sites(context).digest.call(context, recv, recv, arg0).convertToString().getBytes();
            return RubyDigest.toHexString(context, digest);
        }

        @JRubyMethod(name = "hexdigest", meta = true)
        public static IRubyObject s_hexdigest(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            byte[] digest = sites(context).digest.call(context, recv, recv, arg0, arg1).convertToString().getBytes();
            return RubyDigest.toHexString(context, digest);
        }

        @JRubyMethod(name = "bubblebabble", meta = true)
        public static RubyString bubblebabble(ThreadContext context, IRubyObject recv, IRubyObject arg) {
            byte[] digest = sites(context).digest.call(context, recv, recv, arg).convertToString().getBytes();
            return newString(context, BubbleBabble.bubblebabble(digest, 0, digest.length));
        }

        @Deprecated
        public static RubyString bubblebabble(IRubyObject recv, IRubyObject arg) {
            return bubblebabble(recv.getRuntime().getCurrentContext(), recv, arg);
        }

        @Deprecated
        public static IRubyObject s_hexdigest(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
            return s_hexdigest(context, recv, args);
        }
    }


    @JRubyClass(name="Digest::Base")
    public static class DigestBase extends RubyObject {
        private MessageDigest algo;
        private int blockLength = 0;

        public DigestBase(Ruby runtime, RubyClass type) {
            super(runtime,type);

            var context = runtime.getCurrentContext();

            if(type == getModule(context, "Digest").getClass(context, "Base")) {
                throw runtime.newNotImplementedError("Digest::Base is an abstract class");
            }

            Metadata metadata = getMetadata(type);
            if(metadata == null) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }

            try {
                setAlgorithm(metadata);
            } catch(NoSuchAlgorithmException e) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }

        }

        // if subclass extends particular digest we need to walk to find it...we should rearchitect digest to avoid walking type systems
        private Metadata getMetadata(RubyModule type) {
            for (RubyModule current = type; current != null; current = current.getSuperClass()) {
                Metadata metadata = (Metadata) current.getInternalVariable("metadata");

                if (metadata != null) return metadata;
            }

            return null;
        }

        @JRubyMethod(visibility = Visibility.PRIVATE)
        public IRubyObject initialize_copy(ThreadContext context, IRubyObject obj) {
            if (this == obj) return this;

            DigestBase from = (DigestBase) obj;
            from.checkFrozen();

            try {
                this.algo = (MessageDigest) from.algo.clone();
            } catch (CloneNotSupportedException e) {
                String name = from.algo.getAlgorithm();
                throw typeError(context, "Could not initialize copy of digest (" + name + ")");
            }
            return this;
        }

        @JRubyMethod(name = {"update", "<<"})
        public IRubyObject update(IRubyObject obj) {
            ByteList bytes = obj.convertToString().getByteList();
            algo.update(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
            return this;
        }

        @JRubyMethod()
        public IRubyObject finish() {
            IRubyObject digest = RubyString.newStringNoCopy(getRuntime(), algo.digest());
            algo.reset();
            return digest;
        }

        /**
         * @return ""
         * @deprecated Use {@link DigestBase#digest_length(ThreadContext)} instead.
         */
        @Deprecated(since = "10.0", forRemoval = true)
        public IRubyObject digest_length() {
            return digest_length(getCurrentContext());
        }

        @JRubyMethod()
        public IRubyObject digest_length(ThreadContext context) {
            return asFixnum(context, algo.getDigestLength());
        }

        /**
         * @return ""
         * @deprecated Use {@link DigestBase#block_length(ThreadContext)} instead.
         */
        @Deprecated(since = "10.0", forRemoval = true)
        public IRubyObject block_length() {
            return block_length(getCurrentContext());
        }

            @JRubyMethod()
        public IRubyObject block_length(ThreadContext context) {
            if (blockLength == 0) throw runtimeError(context, getMetaClass() + " doesn't implement block_length()");
            return asFixnum(context, blockLength);
        }

        @JRubyMethod()
        public IRubyObject reset() {
            algo.reset();
            return this;
        }

        @JRubyMethod()
        public IRubyObject bubblebabble(ThreadContext context) {
            final byte[] digest = algo.digest();
            return newString(context, BubbleBabble.bubblebabble(digest, 0, digest.length));
        }

        private void setAlgorithm(Metadata metadata) throws NoSuchAlgorithmException {
           this.algo = createMessageDigest(metadata.name());
           this.blockLength = metadata.blockLength();
        }

    }

    private static JavaSites.DigestSites sites(ThreadContext context) {
        return context.sites.Digest;
    }
}// RubyDigest
