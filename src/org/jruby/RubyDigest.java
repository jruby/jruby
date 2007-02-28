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
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyDigest {
    public static void createDigest(Ruby runtime) {
        try {
            Class c = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            java.security.Security.insertProviderAt((java.security.Provider)c.newInstance(),2);
        } catch(Exception e) {
            runtime.getWarnings().warn("Couldn't find org.bouncycastle.jce.provider.BouncyCastleProvider. Digest will probably not be functional.");
        }

        RubyModule mDigest = runtime.defineModule("Digest");
        RubyClass cDigestBase = mDigest.defineClassUnder("Base",runtime.getObject(), Base.BASE_ALLOCATOR);

        CallbackFactory basecb = runtime.callbackFactory(Base.class);
        
        cDigestBase.getMetaClass().defineFastMethod("digest",basecb.getFastSingletonMethod("s_digest",RubyKernel.IRUBY_OBJECT));
        cDigestBase.getMetaClass().defineFastMethod("hexdigest",basecb.getFastSingletonMethod("s_hexdigest",RubyKernel.IRUBY_OBJECT));

        cDigestBase.defineMethod("initialize",basecb.getOptMethod("initialize"));
        cDigestBase.defineFastMethod("initialize_copy",basecb.getFastMethod("initialize_copy",RubyKernel.IRUBY_OBJECT));
        cDigestBase.defineFastMethod("update",basecb.getFastMethod("update",RubyKernel.IRUBY_OBJECT));
        cDigestBase.defineFastMethod("<<",basecb.getFastMethod("update",RubyKernel.IRUBY_OBJECT));
        cDigestBase.defineFastMethod("digest",basecb.getFastMethod("digest"));
        cDigestBase.defineFastMethod("hexdigest",basecb.getFastMethod("hexdigest"));
        cDigestBase.defineFastMethod("to_s",basecb.getFastMethod("hexdigest"));
        cDigestBase.defineFastMethod("==",basecb.getFastMethod("eq",RubyKernel.IRUBY_OBJECT));
    }

    public static void createDigestMD5(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_MD5 = mDigest.defineClassUnder("MD5",cDigestBase,cDigestBase.getAllocator());
        cDigest_MD5.setClassVar("metadata",runtime.newString("MD5"));
    }

    public static void createDigestRMD160(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_RMD160 = mDigest.defineClassUnder("RMD160",cDigestBase,cDigestBase.getAllocator());
        cDigest_RMD160.setClassVar("metadata",runtime.newString("RIPEMD160"));
    }

    public static void createDigestSHA1(Ruby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_SHA1 = mDigest.defineClassUnder("SHA1",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA1.setClassVar("metadata",runtime.newString("SHA1"));
    }

    public static void createDigestSHA2(Ruby runtime) {
        try {
            MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw runtime.newLoadError("SHA2 not supported");
        }
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_SHA2_256 = mDigest.defineClassUnder("SHA256",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_256.setClassVar("metadata",runtime.newString("SHA-256"));
        RubyClass cDigest_SHA2_384 = mDigest.defineClassUnder("SHA384",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_384.setClassVar("metadata",runtime.newString("SHA-384"));
        RubyClass cDigest_SHA2_512 = mDigest.defineClassUnder("SHA512",cDigestBase,cDigestBase.getAllocator());
        cDigest_SHA2_512.setClassVar("metadata",runtime.newString("SHA-512"));
    }

    public static class Base extends RubyObject {
        protected static ObjectAllocator BASE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Base(runtime, klass);
            }
        };
        public static IRubyObject s_digest(IRubyObject recv, IRubyObject str) {
            String name = ((RubyClass)recv).getClassVar("metadata").toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name,"BC");
                return RubyString.newString(recv.getRuntime(), md.digest(str.convertToString().getBytes()));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(NoSuchProviderException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }
        public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject str) {
            String name = ((RubyClass)recv).getClassVar("metadata").toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name,"BC");
                return RubyString.newString(recv.getRuntime(), ByteList.plain(toHex(md.digest(str.convertToString().getBytes()))));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(NoSuchProviderException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }

        private MessageDigest algo;
        private StringBuffer data;

        public Base(Ruby runtime, RubyClass type) {
            super(runtime,type);
            data = new StringBuffer();

            if(type == runtime.getModule("Digest").getClass("Base")) {
                throw runtime.newNotImplementedError("Digest::Base is an abstract class");
            }
            if(!type.isClassVarDefined("metadata")) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }
            try {
                setAlgorithm(type.getClassVar("metadata"));
            } catch(NoSuchAlgorithmException e) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            } catch(NoSuchProviderException e) {
                throw runtime.newNotImplementedError("the " + type + "() function is unimplemented on this machine");
            }

        }
        
        public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
            if(args.length > 0 && !args[0].isNil()) {
                update(args[0]);
            }
            return this;
        }

        public IRubyObject initialize_copy(IRubyObject obj) {
            if(this == obj) {
                return this;
            }
            ((RubyObject)obj).checkFrozen();

            data = new StringBuffer(((Base)obj).data.toString());
            String name = ((Base)obj).algo.getAlgorithm();
            try {
                algo = MessageDigest.getInstance(name,"BC");
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(NoSuchProviderException e) {
                throw getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
             return this;
        }

        public IRubyObject update(IRubyObject obj) {
            data.append(obj);
            return this;
        }

        public IRubyObject digest() {
            algo.reset();
            return RubyString.newString(getRuntime(), algo.digest(ByteList.plain(data)));
        }

        public IRubyObject hexdigest() {
            algo.reset();
            return RubyString.newString(getRuntime(), ByteList.plain(toHex(algo.digest(ByteList.plain(data)))));
        }

        public IRubyObject eq(IRubyObject oth) {
            boolean ret = this == oth;
            if(!ret && oth instanceof Base) {
                Base b = (Base)oth;
                ret = this.algo.getAlgorithm().equals(b.algo.getAlgorithm()) &&
                    this.digest().equals(b.digest());
            }

            return ret ? getRuntime().getTrue() : getRuntime().getFalse();
        }

       private void setAlgorithm(IRubyObject algo) throws NoSuchAlgorithmException, NoSuchProviderException {
            this.algo = MessageDigest.getInstance(algo.toString(),"BC");
        }

        private static String toHex(byte[] val) {
            StringBuffer out = new StringBuffer();
            for(int i=0,j=val.length;i<j;i++) {
                String ve = Integer.toString((((int)((char)val[i])) & 0xFF),16);
                if(ve.length() == 1) {
                    ve = "0" + ve;
                }
                out.append(ve);
            }
            return out.toString();
        }
    }
}// RubyDigest
