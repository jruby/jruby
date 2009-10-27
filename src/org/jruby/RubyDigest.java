/*
 ***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.util.Arrays;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.anno.JRubyClass;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
@JRubyModule(name="Digest")
public class RubyDigest {
    private static Provider provider = null;

    public static void createDigest(Ruby runtime) {
        // We're not setting the provider or anything, but it seems that BouncyCastle does some internal things in its
        // provider's constructor which require it to be executed in a secure context.
        // Ideally this hack should be removed. See JRUBY-3919 and this BC bug:
        //   http://www.bouncycastle.org/jira/browse/BJA-227
        provider = (Provider) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
                } catch(Exception e) {
                    // provider is not available
                    return null;
                }
            }
        });

        RubyModule mDigest = runtime.defineModule("Digest");
        RubyClass cDigestBase = mDigest.defineClassUnder("Base",runtime.getObject(), Base.BASE_ALLOCATOR);

        cDigestBase.defineAnnotatedMethods(Base.class);
    }

    private static MessageDigest createMessageDigest(Ruby runtime, String providerName) throws NoSuchAlgorithmException {
        if(provider != null) {
            try {
                return MessageDigest.getInstance(providerName, provider);
            } catch(NoSuchAlgorithmException e) {
                // bouncy castle doesn't support algorithm
            }
        }
        // fall back to system JCA providers
        return MessageDigest.getInstance(providerName);
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

    public static void createDigestMD5(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.fastGetModule("Digest");
        RubyClass cDigestBase = mDigest.fastGetClass("Base");
        RubyClass cDigest_MD5 = mDigest.defineClassUnder("MD5",cDigestBase,cDigestBase.getAllocator());
        cDigest_MD5.defineFastMethod("block_length", new Callback() {
            public Arity getArity() {
                return Arity.NO_ARGUMENTS;
            }
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                return RubyFixnum.newFixnum(recv.getRuntime(), 64);
            }
        });
        cDigest_MD5.setInternalModuleVariable("metadata",runtime.newString("MD5"));
    }

    public static void createDigestRMD160(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        if(provider == null) {
            throw runtime.newLoadError("RMD160 not supported without BouncyCastle");
        }

        RubyModule mDigest = runtime.fastGetModule("Digest");
        RubyClass cDigestBase = mDigest.fastGetClass("Base");
        RubyClass cDigest_RMD160 = mDigest.defineClassUnder("RMD160",cDigestBase,cDigestBase.getAllocator());
        cDigest_RMD160.setInternalModuleVariable("metadata",runtime.newString("RIPEMD160"));
    }

    public static void createDigestSHA1(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.fastGetModule("Digest");
        RubyClass cDigestBase = mDigest.fastGetClass("Base");
        RubyClass cDigest_SHA1 = mDigest.defineClassUnder("SHA1",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA1.setInternalModuleVariable("metadata",runtime.newString("SHA1"));
    }

    public static void createDigestSHA2(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        try {
            createMessageDigest(runtime, "SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw runtime.newLoadError("SHA2 not supported");
        }

        RubyModule mDigest = runtime.fastGetModule("Digest");
        RubyClass cDigestBase = mDigest.fastGetClass("Base");
        RubyClass cDigest_SHA2_256 = mDigest.defineClassUnder("SHA256",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_256.setInternalModuleVariable("metadata",runtime.newString("SHA-256"));
        cDigest_SHA2_256.defineFastMethod("block_length", new Callback() {
            public Arity getArity() {
                return Arity.NO_ARGUMENTS;
            }
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                return RubyFixnum.newFixnum(recv.getRuntime(), 64);
            }
        });
        RubyClass cDigest_SHA2_384 = mDigest.defineClassUnder("SHA384",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_384.setInternalModuleVariable("metadata",runtime.newString("SHA-384"));
        cDigest_SHA2_384.defineFastMethod("block_length", new Callback() {
            public Arity getArity() {
                return Arity.NO_ARGUMENTS;
            }
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                return RubyFixnum.newFixnum(recv.getRuntime(), 128);
            }
        });
        RubyClass cDigest_SHA2_512 = mDigest.defineClassUnder("SHA512",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_512.setInternalModuleVariable("metadata",runtime.newString("SHA-512"));
        cDigest_SHA2_512.defineFastMethod("block_length", new Callback() {
            public Arity getArity() {
                return Arity.NO_ARGUMENTS;
            }
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args, Block block) {
                return RubyFixnum.newFixnum(recv.getRuntime(), 128);
            }
        });
    }

    @JRubyClass(name="Digest::Base")
    public static class Base extends RubyObject {
        protected static final ObjectAllocator BASE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Base(runtime, klass);
            }
        };
        
        @JRubyMethod(name = "digest", required = 1, meta = true)
        public static IRubyObject s_digest(IRubyObject recv, IRubyObject str) {
            Ruby runtime = recv.getRuntime();
            String name = ((RubyClass)recv).searchInternalModuleVariable("metadata").toString();
            try {
                MessageDigest md = createMessageDigest(runtime, name);
                return RubyString.newStringShared(runtime, md.digest(str.convertToString().getBytes()));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }
        
        @JRubyMethod(name = "hexdigest", required = 1, meta = true)
        public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject str) {
            Ruby runtime = recv.getRuntime();
            String name = ((RubyClass)recv).searchInternalModuleVariable("metadata").toString();
            try {
                MessageDigest md = createMessageDigest(runtime, name);
                return RubyString.newStringNoCopy(runtime, ByteList.plain(toHex(md.digest(str.convertToString().getBytes()))));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }

        private MessageDigest algo;

        public Base(Ruby runtime, RubyClass type) {
            super(runtime,type);

            if(type == runtime.fastGetModule("Digest").fastGetClass("Base")) {
                throw runtime.newNotImplementedError("Digest::Base is an abstract class");
            }
            if(!type.hasInternalModuleVariable("metadata")) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }
            try {
                setAlgorithm(type.searchInternalModuleVariable("metadata"));
            } catch(NoSuchAlgorithmException e) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }

        }
        
        @JRubyMethod(name = "initialize", optional = 1, frame = true)
        public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
            if(args.length > 0 && !args[0].isNil()) {
                update(args[0]);
            }
            return this;
        }

        @JRubyMethod(name = "initialize_copy", required = 1)
        public IRubyObject initialize_copy(IRubyObject obj) {
            if(this == obj) {
                return this;
            }
            ((RubyObject)obj).checkFrozen();

            String name = ((Base)obj).algo.getAlgorithm();
            try {
                algo = (MessageDigest)((Base)obj).algo.clone();
            } catch(CloneNotSupportedException e) {
                throw getRuntime().newTypeError("Could not initialize copy of digest (" + name + ")");
            }
            return this;
        }

        @JRubyMethod(name = {"update", "<<"}, required = 1)
        public IRubyObject update(IRubyObject obj) {
            ByteList bytes = obj.convertToString().getByteList();
            algo.update(bytes.bytes, bytes.begin, bytes.realSize);
            return this;
        }

        @JRubyMethod(name = "digest", optional = 1)
        public IRubyObject digest(IRubyObject[] args) {
            if (args.length == 1) {
                algo.reset();
                update(args[0]);
            }

            IRubyObject digest = getDigestString();

            if (args.length == 1) {
                algo.reset();
            }
            return digest;
        }
        
        @JRubyMethod(name = "digest!")
        public IRubyObject digest_bang() {
            IRubyObject digest = getDigestStringNoClone();
            reset();
            return digest;
        }

        @JRubyMethod(name = {"hexdigest"}, optional = 1)
        public IRubyObject hexdigest(IRubyObject[] args) {
            if (args.length == 1) {
                algo.reset();
                update(args[0]);
            }

            IRubyObject digest = getDigestHexString();

            if (args.length == 1) {
                algo.reset();
            }
            
            return digest;
        }
        
        @JRubyMethod(name = {"to_s"})
        public IRubyObject to_s() {
            return getDigestHexString();
        }

        @JRubyMethod(name = {"hexdigest!"})
        public IRubyObject hexdigest_bang() {
            IRubyObject digest = getDigestHexStringNoClone();
            reset();
            return digest;
        }
        
        @JRubyMethod(name = "inspect")
        public IRubyObject inspect() {
            return RubyString.newStringNoCopy(getRuntime(), ByteList.plain("#<" + getMetaClass().getRealClass().getName() + ": " + getDigestHex() + ">"));
        }

        @JRubyMethod(name = "==", required = 1)
        public IRubyObject op_equal(IRubyObject oth) {
            boolean ret = this == oth;
            if(!ret) {
                if (oth instanceof Base) {
                    Base b = (Base)oth;
                    ret = this.algo.getAlgorithm().equals(b.algo.getAlgorithm()) &&
                            Arrays.equals(getDigest(), b.getDigest());
                } else {
                    IRubyObject str = oth.convertToString();
                    ret = this.to_s().equals(str);
                }
            }

            return ret ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        
        @JRubyMethod(name = {"length", "size", "digest_length"})
        public IRubyObject length() {
            return RubyFixnum.newFixnum(getRuntime(), algo.getDigestLength());
        }

        @JRubyMethod(name = {"block_length"})
        public IRubyObject block_length() {
            throw getRuntime().newRuntimeError(
                    this.getMetaClass() + " doesn't implement block_length()");
        }

        @JRubyMethod(name = {"reset"})
        public IRubyObject reset() {
            algo.reset();
            return getRuntime().getNil();
        }

       private void setAlgorithm(IRubyObject algo) throws NoSuchAlgorithmException {
           this.algo = createMessageDigest(getRuntime(), algo.toString());
        }

        final static byte[] digits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
        };

        private byte[] getDigest() {
            MessageDigest copy;
            try {
                copy = (MessageDigest)algo.clone();
            } catch (CloneNotSupportedException cnse) {
                copy = algo;
            }
            return copy.digest();
        }

        private byte[] getDigestNoClone() {
            return algo.digest();
        }

        private ByteList getDigestHex() {
            return toHex(getDigest());
        }

        private ByteList getDigestHexNoClone() {
            return toHex(getDigestNoClone());
        }

        private IRubyObject getDigestString() {
            return RubyString.newStringNoCopy(getRuntime(), getDigest());
        }

        private IRubyObject getDigestStringNoClone() {
            return RubyString.newStringNoCopy(getRuntime(), getDigestNoClone());
        }

        private IRubyObject getDigestHexString() {
            return RubyString.newStringNoCopy(getRuntime(), getDigestHex());
        }

        private IRubyObject getDigestHexStringNoClone() {
            return RubyString.newStringNoCopy(getRuntime(), getDigestHexNoClone());
        }

        private static ByteList toHex(byte[] val) {
            ByteList byteList = new ByteList(val.length * 2);
            for(int i=0,j=val.length;i<j;i++) {
                int b = val[i] & 0xFF;
                byteList.append(digits[b >> 4]);
                byteList.append(digits[b & 0xF]);
            }
            return byteList;
        }

    }
}// RubyDigest
